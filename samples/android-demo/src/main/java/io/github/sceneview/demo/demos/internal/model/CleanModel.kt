package io.github.sceneview.demo.demos.internal.model

import io.github.sceneview.demo.demos.internal.RerunMesh
import io.github.sceneview.demo.demos.internal.RerunMeshSimplifier
import kotlin.math.floor

/** What the finished model's build did and how long each stage took, for the log, tests and corpus. */
internal data class CleanModelStats(
    val rawTriangles: Int,
    /** The triangles cleaning took: [rawTriangles], or [CleanModel.CLEAN_INPUT_TRIANGLES] for a bigger surface. */
    val cleanInputTriangles: Int,
    val geometry: CleanGeometryStats,
    /** The budget the simplification aimed at; it may stop under it (flat walls) or over it (colour edges). */
    val simplifyTarget: Int,
    val simplifiedTriangles: Int,
    val finalTriangles: Int,
    val finalVertices: Int,
    val texture: PhotoTextureStats?,
    /** The textured share of a texture dropped for covering too little of the model, `null` if none was. */
    val droppedTextureShare: Float?,
    val atlasWidth: Int,
    val atlasHeight: Int,
    val glbBytes: Int,
    val prepareMs: Long,
    val cleanMs: Long,
    val simplifyMs: Long,
    val textureMs: Long,
    val writeMs: Long,
) {
    val totalMs: Long get() = prepareMs + cleanMs + simplifyMs + textureMs + writeMs
}

internal class CleanModelResult(val glb: ByteArray, val model: ModelMesh, val stats: CleanModelStats)

/**
 * The finished model of a scan, from the marching cubes' raw surface: brought within what
 * cleaning takes ([CLEAN_INPUT_TRIANGLES]), cleaned ([CleanGeometry]), simplified to a phone's
 * budget, textured from the key photos when the recording has them and they cover the model
 * ([PhotoTexturing]), its normals split at creases ([ModelAssembly]), and written as a GLB
 * ([ModelGlb]). Blocking CPU work, for a worker thread; [progress] (0–1) may throw to stop it.
 */
internal object CleanModel {
    fun build(
        raw: RerunMesh,
        photos: ModelPhotos?,
        encoder: AtlasEncoder?,
        targetTriangles: Int = RerunMeshSimplifier.DEFAULT_TARGET_TRIANGLES,
        progress: (Float) -> Unit = {},
    ): CleanModelResult {
        require(raw.triangleCount > 0) { "a model needs triangles" }
        var mark = System.nanoTime()
        fun lap(): Long = ((System.nanoTime() - mark) / NANOS_PER_MS).also { mark = System.nanoTime() }
        val input = prepare(raw) { progress(it * PREPARE_SHARE) }
        val prepareMs = lap()
        val (clean, geometry) = CleanGeometry.clean(input) { progress(PREPARE_SHARE + it * CLEAN_SHARE) }
        val cleanMs = lap()
        val textured = photos != null && photos.photos.isNotEmpty() && encoder != null
        val simplifyTarget = if (textured) minOf(targetTriangles, TEXTURED_TARGET_TRIANGLES) else targetTriangles
        val shape = simplify(clean, textured, targetTriangles) { progress(BEFORE_SIMPLIFY + it * SIMPLIFY_SHARE) }
        var simplifyMs = lap()
        val tried = if (textured) {
            PhotoTexturing.texture(shape, photos!!) { progress(BEFORE_TEXTURE + it * TEXTURE_SHARE) }
        } else {
            null
        }
        var textureMs = lap()
        // A shape simplified for a texture keeps only coarse colours: worth it only if the photos
        // cover most of it. Photos that failed to decode, wrong poses, a corner of the room seen —
        // the colours are simplified again, edges kept, as without photos.
        val covered = tried != null && tried.stats.texturedShare >= MIN_TEXTURED_SHARE
        val texture = tried.takeIf { covered }
        val simplified = if (textured && !covered) {
            simplify(clean, textured = false, targetTriangles) { progress(BEFORE_TEXTURE + it * TEXTURE_SHARE) }
                .also { simplifyMs += lap() }
        } else {
            shape
        }
        val model = ModelAssembly.assemble(simplified, texture)
        textureMs += lap()
        val glb = ModelGlb.write(model, encoder)
        val writeMs = lap()
        progress(1f)
        return CleanModelResult(
            glb,
            model,
            CleanModelStats(
                rawTriangles = raw.triangleCount,
                cleanInputTriangles = input.triangleCount,
                geometry = geometry,
                simplifyTarget = if (covered) simplifyTarget else targetTriangles,
                simplifiedTriangles = simplified.triangleCount,
                finalTriangles = model.triangleCount,
                finalVertices = model.vertexCount,
                texture = texture?.stats,
                droppedTextureShare = tried?.stats?.texturedShare?.takeIf { !covered },
                atlasWidth = model.texture?.width ?: 0,
                atlasHeight = model.texture?.height ?: 0,
                glbBytes = glb.size,
                prepareMs = prepareMs,
                cleanMs = cleanMs,
                simplifyMs = simplifyMs,
                textureMs = textureMs,
                writeMs = writeMs,
            ),
        )
    }

    /**
     * [raw] as cleaning takes it. A surface over [CLEAN_INPUT_TRIANGLES] — a long scan of a big
     * room, up to the extraction's cap — is first simplified on its shape alone down to it (the
     * flat runs go first; no colour edge holds a collapse back, so the budget is reached), its
     * colours then averaged back from the raw vertices around each one. Cleaning a million
     * triangles would take a phone's whole heap; the simplifier's workspace
     * ([RerunMeshSimplifier.workspaceBytes]) is the smaller peak.
     */
    private fun prepare(raw: RerunMesh, progress: (Float) -> Unit): WorkMesh {
        if (raw.triangleCount <= CLEAN_INPUT_TRIANGLES) return CleanGeometry.of(raw)
        val grey = IntArray(raw.vertexCount) { FLAT_COLOR }
        val out = RerunMeshSimplifier.simplify(
            RerunMesh(raw.positions, raw.normals, grey, raw.indices),
            CLEAN_INPUT_TRIANGLES,
            progress = progress,
        )
        val colors = averageColors(raw.positions, raw.colors, out.positions, PREPARE_RADIUS_M)
        return WorkMesh(out.positions, colors, out.indices)
    }

    /**
     * [clean] within [target] triangles. A model the photos will texture is simplified on its shape
     * alone — the texture carries the colour, so a plain wall comes down to a few large faces —
     * as far as the shape allows ([MAX_TEXTURED_ERROR_M]) and no edge longer than
     * [MAX_TEXTURED_EDGE_M], so each face still fits in one photo; its vertices then take the
     * mean colour of the cleaned vertices around them, for the faces no photo covers.
     * Without photos, colour edges are kept, as the vertex colours are all there is.
     */
    private fun simplify(clean: WorkMesh, textured: Boolean, target: Int, progress: (Float) -> Unit): WorkMesh {
        val normals = vertexNormals(clean.positions, clean.indices)
        if (!textured) {
            val out = RerunMeshSimplifier.simplify(RerunMesh(clean.positions, normals, clean.colors, clean.indices), target, progress = progress)
            return WorkMesh(out.positions, out.colors, out.indices)
        }
        val grey = IntArray(clean.vertexCount) { FLAT_COLOR }
        val out = RerunMeshSimplifier.simplify(
            RerunMesh(clean.positions, normals, grey, clean.indices),
            minOf(target, TEXTURED_TARGET_TRIANGLES),
            maxEdgeM = MAX_TEXTURED_EDGE_M,
            maxErrorM = MAX_TEXTURED_ERROR_M,
            progress = progress,
        )
        return WorkMesh(out.positions, averageColors(clean.positions, clean.colors, out.positions, CELL_M), out.indices)
    }

    /**
     * For each of [positions], the mean colour of the [sourcePositions] within [radius] of it —
     * the nearest one's where none is that close, within a cell or two — from a hashed grid of
     * [CELL_M] cells. [radius] is at most [CELL_M].
     */
    private fun averageColors(sourcePositions: FloatArray, sourceColors: IntArray, positions: FloatArray, radius: Float): IntArray {
        val cells = HashMap<Long, IntList>()
        val p = sourcePositions
        for (v in 0 until sourceColors.size) {
            cells.getOrPut(cell(p[v * 3], p[v * 3 + 1], p[v * 3 + 2])) { IntList(4) }.add(v)
        }
        val radiusSq = radius * radius
        return IntArray(positions.size / 3) { i ->
            val x = positions[i * 3]
            val y = positions[i * 3 + 1]
            val z = positions[i * 3 + 2]
            val cx = floor(x / CELL_M).toLong()
            val cy = floor(y / CELL_M).toLong()
            val cz = floor(z / CELL_M).toLong()
            var nearest = -1
            var nearestD = Float.MAX_VALUE
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            for (dz in -1..1) for (dy in -1..1) for (dx in -1..1) {
                val list = cells[key(cx + dx, cy + dy, cz + dz)] ?: continue
                for (j in 0 until list.size) {
                    val v = list[j]
                    val ex = p[v * 3] - x
                    val ey = p[v * 3 + 1] - y
                    val ez = p[v * 3 + 2] - z
                    val d = ex * ex + ey * ey + ez * ez
                    if (d < nearestD) {
                        nearestD = d
                        nearest = v
                    }
                    if (d <= radiusSq) {
                        val c = sourceColors[v]
                        r += c shr 16 and 0xFF
                        g += c shr 8 and 0xFF
                        b += c and 0xFF
                        count++
                    }
                }
            }
            when {
                count > 0 -> PhotoTexturing.rgb(r / count, g / count, b / count)
                nearest >= 0 -> sourceColors[nearest]
                else -> FLAT_COLOR
            }
        }
    }

    private fun cell(x: Float, y: Float, z: Float): Long =
        key(floor(x / CELL_M).toLong(), floor(y / CELL_M).toLong(), floor(z / CELL_M).toLong())

    private fun key(x: Long, y: Long, z: Long): Long =
        ((x and KEY_MASK) shl (2 * KEY_BITS)) or ((y and KEY_MASK) shl KEY_BITS) or (z and KEY_MASK)

    /** A face this long still fits in a photo taken a metre and a half away. */
    const val MAX_TEXTURED_EDGE_M = 0.45f

    /** A few millimetres: under the depth's own noise, so no shape a scan really saw is lost. */
    const val MAX_TEXTURED_ERROR_M = 0.004f

    /**
     * A textured room needs fewer faces than a vertex-coloured one: the photos carry the detail,
     * and [MAX_TEXTURED_ERROR_M] stops the simplification before a curve flattens.
     */
    const val TEXTURED_TARGET_TRIANGLES = 50_000

    /**
     * The most triangles cleaning is given, under [CleanGeometry.MAX_TRIANGLES]: a synthetic
     * room's 394k and the recorded corpus's scans pass as they are; a fifteen-minute scan near the
     * extraction's million is simplified down to it first.
     */
    const val CLEAN_INPUT_TRIANGLES = 400_000

    /** Under this share textured, a texture is dropped for the vertex-coloured model. */
    const val MIN_TEXTURED_SHARE = 0.5f
    private const val FLAT_COLOR = 0xFFB4AFA5.toInt()
    private const val CELL_M = 0.04f
    private const val KEY_BITS = 21
    private const val KEY_MASK = (1L shl KEY_BITS) - 1
    private const val PREPARE_RADIUS_M = 0.02f
    private const val PREPARE_SHARE = 0.1f
    private const val CLEAN_SHARE = 0.3f
    private const val SIMPLIFY_SHARE = 0.15f
    private const val TEXTURE_SHARE = 0.4f
    private const val BEFORE_SIMPLIFY = PREPARE_SHARE + CLEAN_SHARE
    private const val BEFORE_TEXTURE = BEFORE_SIMPLIFY + SIMPLIFY_SHARE
    private const val NANOS_PER_MS = 1_000_000L
}

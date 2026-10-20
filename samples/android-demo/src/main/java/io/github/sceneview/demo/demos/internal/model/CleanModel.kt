package io.github.sceneview.demo.demos.internal.model

import io.github.sceneview.demo.demos.internal.RerunMesh
import io.github.sceneview.demo.demos.internal.RerunMeshGlb
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
    /** The raw surface's own `.glb` binary: the finished model's [glbBytes] never pass it. */
    val glbLimitBytes: Int,
    /** How many times the atlas was halved to keep the `.glb` within [glbLimitBytes]. */
    val atlasHalvings: Int,
    /** Whether the texture was dropped, then the budget lowered, to keep within [glbLimitBytes]. */
    val glbGuarded: Boolean,
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
 *
 * The finished `.glb` never weighs more than the raw surface's own: over it, the atlas is halved,
 * then the texture dropped, then the budget halved. With `consume`, the raw surface's arrays are
 * worked in instead of copied — for a caller that already wrote its `.glb` and is done with it.
 */
internal object CleanModel {
    @Suppress("LongMethod", "LongParameterList")
    fun build(
        raw: RerunMesh,
        photos: ModelPhotos?,
        encoder: AtlasEncoder?,
        targetTriangles: Int = RerunMeshSimplifier.DEFAULT_TARGET_TRIANGLES,
        consume: Boolean = false,
        progress: (Float) -> Unit = {},
    ): CleanModelResult {
        require(raw.triangleCount > 0) { "a model needs triangles" }
        var mark = System.nanoTime()
        fun lap(): Long = ((System.nanoTime() - mark) / NANOS_PER_MS).also { mark = System.nanoTime() }
        val rawTriangles = raw.triangleCount
        val glbLimit = RerunMeshGlb.binaryBytes(raw)
        val input = prepare(raw, consume) { progress(it * PREPARE_SHARE) }
        val cleanInputTriangles = input.triangleCount
        val prepareMs = lap()
        val (clean, geometry) = CleanGeometry.clean(input) { progress(PREPARE_SHARE + it * CLEAN_SHARE) }
        val cleanMs = lap()
        val textured = photos != null && photos.photos.isNotEmpty() && encoder != null
        val simplifyTarget = if (textured) minOf(targetTriangles, TEXTURED_TARGET_TRIANGLES) else targetTriangles
        val ceiling = errorCeiling(geometry)
        val shape = simplify(clean, textured, targetTriangles, ceiling) { progress(BEFORE_SIMPLIFY + it * SIMPLIFY_SHARE) }
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
        var texture = tried.takeIf { covered }
        var simplified = if (textured && !covered) {
            simplify(clean, textured = false, targetTriangles, ceiling) { progress(BEFORE_TEXTURE + it * TEXTURE_SHARE) }
                .also { simplifyMs += lap() }
        } else {
            shape
        }
        var model = ModelAssembly.assemble(simplified, texture)
        textureMs += lap()
        var glb = ModelGlb.write(model, encoder)
        // Never heavier than the raw surface: the seams' duplicated vertices and the atlas are
        // paid for by a coarser atlas first, then by the vertex colours alone, then by fewer faces.
        var halvings = 0
        while (glb.size > glbLimit && texture != null && halvings < MAX_ATLAS_HALVINGS) {
            texture = texture.halved()
            model = model.withTexture(texture)
            glb = ModelGlb.write(model, encoder)
            halvings++
        }
        var guardTarget = minOf(targetTriangles, simplified.triangleCount)
        val guarded = glb.size > glbLimit
        while (glb.size > glbLimit && guardTarget >= MIN_GUARD_TRIANGLES) {
            if (texture == null) guardTarget /= 2
            texture = null
            simplified = simplify(clean, textured = false, guardTarget, ceiling) {}
            model = ModelAssembly.assemble(simplified, null)
            glb = ModelGlb.write(model, encoder)
        }
        val writeMs = lap()
        progress(1f)
        return CleanModelResult(
            glb,
            model,
            CleanModelStats(
                rawTriangles = rawTriangles,
                cleanInputTriangles = cleanInputTriangles,
                geometry = geometry,
                simplifyTarget = if (texture != null) simplifyTarget else targetTriangles,
                simplifiedTriangles = simplified.triangleCount,
                finalTriangles = model.triangleCount,
                finalVertices = model.vertexCount,
                texture = texture?.stats,
                droppedTextureShare = tried?.stats?.texturedShare?.takeIf { texture == null },
                atlasWidth = model.texture?.width ?: 0,
                atlasHeight = model.texture?.height ?: 0,
                glbBytes = glb.size,
                glbLimitBytes = glbLimit,
                atlasHalvings = halvings,
                glbGuarded = guarded,
                prepareMs = prepareMs,
                cleanMs = cleanMs,
                simplifyMs = simplifyMs,
                textureMs = textureMs,
                writeMs = writeMs,
            ),
        )
    }

    /**
     * How far the textured simplification may go past [MAX_TEXTURED_ERROR_M] when that cap holds
     * it over budget: twice the depth noise the flat faces measured ([CleanGeometryStats.noiseM]),
     * within [MAX_TEXTURED_ERROR_CEILING_M]. A scan whose walls wander by a centimetre loses
     * nothing real to a collapse of two; a clean synthetic one stays at the cap.
     */
    private fun errorCeiling(geometry: CleanGeometryStats): Float =
        if (geometry.planes == 0) {
            MAX_TEXTURED_ERROR_CEILING_M
        } else {
            (NOISE_CEILING_FACTOR * geometry.noiseM).coerceIn(MAX_TEXTURED_ERROR_M, MAX_TEXTURED_ERROR_CEILING_M)
        }

    /**
     * [raw] as cleaning takes it. A surface over [CLEAN_INPUT_TRIANGLES] — a long scan of a big
     * room, up to the extraction's cap — is first simplified on its shape alone down to it (the
     * flat runs go first; no colour edge holds a collapse back, so the budget is reached), its
     * colours then averaged back from the raw vertices around each one. Cleaning a million
     * triangles would take a phone's whole heap; the simplifier's workspace
     * ([RerunMeshSimplifier.workspaceBytes]) is the smaller peak.
     */
    private fun prepare(raw: RerunMesh, consume: Boolean, progress: (Float) -> Unit): WorkMesh {
        if (raw.triangleCount <= CLEAN_INPUT_TRIANGLES) return if (consume) CleanGeometry.consuming(raw) else CleanGeometry.of(raw)
        val grey = IntArray(raw.vertexCount) { FLAT_COLOR }
        val out = RerunMeshSimplifier.simplify(
            RerunMesh(raw.positions, raw.normals, grey, raw.indices),
            CLEAN_INPUT_TRIANGLES,
            consume = consume,
            progress = progress,
        )
        val colors = averageColors(raw.positions, raw.colors, out.positions, PREPARE_RADIUS_M)
        return WorkMesh(out.positions, colors, out.indices)
    }

    /**
     * [clean] within [target] triangles. A model the photos will texture is simplified on its shape
     * alone — the texture carries the colour, so a plain wall comes down to a few large faces —
     * every collapse within [MAX_TEXTURED_ERROR_M] — past the budget too when it starts under
     * it, so a small scan still sheds its flat runs — then on to the budget
     * within [ceiling] when the noise held it back, and no edge longer than
     * [MAX_TEXTURED_EDGE_M], so each face still fits in one photo; its vertices then take the
     * mean colour of the cleaned vertices around them, for the faces no photo covers.
     * Without photos, colour edges are kept, as the vertex colours are all there is.
     */
    private fun simplify(clean: WorkMesh, textured: Boolean, target: Int, ceiling: Float, progress: (Float) -> Unit): WorkMesh {
        val normals = vertexNormals(clean.positions, clean.indices)
        if (!textured) {
            val out = RerunMeshSimplifier.simplify(
                RerunMesh(clean.positions, normals, clean.colors, clean.indices),
                target,
                collapseBorders = true,
                progress = progress,
            )
            return WorkMesh(out.positions, out.colors, out.indices)
        }
        val grey = IntArray(clean.vertexCount) { FLAT_COLOR }
        val out = RerunMeshSimplifier.simplify(
            RerunMesh(clean.positions, normals, grey, clean.indices),
            minOf(target, TEXTURED_TARGET_TRIANGLES),
            maxEdgeM = MAX_TEXTURED_EDGE_M,
            maxErrorM = MAX_TEXTURED_ERROR_M,
            maxErrorCeilingM = ceiling,
            belowTarget = true,
            collapseBorders = true,
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

    /** The furthest a scan's noise lets [MAX_TEXTURED_ERROR_M] grow: still under a skirting board. */
    const val MAX_TEXTURED_ERROR_CEILING_M = 0.02f
    private const val NOISE_CEILING_FACTOR = 2f

    /** An atlas halved twice is a quarter as fine: past that, the vertex colours are the lighter model. */
    private const val MAX_ATLAS_HALVINGS = 2

    /** The fewest triangles the size guard brings a model down to. */
    private const val MIN_GUARD_TRIANGLES = 2_000

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

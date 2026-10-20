package io.github.sceneview.demo.demos.internal.model

import io.github.sceneview.demo.demos.internal.RerunMesh
import io.github.sceneview.demo.demos.internal.RerunMeshSimplifier
import kotlin.math.floor

/** What the finished model's build did and how long each stage took, for the log, tests and corpus. */
internal data class CleanModelStats(
    val rawTriangles: Int,
    val geometry: CleanGeometryStats,
    val simplifiedTriangles: Int,
    val finalTriangles: Int,
    val finalVertices: Int,
    val texture: PhotoTextureStats?,
    val atlasWidth: Int,
    val atlasHeight: Int,
    val glbBytes: Int,
    val cleanMs: Long,
    val simplifyMs: Long,
    val textureMs: Long,
    val writeMs: Long,
) {
    val totalMs: Long get() = cleanMs + simplifyMs + textureMs + writeMs
}

internal class CleanModelResult(val glb: ByteArray, val model: ModelMesh, val stats: CleanModelStats)

/**
 * The finished model of a scan, from the marching cubes' raw surface: cleaned ([CleanGeometry]),
 * simplified to a phone's budget, textured from the key photos when the recording has them
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
        val (clean, geometry) = CleanGeometry.clean(CleanGeometry.of(raw)) { progress(it * CLEAN_SHARE) }
        val cleanMs = lap()
        val textured = photos != null && photos.photos.isNotEmpty() && encoder != null
        val simplified = simplify(clean, textured, targetTriangles) { progress(CLEAN_SHARE + it * SIMPLIFY_SHARE) }
        val simplifyMs = lap()
        val texture = if (textured) {
            PhotoTexturing.texture(simplified, photos!!) {
                progress(CLEAN_SHARE + SIMPLIFY_SHARE + it * TEXTURE_SHARE)
            }
        } else {
            null
        }
        val model = ModelAssembly.assemble(simplified, texture)
        val textureMs = lap()
        val glb = ModelGlb.write(model, encoder)
        val writeMs = lap()
        progress(1f)
        return CleanModelResult(
            glb,
            model,
            CleanModelStats(
                rawTriangles = raw.triangleCount,
                geometry = geometry,
                simplifiedTriangles = simplified.triangleCount,
                finalTriangles = model.triangleCount,
                finalVertices = model.vertexCount,
                texture = texture?.stats,
                atlasWidth = model.texture?.width ?: 0,
                atlasHeight = model.texture?.height ?: 0,
                glbBytes = glb.size,
                cleanMs = cleanMs,
                simplifyMs = simplifyMs,
                textureMs = textureMs,
                writeMs = writeMs,
            ),
        )
    }

    /**
     * [clean] within [target] triangles. A model the photos will texture is simplified on its shape
     * alone — the texture carries the colour, so a plain wall comes down to a few large faces —
     * as far as the shape allows ([MAX_TEXTURED_ERROR_M]) and no edge longer than
     * [MAX_TEXTURED_EDGE_M], so each face still fits in one photo; its
     * vertices then take the colour of the nearest cleaned vertex, for the faces no photo covers.
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
        return WorkMesh(out.positions, nearestColors(clean, out.positions), out.indices)
    }

    /** For each of [positions], the colour of [clean]'s nearest vertex (a hashed grid, a few centimetres a cell). */
    private fun nearestColors(clean: WorkMesh, positions: FloatArray): IntArray {
        val cells = HashMap<Long, IntList>()
        val p = clean.positions
        for (v in 0 until clean.vertexCount) {
            cells.getOrPut(cell(p[v * 3], p[v * 3 + 1], p[v * 3 + 2])) { IntList(4) }.add(v)
        }
        return IntArray(positions.size / 3) { i ->
            val x = positions[i * 3]
            val y = positions[i * 3 + 1]
            val z = positions[i * 3 + 2]
            val cx = floor(x / CELL_M).toLong()
            val cy = floor(y / CELL_M).toLong()
            val cz = floor(z / CELL_M).toLong()
            var best = -1
            var bestD = Float.MAX_VALUE
            for (dz in -1..1) for (dy in -1..1) for (dx in -1..1) {
                val list = cells[key(cx + dx, cy + dy, cz + dz)] ?: continue
                for (j in 0 until list.size) {
                    val v = list[j]
                    val ex = p[v * 3] - x
                    val ey = p[v * 3 + 1] - y
                    val ez = p[v * 3 + 2] - z
                    val d = ex * ex + ey * ey + ez * ez
                    if (d < bestD) {
                        bestD = d
                        best = v
                    }
                }
            }
            if (best >= 0) clean.colors[best] else FLAT_COLOR
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
    private const val FLAT_COLOR = 0xFFB4AFA5.toInt()
    private const val CELL_M = 0.04f
    private const val KEY_BITS = 21
    private const val KEY_MASK = (1L shl KEY_BITS) - 1
    private const val CLEAN_SHARE = 0.35f
    private const val SIMPLIFY_SHARE = 0.2f
    private const val TEXTURE_SHARE = 0.4f
    private const val NANOS_PER_MS = 1_000_000L
}

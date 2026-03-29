package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.rendering.Vertex

/**
 * Generates a terrain mesh from a 2D heightmap function.
 *
 * Creates a grid in the XZ plane where the Y value of each vertex is determined
 * by the heightmap function. Normals are computed from the height gradient.
 *
 * @param width Width of the terrain in world units (along X). Default 10.
 * @param depth Depth of the terrain in world units (along Z). Default 10.
 * @param center Offset applied to every vertex position.
 * @param segmentsX Number of subdivisions along X. Default 64.
 * @param segmentsZ Number of subdivisions along Z. Default 64.
 * @param heightFunction Function that returns the height (Y value) for a given (x, z) position.
 *                        Default returns 0 (flat plane).
 */
fun generateHeightmap(
    width: Float = 10f,
    depth: Float = 10f,
    center: Float3 = Float3(0f),
    segmentsX: Int = 64,
    segmentsZ: Int = 64,
    heightFunction: (Float, Float) -> Float = { _, _ -> 0f }
): GeometryData {
    require(segmentsX >= 1) { "segmentsX must be >= 1" }
    require(segmentsZ >= 1) { "segmentsZ must be >= 1" }

    val halfWidth = width / 2f
    val halfDepth = depth / 2f
    val dx = width / segmentsX
    val dz = depth / segmentsZ

    // First pass: compute heights for normal computation
    val heights = Array(segmentsX + 1) { x ->
        FloatArray(segmentsZ + 1) { z ->
            val worldX = -halfWidth + x * dx
            val worldZ = -halfDepth + z * dz
            heightFunction(worldX, worldZ)
        }
    }

    val vertices = buildList {
        for (x in 0..segmentsX) {
            for (z in 0..segmentsZ) {
                val worldX = -halfWidth + x * dx
                val worldZ = -halfDepth + z * dz
                val y = heights[x][z]

                // Compute normal from height gradient using central differences
                val hL = if (x > 0) heights[x - 1][z] else y
                val hR = if (x < segmentsX) heights[x + 1][z] else y
                val hD = if (z > 0) heights[x][z - 1] else y
                val hU = if (z < segmentsZ) heights[x][z + 1] else y

                val normal = normalize(Float3(
                    (hL - hR) / (2f * dx),
                    1f,
                    (hD - hU) / (2f * dz)
                ))

                val position = Float3(worldX, y, worldZ) + center
                val uv = Float2(
                    x.toFloat() / segmentsX,
                    z.toFloat() / segmentsZ
                )

                add(Vertex(position = position, normal = normal, uvCoordinate = uv))
            }
        }
    }

    val indices = buildList {
        val cols = segmentsZ + 1
        for (x in 0 until segmentsX) {
            for (z in 0 until segmentsZ) {
                val a = x * cols + z
                val b = a + cols
                val c = a + 1
                val d = b + 1

                add(a); add(b); add(d)
                add(a); add(d); add(c)
            }
        }
    }

    return GeometryData(vertices, indices)
}

/**
 * Generates a heightmap terrain using Perlin noise.
 *
 * Convenience function that combines [generateHeightmap] with [io.github.sceneview.math.Noise].
 *
 * @param width Width of the terrain. Default 10.
 * @param depth Depth of the terrain. Default 10.
 * @param center Offset applied to every vertex position.
 * @param segmentsX Grid resolution along X. Default 64.
 * @param segmentsZ Grid resolution along Z. Default 64.
 * @param heightScale Maximum height variation. Default 2.
 * @param noiseScale Scale of the noise sampling (higher = more detail). Default 0.3.
 * @param octaves Number of fBm octaves. Default 4.
 */
fun generatePerlinTerrain(
    width: Float = 10f,
    depth: Float = 10f,
    center: Float3 = Float3(0f),
    segmentsX: Int = 64,
    segmentsZ: Int = 64,
    heightScale: Float = 2f,
    noiseScale: Float = 0.3f,
    octaves: Int = 4
): GeometryData = generateHeightmap(
    width = width,
    depth = depth,
    center = center,
    segmentsX = segmentsX,
    segmentsZ = segmentsZ,
    heightFunction = { x, z ->
        io.github.sceneview.math.Noise.fbm2D(
            x * noiseScale, z * noiseScale,
            octaves = octaves
        ) * heightScale
    }
)

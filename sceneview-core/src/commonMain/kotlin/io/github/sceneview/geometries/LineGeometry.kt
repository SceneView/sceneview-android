package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.rendering.Vertex

/**
 * Generates a single line segment between two 3D points.
 *
 * Produces two vertices and two indices (a single LINES primitive pair).
 * No normals or UVs are generated — line primitives typically don't need them.
 *
 * @param start The starting point of the line segment.
 * @param end The ending point of the line segment.
 */
fun generateLine(
    start: Float3 = Float3(0f),
    end: Float3 = Float3(x = 1f)
): GeometryData {
    val vertices = listOf(
        Vertex(position = start),
        Vertex(position = end)
    )
    val indices = listOf(0, 1)
    return GeometryData(vertices, indices)
}

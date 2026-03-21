package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.rendering.Vertex

/**
 * Generates a quad (two-triangle plane) as a list of vertices and triangle indices.
 *
 * The plane lies in the XZ plane by default, facing up along Y.
 *
 * @param size Width (X) and depth (Z) of the plane.
 * @param center Offset applied to every vertex position.
 * @param normal Surface normal for all four vertices.
 */
fun generatePlane(
    size: Float2 = Float2(1f),
    center: Float3 = Float3(0f),
    normal: Float3 = Float3(y = 1f)
): GeometryData {
    val extents = Float3(size.x, size.y, 0f) / 2f

    val vertices = listOf(
        Vertex(
            position = center + Float3(x = -extents.x, y = -extents.y, z = extents.z),
            normal = normal,
            uvCoordinate = Float2(0f, 0f)
        ),
        Vertex(
            position = center + Float3(x = -extents.x, y = extents.y, z = -extents.z),
            normal = normal,
            uvCoordinate = Float2(0f, 1f)
        ),
        Vertex(
            position = center + Float3(x = extents.x, y = extents.y, z = -extents.z),
            normal = normal,
            uvCoordinate = Float2(1f, 1f)
        ),
        Vertex(
            position = center + Float3(x = extents.x, y = -extents.y, z = extents.z),
            normal = normal,
            uvCoordinate = Float2(1f, 0f)
        )
    )

    val indices = listOf(
        3, 1, 0, // First triangle
        3, 2, 1  // Second triangle
    )

    return GeometryData(vertices, indices)
}

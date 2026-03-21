package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.rendering.Vertex

/**
 * Generates a unit cube (or sized box) as a list of vertices and triangle indices.
 *
 * Each face has 4 unique vertices (with per-face normals), giving 24 vertices total.
 * Indices form 12 triangles (2 per face), giving 36 indices.
 *
 * @param size Full extents along each axis. Default is 1x1x1.
 * @param center Offset applied to every vertex position.
 */
fun generateCube(
    size: Float3 = Float3(1f),
    center: Float3 = Float3(0f)
): GeometryData {
    val extents = size * 0.5f

    val p0 = center + Float3(-extents.x, -extents.y, extents.z)
    val p1 = center + Float3(extents.x, -extents.y, extents.z)
    val p2 = center + Float3(extents.x, -extents.y, -extents.z)
    val p3 = center + Float3(-extents.x, -extents.y, -extents.z)
    val p4 = center + Float3(-extents.x, extents.y, extents.z)
    val p5 = center + Float3(extents.x, extents.y, extents.z)
    val p6 = center + Float3(extents.x, extents.y, -extents.z)
    val p7 = center + Float3(-extents.x, extents.y, -extents.z)

    val up = Float3(y = 1f)
    val down = Float3(y = -1f)
    val front = Float3(z = -1f)
    val back = Float3(z = 1f)
    val left = Float3(x = -1f)
    val right = Float3(x = 1f)

    val uv00 = Float2(0f, 0f)
    val uv10 = Float2(1f, 0f)
    val uv01 = Float2(0f, 1f)
    val uv11 = Float2(1f, 1f)

    val vertices = listOf(
        // Bottom
        Vertex(position = p0, normal = down, uvCoordinate = uv01),
        Vertex(position = p1, normal = down, uvCoordinate = uv11),
        Vertex(position = p2, normal = down, uvCoordinate = uv10),
        Vertex(position = p3, normal = down, uvCoordinate = uv00),
        // Left
        Vertex(position = p7, normal = left, uvCoordinate = uv01),
        Vertex(position = p4, normal = left, uvCoordinate = uv11),
        Vertex(position = p0, normal = left, uvCoordinate = uv10),
        Vertex(position = p3, normal = left, uvCoordinate = uv00),
        // Back
        Vertex(position = p4, normal = back, uvCoordinate = uv01),
        Vertex(position = p5, normal = back, uvCoordinate = uv11),
        Vertex(position = p1, normal = back, uvCoordinate = uv10),
        Vertex(position = p0, normal = back, uvCoordinate = uv00),
        // Front
        Vertex(position = p6, normal = front, uvCoordinate = uv01),
        Vertex(position = p7, normal = front, uvCoordinate = uv11),
        Vertex(position = p3, normal = front, uvCoordinate = uv10),
        Vertex(position = p2, normal = front, uvCoordinate = uv00),
        // Right
        Vertex(position = p5, normal = right, uvCoordinate = uv01),
        Vertex(position = p6, normal = right, uvCoordinate = uv11),
        Vertex(position = p2, normal = right, uvCoordinate = uv10),
        Vertex(position = p1, normal = right, uvCoordinate = uv00),
        // Top
        Vertex(position = p7, normal = up, uvCoordinate = uv01),
        Vertex(position = p6, normal = up, uvCoordinate = uv11),
        Vertex(position = p5, normal = up, uvCoordinate = uv10),
        Vertex(position = p4, normal = up, uvCoordinate = uv00)
    )

    val indices = buildList {
        val sideCount = 6
        val verticesPerSide = 4
        for (i in 0 until sideCount) {
            val base = verticesPerSide * i
            // First triangle
            add(3 + base); add(1 + base); add(0 + base)
            // Second triangle
            add(3 + base); add(2 + base); add(1 + base)
        }
    }

    return GeometryData(vertices, indices)
}

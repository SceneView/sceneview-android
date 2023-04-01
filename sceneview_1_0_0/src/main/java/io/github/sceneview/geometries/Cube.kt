package io.github.sceneview.geometries

import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

class Cube private constructor(
    /**
     * Size of the constructed cube
     */
    val center: Position,
    /**
     * Center of the constructed cube
     */
    val size: Size,
    vertexBuffer: VertexBuffer,
    indexBuffer: IndexBuffer
) : Geometry(vertexBuffer, indexBuffer) {
    /**
     * Creates a [Geometry] in the shape of a cube with the give specifications.
     *
     * @param size the size of the constructed cube
     * @param center the center of the constructed cube
     */
    class Builder(val size: Size = Size(2.0f), val center: Position = Position(0.0f)) :
        BaseBuilder<Cube>(
            vertices = mutableListOf<Vertex>().apply {
                val extents = size * 0.5f
                val p0 = center + Size(-extents.x, -extents.y, extents.z)
                val p1 = center + Size(extents.x, -extents.y, extents.z)
                val p2 = center + Size(extents.x, -extents.y, -extents.z)
                val p3 = center + Size(-extents.x, -extents.y, -extents.z)
                val p4 = center + Size(-extents.x, extents.y, extents.z)
                val p5 = center + Size(extents.x, extents.y, extents.z)
                val p6 = center + Size(extents.x, extents.y, -extents.z)
                val p7 = center + Size(-extents.x, extents.y, -extents.z)
                val up = Direction(y = 1.0f)
                val down = Direction(y = -1.0f)
                val front = Direction(z = -1.0f)
                val back = Direction(z = 1.0f)
                val left = Direction(x = -1.0f)
                val right = Direction(x = 1.0f)
                val uv00 = UvCoordinate(x = 0.0f, y = 0.0f)
                val uv10 = UvCoordinate(x = 1.0f, y = 0.0f)
                val uv01 = UvCoordinate(x = 0.0f, y = 1.0f)
                val uv11 = UvCoordinate(x = 1.0f, y = 1.0f)
                addAll(
                    listOf(
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
                )
            },
            submeshes = mutableListOf<Submesh>().apply {
                val sideCount = 6
                val verticesPerSide = 4
                for (i in 0 until sideCount) {
                    add(
                        Submesh(
                            // First triangle for this side.
                            3 + verticesPerSide * i,
                            1 + verticesPerSide * i,
                            0 + verticesPerSide * i,
                            // Second triangle for this side.
                            3 + verticesPerSide * i,
                            2 + verticesPerSide * i,
                            1 + verticesPerSide * i
                        )
                    )
                }
            }
        ) {
        override fun build(
            vertexBuffer: VertexBuffer,
            indexBuffer: IndexBuffer
        ) = Cube(size, center, vertexBuffer, indexBuffer)
    }
}
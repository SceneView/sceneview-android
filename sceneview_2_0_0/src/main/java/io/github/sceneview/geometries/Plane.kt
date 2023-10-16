package io.github.sceneview.geometries

import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

class Plane private constructor(
    /**
     * Size of the constructed plane
     */
    val size: Size,
    /**
     * Center of the constructed plane
     */
    val center: Position,
    /**
     * Looking at direction
     */
    val normal: Direction,
    vertexBuffer: VertexBuffer,
    indexBuffer: IndexBuffer
) : Geometry(vertexBuffer, indexBuffer) {
    /**
     * Creates a [Geometry] in the shape of a plane with the give specifications
     *
     * @param center Center of the constructed plane
     * @param size  Size of the constructed plane
     * @param normal Looking at direction
     */
    class Builder(
        val size: Size = Size(x = 2.0f, y = 2.0f),
        val center: Position = Position(0.0f),
        val normal: Direction = Direction(y = 1.0f) // Looking upper
    ) : BaseGeometryBuilder<Plane>(
        vertices = getVertices(center, size, normal),
        submeshes = mutableListOf(
            Geometry.Submesh(
                // First triangle for this side.
                3, 1, 0,
                // Second triangle for this side.
                3, 2, 1
            )
        )
    ) {
        override fun build(
            vertexBuffer: VertexBuffer,
            indexBuffer: IndexBuffer
        ) = Plane(size, center, normal, vertexBuffer, indexBuffer)
    }

    fun update(
        engine: Engine,
        center: Position = this.center,
        size: Size = this.size,
        normal: Direction = this.normal
    ) {
        setBufferVertices(engine, getVertices(center, size, normal))
    }

    companion object {
        fun getVertices(
            center: Position = Position(0.0f),
            size: Size = Size(x = 2.0f, y = 2.0f),
            normal: Direction = Direction(y = 1.0f)
        ): List<Vertex> = mutableListOf<Vertex>().apply {
            val extents = size / 2.0f

            val p0 = center + Size(x = -extents.x, -extents.y, extents.z)
            val p1 = center + Size(x = -extents.x, extents.y, -extents.z)
            val p2 = center + Size(x = extents.x, extents.y, -extents.z)
            val p3 = center + Size(x = extents.x, -extents.y, extents.z)

            val uv00 = UvCoordinate(x = 0.0f, y = 0.0f)
            val uv10 = UvCoordinate(1.0f, 0.0f)
            val uv01 = UvCoordinate(0.0f, 1.0f)
            val uv11 = UvCoordinate(1.0f, 1.0f)

            add(Vertex(position = p0, normal = normal, uvCoordinate = uv00))
            add(Vertex(position = p1, normal = normal, uvCoordinate = uv01))
            add(Vertex(position = p2, normal = normal, uvCoordinate = uv11))
            add(Vertex(position = p3, normal = normal, uvCoordinate = uv10))
        }
    }
}

package io.github.sceneview.geometries

import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

class Plane private constructor(
    engine: Engine,
    size: Size,
    center: Position,
    normal: Direction,
    vertexBuffer: VertexBuffer,
    indexBuffer: IndexBuffer,
    vertices: List<Vertex>,
    submeshes: List<Submesh>,
) : Geometry(engine, vertexBuffer, indexBuffer, vertices, submeshes) {

    /**
     * Creates a [Geometry] in the shape of a plane with the give specifications
     */
    class Builder : BaseBuilder<Plane>() {
        /**
         * Size of the constructed plane
         */
        var size: Size = DEFAULT_SIZE
            private set

        /**
         * Center of the constructed plane
         */
        var center: Position = DEFAULT_CENTER
            private set

        /**
         * Looking at direction
         */
        var normal: Direction = DEFAULT_NORMAL
            private set

        fun size(size: Size) = apply { this.size = size }
        fun center(center: Position) = apply { this.center = center }
        fun normal(normal: Direction) = apply { this.normal = normal }

        init {
            submeshes(SUBMESHES)
        }

        override fun build(engine: Engine): Plane {
            vertices(getVertices(size, center, normal))
            return Plane(
                engine,
                size,
                center,
                normal,
                vertexBuilder.build(engine),
                indexBuilder.build(engine),
                vertices,
                submeshes
            )
        }
    }

    /**
     * Size of the constructed plane
     */
    var size: Size = size
        private set

    /**
     * Center of the constructed plane
     */
    var center: Position = center
        private set

    /**
     * Looking at direction
     */
    var normal: Direction = normal
        private set

    fun update(
        size: Size = this.size,
        center: Position = this.center,
        normal: Direction = this.normal
    ) {
        this.size = size
        this.center = center
        this.normal = normal
        setVertices(getVertices(size, center, normal))
    }

    companion object {
        val DEFAULT_SIZE = Size(x = 1.0f, y = 1.0f)
        val DEFAULT_CENTER = Position(0.0f)
        val DEFAULT_NORMAL = Direction(y = 1.0f) // Looking upper

        val SUBMESHES = listOf(
            Submesh(
                // First triangle for this side.
                3, 1, 0,
                // Second triangle for this side.
                3, 2, 1
            )
        )

        fun getVertices(size: Size, center: Position, normal: Direction) =
            mutableListOf<Vertex>().apply {
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
            }.toList()
    }
}

package io.github.sceneview.geometries

import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

class Plane private constructor(
    primitiveType: PrimitiveType,
    vertices: List<Vertex>,
    vertexBuffer: VertexBuffer,
    indices: List<List<Int>>,
    indexBuffer: IndexBuffer,
    primitivesOffsets: List<IntRange>,
    boundingBox: Box,
    size: Size,
    center: Position,
    normal: Direction,
    uvScale: UvScale
) : Geometry(
    primitiveType,
    vertices,
    vertexBuffer,
    indices,
    indexBuffer,
    primitivesOffsets,
    boundingBox
) {
    class Builder : Geometry.Builder(PrimitiveType.TRIANGLES) {
        var size: Size = DEFAULT_SIZE
            private set

        var center: Position = DEFAULT_CENTER
            private set

        var normal: Direction = DEFAULT_NORMAL
            private set

        var uvScale: UvScale = UvScale(1.0f)
            private set

        /**
         * Size of the constructed plane
         */
        fun size(size: Size) = apply { this.size = size }

        /**
         * Center of the constructed plane
         */
        fun center(center: Position) = apply { this.center = center }

        /**
         * Looking at direction
         */
        fun normal(normal: Direction) = apply { this.normal = normal }

        /**
         * UVs coordinates
         *
         * One way to tile the texture is by adjusting the UV coordinates of your model to extend
         * beyond 0 to 1 and setting the TextureSampler's WrapMode to REPEAT.
         */
        fun uvScale(uvScale: UvScale) = apply { this.uvScale = uvScale }

        override fun build(engine: Engine): Plane {
            vertices(getVertices(size, center, normal, uvScale))
            primitivesIndices(INDICES)
            return build(engine) { vertexBuffer, indexBuffer, offsets, boundingBox ->
                Plane(
                    primitiveType, vertices, vertexBuffer, indices, indexBuffer, offsets,
                    boundingBox, size, center, normal, uvScale
                )
            }
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

    /**
     * UVs coordinates
     *
     * One way to tile the texture is by adjusting the UV coordinates of your model to extend
     * beyond 0 to 1 and setting the TextureSampler's WrapMode to REPEAT.
     */
    var uvScale: UvScale = uvScale
        private set

    /**
     * Update the geometry
     *
     * @param size Size of the constructed plane
     * @param center Center of the constructed plane
     * @param normal Looking at direction
     * @param uvScale UVs coordinates
     *
     * One way to tile the texture is by adjusting the UV coordinates of your model to extend
     * beyond 0 to 1 and setting the TextureSampler's WrapMode to REPEAT.
     */
    fun update(
        engine: Engine,
        size: Size = this.size,
        center: Position = this.center,
        normal: Direction = this.normal,
        uvScale: UvScale = this.uvScale,
    ) = apply {
        update(engine = engine, vertices = getVertices(size, center, normal, uvScale))

        this.size = size
        this.center = center
        this.normal = normal
        this.uvScale = uvScale
    }

    companion object {
        val DEFAULT_SIZE = Size(x = 1.0f, y = 1.0f)
        val DEFAULT_CENTER = Position(0.0f)
        val DEFAULT_NORMAL = Direction(y = 1.0f) // Looking upper
        val UV_COORDINATES = listOf(
            // uv00
            UvCoordinate(0.0f, 0.0f),
            // uv01
            UvCoordinate(0.0f, 1.0f),
            // uv11
            UvCoordinate(1.0f, 1.0f),
            // uv10
            UvCoordinate(1.0f, 0.0f)
        )

        val INDICES = listOf(
            listOf(
                // First triangle for this side.
                3, 1, 0,
                // Second triangle for this side.
                3, 2, 1
            )
        )

        fun getVertices(
            size: Size,
            center: Position,
            normal: Direction,
            uvScale: UvScale = UvScale(1.0f),
        ): List<Vertex> {
            val extents = size / 2.0f
            return listOf(
                Vertex(
                    position = center + Size(x = -extents.x, -extents.y, extents.z),
                    normal = normal,
                    uvCoordinate = UV_COORDINATES[0] * uvScale
                ),
                Vertex(
                    position = center + Size(x = -extents.x, extents.y, -extents.z),
                    normal = normal,
                    uvCoordinate = UV_COORDINATES[1] * uvScale
                ),
                Vertex(
                    position = center + Size(x = extents.x, extents.y, -extents.z),
                    normal = normal,
                    uvCoordinate = UV_COORDINATES[2] * uvScale
                ),
                Vertex(
                    position = center + Size(x = extents.x, -extents.y, extents.z),
                    normal = normal,
                    uvCoordinate = UV_COORDINATES[3] * uvScale
                )
            )
        }
    }
}
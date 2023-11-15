package io.github.sceneview.geometries

import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position

class Polygon private constructor(
    engine: Engine,
    primitiveType: PrimitiveType,
    vertices: List<Vertex>,
    vertexBuffer: VertexBuffer,
    indices: List<PrimitiveIndices>,
    indexBuffer: IndexBuffer,
    primitivesOffsets: List<IntRange>,
    boundingBox: Box
) : Geometry(
    engine,
    primitiveType,
    vertices,
    vertexBuffer,
    indices,
    indexBuffer,
    primitivesOffsets,
    boundingBox
) {
    class Builder : Geometry.Builder(PrimitiveType.TRIANGLE_STRIP) {

        fun boundary(vertices: List<Vertex>) = apply {
            vertices(vertices)
        }

        fun boundary(
            positions: List<Position>,
            normal: Direction? = DEFAULT_NORMAL,
            uvCoordinate: UvCoordinate? = DEFAULT_UV_COORDINATE,
            color: Color? = null
        ) = boundary(positions.map { Vertex(it, normal, uvCoordinate, color) })

        override fun build(engine: Engine): Polygon {
            indices(getIndices(vertices))
            return build(engine) { vertexBuffer, indexBuffer, offsets, boundingBox ->
                Polygon(
                    engine, primitiveType, vertices, vertexBuffer, indices, indexBuffer, offsets,
                    boundingBox
                )
            }
        }
    }

    var boundary
        get() = vertices
        set(value) {
            if (vertices != value) {
                vertices = value
                indices = getIndices(value)
            }
        }

    fun update(
        positions: List<Position>,
        normal: Direction? = DEFAULT_NORMAL,
        uvCoordinate: UvCoordinate? = DEFAULT_UV_COORDINATE,
        color: Color? = null
    ) = apply {
        boundary = positions.map { Vertex(it, normal, uvCoordinate, color) }
    }

    companion object {
        val DEFAULT_NORMAL = Direction(y = 1.0f) // Up
        val DEFAULT_UV_COORDINATE = UvCoordinate(1.0f, 1.0f)

        fun getIndices(vertices: List<Vertex>) = buildList {
            val positions = vertices.map { it.position }
            positions.forEachIndexed { positionIndex, position ->
                if (positionIndex > 0) {
                    val direction = position - positions[positionIndex - 1]
                    val isForward = direction.x > 0
                    val index = (positionIndex * 2) + 3
                    add(
                        if (isForward) {
                            PrimitiveIndices(
                                index, index + 1, index + 2, index + 2, index + 1, index + 3
                            )
                        } else {
                            PrimitiveIndices(
                                index, index + 2, index + 1, index + 2, index + 3, index + 1
                            )
                        }
                    )
                }
            }
        }
    }
}
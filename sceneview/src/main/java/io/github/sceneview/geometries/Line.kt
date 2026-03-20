package io.github.sceneview.geometries

import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer
import dev.romainguy.kotlin.math.abs
import dev.romainguy.kotlin.math.max
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

/**
 * A line segment geometry between two 3D points.
 *
 * Uses Filament's [PrimitiveType.LINES] primitive type — two index entries per line segment.
 */
class Line private constructor(
    primitiveType: PrimitiveType,
    vertices: List<Vertex>,
    vertexBuffer: VertexBuffer,
    indices: List<List<Int>>,
    indexBuffer: IndexBuffer,
    primitivesOffsets: List<IntRange>,
    boundingBox: Box,
    start: Position,
    end: Position
) : Geometry(
    primitiveType,
    vertices,
    vertexBuffer,
    indices,
    indexBuffer,
    primitivesOffsets,
    boundingBox
) {
    class Builder : Geometry.Builder(PrimitiveType.LINES) {
        var start: Position = DEFAULT_START
            private set
        var end: Position = DEFAULT_END
            private set

        fun start(start: Position) = apply { this.start = start }
        fun end(end: Position) = apply { this.end = end }

        override fun build(engine: Engine): Line {
            vertices(getVertices(start, end))
            primitivesIndices(getIndices())
            return build(engine) { vertexBuffer, indexBuffer, offsets, boundingBox ->
                Line(
                    primitiveType, vertices, vertexBuffer, indices, indexBuffer, offsets,
                    boundingBox, start, end
                )
            }
        }
    }

    var start: Position = start
        private set
    var end: Position = end
        private set

    fun update(
        engine: Engine,
        start: Position = this.start,
        end: Position = this.end
    ) = apply {
        update(
            engine = engine,
            vertices = getVertices(start, end),
            primitivesIndices = this.primitivesIndices
        )
        this.start = start
        this.end = end
    }

    companion object {
        val DEFAULT_START = Position(x = 0f, y = 0f, z = 0f)
        val DEFAULT_END = Position(x = 1f, y = 0f, z = 0f)

        fun getVertices(start: Position, end: Position): List<Vertex> = listOf(
            Vertex(position = start),
            Vertex(position = end)
        )

        fun getIndices(): List<List<Int>> = listOf(listOf(0, 1))
    }
}

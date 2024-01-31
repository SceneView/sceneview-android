package io.github.sceneview.geometries

import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Position2
import io.github.sceneview.triangulation.Delaunator
import io.github.sceneview.triangulation.Earcut

class Shape private constructor(
    engine: Engine,
    primitiveType: PrimitiveType,
    vertices: List<Vertex>,
    vertexBuffer: VertexBuffer,
    indices: List<List<Int>>,
    indexBuffer: IndexBuffer,
    primitivesOffsets: List<IntRange>,
    boundingBox: Box,
    polygonPath: List<Position2>,
    polygonHoles: List<Int>,
    delaunayPoints: List<Position2>,
    normal: Direction,
    uvScale: UvScale,
    color: Color?
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
    class Builder : Geometry.Builder(PrimitiveType.TRIANGLES) {
        var polygonPath: List<Position2> = listOf()
            private set
        var polygonHoles: List<Int> = listOf()
            private set
        var delaunayPoints: List<Position2> = listOf()
            private set
        var normal: Direction = DEFAULT_NORMAL
            private set
        var uvScale: UvScale = UvScale(1.0f)
            private set
        var color: Color? = null
            private set

        fun polygonPath(path: List<Position2>, holes: List<Int> = listOf()) = apply {
            this.polygonPath = path
            this.polygonHoles = holes
        }

        fun delaunayPoints(delaunayPoints: List<Position2>) = apply {
            this.delaunayPoints = delaunayPoints
        }

        fun normal(normal: Direction) = apply { this.normal = normal }
        fun uvScale(uvScale: UvScale) = apply { this.uvScale = uvScale }
        fun color(color: Color?) = apply { this.color = color }

        override fun build(engine: Engine): Shape {
            vertices(getVertices(polygonPath + delaunayPoints))
            primitivesIndices(
                getPolygonIndices(
                    polygonPath,
                    polygonHoles
                ) + getDelaunayIndices(delaunayPoints)
            )
            return build(engine) { vertexBuffer, indexBuffer, offsets, boundingBox ->
                Shape(
                    engine,
                    primitiveType,
                    vertices,
                    vertexBuffer,
                    indices,
                    indexBuffer,
                    offsets,
                    boundingBox,
                    polygonPath,
                    polygonHoles,
                    delaunayPoints,
                    normal,
                    uvScale,
                    color
                )
            }
        }
    }

    var polygonPath: List<Position2> = polygonPath
        private set
    var polygonHoles: List<Int> = polygonHoles
        private set
    var delaunayPoints: List<Position2> = delaunayPoints
        private set
    var normal: Direction = normal
        private set
    var uvScale: UvScale = uvScale
        private set
    var color: Color? = color
        private set

    fun update(
        polygonPath: List<Position2> = this.polygonPath,
        polygonHoles: List<Int> = this.polygonHoles,
        delaunayPoints: List<Position2> = this.delaunayPoints,
        normal: Direction = this.normal,
        uvScale: UvScale = this.uvScale,
        color: Color? = this.color
    ) = apply {
        this.polygonPath = polygonPath
        this.polygonHoles = polygonHoles
        this.delaunayPoints = delaunayPoints
        this.normal = normal
        this.uvScale = uvScale
        this.color = color
        vertices = getVertices(polygonPath + delaunayPoints)
        primitivesIndices =
            getPolygonIndices(polygonPath, polygonHoles) + getDelaunayIndices(delaunayPoints)
    }

    companion object {
        val DEFAULT_NORMAL = Direction(z = 1.0f) // Reward

        fun getVertices(
            positions: List<Position2>,
            normal: Direction = DEFAULT_NORMAL,
            uvScale: UvScale = UvScale(1.0f),
            color: Color? = null
        ) = positions.map { Vertex(Position(it), normal, it * uvScale, color) }

        fun getPolygonIndices(positions: List<Position2>, holes: List<Int> = listOf()) =
            if (positions.isNotEmpty()) {
                listOf(Earcut.triangulate(positions.map { it.xy }, holes))
            } else {
                listOf()
            }

        fun getDelaunayIndices(positions: List<Position2>) = if (positions.isNotEmpty()) {
            listOf(Delaunator(positions.map {
                Delaunator.Point(it.x.toDouble(), it.y.toDouble())
            }).triangles.toList())
        } else {
            listOf()
        }
    }
}
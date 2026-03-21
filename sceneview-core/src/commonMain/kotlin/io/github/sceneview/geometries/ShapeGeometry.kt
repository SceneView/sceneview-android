package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.rendering.Vertex
import io.github.sceneview.triangulation.Delaunator
import io.github.sceneview.triangulation.Earcut

/**
 * Generates a flat 2D shape from a polygon outline, optionally with holes and/or
 * additional Delaunay-triangulated interior points.
 *
 * The shape is placed in the XY plane with the given [normal] direction.
 * Earcut is used for the polygon triangulation and Delaunator for any extra interior points.
 *
 * @param polygonPath 2D polygon outline vertices (counter-clockwise winding).
 * @param polygonHoles Hole start-indices into [polygonPath], as required by [Earcut].
 * @param delaunayPoints Additional interior points to triangulate via Delaunay.
 * @param normal Surface normal assigned to every vertex.
 * @param uvScale Scale factor applied to position XY to produce UV coordinates.
 * @param color Optional per-vertex color.
 */
fun generateShape(
    polygonPath: List<Float2>,
    polygonHoles: List<Int> = emptyList(),
    delaunayPoints: List<Float2> = emptyList(),
    normal: Float3 = Float3(z = 1f),
    uvScale: Float2 = Float2(1f),
    color: Float4? = null
): GeometryData {
    val allPositions = polygonPath + delaunayPoints

    val vertices = allPositions.map { pos ->
        Vertex(
            position = Float3(pos.x, pos.y, 0f),
            normal = normal,
            uvCoordinate = pos * uvScale,
            color = color
        )
    }

    val polygonIndices = if (polygonPath.isNotEmpty()) {
        Earcut.triangulate(polygonPath, polygonHoles)
    } else {
        emptyList()
    }

    val delaunayIndices = if (delaunayPoints.isNotEmpty()) {
        val offset = polygonPath.size
        Delaunator(delaunayPoints.map {
            Delaunator.Point(it.x.toDouble(), it.y.toDouble())
        }).triangles.map { it + offset }
    } else {
        emptyList()
    }

    return GeometryData(vertices, polygonIndices + delaunayIndices)
}

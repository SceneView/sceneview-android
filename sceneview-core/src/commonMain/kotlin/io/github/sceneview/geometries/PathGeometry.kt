package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.rendering.Vertex

/**
 * Generates a polyline (path) through a list of 3D points.
 *
 * Produces one vertex per point and LINES-style index pairs so that each consecutive
 * pair of vertices forms one line segment. When [closed] is `true`, an extra segment
 * connects the last point back to the first.
 *
 * @param points The ordered list of points forming the polyline. Must contain at least 2 points.
 * @param closed Whether to connect the last point back to the first.
 */
fun generatePath(
    points: List<Float3>,
    closed: Boolean = false
): GeometryData {
    require(points.size >= 2) { "Path requires at least 2 points" }

    val vertices = points.map { Vertex(position = it) }

    val indices = buildList {
        for (i in 0 until points.size - 1) {
            add(i)
            add(i + 1)
        }
        if (closed && points.size >= 2) {
            add(points.size - 1)
            add(0)
        }
    }

    return GeometryData(vertices, indices)
}

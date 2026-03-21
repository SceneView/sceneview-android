package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.max
import dev.romainguy.kotlin.math.min
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.rendering.Vertex

/**
 * Pure geometry data — vertices and triangle indices — with no rendering-engine dependency.
 *
 * Platform-specific code converts this into native vertex/index buffers.
 */
data class GeometryData(
    val vertices: List<Vertex>,
    val indices: List<Int>
)

/**
 * Axis-aligned bounding box computed from vertex positions.
 */
data class BoundingBox(
    val center: Position,
    val halfExtent: Size
) {
    val min: Position get() = center - halfExtent
    val max: Position get() = center + halfExtent
}

/**
 * Computes the axis-aligned bounding box of this geometry's vertices.
 */
fun GeometryData.boundingBox(): BoundingBox {
    require(vertices.isNotEmpty()) { "Cannot compute bounding box of empty geometry" }
    var minPos = Float3(vertices.first().position)
    var maxPos = Float3(vertices.first().position)
    for (vertex in vertices) {
        minPos = min(minPos, vertex.position)
        maxPos = max(maxPos, vertex.position)
    }
    val halfExtent = (maxPos - minPos) / 2.0f
    val center = minPos + halfExtent
    return BoundingBox(center, halfExtent)
}

/**
 * Whether any vertex has a normal defined.
 */
val GeometryData.hasNormals: Boolean get() = vertices.any { it.normal != null }

/**
 * Whether any vertex has UV coordinates defined.
 */
val GeometryData.hasUvCoordinates: Boolean get() = vertices.any { it.uvCoordinate != null }

/**
 * Whether any vertex has color defined.
 */
val GeometryData.hasColors: Boolean get() = vertices.any { it.color != null }

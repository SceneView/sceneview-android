package io.github.sceneview.rendering

import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position

/**
 * Cross-platform vertex definition for geometry construction.
 *
 * Used to define the vertices of a mesh in a platform-agnostic way.
 * Platform-specific geometry builders convert these to native vertex buffers.
 *
 * @param position The position of the vertex in local space.
 * @param normal The surface normal direction at this vertex (for lighting).
 * @param uvCoordinate Texture coordinate. Values should be between 0 and 1.
 * @param color Per-vertex color (RGBA).
 */
data class Vertex(
    val position: Position = Position(),
    val normal: Direction? = null,
    val uvCoordinate: Float2? = null,
    val color: Color? = null
)

val List<Vertex>.hasNormals get() = any { it.normal != null }
val List<Vertex>.hasUvCoordinates get() = any { it.uvCoordinate != null }
val List<Vertex>.hasColors get() = any { it.color != null }

/**
 * Computes primitive offset ranges from a list of index lists.
 */
fun List<List<Int>>.getOffsets(): List<IntRange> {
    var indexStart = 0
    return map { primitiveIndices ->
        (indexStart until indexStart + primitiveIndices.size).also {
            indexStart += primitiveIndices.size
        }
    }
}

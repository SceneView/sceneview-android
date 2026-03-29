package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.rendering.Vertex
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates a rounded cube (box with chamfered/beveled edges) as vertices and triangle indices.
 *
 * Creates a cube where each edge and corner is rounded with the specified radius.
 * This is achieved by generating a cube-sphere hybrid: a sphere that is squashed
 * toward a cube shape, with the corners rounded.
 *
 * @param size Full extents along each axis. Default 1x1x1.
 * @param radius Corner rounding radius. Must be <= half the smallest dimension.
 *               Default 0.1.
 * @param center Offset applied to every vertex position.
 * @param segments Number of segments for the rounded parts. Default 4.
 */
fun generateRoundedCube(
    size: Float3 = Float3(1f),
    radius: Float = 0.1f,
    center: Float3 = Float3(0f),
    segments: Int = 4
): GeometryData {
    val halfSize = size * 0.5f
    val clampedRadius = radius.coerceIn(0f, minOf(halfSize.x, halfSize.y, halfSize.z))
    val inner = Float3(
        halfSize.x - clampedRadius,
        halfSize.y - clampedRadius,
        halfSize.z - clampedRadius
    )

    val vertices = mutableListOf<Vertex>()
    val indices = mutableListOf<Int>()

    val stacks = segments * 2 + 1
    val slices = segments * 4

    // Generate vertices as a modified sphere
    for (stack in 0..stacks) {
        val phi = kotlin.math.PI.toFloat() * stack.toFloat() / stacks.toFloat()
        for (slice in 0..slices) {
            val theta = 2f * kotlin.math.PI.toFloat() * slice.toFloat() / slices.toFloat()

            // Sphere direction
            val sx = sin(phi) * cos(theta)
            val sy = cos(phi)
            val sz = sin(phi) * sin(theta)

            val normal = normalize(Float3(sx, sy, sz))

            // Push the sphere surface out from the inner box
            val position = Float3(
                inner.x * sign(sx) + clampedRadius * sx,
                inner.y * sign(sy) + clampedRadius * sy,
                inner.z * sign(sz) + clampedRadius * sz
            ) + center

            val uv = Float2(
                slice.toFloat() / slices,
                stack.toFloat() / stacks
            )

            vertices.add(Vertex(position = position, normal = normal, uvCoordinate = uv))
        }
    }

    // Generate indices
    for (stack in 0 until stacks) {
        for (slice in 0 until slices) {
            val row = slices + 1
            val a = stack * row + slice
            val b = a + row
            val c = a + 1
            val d = b + 1

            indices.add(a); indices.add(b); indices.add(d)
            indices.add(a); indices.add(d); indices.add(c)
        }
    }

    return GeometryData(vertices, indices)
}

private fun sign(v: Float): Float = when {
    v > 0f -> 1f
    v < 0f -> -1f
    else -> 0f
}

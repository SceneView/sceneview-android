package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.PI
import dev.romainguy.kotlin.math.TWO_PI
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.rendering.Vertex
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates a UV sphere as a list of vertices and triangle indices.
 *
 * @param radius Sphere radius. Default 1.
 * @param center Offset applied to every vertex position.
 * @param stacks Number of horizontal subdivisions (latitude). Default 24.
 * @param slices Number of vertical subdivisions (longitude). Default 24.
 */
fun generateSphere(
    radius: Float = 1f,
    center: Float3 = Float3(0f),
    stacks: Int = 24,
    slices: Int = 24
): GeometryData {
    val vertices = buildList {
        for (stack in 0..stacks) {
            val phi = PI * stack.toFloat() / stacks.toFloat()
            for (slice in 0..slices) {
                val theta = TWO_PI * (if (slice == slices) 0 else slice).toFloat() / slices
                var position = Float3(
                    x = sin(phi) * cos(theta),
                    y = cos(phi),
                    z = sin(phi) * sin(theta)
                ) * radius
                val normal = normalize(position)
                position += center
                val uvCoordinate = Float2(
                    x = 1f - slice.toFloat() / slices,
                    y = 1f - stack.toFloat() / stacks
                )
                add(Vertex(position = position, normal = normal, uvCoordinate = uvCoordinate))
            }
        }
    }

    val indices = buildList {
        var v = 0
        for (stack in 0 until stacks) {
            for (slice in 0 until slices) {
                val topCap = stack == 0
                val bottomCap = stack == stacks - 1
                val next = slice + 1
                if (!topCap) {
                    add(v + slice)
                    add(v + next)
                    add(v + slice + slices + 1)
                }
                if (!bottomCap) {
                    add(v + next)
                    add(v + next + slices + 1)
                    add(v + slice + slices + 1)
                }
            }
            v += slices + 1
        }
    }

    return GeometryData(vertices, indices)
}

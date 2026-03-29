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
 * Generates a capsule (cylinder with hemispherical caps) as vertices and triangle indices.
 *
 * The capsule is centered at [center] with its axis along Y.
 *
 * @param radius Radius of the capsule. Default 0.5.
 * @param height Full height including the hemispherical caps. Default 2.
 * @param center Offset applied to every vertex position.
 * @param slices Number of radial subdivisions. Default 24.
 * @param hemisphereStacks Number of stacks per hemisphere cap. Default 8.
 * @param cylinderStacks Number of stacks for the cylinder body. Default 1.
 */
fun generateCapsule(
    radius: Float = 0.5f,
    height: Float = 2f,
    center: Float3 = Float3(0f),
    slices: Int = 24,
    hemisphereStacks: Int = 8,
    cylinderStacks: Int = 1
): GeometryData {
    val cylinderHeight = (height - 2f * radius).coerceAtLeast(0f)
    val halfCylHeight = cylinderHeight / 2f

    val vertices = mutableListOf<Vertex>()
    val indices = mutableListOf<Int>()

    // --- Top hemisphere ---
    for (stack in 0..hemisphereStacks) {
        val phi = PI / 2f * stack.toFloat() / hemisphereStacks // 0 to PI/2
        for (slice in 0..slices) {
            val theta = TWO_PI * slice.toFloat() / slices
            val x = radius * cos(phi) * cos(theta)
            val y = radius * sin(phi) + halfCylHeight
            val z = radius * cos(phi) * sin(theta)
            val nx = cos(phi) * cos(theta)
            val ny = sin(phi)
            val nz = cos(phi) * sin(theta)
            val u = slice.toFloat() / slices
            val v = 0.5f - (y / height)

            vertices.add(Vertex(
                position = Float3(x, y, z) + center,
                normal = normalize(Float3(nx, ny, nz)),
                uvCoordinate = Float2(u, v)
            ))
        }
    }

    // --- Cylinder body ---
    for (stack in 0..cylinderStacks) {
        val y = halfCylHeight - cylinderHeight * stack.toFloat() / cylinderStacks
        for (slice in 0..slices) {
            val theta = TWO_PI * slice.toFloat() / slices
            val x = radius * cos(theta)
            val z = radius * sin(theta)
            val u = slice.toFloat() / slices
            val v = 0.5f - (y / height)

            vertices.add(Vertex(
                position = Float3(x, y, z) + center,
                normal = normalize(Float3(cos(theta), 0f, sin(theta))),
                uvCoordinate = Float2(u, v)
            ))
        }
    }

    // --- Bottom hemisphere ---
    for (stack in 0..hemisphereStacks) {
        val phi = PI / 2f * stack.toFloat() / hemisphereStacks // 0 to PI/2
        for (slice in 0..slices) {
            val theta = TWO_PI * slice.toFloat() / slices
            val x = radius * cos(phi) * cos(theta)
            val y = -radius * sin(phi) - halfCylHeight
            val z = radius * cos(phi) * sin(theta)
            val nx = cos(phi) * cos(theta)
            val ny = -sin(phi)
            val nz = cos(phi) * sin(theta)
            val u = slice.toFloat() / slices
            val v = 0.5f - (y / height)

            vertices.add(Vertex(
                position = Float3(x, y, z) + center,
                normal = normalize(Float3(nx, ny, nz)),
                uvCoordinate = Float2(u, v)
            ))
        }
    }

    // --- Generate indices ---
    val totalStacks = hemisphereStacks + cylinderStacks + hemisphereStacks + 2
    val verticesPerRow = slices + 1
    val totalRows = (hemisphereStacks + 1) + (cylinderStacks + 1) + (hemisphereStacks + 1)

    for (row in 0 until totalRows - 1) {
        for (col in 0 until slices) {
            val a = row * verticesPerRow + col
            val b = a + verticesPerRow
            val c = a + 1
            val d = b + 1

            indices.add(a); indices.add(b); indices.add(d)
            indices.add(a); indices.add(d); indices.add(c)
        }
    }

    return GeometryData(vertices, indices)
}

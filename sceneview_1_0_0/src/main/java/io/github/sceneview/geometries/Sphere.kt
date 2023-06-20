package io.github.sceneview.geometries

import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import dev.romainguy.kotlin.math.PI
import dev.romainguy.kotlin.math.TWO_PI
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.math.Position
import kotlin.math.cos
import kotlin.math.sin

class Sphere(
    /**
     * The radius of the constructed sphere
     */
    val radius: Float,
    /**
     * The center of the constructed sphere
     */
    val center: Position,
    val stacks: Int,
    val slices: Int,
    vertexBuffer: VertexBuffer,
    indexBuffer: IndexBuffer
) : Geometry(vertexBuffer, indexBuffer) {
    /**
     * Creates a [Geometry] in the shape of a sphere with the give specifications.
     *
     * @param radius the radius of the constructed sphere
     * @param center the center of the constructed sphere
     */
    class Builder(
        val radius: Float = 1.0f,
        val center: Position = Position(0.0f),
        val stacks: Int = 24,
        val slices: Int = 24
    ) : BaseGeometryBuilder<Sphere>(
        vertices = mutableListOf<Vertex>().apply {
            for (stack in 0..stacks) {
                val phi = PI * stack.toFloat() / stacks.toFloat()
                for (slice in 0..slices) {
                    val theta = TWO_PI * (if (slice == slices) 0 else slice).toFloat() / slices
                    var position = Position(
                        x = sin(phi) * cos(theta), y = cos(phi), z = sin(phi) * sin(theta)
                    ) * radius
                    val normal = normalize(position)
                    position += center
                    val uvCoordinate = UvCoordinate(
                        x = 1.0f - slice.toFloat() / slices, y = 1.0f - stack.toFloat() / stacks
                    )
                    add(
                        Vertex(
                            position = position,
                            normal = normal,
                            uvCoordinate = uvCoordinate
                        )
                    )
                }
            }
        },
        submeshes = mutableListOf<Submesh>().apply {
            var v = 0
            for (stack in 0 until stacks) {
                val triangleIndices = mutableListOf<Int>()
                for (slice in 0 until slices) {
                    // Skip triangles at the caps that would have an area of zero.
                    val topCap = stack == 0
                    val bottomCap = stack == stacks - 1
                    val next = slice + 1
                    if (!topCap) {
                        triangleIndices.add(v + slice)
                        triangleIndices.add(v + next)
                        triangleIndices.add(v + slice + slices + 1)
                    }
                    if (!bottomCap) {
                        triangleIndices.add(v + next)
                        triangleIndices.add(v + next + slices + 1)
                        triangleIndices.add(v + slice + slices + 1)
                    }
                }
                add(Submesh(triangleIndices))
                v += slices + 1
            }
        }) {
        override fun build(vertexBuffer: VertexBuffer, indexBuffer: IndexBuffer) =
            Sphere(radius, center, stacks, slices, vertexBuffer, indexBuffer)
    }
}
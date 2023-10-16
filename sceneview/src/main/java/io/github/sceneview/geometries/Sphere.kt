package io.github.sceneview.geometries

import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import dev.romainguy.kotlin.math.PI
import dev.romainguy.kotlin.math.TWO_PI
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.math.Position
import kotlin.math.cos
import kotlin.math.sin

class Sphere(
    engine: Engine,
    radius: Float,
    center: Position,
    stacks: Int,
    slices: Int,
    vertexBuffer: VertexBuffer,
    indexBuffer: IndexBuffer,
    vertices: List<Vertex>,
    submeshes: List<Submesh>
) : Geometry(engine, vertexBuffer, indexBuffer, vertices, submeshes) {
    /**
     * Creates a [Geometry] in the shape of a sphere with the give specifications.
     */
    class Builder : BaseBuilder<Sphere>() {
        /**
         * The radius of the constructed sphere
         */
        var radius: Float = DEFAULT_RADIUS
            private set

        /**
         * The center of the constructed sphere
         */
        var center: Position = DEFAULT_CENTER
            private set
        var stacks: Int = DEFAULT_STACKS
            private set
        var slices: Int = DEFAULT_SLICES
            private set

        fun radius(radius: Float) = apply { this.radius = radius }
        fun center(center: Position) = apply { this.center = center }
        fun stacks(stacks: Int) = apply { this.stacks = stacks }
        fun slices(slices: Int) = apply { this.slices = slices }

        override fun build(engine: Engine): Sphere {
            vertices(getVertices(radius, center, stacks, slices))
            submeshes(getSubmeshes(stacks, slices))
            return Sphere(
                engine,
                radius,
                center,
                stacks,
                slices,
                vertexBuilder.build(engine),
                indexBuilder.build(engine),
                vertices,
                submeshes
            )
        }
    }

    /**
     * The radius of the constructed sphere
     */
    var radius: Float = radius
        private set

    /**
     * The center of the constructed sphere
     */
    var center: Position = center
        private set
    var stacks: Int = stacks
        private set
    var slices: Int = slices
        private set

    fun update(
        radius: Float = this.radius,
        center: Position = this.center,
        stacks: Int = this.stacks,
        slices: Int = this.slices
    ) {
        this.radius = radius
        this.center = center
        setVertices(getVertices(radius, center, stacks, slices))
        if (stacks != this.stacks || slices != this.slices) {
            this.stacks = stacks
            this.slices = slices
            setSubmeshes(getSubmeshes(stacks, slices))
        }
    }

    companion object {
        val DEFAULT_RADIUS = 1.0f
        val DEFAULT_CENTER = Position(0.0f)
        val DEFAULT_STACKS = 24
        val DEFAULT_SLICES = 24

        fun getVertices(radius: Float, center: Position, stacks: Int, slices: Int) =
            mutableListOf<Vertex>().apply {
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
            }.toList()

        fun getSubmeshes(stacks: Int, slices: Int) = mutableListOf<Submesh>().apply {
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
        }.toList()
    }
}
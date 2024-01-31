package io.github.sceneview.geometries

import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import dev.romainguy.kotlin.math.TWO_PI
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import kotlin.math.cos
import kotlin.math.sin

class Cylinder(
    /**
     * The radius of the constructed cylinder
     */
    val radius: Float,
    /**
     * The height of the constructed cylinder
     */
    val height: Float,
    /**
     * The center of the constructed cylinder
     */
    val center: Position,
    /**
     * Number of faces
     */
    val sideCount: Int,
    vertexBuffer: VertexBuffer,
    indexBuffer: IndexBuffer
) : Geometry(vertexBuffer, indexBuffer) {
    /**
     * Creates a [Geometry] in the shape of a cylinder with the give specifications.
     *
     * @param radius the radius of the constructed cylinder
     * @param height the height of the constructed cylinder
     * @param center the center of the constructed cylinder
     */
    class Builder(
        val radius: Float = 1.0f,
        val height: Float = 2.0f,
        val center: Position = Position(0.0f),
        val sideCount: Int = 24
    ) : BaseGeometryBuilder<Cylinder>(
        vertices = mutableListOf<Geometry.Vertex>().apply {
            val halfHeight = height / 2
            val thetaIncrement = TWO_PI / sideCount
            var theta = 0f
            val uStep = 1.0f / sideCount
            val lowerCapVertices = mutableListOf<Geometry.Vertex>()
            val upperCapVertices = mutableListOf<Geometry.Vertex>()
            val upperEdgeVertices = mutableListOf<Geometry.Vertex>()

            // Generate vertices along the sides of the cylinder
            for (side in 0..sideCount) {
                // Calculate edge vertices along bottom of cylinder
                var lowerPosition = Position(
                    x = radius * cos(theta), y = -halfHeight, z = radius * sin(theta)
                )
                lowerPosition += center
                add(
                    Geometry.Vertex(
                        position = lowerPosition,
                        normal = normalize(
                            Direction(x = lowerPosition.x, y = 0.0f, z = lowerPosition.z)
                        ),
                        uvCoordinate = UvCoordinate(x = uStep * side, y = 0.0f)
                    )
                )

                // Create a copy of lower vertex with bottom-facing normals for cap
                lowerCapVertices.add(
                    Geometry.Vertex(
                        position = lowerPosition,
                        normal = Direction(y = -1.0f),
                        uvCoordinate = UvCoordinate(x = uStep * side, y = 0.0f)
                    )
                )

                // Calculate edge vertices along top of cylinder
                var upperPosition = Position(
                    x = radius * cos(theta), y = halfHeight, z = radius * sin(theta)
                )
                upperPosition += center
                upperEdgeVertices.add(
                    Geometry.Vertex(
                        position = upperPosition,
                        normal = normalize(
                            Direction(
                                x = upperPosition.x,
                                y = 0.0f,
                                z = upperPosition.z
                            )
                        ),
                        uvCoordinate = UvCoordinate(x = uStep * side, y = 1.0f)
                    )
                )

                // Create a copy of upper vertex with up-facing normals for cap
                upperCapVertices.add(
                    Geometry.Vertex(
                        position = upperPosition,
                        normal = Direction(y = 1.0f),
                        uvCoordinate = UvCoordinate(
                            x = (cos(theta) + 1.0f) / 2.0f, y = (sin(theta) + 1.0f) / 2.0f
                        )
                    )
                )
                theta += thetaIncrement
            }
            addAll(upperEdgeVertices)

            // Generate vertices for the centers of the caps of the cylinder
            val lowerCenterIndex = size
            add(
                Geometry.Vertex(
                    position = center + Size(y = -halfHeight),
                    normal = Direction(y = -1.0f),
                    uvCoordinate = UvCoordinate(x = 0.5f, y = 0.5f)
                )
            )
            addAll(lowerCapVertices)
            val upperCenterIndex = size
            add(
                Geometry.Vertex(
                    position = center + Size(y = halfHeight),
                    normal = Direction(y = 1.0f),
                    uvCoordinate = UvCoordinate(x = 0.5f, y = 0.5f)
                )
            )
            addAll(upperCapVertices)
        },
        submeshes = mutableListOf<Geometry.Submesh>().apply {
            // Create triangles for each side
            for (side in 0 until sideCount) {
                val bottomRight = side + 1
                val topLeft = side + sideCount + 1
                val topRight = side + sideCount + 2
                val lowerCenterIndex = 2 * sideCount
                val upperCenterIndex = lowerCenterIndex + 1 + sideCount

                add(
                    Geometry.Submesh(
                        // First triangle of side
                        side, topRight, bottomRight,
                        // Second triangle of side
                        side, topLeft, topRight,
                        // Add bottom cap triangle
                        lowerCenterIndex, lowerCenterIndex + side + 1, lowerCenterIndex + side + 2,
                        // Add top cap triangle
                        upperCenterIndex, upperCenterIndex + side + 2, lowerCenterIndex + side + 1
                    )
                )
            }
        }) {
        override fun build(vertexBuffer: VertexBuffer, indexBuffer: IndexBuffer) =
            Cylinder(radius, height, center, sideCount, vertexBuffer, indexBuffer)
    }
}
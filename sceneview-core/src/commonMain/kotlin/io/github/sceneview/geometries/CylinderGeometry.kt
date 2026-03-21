package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.TWO_PI
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.rendering.Vertex
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates a capped cylinder as a list of vertices and triangle indices.
 *
 * The cylinder is centred at [center] with its axis along Y.
 *
 * @param radius Cylinder radius. Default 1.
 * @param height Full height along Y. Default 2.
 * @param center Offset applied to every vertex position.
 * @param sideCount Number of radial subdivisions. Default 24.
 */
fun generateCylinder(
    radius: Float = 1f,
    height: Float = 2f,
    center: Float3 = Float3(0f),
    sideCount: Int = 24
): GeometryData {
    val vertices = buildList {
        val halfHeight = height / 2f
        val thetaIncrement = TWO_PI / sideCount
        var theta = 0f
        val uStep = 1f / sideCount

        val lowerCapVertices = mutableListOf<Vertex>()
        val upperCapVertices = mutableListOf<Vertex>()
        val upperEdgeVertices = mutableListOf<Vertex>()

        for (side in 0..sideCount) {
            // Lower edge vertex (side normal)
            val lowerPosition = Float3(
                x = radius * cos(theta), y = -halfHeight, z = radius * sin(theta)
            ) + center
            add(
                Vertex(
                    position = lowerPosition,
                    normal = normalize(Float3(x = lowerPosition.x - center.x, y = 0f, z = lowerPosition.z - center.z)),
                    uvCoordinate = Float2(x = uStep * side, y = 0f)
                )
            )

            // Lower cap copy (downward normal)
            lowerCapVertices.add(
                Vertex(
                    position = lowerPosition,
                    normal = Float3(y = -1f),
                    uvCoordinate = Float2(x = uStep * side, y = 0f)
                )
            )

            // Upper edge vertex (side normal)
            val upperPosition = Float3(
                x = radius * cos(theta), y = halfHeight, z = radius * sin(theta)
            ) + center
            upperEdgeVertices.add(
                Vertex(
                    position = upperPosition,
                    normal = normalize(Float3(x = upperPosition.x - center.x, y = 0f, z = upperPosition.z - center.z)),
                    uvCoordinate = Float2(x = uStep * side, y = 1f)
                )
            )

            // Upper cap copy (upward normal)
            upperCapVertices.add(
                Vertex(
                    position = upperPosition,
                    normal = Float3(y = 1f),
                    uvCoordinate = Float2(
                        x = (cos(theta) + 1f) / 2f,
                        y = (sin(theta) + 1f) / 2f
                    )
                )
            )
            theta += thetaIncrement
        }

        addAll(upperEdgeVertices)

        // Lower cap center
        add(
            Vertex(
                position = center + Float3(y = -halfHeight),
                normal = Float3(y = -1f),
                uvCoordinate = Float2(x = 0.5f, y = 0.5f)
            )
        )
        addAll(lowerCapVertices)

        // Upper cap center
        add(
            Vertex(
                position = center + Float3(y = halfHeight),
                normal = Float3(y = 1f),
                uvCoordinate = Float2(x = 0.5f, y = 0.5f)
            )
        )
        addAll(upperCapVertices)
    }

    val indices = buildList {
        for (side in 0 until sideCount) {
            val bottomRight = side + 1
            val topLeft = side + sideCount + 1
            val topRight = side + sideCount + 2
            val lowerCenterIndex = 2 * (sideCount + 1)
            val upperCenterIndex = lowerCenterIndex + sideCount + 2

            // First triangle of side
            add(side); add(topRight); add(bottomRight)
            // Second triangle of side
            add(side); add(topLeft); add(topRight)
            // Bottom cap triangle
            add(lowerCenterIndex); add(lowerCenterIndex + side + 1); add(lowerCenterIndex + side + 2)
            // Top cap triangle
            add(upperCenterIndex); add(upperCenterIndex + side + 2); add(upperCenterIndex + side + 1)
        }
    }

    return GeometryData(vertices, indices)
}

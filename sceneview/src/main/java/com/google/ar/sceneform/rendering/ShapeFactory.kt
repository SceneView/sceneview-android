package com.google.ar.sceneform.rendering

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.filament.Engine
import com.google.ar.sceneform.rendering.RenderableDefinition.Submesh
import com.google.ar.sceneform.rendering.Vertex.UvCoordinate
import com.google.ar.sceneform.utilities.AndroidPreconditions
import io.github.sceneview.collision.Vector3
import java.util.concurrent.ExecutionException
import kotlin.math.cos
import kotlin.math.sin

/** Utility class used to dynamically construct [ModelRenderable]s for various shapes. */
@RequiresApi(api = Build.VERSION_CODES.N)
object ShapeFactory {
    private val TAG = ShapeFactory::class.java.simpleName
    private const val COORDS_PER_TRIANGLE = 3

    /**
     * Creates a [ModelRenderable] in the shape of a cube with the give specifications.
     *
     * @param size the size of the constructed cube
     * @param center the center of the constructed cube
     * @param material the material to use for rendering the cube
     * @return renderable representing a cube with the given parameters
     */
    @Suppress("AndroidApiChecker")
    // CompletableFuture requires api level 24
    @JvmStatic
    fun makeCube(engine: Engine, size: Vector3, center: Vector3, material: Material): ModelRenderable {
        AndroidPreconditions.checkMinAndroidApiLevel()

        val extents = size.scaled(0.5f)

        val p0 = Vector3.add(center, Vector3(-extents.x, -extents.y, extents.z))
        val p1 = Vector3.add(center, Vector3(extents.x, -extents.y, extents.z))
        val p2 = Vector3.add(center, Vector3(extents.x, -extents.y, -extents.z))
        val p3 = Vector3.add(center, Vector3(-extents.x, -extents.y, -extents.z))
        val p4 = Vector3.add(center, Vector3(-extents.x, extents.y, extents.z))
        val p5 = Vector3.add(center, Vector3(extents.x, extents.y, extents.z))
        val p6 = Vector3.add(center, Vector3(extents.x, extents.y, -extents.z))
        val p7 = Vector3.add(center, Vector3(-extents.x, extents.y, -extents.z))

        val up = Vector3.up()
        val down = Vector3.down()
        val front = Vector3.forward()
        val back = Vector3.back()
        val left = Vector3.left()
        val right = Vector3.right()

        val uv00 = UvCoordinate(0.0f, 0.0f)
        val uv10 = UvCoordinate(1.0f, 0.0f)
        val uv01 = UvCoordinate(0.0f, 1.0f)
        val uv11 = UvCoordinate(1.0f, 1.0f)

        val vertices = ArrayList(
            listOf(
                // Bottom
                Vertex.builder().setPosition(p0).setNormal(down).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p1).setNormal(down).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p2).setNormal(down).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p3).setNormal(down).setUvCoordinate(uv00).build(),
                // Left
                Vertex.builder().setPosition(p7).setNormal(left).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p4).setNormal(left).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p0).setNormal(left).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p3).setNormal(left).setUvCoordinate(uv00).build(),
                // Back
                Vertex.builder().setPosition(p4).setNormal(back).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p5).setNormal(back).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p1).setNormal(back).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p0).setNormal(back).setUvCoordinate(uv00).build(),
                // Front
                Vertex.builder().setPosition(p6).setNormal(front).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p7).setNormal(front).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p3).setNormal(front).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p2).setNormal(front).setUvCoordinate(uv00).build(),
                // Right
                Vertex.builder().setPosition(p5).setNormal(right).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p6).setNormal(right).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p2).setNormal(right).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p1).setNormal(right).setUvCoordinate(uv00).build(),
                // Top
                Vertex.builder().setPosition(p7).setNormal(up).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p6).setNormal(up).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p5).setNormal(up).setUvCoordinate(uv10).build(),
                Vertex.builder().setPosition(p4).setNormal(up).setUvCoordinate(uv00).build()
            )
        )

        val numSides = 6
        val verticesPerSide = 4
        val trianglesPerSide = 2

        val triangleIndices = ArrayList<Int>(numSides * trianglesPerSide * COORDS_PER_TRIANGLE)
        for (i in 0 until numSides) {
            // First triangle for this side.
            triangleIndices.add(3 + verticesPerSide * i)
            triangleIndices.add(1 + verticesPerSide * i)
            triangleIndices.add(0 + verticesPerSide * i)

            // Second triangle for this side.
            triangleIndices.add(3 + verticesPerSide * i)
            triangleIndices.add(2 + verticesPerSide * i)
            triangleIndices.add(1 + verticesPerSide * i)
        }

        val submesh = Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material.filamentMaterialInstance)
            .build(engine)

        val renderableDefinition = RenderableDefinition.builder()
            .setVertices(vertices)
            .setSubmeshes(listOf(submesh))
            .build(engine)

        val future = ModelRenderable.builder()
            .setSource(renderableDefinition)
            .build(engine)

        val result: ModelRenderable?
        try {
            result = future.get()
        } catch (ex: ExecutionException) {
            throw AssertionError("Error creating renderable.", ex)
        } catch (ex: InterruptedException) {
            throw AssertionError("Error creating renderable.", ex)
        }

        if (result == null) {
            throw AssertionError("Error creating renderable.")
        }

        return result
    }

    /**
     * Creates a [ModelRenderable] in the shape of a sphere with the give specifications.
     *
     * @param radius the radius of the constructed sphere
     * @param center the center of the constructed sphere
     * @param material the material to use for rendering the sphere
     * @return renderable representing a sphere with the given parameters
     */
    @Suppress("AndroidApiChecker")
    // CompletableFuture requires api level 24
    @JvmStatic
    fun makeSphere(engine: Engine, radius: Float, center: Vector3, material: Material): ModelRenderable {
        AndroidPreconditions.checkMinAndroidApiLevel()

        val stacks = 24
        val slices = 24

        // Create Vertices.
        val vertices = ArrayList<Vertex>((slices + 1) * stacks + 2)
        val pi = Math.PI.toFloat()
        val doublePi = pi * 2.0f

        for (stack in 0..stacks) {
            val phi = pi * stack.toFloat() / stacks
            val sinPhi = sin(phi)
            val cosPhi = cos(phi)

            for (slice in 0..slices) {
                val theta = doublePi * (if (slice == slices) 0 else slice).toFloat() / slices
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)

                var position = Vector3(sinPhi * cosTheta, cosPhi, sinPhi * sinTheta).scaled(radius)
                val normal = position.normalized()
                position = Vector3.add(position, center)
                val uvCoordinate = UvCoordinate(
                    1.0f - slice.toFloat() / slices,
                    1.0f - stack.toFloat() / stacks
                )

                val vertex = Vertex.builder()
                    .setPosition(position)
                    .setNormal(normal)
                    .setUvCoordinate(uvCoordinate)
                    .build()

                vertices.add(vertex)
            }
        }

        // Create triangles.
        val numFaces = vertices.size
        val numTriangles = numFaces * 2
        val numIndices = numTriangles * 3
        val triangleIndices = ArrayList<Int>(numIndices)

        var v = 0
        for (stack in 0 until stacks) {
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
            v += slices + 1
        }

        val submesh = Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material.filamentMaterialInstance)
            .build(engine)
        val renderableDefinition = RenderableDefinition.builder()
            .setVertices(vertices)
            .setSubmeshes(listOf(submesh))
            .build(engine)

        val future = ModelRenderable.builder()
            .setSource(renderableDefinition)
            .build(engine)

        val result: ModelRenderable?
        try {
            result = future.get()
        } catch (ex: ExecutionException) {
            throw AssertionError("Error creating renderable.", ex)
        } catch (ex: InterruptedException) {
            throw AssertionError("Error creating renderable.", ex)
        }

        if (result == null) {
            throw AssertionError("Error creating renderable.")
        }

        return result
    }

    /**
     * Creates a [ModelRenderable] in the shape of a cylinder with the give specifications.
     *
     * @param radius the radius of the constructed cylinder
     * @param height the height of the constructed cylinder
     * @param center the center of the constructed cylinder
     * @param material the material to use for rendering the cylinder
     * @return renderable representing a cylinder with the given parameters
     */
    @Suppress("AndroidApiChecker")
    // CompletableFuture requires api level 24
    @JvmStatic
    fun makeCylinder(
        engine: Engine,
        radius: Float,
        height: Float,
        center: Vector3,
        material: Material
    ): ModelRenderable {
        AndroidPreconditions.checkMinAndroidApiLevel()

        val numberOfSides = 24
        val halfHeight = height / 2
        val thetaIncrement = (2 * Math.PI).toFloat() / numberOfSides

        var theta = 0f
        val uStep = 1.0f / numberOfSides

        val vertices = ArrayList<Vertex>((numberOfSides + 1) * 4)
        val lowerCapVertices = ArrayList<Vertex>(numberOfSides + 1)
        val upperCapVertices = ArrayList<Vertex>(numberOfSides + 1)
        val upperEdgeVertices = ArrayList<Vertex>(numberOfSides + 1)

        // Generate vertices along the sides of the cylinder.
        for (side in 0..numberOfSides) {
            val cosTheta = cos(theta)
            val sinTheta = sin(theta)

            // Calculate edge vertices along bottom of cylinder
            val lowerPosition = Vector3(radius * cosTheta, -halfHeight, radius * sinTheta)
            var normal = Vector3(lowerPosition.x, 0f, lowerPosition.z).normalized()
            val lowerPositionWithCenter = Vector3.add(lowerPosition, center)
            var uvCoordinate = UvCoordinate(uStep * side, 0f)

            var vertex = Vertex.builder()
                .setPosition(lowerPositionWithCenter)
                .setNormal(normal)
                .setUvCoordinate(uvCoordinate)
                .build()
            vertices.add(vertex)

            // Create a copy of lower vertex with bottom-facing normals for cap.
            vertex = Vertex.builder()
                .setPosition(lowerPositionWithCenter)
                .setNormal(Vector3.down())
                .setUvCoordinate(UvCoordinate((cosTheta + 1f) / 2, (sinTheta + 1f) / 2))
                .build()
            lowerCapVertices.add(vertex)

            // Calculate edge vertices along top of cylinder
            val upperPosition = Vector3(radius * cosTheta, halfHeight, radius * sinTheta)
            normal = Vector3(upperPosition.x, 0f, upperPosition.z).normalized()
            val upperPositionWithCenter = Vector3.add(upperPosition, center)
            uvCoordinate = UvCoordinate(uStep * side, 1f)

            vertex = Vertex.builder()
                .setPosition(upperPositionWithCenter)
                .setNormal(normal)
                .setUvCoordinate(uvCoordinate)
                .build()
            upperEdgeVertices.add(vertex)

            // Create a copy of upper vertex with up-facing normals for cap.
            vertex = Vertex.builder()
                .setPosition(upperPositionWithCenter)
                .setNormal(Vector3.up())
                .setUvCoordinate(UvCoordinate((cosTheta + 1f) / 2, (sinTheta + 1f) / 2))
                .build()
            upperCapVertices.add(vertex)

            theta += thetaIncrement
        }
        vertices.addAll(upperEdgeVertices)

        // Generate vertices for the centers of the caps of the cylinder.
        val lowerCenterIndex = vertices.size
        vertices.add(
            Vertex.builder()
                .setPosition(Vector3.add(center, Vector3(0f, -halfHeight, 0f)))
                .setNormal(Vector3.down())
                .setUvCoordinate(UvCoordinate(.5f, .5f))
                .build()
        )
        vertices.addAll(lowerCapVertices)

        val upperCenterIndex = vertices.size
        vertices.add(
            Vertex.builder()
                .setPosition(Vector3.add(center, Vector3(0f, halfHeight, 0f)))
                .setNormal(Vector3.up())
                .setUvCoordinate(UvCoordinate(.5f, .5f))
                .build()
        )
        vertices.addAll(upperCapVertices)

        val triangleIndices = ArrayList<Int>()

        // Create triangles for each side
        for (side in 0 until numberOfSides) {
            val bottomLeft = side
            val bottomRight = side + 1
            val topLeft = side + numberOfSides + 1
            val topRight = side + numberOfSides + 2

            // First triangle of side.
            triangleIndices.add(bottomLeft)
            triangleIndices.add(topRight)
            triangleIndices.add(bottomRight)

            // Second triangle of side.
            triangleIndices.add(bottomLeft)
            triangleIndices.add(topLeft)
            triangleIndices.add(topRight)

            // Add bottom cap triangle.
            triangleIndices.add(lowerCenterIndex)
            triangleIndices.add(lowerCenterIndex + side + 1)
            triangleIndices.add(lowerCenterIndex + side + 2)

            // Add top cap triangle.
            triangleIndices.add(upperCenterIndex)
            triangleIndices.add(upperCenterIndex + side + 2)
            triangleIndices.add(upperCenterIndex + side + 1)
        }

        val submesh = Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material.filamentMaterialInstance)
            .build(engine)

        val renderableDefinition = RenderableDefinition.builder()
            .setVertices(vertices)
            .setSubmeshes(listOf(submesh))
            .build(engine)

        val future = ModelRenderable.builder()
            .setSource(renderableDefinition)
            .build(engine)

        val result: ModelRenderable?
        try {
            result = future.get()
        } catch (ex: ExecutionException) {
            throw AssertionError("Error creating renderable.", ex)
        } catch (ex: InterruptedException) {
            throw AssertionError("Error creating renderable.", ex)
        }

        if (result == null) {
            throw AssertionError("Error creating renderable.")
        }

        return result
    }
}

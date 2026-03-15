package com.google.ar.sceneform.rendering

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.ar.sceneform.utilities.AndroidPreconditions
import io.github.sceneview.collision.Vector3
import java.util.concurrent.ExecutionException

object PlaneFactory {
    private const val COORDS_PER_TRIANGLE = 3

    /**
     * Creates a [ModelRenderable] in the shape of a plane with the given specifications.
     *
     * @param size the size of the constructed plane
     * @param center the center of the constructed plane
     * @param material the material to use for rendering the plane
     * @return renderable representing a plane with the given parameters
     */
    @Suppress("AndroidApiChecker")
    // CompletableFuture requires api level 24
    @JvmStatic
    fun makePlane(engine: Engine, size: Vector3, center: Vector3, material: MaterialInstance): ModelRenderable {
        AndroidPreconditions.checkMinAndroidApiLevel()

        val extents = size.scaled(0.5f)

        val p0 = Vector3.add(center, Vector3(-extents.x, -extents.y, extents.z))
        val p1 = Vector3.add(center, Vector3(-extents.x, extents.y, -extents.z))
        val p2 = Vector3.add(center, Vector3(extents.x, extents.y, -extents.z))
        val p3 = Vector3.add(center, Vector3(extents.x, -extents.y, extents.z))

        val front = Vector3()

        val uv00 = Vertex.UvCoordinate(0.0f, 0.0f)
        val uv10 = Vertex.UvCoordinate(1.0f, 0.0f)
        val uv01 = Vertex.UvCoordinate(0.0f, 1.0f)
        val uv11 = Vertex.UvCoordinate(1.0f, 1.0f)

        val vertices = ArrayList(
            listOf(
                Vertex.builder().setPosition(p0).setNormal(front).setUvCoordinate(uv00).build(),
                Vertex.builder().setPosition(p1).setNormal(front).setUvCoordinate(uv01).build(),
                Vertex.builder().setPosition(p2).setNormal(front).setUvCoordinate(uv11).build(),
                Vertex.builder().setPosition(p3).setNormal(front).setUvCoordinate(uv10).build()
            )
        )

        val trianglesPerSide = 2

        val triangleIndices = ArrayList<Int>(trianglesPerSide * COORDS_PER_TRIANGLE)
        // First triangle.
        triangleIndices.add(3)
        triangleIndices.add(1)
        triangleIndices.add(0)

        // Second triangle.
        triangleIndices.add(3)
        triangleIndices.add(2)
        triangleIndices.add(1)

        val submesh = RenderableDefinition.Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material)
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

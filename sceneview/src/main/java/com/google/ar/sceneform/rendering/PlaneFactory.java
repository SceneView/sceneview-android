package com.google.ar.sceneform.rendering;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.AndroidPreconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import androidx.annotation.Nullable;

public class PlaneFactory {
    private static final int COORDS_PER_TRIANGLE = 3;

    /**
     * Creates a {@link ModelRenderable} in the shape of a plane with the given specifications.
     *
     * @param size     the size of the constructed plane
     * @param center   the center of the constructed plane
     * @param material the material to use for rendering the plane
     * @return renderable representing a plane with the given parameters
     */
    @SuppressWarnings("AndroidApiChecker")
    // CompletableFuture requires api level 24
    public static ModelRenderable makePlane(Vector3 size, Vector3 center, Material material) {
        AndroidPreconditions.checkMinAndroidApiLevel();

        Vector3 extents = size.scaled(0.5f);

        Vector3 p0 = Vector3.add(center, new Vector3(-extents.x, -extents.y, extents.z));
        Vector3 p1 = Vector3.add(center, new Vector3(-extents.x, extents.y, -extents.z));
        Vector3 p2 = Vector3.add(center, new Vector3(extents.x, extents.y, -extents.z));
        Vector3 p3 = Vector3.add(center, new Vector3(extents.x, -extents.y, extents.z));

        Vector3 front = new Vector3();

        Vertex.UvCoordinate uv00 = new Vertex.UvCoordinate(0.0f, 0.0f);
        Vertex.UvCoordinate uv10 = new Vertex.UvCoordinate(1.0f, 0.0f);
        Vertex.UvCoordinate uv01 = new Vertex.UvCoordinate(0.0f, 1.0f);
        Vertex.UvCoordinate uv11 = new Vertex.UvCoordinate(1.0f, 1.0f);

        ArrayList<Vertex> vertices = new ArrayList<>(
                Arrays.asList(
                        Vertex.builder().setPosition(p0).setNormal(front).setUvCoordinate(uv00).build(),
                        Vertex.builder().setPosition(p1).setNormal(front).setUvCoordinate(uv01).build(),
                        Vertex.builder().setPosition(p2).setNormal(front).setUvCoordinate(uv11).build(),
                        Vertex.builder().setPosition(p3).setNormal(front).setUvCoordinate(uv10).build()
                )
        );

        final int trianglesPerSide = 2;

        ArrayList<Integer> triangleIndices = new ArrayList<>(trianglesPerSide * COORDS_PER_TRIANGLE);
        // First triangle.
        triangleIndices.add(3);
        triangleIndices.add(1);
        triangleIndices.add(0);

        // Second triangle.
        triangleIndices.add(3);
        triangleIndices.add(2);
        triangleIndices.add(1);

        RenderableDefinition.Submesh submesh = RenderableDefinition.Submesh.builder()
                .setTriangleIndices(triangleIndices)
                .setMaterial(material)
                .build();

        RenderableDefinition renderableDefinition = RenderableDefinition.builder()
                .setVertices(vertices)
                .setSubmeshes(Arrays.asList(submesh))
                .build();

        CompletableFuture<ModelRenderable> future = ModelRenderable.builder()
                .setSource(renderableDefinition)
                .build();

        @Nullable ModelRenderable result;
        try {
            result = future.get();
        } catch (ExecutionException | InterruptedException ex) {
            throw new AssertionError("Error creating renderable.", ex);
        }

        if (result == null) {
            throw new AssertionError("Error creating renderable.");
        }

        return result;
    }
}

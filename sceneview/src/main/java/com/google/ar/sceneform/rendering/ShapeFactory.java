package com.google.ar.sceneform.rendering;

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.filament.Engine;
import io.github.sceneview.collision.Vector3;
import com.google.ar.sceneform.rendering.RenderableDefinition.Submesh;
import com.google.ar.sceneform.rendering.Vertex.UvCoordinate;
import com.google.ar.sceneform.utilities.AndroidPreconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/** Utility class used to dynamically construct {@link ModelRenderable}s for various shapes. */
@RequiresApi(api = Build.VERSION_CODES.N)
public final class ShapeFactory {
  private static final String TAG = ShapeFactory.class.getSimpleName();
  private static final int COORDS_PER_TRIANGLE = 3;

  /**
   * Creates a {@link ModelRenderable} in the shape of a cube with the give specifications.
   *
   * @param size the size of the constructed cube
   * @param center the center of the constructed cube
   * @param material the material to use for rendering the cube
   * @return renderable representing a cube with the given parameters
   */
  @SuppressWarnings("AndroidApiChecker")
  // CompletableFuture requires api level 24
  public static ModelRenderable makeCube(Engine engine, Vector3 size, Vector3 center, Material material) {
    AndroidPreconditions.checkMinAndroidApiLevel();

    Vector3 extents = size.scaled(0.5f);

    Vector3 p0 = Vector3.add(center, new Vector3(-extents.x, -extents.y, extents.z));
    Vector3 p1 = Vector3.add(center, new Vector3(extents.x, -extents.y, extents.z));
    Vector3 p2 = Vector3.add(center, new Vector3(extents.x, -extents.y, -extents.z));
    Vector3 p3 = Vector3.add(center, new Vector3(-extents.x, -extents.y, -extents.z));
    Vector3 p4 = Vector3.add(center, new Vector3(-extents.x, extents.y, extents.z));
    Vector3 p5 = Vector3.add(center, new Vector3(extents.x, extents.y, extents.z));
    Vector3 p6 = Vector3.add(center, new Vector3(extents.x, extents.y, -extents.z));
    Vector3 p7 = Vector3.add(center, new Vector3(-extents.x, extents.y, -extents.z));

    Vector3 up = Vector3.up();
    Vector3 down = Vector3.down();
    Vector3 front = Vector3.forward();
    Vector3 back = Vector3.back();
    Vector3 left = Vector3.left();
    Vector3 right = Vector3.right();

    UvCoordinate uv00 = new UvCoordinate(0.0f, 0.0f);
    UvCoordinate uv10 = new UvCoordinate(1.0f, 0.0f);
    UvCoordinate uv01 = new UvCoordinate(0.0f, 1.0f);
    UvCoordinate uv11 = new UvCoordinate(1.0f, 1.0f);

    ArrayList<Vertex> vertices =
        new ArrayList<>(
            Arrays.asList(
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
                Vertex.builder().setPosition(p4).setNormal(up).setUvCoordinate(uv00).build()));

    final int numSides = 6;
    final int verticesPerSide = 4;
    final int trianglesPerSide = 2;

    ArrayList<Integer> triangleIndices =
        new ArrayList<>(numSides * trianglesPerSide * COORDS_PER_TRIANGLE);
    for (int i = 0; i < numSides; i++) {
      // First triangle for this side.
      triangleIndices.add(3 + verticesPerSide * i);
      triangleIndices.add(1 + verticesPerSide * i);
      triangleIndices.add(0 + verticesPerSide * i);

      // Second triangle for this side.
      triangleIndices.add(3 + verticesPerSide * i);
      triangleIndices.add(2 + verticesPerSide * i);
      triangleIndices.add(1 + verticesPerSide * i);
    }

    Submesh submesh = Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material.filamentMaterialInstance)
            .build(engine);

    RenderableDefinition renderableDefinition = RenderableDefinition.builder()
            .setVertices(vertices)
            .setSubmeshes(Arrays.asList(submesh))
            .build(engine);

    CompletableFuture<ModelRenderable> future = ModelRenderable.builder()
            .setSource(renderableDefinition)
            .build(engine);

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

  /**
   * Creates a {@link ModelRenderable} in the shape of a sphere with the give specifications.
   *
   * @param radius the radius of the constructed sphere
   * @param center the center of the constructed sphere
   * @param material the material to use for rendering the sphere
   * @return renderable representing a sphere with the given parameters
   */
  @SuppressWarnings("AndroidApiChecker")
  // CompletableFuture requires api level 24
  public static ModelRenderable makeSphere(Engine engine, float radius, Vector3 center, Material material) {
    AndroidPreconditions.checkMinAndroidApiLevel();

    final int stacks = 24;
    final int slices = 24;

    // Create Vertices.
    ArrayList<Vertex> vertices = new ArrayList<>((slices + 1) * stacks + 2);
    float pi = (float) Math.PI;
    float doublePi = pi * 2.0f;

    for (int stack = 0; stack <= stacks; stack++) {
      float phi = pi * (float) stack / stacks;
      float sinPhi = (float) Math.sin(phi);
      float cosPhi = (float) Math.cos(phi);

      for (int slice = 0; slice <= slices; slice++) {
        float theta = doublePi * (float) (slice == slices ? 0 : slice) / slices;
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);

        Vector3 position = new Vector3(sinPhi * cosTheta, cosPhi, sinPhi * sinTheta).scaled(radius);
        Vector3 normal = position.normalized();
        position = Vector3.add(position, center);
        UvCoordinate uvCoordinate =
            new UvCoordinate(
                1.0f - ((float) slice / slices), 1.0f - ((float) stack / stacks));

        Vertex vertex =
            Vertex.builder()
                .setPosition(position)
                .setNormal(normal)
                .setUvCoordinate(uvCoordinate)
                .build();

        vertices.add(vertex);
      }
    }

    // Create triangles.
    int numFaces = vertices.size();
    int numTriangles = numFaces * 2;
    int numIndices = numTriangles * 3;
    ArrayList<Integer> triangleIndices = new ArrayList<>(numIndices);

    int v = 0;
    for (int stack = 0; stack < stacks; stack++) {
      for (int slice = 0; slice < slices; slice++) {
        // Skip triangles at the caps that would have an area of zero.
        boolean topCap = stack == 0;
        boolean bottomCap = stack == stacks - 1;

        int next = slice + 1;

        if (!topCap) {
          triangleIndices.add(v + slice);
          triangleIndices.add(v + next);
          triangleIndices.add(v + slice + slices + 1);
        }

        if (!bottomCap) {
          triangleIndices.add(v + next);
          triangleIndices.add(v + next + slices + 1);
          triangleIndices.add(v + slice + slices + 1);
        }
      }
      v += slices + 1;
    }

    Submesh submesh = Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material.filamentMaterialInstance)
            .build(engine);
    RenderableDefinition renderableDefinition = RenderableDefinition.builder()
            .setVertices(vertices)
            .setSubmeshes(Arrays.asList(submesh))
            .build(engine);

    CompletableFuture<ModelRenderable> future = ModelRenderable.builder()
            .setSource(renderableDefinition)
            .build(engine);

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

  /**
   * Creates a {@link ModelRenderable} in the shape of a cylinder with the give specifications.
   *
   * @param radius the radius of the constructed cylinder
   * @param height the height of the constructed cylinder
   * @param center the center of the constructed cylinder
   * @param material the material to use for rendering the cylinder
   * @return renderable representing a cylinder with the given parameters
   */
  @SuppressWarnings("AndroidApiChecker")
  // CompletableFuture requires api level 24
  public static ModelRenderable makeCylinder(Engine engine, float radius, float height,
                                             Vector3 center, Material material) {
    AndroidPreconditions.checkMinAndroidApiLevel();

    final int numberOfSides = 24;
    final float halfHeight = height / 2;
    final float thetaIncrement = (float) (2 * Math.PI) / numberOfSides;

    float theta = 0;
    float uStep = (float) 1.0 / numberOfSides;

    ArrayList<Vertex> vertices = new ArrayList<>((numberOfSides + 1) * 4);
    ArrayList<Vertex> lowerCapVertices = new ArrayList<>(numberOfSides + 1);
    ArrayList<Vertex> upperCapVertices = new ArrayList<>(numberOfSides + 1);
    ArrayList<Vertex> upperEdgeVertices = new ArrayList<>(numberOfSides + 1);

    // Generate vertices along the sides of the cylinder.
    for (int side = 0; side <= numberOfSides; side++) {
      float cosTheta = (float) Math.cos(theta);
      float sinTheta = (float) Math.sin(theta);

      // Calculate edge vertices along bottom of cylinder
      Vector3 lowerPosition = new Vector3(radius * cosTheta, -halfHeight, radius * sinTheta);
      Vector3 normal = new Vector3(lowerPosition.x, 0, lowerPosition.z).normalized();
      lowerPosition = Vector3.add(lowerPosition, center);
      UvCoordinate uvCoordinate = new UvCoordinate(uStep * side, 0);

      Vertex vertex =
          Vertex.builder()
              .setPosition(lowerPosition)
              .setNormal(normal)
              .setUvCoordinate(uvCoordinate)
              .build();
      vertices.add(vertex);

      // Create a copy of lower vertex with bottom-facing normals for cap.
      vertex =
          Vertex.builder()
              .setPosition(lowerPosition)
              .setNormal(Vector3.down())
              .setUvCoordinate(new UvCoordinate((cosTheta + 1f) / 2, (sinTheta + 1f) / 2))
              .build();
      lowerCapVertices.add(vertex);

      // Calculate edge vertices along top of cylinder
      Vector3 upperPosition = new Vector3(radius * cosTheta, halfHeight, radius * sinTheta);
      normal = new Vector3(upperPosition.x, 0, upperPosition.z).normalized();
      upperPosition = Vector3.add(upperPosition, center);
      uvCoordinate = new UvCoordinate(uStep * side, 1);

      vertex =
          Vertex.builder()
              .setPosition(upperPosition)
              .setNormal(normal)
              .setUvCoordinate(uvCoordinate)
              .build();
      upperEdgeVertices.add(vertex);

      // Create a copy of upper vertex with up-facing normals for cap.
      vertex =
          Vertex.builder()
              .setPosition(upperPosition)
              .setNormal(Vector3.up())
              .setUvCoordinate(new UvCoordinate((cosTheta + 1f) / 2, (sinTheta + 1f) / 2))
              .build();
      upperCapVertices.add(vertex);

      theta += thetaIncrement;
    }
    vertices.addAll(upperEdgeVertices);

    // Generate vertices for the centers of the caps of the cylinder.
    final int lowerCenterIndex = vertices.size();
    vertices.add(
        Vertex.builder()
            .setPosition(Vector3.add(center, new Vector3(0, -halfHeight, 0)))
            .setNormal(Vector3.down())
            .setUvCoordinate(new UvCoordinate(.5f, .5f))
            .build());
    vertices.addAll(lowerCapVertices);

    final int upperCenterIndex = vertices.size();
    vertices.add(
        Vertex.builder()
            .setPosition(Vector3.add(center, new Vector3(0, halfHeight, 0)))
            .setNormal(Vector3.up())
            .setUvCoordinate(new UvCoordinate(.5f, .5f))
            .build());
    vertices.addAll(upperCapVertices);

    ArrayList<Integer> triangleIndices = new ArrayList<>();

    // Create triangles for each side
    for (int side = 0; side < numberOfSides; side++) {
      int bottomLeft = side;
      int bottomRight = side + 1;
      int topLeft = side + numberOfSides + 1;
      int topRight = side + numberOfSides + 2;

      // First triangle of side.
      triangleIndices.add(bottomLeft);
      triangleIndices.add(topRight);
      triangleIndices.add(bottomRight);

      // Second triangle of side.
      triangleIndices.add(bottomLeft);
      triangleIndices.add(topLeft);
      triangleIndices.add(topRight);

      // Add bottom cap triangle.
      triangleIndices.add(lowerCenterIndex);
      triangleIndices.add(lowerCenterIndex + side + 1);
      triangleIndices.add(lowerCenterIndex + side + 2);

      // Add top cap triangle.
      triangleIndices.add(upperCenterIndex);
      triangleIndices.add(upperCenterIndex + side + 2);
      triangleIndices.add(upperCenterIndex + side + 1);
    }

    Submesh submesh = Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material.filamentMaterialInstance)
            .build(engine);

    RenderableDefinition renderableDefinition = RenderableDefinition.builder()
            .setVertices(vertices)
            .setSubmeshes(Arrays.asList(submesh))
            .build(engine);

    CompletableFuture<ModelRenderable> future = ModelRenderable.builder()
            .setSource(renderableDefinition)
            .build(engine);

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

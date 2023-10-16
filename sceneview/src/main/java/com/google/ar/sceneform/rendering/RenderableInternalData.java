package com.google.ar.sceneform.rendering;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.filament.Box;
import com.google.android.filament.Engine;
import com.google.android.filament.Entity;
import com.google.android.filament.EntityInstance;
import com.google.android.filament.IndexBuffer;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.VertexBuffer;
import io.github.sceneview.collision.Vector3;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the data used by a {@link Renderable} for rendering
 */
class RenderableInternalData implements IRenderableInternalData {
  private static final String TAG = RenderableInternalData.class.getSimpleName();

  /** Represents the data used to render each mesh of the renderable. */
  static class MeshData {
    // The start index into the triangle indices buffer for this mesh.
    int indexStart;
    // The end index into the triangle indices buffer for this mesh.
    int indexEnd;
  }

  // Geometry data.
  private final Vector3 centerAabb = Vector3.zero();
  private final Vector3 extentsAabb = Vector3.zero();

  // Transform data.
  private float transformScale = 1f;
  private final Vector3 transformOffset = Vector3.zero();

  // Raw buffers.
  @Nullable private IntBuffer rawIndexBuffer;
  @Nullable private FloatBuffer rawPositionBuffer;
  @Nullable private FloatBuffer rawTangentsBuffer;
  @Nullable private FloatBuffer rawUvBuffer;
  @Nullable private FloatBuffer rawColorBuffer;

  // Filament Geometry buffers.
  @Nullable private IndexBuffer indexBuffer;
  @Nullable private VertexBuffer vertexBuffer;

  // Represents the set of meshes to render.
  private final ArrayList<MeshData> meshes = new ArrayList<>();





  @Override
  public void setCenterAabb(Vector3 minAabb) {
    this.centerAabb.set(minAabb);
  }

  @Override
  public Vector3 getCenterAabb() {
    return new Vector3(centerAabb);
  }

  @Override
  public void setExtentsAabb(Vector3 maxAabb) {
    this.extentsAabb.set(maxAabb);
  }

  @Override
  public Vector3 getExtentsAabb() {
    return new Vector3(extentsAabb);
  }

  @Override
  public Vector3 getSizeAabb() {
    return extentsAabb.scaled(2.0f);
  }

  @Override
  public void setTransformScale(float scale) {
    this.transformScale = scale;
  }

  @Override
  public float getTransformScale() {
    return transformScale;
  }

  @Override
  public void setTransformOffset(Vector3 offset) {
    this.transformOffset.set(offset);
  }

  @Override
  public Vector3 getTransformOffset() {
    return new Vector3(transformOffset);
  }

  @Override
  public ArrayList<MeshData> getMeshes() {
    return meshes;
  }

  @Override
  public void setIndexBuffer(@Nullable IndexBuffer indexBuffer) {
    this.indexBuffer = indexBuffer;
  }

  @Override
  @Nullable
  public IndexBuffer getIndexBuffer() {
    return indexBuffer;
  }

  @Override
  public void setVertexBuffer(@Nullable VertexBuffer vertexBuffer) {
    this.vertexBuffer = vertexBuffer;
  }

  @Override
  @Nullable
  public VertexBuffer getVertexBuffer() {
    return vertexBuffer;
  }

  @Override
  public void setRawIndexBuffer(@Nullable IntBuffer rawIndexBuffer) {
    this.rawIndexBuffer = rawIndexBuffer;
  }

  @Override
  @Nullable
  public IntBuffer getRawIndexBuffer() {
    return rawIndexBuffer;
  }

  @Override
  public void setRawPositionBuffer(@Nullable FloatBuffer rawPositionBuffer) {
    this.rawPositionBuffer = rawPositionBuffer;
  }

  @Override
  @Nullable
  public FloatBuffer getRawPositionBuffer() {
    return rawPositionBuffer;
  }

  @Override
  public void setRawTangentsBuffer(@Nullable FloatBuffer rawTangentsBuffer) {
    this.rawTangentsBuffer = rawTangentsBuffer;
  }

  @Override
  @Nullable
  public FloatBuffer getRawTangentsBuffer() {
    return rawTangentsBuffer;
  }

  @Override
  public void setRawUvBuffer(@Nullable FloatBuffer rawUvBuffer) {
    this.rawUvBuffer = rawUvBuffer;
  }

  @Override
  @Nullable
  public FloatBuffer getRawUvBuffer() {
    return rawUvBuffer;
  }

  @Override
  public void setRawColorBuffer(@Nullable FloatBuffer rawColorBuffer) {
    this.rawColorBuffer = rawColorBuffer;
  }

  @Override
  @Nullable
  public FloatBuffer getRawColorBuffer() {
    return rawColorBuffer;
  }


  private void setupSkeleton(RenderableManager.Builder builder) {return ;}





  @Override
  public void buildInstanceData(Engine engine, RenderableInstance instance, @Entity int renderedEntity) {
    Renderable renderable = instance.getRenderable();
    IRenderableInternalData renderableData = renderable.getRenderableData();
    ArrayList<MaterialInstance> materialBindings = renderable.getMaterialBindings();
    RenderableManager renderableManager = engine.getRenderableManager();
    @EntityInstance int renderableInstance = renderableManager.getInstance(renderedEntity);

    // Determine if a new filament Renderable needs to be created.
    int meshCount = renderableData.getMeshes().size();
    if (renderableInstance == 0
            || renderableManager.getPrimitiveCount(renderableInstance) != meshCount) {
      // Destroy the old one if it exists.
      if (renderableInstance != 0) {
        try {
          renderableManager.destroy(renderedEntity);
        } catch (Exception e) {}
      }

      // Build the filament renderable.
      RenderableManager.Builder builder =
              new RenderableManager.Builder(meshCount)
                      .priority(renderable.getRenderPriority())
                      .castShadows(renderable.isShadowCaster())
                      .receiveShadows(renderable.isShadowReceiver());

      setupSkeleton(builder);

      builder.build(engine, renderedEntity);

      renderableInstance = renderableManager.getInstance(renderedEntity);
      if (renderableInstance == 0) {
        throw new AssertionError("Unable to create RenderableInstance.");
      }
    } else {
      renderableManager.setPriority(renderableInstance, renderable.getRenderPriority());
      renderableManager.setCastShadows(renderableInstance, renderable.isShadowCaster());
      renderableManager.setReceiveShadows(renderableInstance, renderable.isShadowReceiver());
    }

    // Update the bounding box.
    Vector3 extents = renderableData.getExtentsAabb();
    Vector3 center = renderableData.getCenterAabb();
    Box filamentBox = new Box(center.x, center.y, center.z, extents.x, extents.y, extents.z);
    renderableManager.setAxisAlignedBoundingBox(renderableInstance, filamentBox);

    if (materialBindings.size() != meshCount) {
      throw new AssertionError("Material Bindings are out of sync with meshes.");
    }

    // Update the geometry and material instances.
    final RenderableManager.PrimitiveType primitiveType = RenderableManager.PrimitiveType.TRIANGLES;
    for (int mesh = 0; mesh < meshCount; ++mesh) {
      // Update the geometry assigned to the filament renderable.
      MeshData meshData = renderableData.getMeshes().get(mesh);
      @Nullable VertexBuffer vertexBuffer = renderableData.getVertexBuffer();
      @Nullable IndexBuffer indexBuffer = renderableData.getIndexBuffer();
      if (vertexBuffer == null || indexBuffer == null) {
        throw new AssertionError("Internal Error: Failed to get vertex or index buffer");
      }
      renderableManager.setGeometryAt(
              renderableInstance,
              mesh,
              primitiveType,
              vertexBuffer,
              indexBuffer,
              meshData.indexStart,
              meshData.indexEnd - meshData.indexStart);

      // Update the material instances assigned to the filament renderable.
      MaterialInstance material = materialBindings.get(mesh);
      renderableManager.setMaterialInstanceAt(renderableInstance, mesh, material);
    }
  }

  @Override
  public void setAnimationNames(@NonNull List<String> animationNames) {}

  @NonNull
  @Override
  public List<String> getAnimationNames() {
    return Collections.emptyList();
  }
}

package com.google.ar.sceneform.rendering;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.filament.IndexBuffer;
import com.google.android.filament.VertexBuffer;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.UbershaderLoader;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.RenderableInternalData.MeshData;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Represents the data used by a {@link Renderable} for rendering natively loaded glTF data. */
@SuppressWarnings("AndroidJdkLibsChecker")
public class RenderableInternalFilamentAssetData implements IRenderableInternalData {

  Context context;
  Buffer gltfByteBuffer;
  boolean isGltfBinary;
  ResourceLoader resourceLoader;
  @Nullable Function<String, Uri> urlResolver;
  static UbershaderLoader ubershaderLoader;

  static UbershaderLoader getUberShaderLoader() {
    if(ubershaderLoader == null) {
      ubershaderLoader = new UbershaderLoader(EngineInstance.getEngine().getFilamentEngine());
    }
    return ubershaderLoader;
  }

  @Override
  public void setCenterAabb(Vector3 center) {
    // Not Implemented
  }

  @Override
  public Vector3 getCenterAabb() {
    // Not Implemented
    return Vector3.zero();
  }

  @Override
  public void setExtentsAabb(Vector3 halfExtents) {
    // Not Implemented
  }

  @Override
  public Vector3 getExtentsAabb() {
    throw new IllegalStateException("Not Implemented");
  }

  @Override
  public Vector3 getSizeAabb() {
    // Not Implemented
    return Vector3.zero();
  }

  @Override
  public void setTransformScale(float scale) {
    // Not Implemented
  }

  @Override
  public float getTransformScale() {
    // Not Implemented
    return 1.0f;
  }

  @Override
  public void setTransformOffset(Vector3 offset) {
    // Not Implemented
  }

  @Override
  public Vector3 getTransformOffset() {
    // Not Implemented
    return Vector3.zero();
  }

  @Override
  public ArrayList<MeshData> getMeshes() {
    // Not Implemented
    return new ArrayList<>(1);
  }

  public ArrayList<Integer> getMaterialBindingIds() {
    // Not Implemented
    return new ArrayList<>();
  }

  @Override
  public void setIndexBuffer(@Nullable IndexBuffer indexBuffer) {
    // Not Implemented
  }

  @Nullable
  @Override
  public IndexBuffer getIndexBuffer() {
    // Not Implemented
    return null;
  }

  @Override
  public void setVertexBuffer(@Nullable VertexBuffer vertexBuffer) {
    // Not Implemented
  }

  @Nullable
  @Override
  public VertexBuffer getVertexBuffer() {
    // Not Implemented
    return null;
  }

  @Override
  public void setRawIndexBuffer(@Nullable IntBuffer rawIndexBuffer) {
    // Not Implemented
  }

  @Nullable
  @Override
  public IntBuffer getRawIndexBuffer() {
    // Not Implemented
    return null;
  }

  @Override
  public void setRawPositionBuffer(@Nullable FloatBuffer rawPositionBuffer) {
    // Not Implemented
  }

  @Nullable
  @Override
  public FloatBuffer getRawPositionBuffer() {
    // Not Implemented
    return null;
  }

  @Override
  public void setRawTangentsBuffer(@Nullable FloatBuffer rawTangentsBuffer) {
    // Not Implemented
  }

  @Nullable
  @Override
  public FloatBuffer getRawTangentsBuffer() {
    // Not Implemented
    return null;
  }

  @Override
  public void setRawUvBuffer(@Nullable FloatBuffer rawUvBuffer) {
    // Not Implemented
  }

  @Nullable
  @Override
  public FloatBuffer getRawUvBuffer() {
    // Not Implemented
    return null;
  }

  @Override
  public void setRawColorBuffer(@Nullable FloatBuffer rawColorBuffer) {
    // Not Implemented
  }

  @Nullable
  @Override
  public FloatBuffer getRawColorBuffer() {
    // Not Implemented
    return null;
  }

  @Override
  public void setAnimationNames(@NonNull List<String> animationNames) {
    // Not Implemented
  }

  @Override
  @NonNull
  public List<String> getAnimationNames() {
    // Not Implemented
    return new ArrayList<>();
  }





















  @Override
  public void buildInstanceData(RenderableInstance instance, int renderedEntity) {
  }

  @Override
  public void dispose() {}
}

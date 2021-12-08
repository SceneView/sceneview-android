package com.google.ar.sceneform.rendering;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.filament.Entity;
import com.google.android.filament.IndexBuffer;
import com.google.android.filament.VertexBuffer;


import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.RenderableInternalData.MeshData;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;


// TODO: Split IRenderableInternalData into RenderableInternalSfbData and
// RenderableInternalDefinitionData
interface IRenderableInternalData {

  void setCenterAabb(Vector3 minAabb);

  Vector3 getCenterAabb();

  void setExtentsAabb(Vector3 maxAabb);

  Vector3 getExtentsAabb();

  Vector3 getSizeAabb();

  void setTransformScale(float scale);

  float getTransformScale();

  void setTransformOffset(Vector3 offset);

  Vector3 getTransformOffset();

  ArrayList<MeshData> getMeshes();

  void setIndexBuffer(@Nullable IndexBuffer indexBuffer);

  @Nullable
  IndexBuffer getIndexBuffer();

  void setVertexBuffer(@Nullable VertexBuffer vertexBuffer);

  @Nullable
  VertexBuffer getVertexBuffer();

  void setRawIndexBuffer(@Nullable IntBuffer rawIndexBuffer);

  @Nullable
  IntBuffer getRawIndexBuffer();

  void setRawPositionBuffer(@Nullable FloatBuffer rawPositionBuffer);

  @Nullable
  FloatBuffer getRawPositionBuffer();

  void setRawTangentsBuffer(@Nullable FloatBuffer rawTangentsBuffer);

  @Nullable
  FloatBuffer getRawTangentsBuffer();

  void setRawUvBuffer(@Nullable FloatBuffer rawUvBuffer);

  @Nullable
  FloatBuffer getRawUvBuffer();

  void setRawColorBuffer(@Nullable FloatBuffer rawColorBuffer);

  @Nullable
  FloatBuffer getRawColorBuffer();

  void setAnimationNames(@NonNull List<String> animationNames);

  @NonNull
  List<String> getAnimationNames();











  void buildInstanceData(RenderableInstance instance, @Entity int renderedEntity);
  /**
   * Removes any memory used by the object.
   *
   * @hide
   */
  void dispose();
}

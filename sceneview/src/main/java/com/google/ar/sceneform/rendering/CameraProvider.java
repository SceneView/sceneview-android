package com.google.ar.sceneform.rendering;

import com.google.ar.sceneform.common.TransformProvider;
import com.google.ar.sceneform.math.Matrix;

/**
 * Required interface for a virtual camera.
 *
 * @hide
 */
public interface CameraProvider extends TransformProvider {
  boolean isRendered();

  float getNearClipPlane();

  float getFarClipPlane();

  Matrix getViewMatrix();

  Matrix getProjectionMatrix();
}

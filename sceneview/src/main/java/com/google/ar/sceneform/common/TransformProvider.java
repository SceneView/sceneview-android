package com.google.ar.sceneform.common;

import com.google.ar.sceneform.math.Matrix;

/**
 * Interface for providing information about a 3D transformation. See {@link
 * com.google.ar.sceneform.Node}.
 *
 * @hide
 */
public interface TransformProvider {
  Matrix getTransformationMatrix();
}

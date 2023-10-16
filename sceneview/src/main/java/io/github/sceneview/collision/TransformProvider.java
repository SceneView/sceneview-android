package io.github.sceneview.collision;

/**
 * Interface for providing information about a 3D transformation.
 *
 * @hide
 */
public interface TransformProvider {
    Matrix getTransformationMatrix();
}

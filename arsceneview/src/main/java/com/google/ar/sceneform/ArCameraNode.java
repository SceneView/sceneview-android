package com.google.ar.sceneform;

import com.google.ar.core.Pose;
import com.google.ar.sceneform.utilities.Preconditions;

import dev.romainguy.kotlin.math.Float3;
import io.github.sceneview.ar.ArSceneView;
import io.github.sceneview.ar.arcore.PoseKt;
import io.github.sceneview.node.NodeParent;

/**
 * Represents a virtual camera, which determines the perspective through which the scene is viewed.
 *
 * <p>If the camera is part of an {@link ArSceneView}, then the camera automatically tracks the
 * camera pose from ARCore. Additionally, the following methods will throw {@link
 * UnsupportedOperationException} when called:
 *
 * <ul>
 *   <li>{@link #setParent(NodeParent)} - CameraNode's parent cannot be changed, it is always the scene.
 *   <li>{@link #setPosition(Float3)} - CameraNode's position cannot be changed, it is controlled
 *       by the ARCore camera pose.
 *   <li>{@link #setRotation(Float3)} - CameraNode's rotation cannot be changed, it is
 *       controlled by the ARCore camera pose.
 * </ul>
 * <p>
 * All other functionality in Node is supported. You can access the position and rotation of the
 * camera, assign a collision shape to the camera, or add children to the camera. Disabling the
 * camera turns off rendering.
 */
public class ArCameraNode extends CameraNode {

    @SuppressWarnings("initialization")
    public ArCameraNode() {
        super(false);
    }

    /**
     * Sets the vertical field of view for the non-ar camera in degrees. If this is an AR camera, then
     * the fov comes from ARCore and cannot be set so an exception is thrown. The default is 90
     * degrees.
     *
     * @throws UnsupportedOperationException if this is an AR camera
     */
    @Override
    public void setVerticalFovDegrees(float verticalFov) {
        throw new UnsupportedOperationException("Cannot set the field of view for AR cameras.");
    }

    /**
     * Gets the vertical field of view for the camera.
     *
     * <p>If this is an AR camera, then it is calculated based on the camera information from ARCore
     * and can vary between device. It can't be calculated until the first frame after the ARCore
     * session is resumed, in which case an IllegalStateException is thrown.
     *
     * <p>Otherwise, this will return the value set by {@link #setVerticalFovDegrees(float)}, with a
     * default of 90 degrees.
     *
     * @throws IllegalStateException if called before the first frame after ARCore is resumed
     */
    @Override
    public float getVerticalFovDegrees() {
        if (areMatricesInitialized) {
            double fovRadians = 2.0 * Math.atan(1.0 / projectionMatrix.data[5]);
            return (float) Math.toDegrees(fovRadians);
        } else {
            throw new IllegalStateException(
                    "Cannot get the field of view for AR cameras until the first frame after ARCore has "
                            + "been resumed.");
        }
    }


    /**
     * Updates the pose and projection of the camera to match the tracked pose from ARCore.
     *
     * @hide Called internally as part of the integration with ARCore, should not be called directly.
     */
    public void updateTrackedPose(com.google.ar.core.Camera camera) {
        Preconditions.checkNotNull(camera, "Parameter \"camera\" was null.");

        // Update the projection matrix.
        camera.getProjectionMatrix(projectionMatrix.data, 0, nearPlane, farPlane);

        // Update the view matrix.
        camera.getViewMatrix(getViewMatrix().data, 0);

        // Update the node's transformation properties to match the tracked pose.
        Pose pose = camera.getDisplayOrientedPose();
        super.setPosition(PoseKt.getPosition(pose));
        super.setQuaternion(PoseKt.getQuaternion(pose));

        areMatricesInitialized = true;
    }

    // Only used if this camera is not controlled by ARCore.
    @Override
    public void refreshProjectionMatrix() {
    }
}

package io.github.sceneview.ar.node

import com.google.android.filament.Engine
import com.google.ar.core.Camera
import dev.romainguy.kotlin.math.degrees
import io.github.sceneview.ar.arcore.transform
import io.github.sceneview.math.toTransform
import io.github.sceneview.node.CameraNode2
import kotlin.math.atan

/**
 * Represents a virtual camera, which determines the perspective through which the scene is viewed.
 *
 *
 * If the camera is part of an [ArSceneView], then the camera automatically tracks the
 * camera pose from ARCore. Additionally, the following methods will throw [ ] when called:
 *
 *
 *  * [.setParent] - CameraNode's parent cannot be changed, it is always the scene.
 *  * [.setPosition] - CameraNode's position cannot be changed, it is controlled
 * by the ARCore camera pose.
 *  * [.setRotation] - CameraNode's rotation cannot be changed, it is
 * controlled by the ARCore camera pose.
 *
 *
 *
 * All other functionality in Node is supported. You can access the position and rotation of the
 * camera, assign a collision shape to the camera, or add children to the camera. Disabling the
 * camera turns off rendering.
 */
class ArCameraNode2(engine: Engine) : CameraNode2(engine, false) {

    override var verticalFovDegrees: Float
        get() {
            val fovRadians = 2.0f * atan(1.0f / projectionTransform.y.x)
            return degrees(fovRadians)
        }
        set(_) {}

    /**
     * Updates the pose and projection of the camera to match the tracked pose from ARCore.
     *
     * Called internally as part of the integration with ARCore, should not be called directly.
     */
    fun updateTrackedPose(camera: Camera) {
        // Update the projection matrix.
        projectionTransform = FloatArray(16).apply {
            camera.getProjectionMatrix(
                this, 0,
                nearClipPlane, farClipPlane
            )
        }.toTransform()

        // Update the view matrix.
        camera.getViewMatrix(viewMatrix.data, 0)

        // Update the node's transformation properties to match the tracked pose.
//        if (camera.trackingState == TrackingState.TRACKING) {
        val cameraTransform = camera.displayOrientedPose.transform
        if (transform != cameraTransform) {
            transform = cameraTransform
        }
//        }
    }
}
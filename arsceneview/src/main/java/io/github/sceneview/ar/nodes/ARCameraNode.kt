package io.github.sceneview.ar.nodes

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.ar.core.*
import dev.romainguy.kotlin.math.Mat4
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ARFrame
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.ar.components.ARComponent
import io.github.sceneview.ar.getProjectionMatrix
import io.github.sceneview.components.FilamentCamera
import io.github.sceneview.managers.NodeManager
import io.github.sceneview.math.toMat4
import io.github.sceneview.nodes.CameraNode
import io.github.sceneview.utils.FrameTime

/**
 * Represents a virtual camera, which determines the perspective through which the scene is viewed
 *
 * If the camera is part of an [ARSceneView], then the camera automatically tracks the camera pose
 * from ARCore
 *
 * You can access the position and rotation of the camera, assign a collision shape to the camera,
 * or add children to the camera in order to make the node follow the screen.
 *
 * Disabling the camera turns off rendering.
 */
open class ARCameraNode(
    engine: Engine,
    nodeManager: NodeManager,
    entity: Entity = EntityManager.get().create(),
    camera: FilamentCamera.() -> Unit = {},
    var onUpdated: ((pose: Pose?) -> Unit)? = null
) : CameraNode(engine, nodeManager, entity, camera), ARComponent {

    override var near: Float = 0.01f
    override val far: Float = 30.0f



    /**
     * Gets the vertical field of view for the camera.
     *
     * If this is an AR camera, then it is calculated based on the camera information from ARCore
     * and can vary between device. It can't be calculated until the first frame after the ARCore
     * session is resumed, in which case an IllegalStateException is thrown.
     *
     *
     * Otherwise, this will return the value set by [.setVerticalFovDegrees], with a
     * default of 90 degrees.
     *
     * @throws IllegalStateException if called before the first frame after ARCore is resumed
     */
    /**
     * Sets the vertical field of view for the non-ar camera in degrees. If this is an AR camera, then
     * the fov comes from ARCore and cannot be set so an exception is thrown. The default is 90
     * degrees.
     *
     * @throws UnsupportedOperationException if this is an AR camera
     */
    var verticalFovDegrees: Float
        get() = if (areMatricesInitialized) {
            val fovRadians = 2.0 * Math.atan(1.0 / projectionMatrix.data.get(5))
            Math.toDegrees(fovRadians).toFloat()
        } else {
            throw IllegalStateException(
                "Cannot get the field of view for AR cameras until the first frame after ARCore has "
                        + "been resumed."
            )
        }
        set(verticalFov) {
            throw UnsupportedOperationException("Cannot set the field of view for AR cameras.")
        }

    constructor(
        sceneView: SceneView,
        entity: Entity = EntityManager.get().create(),
        camera: FilamentCamera.() -> Unit = {},
        onUpdated: ((pose: Pose?) -> Unit)? = null
    ) : this(sceneView.engine, sceneView.nodeManager, entity, camera, onUpdated)

    fun onARFrame(frameTime: FrameTime, frame: ARFrame) {
        val camera = frame.camera

        setCustomProjection(camera.getProjection(near, far), near, far)
        modelTransform = camera.displayOrientedPose.transform
    }
    setCustomProjection()
    val arProjectionMatrix = FloatArray(16).apply {
        frame.getProjectionMatrix(this, 0, camera.near, far)
    }
    frame.camera.getProjectionMatrix()
    // Update the projection matrix.
    camera.getProjectionMatrix(projectionMatrix.data , 0, nearPlane, farPlane)
}


/**
 * Updates the pose and projection of the camera to match the tracked pose from ARCore.
 *
 * @hide Called internally as part of the integration with ARCore, should not be called directly.
 */
fun updateTrackedPose(camera: Camera) {
    Preconditions.checkNotNull(camera, "Parameter \"camera\" was null.")


    // Update the view matrix.
    camera.getViewMatrix(getViewMatrix().data, 0)

    // Update the node's transformation properties to match the tracked pose.
    val pose: Pose = camera.displayOrientedPose
    super.position = pose.position
    super.quaternion = pose.quaternion
    areMatricesInitialized = true
}

// Only used if this camera is not controlled by ARCore.
fun refreshProjectionMatrix() {}
}

fun Camera.getProjection(near: Float, far: Float): Mat4 =
    FloatArray(16).apply { getProjectionMatrix(this, 0, near, far) }.toMat4()
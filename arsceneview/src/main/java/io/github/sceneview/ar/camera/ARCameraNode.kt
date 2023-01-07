package io.github.sceneview.ar.camera

import com.google.android.filament.Engine
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import dev.romainguy.kotlin.math.Mat4
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ARFrame
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.transform
import io.github.sceneview.ar.components.ARComponent
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
    camera: FilamentCamera.() -> Unit = {
        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we define a light that has the same intensity as the sun, it guarantees a
        // proper exposure
        setExposure(16.0f, 1.0f / 125.0f, 100.0f)
    }
) : CameraNode(engine, nodeManager, camera), ARComponent {

    override var near: Float = 0.01f
    override var far: Float = 30.0f

    /**
     * The virtual camera pose in world space for rendering AR content onto the latest frame
     *
     * This is an OpenGL camera pose with +X pointing right, +Y pointing up, and -Z pointing in the
     * direction the camera is looking, with "right" and "up" being relative to current logical
     * display orientation.
     *
     * @see viewMatrix to conveniently compute the OpenGL View Matrix.
     * @see
     *   <li>{@link #getPose()} for the physical pose of the camera. It will differ by a local
     *       rotation about the Z axis by a multiple of 90&deg;.
     *   <li>{@link com.google.ar.core.Frame#getAndroidSensorPose() Frame#getAndroidSensorPose()} for
     *       the pose of the android sensor frame. It will differ in both orientation and location.
     *   <li>{@link com.google.ar.core.Session#setDisplayGeometry(int,int,int)
     *       Session#setDisplayGeometry(int, int, int)} to update the display rotation.
     * </ul>
     *
     * Note: This pose is only useful when {@link #getTrackingState()} returns {@link
     * com.google.ar.core.TrackingState#TRACKING } and otherwise should not be used.
     */
    override var pose: Pose? = null
        set(value) {
            field = value
            // Change the camera.modelTransform instead of transform to keep transform being applied
            // to the ARCameraStream renderable entity.
            value?.transform?.let { modelMatrix = it }
        }

    /**
     * Retrieves the camera's projection matrix. The projection matrix used for rendering always has
     * its far plane set to infinity. This is why it may differ from the matrix set through
     * setProjection() or setLensProjection().
     *
     * Transform containing the camera's projection as a column-major matrix.
     */
    override var projectionMatrix: Mat4
        get() = super.projectionMatrix
        set(value) {
            setCustomProjection(value, near, far)
        }

//    /**
//     * Gets the vertical field of view for the camera.
//     *
//     * If this is an AR camera, then it is calculated based on the camera information from ARCore
//     * and can vary between device. It can't be calculated until the first frame after the ARCore
//     * session is resumed, in which case an IllegalStateException is thrown.
//     *
//     *
//     * Otherwise, this will return the value set by [.setVerticalFovDegrees], with a
//     * default of 90 degrees.
//     *
//     * @throws IllegalStateException if called before the first frame after ARCore is resumed
//     */
//    /**
//     * Sets the vertical field of view for the non-ar camera in degrees. If this is an AR camera, then
//     * the fov comes from ARCore and cannot be set so an exception is thrown. The default is 90
//     * degrees.
//     *
//     * @throws UnsupportedOperationException if this is an AR camera
//     */
//    var verticalFovDegrees: Float
//        get() = if (areMatricesInitialized) {
//            val fovRadians = 2.0 * Math.atan(1.0 / projectionMatrix.data.get(5))
//            Math.toDegrees(fovRadians).toFloat()
//        } else {
//            throw IllegalStateException(
//                "Cannot get the field of view for AR cameras until the first frame after ARCore has "
//                        + "been resumed."
//            )
//        }
//        set(verticalFov) {
//            throw UnsupportedOperationException("Cannot set the field of view for AR cameras.")
//        }

    constructor(
        sceneView: SceneView,
        camera: FilamentCamera.() -> Unit = {}
    ) : this(sceneView.engine, sceneView.nodeManager, camera)

    override fun onARFrame(frameTime: FrameTime, frame: ARFrame) {
        val arCamera = frame.camera
        projectionMatrix = arCamera.getProjection(near, far)
        pose = arCamera.displayOrientedPose
    }
}

fun Camera.getProjection(near: Float, far: Float): Mat4 =
    FloatArray(16).apply { getProjectionMatrix(this, 0, near, far) }.toMat4()
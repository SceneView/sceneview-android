package io.github.sceneview.node

import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.Camera
import com.google.ar.sceneform.collision.Ray
import com.google.ar.sceneform.math.MathHelper
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.utilities.Preconditions
import dev.romainguy.kotlin.math.*
import io.github.sceneview.Filament
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toMat4

/**
 * ### Represents a virtual camera, which determines the perspective through which the scene is
 * viewed
 *
 * // TODO: Move this text to ArCameraNode
 * If the camera is part of an ArSceneView, then the camera automatically tracks the
 * camera pose from ARCore. Additionally, the following methods will throw [ ] when called:
 *
 * [parent] - Camera's parent cannot be changed, it is always the scene.
 *  * [position] - Camera's position cannot be changed, it is controlled
 * by the ARCore camera pose.
 *  * [rotation] - Camera's rotation cannot be changed, it is
 * controlled by the ARCore camera pose.
 *  * [position] - Camera's position cannot be changed, it is controlled
 * by the ARCore camera pose.
 *  * [rotation] - Camera's rotation cannot be changed, it is
 * controlled by the ARCore camera pose.
 *
 * All other functionality in Node is supported. You can access the position and rotation of the
 * camera, assign a collision shape to the camera, or add children to the camera. Disabling the
 * camera turns off rendering.
 */
open class CameraNode : Node {

    val camera: Camera by lazy {
        Filament.engine.createCamera(Filament.entityManager.create()).apply {
            // Set the exposure on the camera, this exposure follows the sunny f/16 rule
            // Since we've defined a light that has the same intensity as the sun, it
            // guarantees a proper exposure
            setExposure(DEFAULT_APERTURE, DEFAULT_SHUTTER_SPEED, DEFAULT_SENSITIVITY)
        }
    }

    /**
     * ### Distance in world units from the camera to the near plane
     *
     * The near plane's position in view space is `z = -near`
     * Precondition:
     * - `near > 0` for [com.google.android.filament.Camera.Projection.PERSPECTIVE]
     * or
     * - `near != far` for [com.google.android.filament.Camera.Projection.ORTHO]
     */
    var nearPlane = DEFAULT_NEAR_PLANE
        set(value) {
            field = value
            updateProjection()
        }

    /**
     * ### Distance in world units from the camera to the far plane
     *
     * The far plane's position in view space is `z = -far`
     * Precondition:
     * - `far > near` for [com.google.android.filament.Camera.Projection.PERSPECTIVE]
     * or
     * - `far != near` for [com.google.android.filament.Camera.Projection.ORTHO]
     */
    var farPlane = DEFAULT_FAR_PLANE
        set(value) {
            field = value
            updateProjection()
        }

    /**
     * ### The camera's projection matrix
     *
     * The projection matrix used for rendering always has its far plane set to infinity.
     * This is why it may differ from the matrix set through [Camera.setProjection] or
     * [Camera.setLensProjection].
     *
     * @return A Mat4 containing the camera's projection as a column-major matrix
     */
    val projectionMatrix: Mat4
        get() = camera.getProjectionMatrix(null).map { it.toFloat() }.toFloatArray().toMat4()

    /**
     * ### The camera's model matrix
     *
     * The view matrix is the inverse of the model matrix.
     *
     * @return A Mat4 containing the camera's view as a column-major matrix
     */
    val viewMatrix: Mat4
        get() = camera.getViewMatrix(null as? FloatArray?).toMat4()

    /**
     * ### Thethe camera's model matrix
     *
     * The model matrix encodes the camera position and orientation, or pose.
     *
     * @return A Mat4 containing the camera's pose as a column-major matrix
     */
    val modelMatrix: Mat4
        get() = camera.getModelMatrix(null as? FloatArray?).toMat4()

    /**
     * ### The transform from the world coordinate system to the coordinate system of the screen
     */
    private val worldToScreen: Transform
        get() = projectionMatrix * viewMatrix

    // TODO: Move this text to ArCameraNode

    private var verticalFov = DEFAULT_VERTICAL_FOV_DEGREES
    protected var areMatricesInitialized = false

    /**
     * ### Focal length in millimeters
     * `focalLength > 0`
     */
    var focalLength = DEFAULT_FOCAL_LENGTH
        set(value) {
            field = value
            updateProjection()
        }

    /**
     * ### Construct a [CameraNode]
     *
     * @param position See [LightNode.position]
     * @param direction See [LightNode.direction]
     */
    constructor() : super(DEFAULT_POSITION, DEFAULT_ROTATION) {
        updateProjection()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        super.onSurfaceChanged(width, height)

        updateProjection()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Filament.engine.destroyCameraComponent(camera.entity)
        Filament.entityManager.destroy(camera.entity)
    }

    private fun updateProjection() {
        doOnAttachedToScene { sceneView ->
            val width = sceneView.view.viewport.width
            val height = sceneView.view.viewport.height
            val aspect = width.toDouble() / height.toDouble()
            camera.setLensProjection(focalLength.toDouble(), aspect, nearPlane, farPlane)
        }
    }

    fun motionEventToRay(motionEvent: MotionEvent): Ray {
        Preconditions.checkNotNull(motionEvent, "Parameter \"motionEvent\" was null.")
        val index = motionEvent.actionIndex
        return screenRay(motionEvent.getX(index), motionEvent.getY(index))
    }

    /**
     * Calculates a ray in world space going from the near-plane of the camera and going through a
     * point in screen space. Screen space is in Android device screen coordinates: TopLeft = (0, 0)
     * BottomRight = (Screen Width, Screen Height) The device coordinate space is unaffected by the
     * orientation of the device.
     *
     * @param x X position in device screen coordinates.
     * @param y Y position in device screen coordinates.
     */
    fun screenRay(x: Float, y: Float): Ray {
        //TODO : Move to kotlin-math
        val startPoint = Vector3()
        val endPoint = Vector3()
        unproject(x, y, 0.0f, startPoint)
        unproject(x, y, 1.0f, endPoint)
        val direction = Vector3.subtract(endPoint, startPoint)
        return Ray(startPoint, direction)
    }

    /**
     * ### Convert a point from world space into screen space.
     *
     * The X value is negative when the point is left of the viewport, between 0 and the width of
     * the [SceneView] when the point is within the viewport, and greater than the width when
     * the point is to the right of the viewport.
     *
     * The Y value is negative when the point is below the viewport, between 0 and the height of
     * the [SceneView] when the point is within the viewport, and greater than the height when
     * the point is above the viewport.
     *
     * The Z value is always 0 since the return value is a 2D coordinate.
     *
     * @param worldPosition the point in world space to convert
     * @return (X, Y) vector that represents the point in screen ([SceneView]) space in pixels
     * or null if the camera is not yet added to a [SceneView]
     */
    fun screenPosition(worldPosition: Position): Float2? {
        return sceneView?.let { sceneView ->
            val clipSpacePosition = worldToScreen * Float4(worldPosition, 1f)
            clipSpacePosition.xy = clipSpacePosition.xy / clipSpacePosition.w
            Float2(
                x = (clipSpacePosition.x + 1.0f) / 2.0f * sceneView.width,
                y = (1.0f - clipSpacePosition.y) / 2.0f * sceneView.height
            )
        }
        // TODO : Delete when confirm working - Sceneform:
//        val m = Matrix()
//        Matrix.multiply(projectionMatrix, getViewMatrix(), m)
//
//        val viewWidth: Int = getViewWidth()
//        val viewHeight: Int = getViewHeight()
//        val x: Float = point.x
//        val y: Float = point.y
//        val z: Float = point.z
//        var w = 1.0f
//
//        // Multiply the world point.
//
//        // Multiply the world point.
//        val screenPoint = Vector3()
//        screenPoint.x = x * m.data[0] + y * m.data[4] + z * m.data[8] + w * m.data[12]
//        screenPoint.y = x * m.data[1] + y * m.data[5] + z * m.data[9] + w * m.data[13]
//        w = x * m.data[3] + y * m.data[7] + z * m.data[11] + w * m.data[15]
//
//        // To clipping space.
//
//        // To clipping space.
//        screenPoint.x = (screenPoint.x / w + 1.0f) * 0.5f
//        screenPoint.y = (screenPoint.y / w + 1.0f) * 0.5f
//
//        // To screen space.
//
//        // To screen space.
//        screenPoint.x = screenPoint.x * viewWidth
//        screenPoint.y = screenPoint.y * viewHeight
//
//        // Invert Y because screen Y points down and Sceneform Y points up.
//
//        // Invert Y because screen Y points down and Sceneform Y points up.
//        screenPoint.y = viewHeight - screenPoint.y
//
//        return screenPoint
    }

    override fun onTransformChanged() {
        super.onTransformChanged()
    }

//    /**
//     * @hide Used to explicitly set the projection matrix for testing.
//     */
//    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
//    fun setProjectionMatrix(matrix: Matrix) {
//        projectionMatrix.set(matrix.data)
//    }

    private fun unproject(x: Float, y: Float, z: Float, dest: Vector3): Boolean {
        var x = x
        var y = y
        var z = z
        Preconditions.checkNotNull(dest, "Parameter \"dest\" was null.")
        val m = Matrix()
        Matrix.multiply(projectionMatrix, getViewMatrix(), m)
        Matrix.invert(m, m)
        val viewWidth = viewWidth
        val viewHeight = viewHeight

        // Invert Y because screen Y points down and Sceneform Y points up.
        y = viewHeight - y

        // Normalize between -1 and 1.
        x = x / viewWidth * 2.0f - 1.0f
        y = y / viewHeight * 2.0f - 1.0f
        z = 2.0f * z - 1.0f
        var w = 1.0f
        dest.x = x * m.data[0] + y * m.data[4] + z * m.data[8] + w * m.data[12]
        dest.y = x * m.data[1] + y * m.data[5] + z * m.data[9] + w * m.data[13]
        dest.z = x * m.data[2] + y * m.data[6] + z * m.data[10] + w * m.data[14]
        w = x * m.data[3] + y * m.data[7] + z * m.data[11] + w * m.data[15]
        if (MathHelper.almostEqualRelativeAndAbs(w, 0.0f)) {
            dest[0f, 0f] = 0f
            return false
        }
        w = 1.0f / w
        dest.set(dest.scaled(w))
        return true
    }

//    private val viewWidth: Int
//        private get() {
//            val scene = sceneView
//            return if (scene == null || EngineInstance.isHeadlessMode()) {
//                FALLBACK_VIEW_WIDTH
//            } else scene.width
//        }
//    private val viewHeight: Int
//        private get() {
//            val scene = sceneView
//            return if (scene == null || EngineInstance.isHeadlessMode()) {
//                FALLBACK_VIEW_HEIGHT
//            } else scene.height
//        }

//    protected fun refreshProjectionMatrix() {
//        val width = viewWidth
//        val height = viewHeight
//        if (width == 0 || height == 0) {
//            return
//        }
//        val aspect = width.toFloat() / height.toFloat()
//        setPerspective(verticalFov, aspect, nearPlane, farPlane)
//    }

//    /**
//     * Set the camera perspective based on the field of view, aspect ratio, near and far planes.
//     * verticalFovInDegrees must be greater than zero and less than 180 degrees. far - near must be
//     * greater than zero. aspect must be greater than zero. near and far must be greater than zero.
//     *
//     * @param verticalFovInDegrees vertical field of view in degrees.
//     * @param aspect               aspect ratio of the viewport, which is widthInPixels / heightInPixels.
//     * @param near                 distance in world units from the camera to the near plane, default is 0.1f
//     * @param far                  distance in world units from the camera to the far plane, default is 100.0f
//     * @throws IllegalArgumentException if any of the following preconditions are not met:
//     *
//     *  * 0 < verticalFovInDegrees < 180
//     *  * aspect > 0
//     *  * near > 0
//     *  * far > near
//     *
//     */
//    private fun setPerspective(
//        verticalFovInDegrees: Float,
//        aspect: Float,
//        near: Float,
//        far: Float
//    ) {
//        require(!(verticalFovInDegrees <= 0.0f || verticalFovInDegrees >= 180.0f)) { "Parameter \"verticalFovInDegrees\" is out of the valid range of (0, 180) degrees." }
//        require(aspect > 0.0f) { "Parameter \"aspect\" must be greater than zero." }
//        val fovInRadians = Math.toRadians(verticalFovInDegrees.toDouble())
//        val top = Math.tan(fovInRadians * 0.5).toFloat() * near
//        val bottom = -top
//        val right = top * aspect
//        val left = -right
//        setPerspective(left, right, bottom, top, near, far)
//    }
//
//    /**
//     * Set the camera perspective projection in terms of six clip planes. right - left must be greater
//     * than zero. top - bottom must be greater than zero. far - near must be greater than zero. near
//     * and far must be greater than zero.
//     *
//     * @param left   offset in world units from the camera to the left plane, at the near plane.
//     * @param right  offset in world units from the camera to the right plane, at the near plane.
//     * @param bottom offset in world units from the camera to the bottom plane, at the near plane.
//     * @param top    offset in world units from the camera to the top plane, at the near plane.
//     * @param near   distance in world units from the camera to the near plane, default is 0.1f
//     * @param far    distance in world units from the camera to the far plane, default is 100.0f
//     * @throws IllegalArgumentException if any of the following preconditions are not met:
//     *
//     *  * left != right
//     *  * bottom != top
//     *  * near > 0
//     *  * far > near
//     *
//     */
//    private fun setPerspective(
//        left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float
//    ) {
//        val data = projectionMatrix.data
//        require(!(left == right || bottom == top || near <= 0.0f || far <= near)) {
//            ("Invalid parameters to setPerspective, valid values: "
//                    + " width != height, bottom != top, near > 0.0f, far > near")
//        }
//        val reciprocalWidth = 1.0f / (right - left)
//        val reciprocalHeight = 1.0f / (top - bottom)
//        val reciprocalDepthRange = 1.0f / (far - near)
//
//        // Right-handed, column major 4x4 matrix.
//        data[0] = 2.0f * near * reciprocalWidth
//        data[1] = 0.0f
//        data[2] = 0.0f
//        data[3] = 0.0f
//        data[4] = 0.0f
//        data[5] = 2.0f * near * reciprocalHeight
//        data[6] = 0.0f
//        data[7] = 0.0f
//        data[8] = (right + left) * reciprocalWidth
//        data[9] = (top + bottom) * reciprocalHeight
//        data[10] = -(far + near) * reciprocalDepthRange
//        data[11] = -1.0f
//        data[12] = 0.0f
//        data[13] = 0.0f
//        data[14] = -2.0f * far * near * reciprocalDepthRange
//        data[15] = 0.0f
//        nearPlane = near
//        farPlane = far
//        areMatricesInitialized = true
//    }

    companion object {
        private val DEFAULT_POSITION = Float3(0.0f, 0.0f, 0.0f)
        private val DEFAULT_QUATERNION = Quaternion()

        private const val DEFAULT_NEAR_PLANE = 0.05 // 5 cm
        private const val DEFAULT_FAR_PLANE = 1000.0 // 1 km
        private const val DEFAULT_FOCAL_LENGTH = 28.0f

        private const val DEFAULT_APERTURE = 16.0f
        private const val DEFAULT_SHUTTER_SPEED = 1.0f / 125.0f
        private const val DEFAULT_SENSITIVITY = 100.0f

//        const val DEFAULT_NEAR_PLANE = 0.01f
//        const val DEFAULT_FAR_PLANE = 30.0f
//        const val FALLBACK_VIEW_WIDTH = 1920
//        const val FALLBACK_VIEW_HEIGHT = 1080
//
//        // Default vertical field of view
//        private const val DEFAULT_VERTICAL_FOV_DEGREES = 90.0f
    }

    init {
        position = DEFAULT_POSITION
        quaternion = DEFAULT_QUATERNION
        Preconditions.checkNotNull(scene, "Parameter \"scene\" was null.")
        super.parent = scene
        if (isFixed) {
            scene.addOnLayoutChangeListener { v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int -> refreshProjectionMatrix() }
        }
    }
}
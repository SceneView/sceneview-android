package io.github.sceneview.ar.node

import io.github.sceneview.node.Node.position
import io.github.sceneview.node.Node.quaternion
import io.github.sceneview.node.Node.parent
import io.github.sceneview.node.Node.getTransformationMatrix
import io.github.sceneview.node.Node.onTransformChanged
import io.github.sceneview.node.Node.sceneView
import io.github.sceneview.SceneView
import com.google.ar.sceneform.rendering.CameraProvider
import io.github.sceneview.node.CameraNode
import android.view.MotionEvent
import android.view.View
import androidx.annotation.VisibleForTesting
import com.google.ar.sceneform.collision.Ray
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.MathHelper
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.utilities.Preconditions
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ArSceneLifecycle
import io.github.sceneview.ar.ArSceneView

/**
 * ### Represents a virtual camera, which determines the perspective through which the scene is
 * viewed
 *
 * The camera automatically tracks the camera pose from ARCore.
 * Camera's parent, position and rotation cannot be changed, it is always the scene and the ones
 * coming from ARCore camera [com.google.ar.core.Pose]
 */
class ArCameraNode(sceneView: ArSceneView, val lifecycle: ArSceneLifecycle) : CameraNode(sceneView, lifecycle) {
    protected val viewMatrix = Matrix()
    protected val projectionMatrix = Matrix()
    protected var nearPlane = DEFAULT_NEAR_PLANE
    protected var farPlane = DEFAULT_FAR_PLANE
    private var verticalFov = DEFAULT_VERTICAL_FOV_DEGREES
    protected var areMatricesInitialized = false

    /**
     * @hide
     */
    fun setNearClipPlane(nearPlane: Float) {
        this.nearPlane = nearPlane

        // If this is an ArCamera, the projection matrix gets re-created when updateTrackedPose is
        // called every frame. Otherwise, update it now.
        if (isFixed) {
            refreshProjectionMatrix()
        }
    }

    override fun getNearClipPlane(): Float {
        return nearPlane
    }

    /**
     * @hide
     */
    fun setFarClipPlane(farPlane: Float) {
        this.farPlane = farPlane

        // If this is an ArCamera, the projection matrix gets re-created when updateTrackedPose is
        // called every frame. Otherwise, update it now.
        if (isFixed) {
            refreshProjectionMatrix()
        }
    }
    /**
     * Gets the vertical field of view for the camera.
     *
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
        get() = verticalFov
        set(verticalFov) {
            this.verticalFov = verticalFov
            if (isFixed) {
                refreshProjectionMatrix()
            }
        }

    override fun getFarClipPlane(): Float {
        return farPlane
    }

    /**
     * @hide Used internally (b/113516741)
     */
    override fun getViewMatrix(): Matrix {
        Matrix.invert(transformationMatrix, viewMatrix)
        return viewMatrix
    }

    /**
     * @hide Used internally (b/113516741) and within rendering package
     */
    override fun getProjectionMatrix(): Matrix {
        return projectionMatrix
    }

    fun motionEventToRay(motionEvent: MotionEvent): Ray {
        Preconditions.checkNotNull(motionEvent, "Parameter \"motionEvent\" was null.")
        val index = motionEvent.actionIndex
        return screenPointToRay(motionEvent.getX(index), motionEvent.getY(index))
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
    fun screenPointToRay(x: Float, y: Float): Ray {
        val startPoint = Vector3()
        val endPoint = Vector3()
        unproject(x, y, 0.0f, startPoint)
        unproject(x, y, 1.0f, endPoint)
        val direction = Vector3.subtract(endPoint, startPoint)
        return Ray(startPoint, direction)
    }

    /**
     * Convert a point from world space into screen space.
     *
     *
     * The X value is negative when the point is left of the viewport, between 0 and the width of
     * the [SceneView] when the point is within the viewport, and greater than the width when
     * the point is to the right of the viewport.
     *
     *
     * The Y value is negative when the point is below the viewport, between 0 and the height of
     * the [SceneView] when the point is within the viewport, and greater than the height when
     * the point is above the viewport.
     *
     *
     * The Z value is always 0 since the return value is a 2D coordinate.
     *
     * @param point the point in world space to convert
     * @return a new vector that represents the point in screen-space.
     */
    fun worldToScreenPoint(point: Vector3): Vector3 {
        val m = Matrix()
        Matrix.multiply(projectionMatrix, getViewMatrix(), m)
        val viewWidth = viewWidth
        val viewHeight = viewHeight
        val x = point.x
        val y = point.y
        val z = point.z
        var w = 1.0f

        // Multiply the world point.
        val screenPoint = Vector3()
        screenPoint.x = x * m.data[0] + y * m.data[4] + z * m.data[8] + w * m.data[12]
        screenPoint.y = x * m.data[1] + y * m.data[5] + z * m.data[9] + w * m.data[13]
        w = x * m.data[3] + y * m.data[7] + z * m.data[11] + w * m.data[15]

        // To clipping space.
        screenPoint.x = (screenPoint.x / w + 1.0f) * 0.5f
        screenPoint.y = (screenPoint.y / w + 1.0f) * 0.5f

        // To screen space.
        screenPoint.x = screenPoint.x * viewWidth
        screenPoint.y = screenPoint.y * viewHeight

        // Invert Y because screen Y points down and Sceneform Y points up.
        screenPoint.y = viewHeight - screenPoint.y
        return screenPoint
    }

    override fun onTransformChanged() {
        super.onTransformChanged()
    }

    /**
     * @hide Used to explicitly set the projection matrix for testing.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun setProjectionMatrix(matrix: Matrix) {
        projectionMatrix.set(matrix.data)
    }

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

    private val viewWidth: Int
        private get() {
            val scene = sceneView
            return if (scene == null || EngineInstance.isHeadlessMode()) {
                FALLBACK_VIEW_WIDTH
            } else scene.width
        }
    private val viewHeight: Int
        private get() {
            val scene = sceneView
            return if (scene == null || EngineInstance.isHeadlessMode()) {
                FALLBACK_VIEW_HEIGHT
            } else scene.height
        }

    protected fun refreshProjectionMatrix() {
        val width = viewWidth
        val height = viewHeight
        if (width == 0 || height == 0) {
            return
        }
        val aspect = width.toFloat() / height.toFloat()
        setPerspective(verticalFov, aspect, nearPlane, farPlane)
    }

    /**
     * Set the camera perspective based on the field of view, aspect ratio, near and far planes.
     * verticalFovInDegrees must be greater than zero and less than 180 degrees. far - near must be
     * greater than zero. aspect must be greater than zero. near and far must be greater than zero.
     *
     * @param verticalFovInDegrees vertical field of view in degrees.
     * @param aspect               aspect ratio of the viewport, which is widthInPixels / heightInPixels.
     * @param near                 distance in world units from the camera to the near plane, default is 0.1f
     * @param far                  distance in world units from the camera to the far plane, default is 100.0f
     * @throws IllegalArgumentException if any of the following preconditions are not met:
     *
     *  * 0 < verticalFovInDegrees < 180
     *  * aspect > 0
     *  * near > 0
     *  * far > near
     *
     */
    private fun setPerspective(
        verticalFovInDegrees: Float,
        aspect: Float,
        near: Float,
        far: Float
    ) {
        require(!(verticalFovInDegrees <= 0.0f || verticalFovInDegrees >= 180.0f)) { "Parameter \"verticalFovInDegrees\" is out of the valid range of (0, 180) degrees." }
        require(aspect > 0.0f) { "Parameter \"aspect\" must be greater than zero." }
        val fovInRadians = Math.toRadians(verticalFovInDegrees.toDouble())
        val top = Math.tan(fovInRadians * 0.5).toFloat() * near
        val bottom = -top
        val right = top * aspect
        val left = -right
        setPerspective(left, right, bottom, top, near, far)
    }

    /**
     * Set the camera perspective projection in terms of six clip planes. right - left must be greater
     * than zero. top - bottom must be greater than zero. far - near must be greater than zero. near
     * and far must be greater than zero.
     *
     * @param left   offset in world units from the camera to the left plane, at the near plane.
     * @param right  offset in world units from the camera to the right plane, at the near plane.
     * @param bottom offset in world units from the camera to the bottom plane, at the near plane.
     * @param top    offset in world units from the camera to the top plane, at the near plane.
     * @param near   distance in world units from the camera to the near plane, default is 0.1f
     * @param far    distance in world units from the camera to the far plane, default is 100.0f
     * @throws IllegalArgumentException if any of the following preconditions are not met:
     *
     *  * left != right
     *  * bottom != top
     *  * near > 0
     *  * far > near
     *
     */
    private fun setPerspective(
        left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float
    ) {
        val data = projectionMatrix.data
        require(!(left == right || bottom == top || near <= 0.0f || far <= near)) {
            ("Invalid parameters to setPerspective, valid values: "
                    + " width != height, bottom != top, near > 0.0f, far > near")
        }
        val reciprocalWidth = 1.0f / (right - left)
        val reciprocalHeight = 1.0f / (top - bottom)
        val reciprocalDepthRange = 1.0f / (far - near)

        // Right-handed, column major 4x4 matrix.
        data[0] = 2.0f * near * reciprocalWidth
        data[1] = 0.0f
        data[2] = 0.0f
        data[3] = 0.0f
        data[4] = 0.0f
        data[5] = 2.0f * near * reciprocalHeight
        data[6] = 0.0f
        data[7] = 0.0f
        data[8] = (right + left) * reciprocalWidth
        data[9] = (top + bottom) * reciprocalHeight
        data[10] = -(far + near) * reciprocalDepthRange
        data[11] = -1.0f
        data[12] = 0.0f
        data[13] = 0.0f
        data[14] = -2.0f * far * near * reciprocalDepthRange
        data[15] = 0.0f
        nearPlane = near
        farPlane = far
        areMatricesInitialized = true
    }

    companion object {
        private val DEFAULT_POSITION = Float3(0.0f, 0.0f, 0.0f)
        private val DEFAULT_QUATERNION = Quaternion()
        const val DEFAULT_NEAR_PLANE = 0.01f
        const val DEFAULT_FAR_PLANE = 30.0f
        const val FALLBACK_VIEW_WIDTH = 1920
        const val FALLBACK_VIEW_HEIGHT = 1080

        // Default vertical field of view for non-ar camera.
        private const val DEFAULT_VERTICAL_FOV_DEGREES = 90.0f
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
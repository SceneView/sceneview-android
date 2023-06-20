package io.github.sceneview.node

import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.Camera
import com.google.ar.sceneform.collision.Ray
import com.google.ar.sceneform.math.MathHelper
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.utilities.Preconditions
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.Filament.engine
import io.github.sceneview.Filament.entityManager
import io.github.sceneview.createCamera
import io.github.sceneview.utils.FrameTime
import java.util.*

/**
 * Represents a virtual camera, which determines the perspective through which the scene is viewed.
 *
 *
 * If the camera is part of an ArSceneView, then the camera automatically tracks the
 * camera pose from ARCore. Additionally, the following methods will throw [ ] when called:
 *
 *
 *  * [.setParent] - CameraNode's parent cannot be changed, it is always the scene.
 *  * [.setPosition] - CameraNode's position cannot be changed, it is controlled
 * by the ARCore camera pose.
 *  * [.setRotation] - CameraNode's rotation cannot be changed, it is
 * controlled by the ARCore camera pose.
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
 *
 * @param isFixed true to use with AR
 */
open class CameraNode(val isFixed: Boolean = true) : Node() {

    val projectionMatrix = Matrix()
    val viewMatrix
        get() = Matrix().apply {
            Matrix.invert(transformationMatrix, this)
        }

    var camera: Camera = engine.createCamera().apply {
        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we define a light that has the same intensity as the sun, it guarantees a
        // proper exposure
        setExposure(16.0f, 1.0f / 125.0f, 100.0f)
    }

    var nearClipPlane: Float = DEFAULT_NEAR_PLANE
        set(value) {
            if (field != value) {
                field = value

                // If this is an ArCamera, the projection matrix gets re-created when updateTrackedPose
                // is called every frame. Otherwise, update it now.
                if (isFixed) {
                    refreshProjectionMatrix()
                }
            }
        }

    var farClipPlane: Float = DEFAULT_FAR_PLANE
        set(value) {
            if (field != value) {
                field = value

                // If this is an ArCamera, the projection matrix gets re-created when updateTrackedPose is
                // called every frame. Otherwise, update it now.
                if (isFixed) {
                    refreshProjectionMatrix()
                }
            }
        }

    /**
     * Vertical field of view for the camera.
     *
     * - Set vertical field of view of a non-ar camera in degrees.
     * - If this is an AR camera, then the fov comes from ARCore and cannot be set. It will be
     * calculated based on the camera information from ARCore and can vary between device.
     * It can't be calculated until the first frame after the ARCore session is resumed, in which
     * case an IllegalStateException is thrown.
     *
     */
    open var verticalFovDegrees: Float = DEFAULT_VERTICAL_FOV_DEGREES
        set(value) {
            if (field != value) {
                field = value
                if (isFixed) {
                    refreshProjectionMatrix()
                }
            }
        }

    private val viewWidth get() = sceneView?.width ?: FALLBACK_VIEW_WIDTH
    private val viewHeight get() = sceneView?.height ?: FALLBACK_VIEW_HEIGHT

    protected var areMatricesInitialized = false
    private var lastTransform = FloatArray(16)
    private var lastProjectionMatrix = FloatArray(16)

    init {
        position = DEFAULT_POSITION
        quaternion = DEFAULT_QUATERNION
    }

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)

        val transform = transformationMatrix.data
        if (!Arrays.equals(transform, lastTransform)) {
            lastTransform = transform
            camera.setModelMatrix(transform)
        }
        val projectionMatrix = projectionMatrix.data
        if (!Arrays.equals(projectionMatrix, lastProjectionMatrix)) {
            lastProjectionMatrix = projectionMatrix
            val projectionMatrixDouble = DoubleArray(projectionMatrix.size)
            for (i in projectionMatrix.indices) {
                projectionMatrixDouble[i] = projectionMatrix[i].toDouble()
            }
            camera.setCustomProjection(
                projectionMatrixDouble,
                nearClipPlane.toDouble(),
                farClipPlane.toDouble()
            )
        }
    }// If this is an ArCamera, the projection matrix gets re-created when updateTrackedPose is
    // called every frame. Otherwise, update it now.


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
        unproject(Vector3(x, y, 0.0f), startPoint)
        unproject(Vector3(x, y, 1.0f), endPoint)
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
        // TODO : Move to Kotlin-Math
        val m = Matrix()
        Matrix.multiply(projectionMatrix, viewMatrix, m)
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

    private fun unproject(point: Vector3, dest: Vector3): Boolean {
        var x = point.x
        var y = point.y
        var z = point.z
        Preconditions.checkNotNull(dest, "Parameter \"dest\" was null.")
        val m = Matrix()
        Matrix.multiply(projectionMatrix, viewMatrix, m)
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

    fun refreshProjectionMatrix() {
        if (viewWidth > 0 && viewHeight > 0) {
            val aspect = viewWidth.toFloat() / viewHeight.toFloat()
            setPerspective(verticalFovDegrees, aspect, nearClipPlane, farClipPlane)
        }
    }

    /**
     * Set the camera perspective based on the field of view, aspect ratio, near and far planes.
     * verticalFovInDegrees must be greater than zero and less than 180 degrees. far - near must be
     * greater than zero. aspect must be greater than zero. near and far must be greater than zero.
     *
     * @param verticalFov vertical field of view in degrees.
     * @param aspect aspect ratio of the viewport, which is widthInPixels / heightInPixels.
     * @param near distance in world units from the camera to the near plane, default is 0.1f
     * @param far  distance in world units from the camera to the far plane, default is 100.0f
     * @throws IllegalArgumentException if any of the following preconditions are not met:
     *
     *  * 0 < verticalFovInDegrees < 180
     *  * aspect > 0
     *  * near > 0
     *  * far > near
     *
     */
    private fun setPerspective(verticalFov: Float, aspect: Float, near: Float, far: Float) {
        // TODO: Move to
        val fovInRadians = Math.toRadians(verticalFov.toDouble())
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
        nearClipPlane = near
        farClipPlane = far
        areMatricesInitialized = true
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        try {
            engine.destroyCameraComponent(camera.entity)
        } catch (e: Exception) {
        }
        try {
            entityManager.destroy(camera.entity)
        } catch (e: Exception) {
        }
    }

    companion object {
        private val DEFAULT_POSITION = Float3(0.0f, 0.0f, 1.0f)
        private val DEFAULT_QUATERNION = Quaternion()
        const val DEFAULT_NEAR_PLANE = 0.01f
        const val DEFAULT_FAR_PLANE = 30.0f
        const val FALLBACK_VIEW_WIDTH = 1920
        const val FALLBACK_VIEW_HEIGHT = 1080

        // Default vertical field of view for non-ar camera.
        private const val DEFAULT_VERTICAL_FOV_DEGREES = 90.0f
    }
}
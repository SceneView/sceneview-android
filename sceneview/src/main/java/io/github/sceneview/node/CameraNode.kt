package io.github.sceneview.node

import android.util.Size
import android.view.MotionEvent
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.Entity
import io.github.sceneview.collision.MathHelper
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.Preconditions
import io.github.sceneview.collision.Ray
import io.github.sceneview.collision.Vector3
import io.github.sceneview.components.CameraComponent
import io.github.sceneview.math.toMatrix
import io.github.sceneview.safeDestroyCamera

private const val kFocalLength = 28.0
private const val kNearPlane = 0.05f     // 5 cm
private const val kFarPlane = 1000.0f    // 1 km
private const val kAperture = 16.0f
private const val kShutterSpeed = 1.0f / 125.0f
private const val kSensitivity = 100.0f

private const val kFallbackViewWidth = 1920
private const val kFallbackViewHeight = 1080

/**
 * Represents a virtual camera, which determines the perspective through which the scene is viewed.
 *
 * All other functionality in Node is supported. You can access the position and rotation of the
 * camera, assign a collision shape to it, or add children to it. Disabling the camera turns off
 * rendering.
 */
open class CameraNode(
    engine: Engine,
    viewSize: Size,
    entity: Entity = EntityManager.get().create().apply {
        engine.createCamera(this).apply {
            // Set the exposure on the camera, this exposure follows the sunny f/16 rule
            // Since we define a light that has the same intensity as the sun, it guarantees a
            // proper exposure
            setExposure(kAperture, kShutterSpeed, kSensitivity)
        }
    }
) : Node(engine, entity), CameraComponent {

    override var isTouchable = false
    override var isEditable = false

    override var viewSize: Size = viewSize
        set(value) {
            field = value
            updateProjection(viewSize = viewSize)
        }

    init {
        transform(position = DEFAULT_POSITION, quaternion = DEFAULT_QUATERNION)
        updateProjection(
            focalLength = kFocalLength,
            viewSize = viewSize,
            near = kNearPlane,
            far = kFarPlane
        )
    }

    fun motionEventToRay(motionEvent: MotionEvent): Ray {
        Preconditions.checkNotNull(motionEvent, "Parameter \"motionEvent\" was null.")
        val index = motionEvent.actionIndex
        return screenPointToRay(motionEvent.getX(index), motionEvent.getY(index))
    }

    /**
     * Calculates a ray in world space going from the near-plane of the camera and going through a
     * point in screen space.
     *
     * Screen space is in Android device screen coordinates: TopLeft = (0, 0),  BottomRight =
     * (Screen Width, Screen Height).
     * The device coordinate space is unaffected by the orientation of the device.
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
     * The X value is negative when the point is left of the viewport, between 0 and the width of
     * the [io.github.sceneview.SceneView] when the point is within the viewport, and greater than
     * the width when the point is to the right of the viewport.
     *
     *
     * The Y value is negative when the point is below the viewport, between 0 and the height of
     * the [io.github.sceneview.SceneView] when the point is within the viewport, and greater than
     * the height when the point is above the viewport.
     *
     * The Z value is always 0 since the return value is a 2D coordinate.
     *
     * @param point the point in world space to convert
     * @return a new vector that represents the point in screen-space.
     */
    fun worldToScreenPoint(point: Vector3): Vector3 {
        // TODO : Move to Kotlin-Math
        val m = Matrix()
        Matrix.multiply(projectionTransform.toMatrix(), viewTransform.toMatrix(), m)
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
        screenPoint.x = screenPoint.x * viewSize.width
        screenPoint.y = screenPoint.y * viewSize.height

        // Invert Y because screen Y points down and Sceneform Y points up.
        screenPoint.y = viewSize.height - screenPoint.y
        return screenPoint
    }

    private fun unproject(point: Vector3, dest: Vector3): Boolean {
        var x = point.x
        var y = point.y
        var z = point.z
        Preconditions.checkNotNull(dest, "Parameter \"dest\" was null.")
        val m = Matrix()
        Matrix.multiply(projectionTransform.toMatrix(), viewTransform.toMatrix(), m)
        Matrix.invert(m, m)

        // Invert Y because screen Y points down and Sceneform Y points up.
        y = viewSize.height - y

        // Normalize between -1 and 1.
        x = x / viewSize.width * 2.0f - 1.0f
        y = y / viewSize.height * 2.0f - 1.0f
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

    override fun destroy() {
        super.destroy()

        engine.safeDestroyCamera(camera)
    }

    companion object {
        private val DEFAULT_POSITION = Float3(0.0f, 0.0f, 1.0f)
        private val DEFAULT_QUATERNION = Quaternion()

        // Default vertical field of view for non-ar camera.
        private const val DEFAULT_VERTICAL_FOV_DEGREES = 90.0f
    }
}
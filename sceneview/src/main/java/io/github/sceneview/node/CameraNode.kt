package io.github.sceneview.node

import android.util.Size
import android.view.MotionEvent
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import io.github.sceneview.Entity
import io.github.sceneview.collision.MathHelper
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.Preconditions
import io.github.sceneview.collision.Ray
import io.github.sceneview.collision.Vector3
import io.github.sceneview.components.CameraComponent
import io.github.sceneview.math.toMatrix
import io.github.sceneview.safeDestroyCamera

/**
 * Represents a virtual camera, which determines the perspective through which the scene is viewed.
 *
 * All other functionality in Node is supported. You can access the position and rotation of the
 * camera, assign a collision shape to it, or add children to it. Disabling the camera turns off
 * rendering.
 */
open class CameraNode(
    engine: Engine,
    entity: Entity,
    /**
     * The parent node.
     *
     * If set to null, this node will not be attached.
     *
     * The local position, rotation, and scale of this node will remain the same.
     * Therefore, the world position, rotation, and scale of this node may be different after the
     * parent changes.
     */
    parent: Node? = null
) : Node(engine, entity, parent), CameraComponent {

    // No rendered object
    final override var isTouchable = false

    // Can receive touchable but not editable child events
    override var isEditable = false

    private var _focalLength = 28.0
    override var focalLength: Double
        get() = super.focalLength
        set(value) {
            _focalLength = value
            updateLensProjection()
        }

    // Near = 5 cm
    private var _near = 0.05f
    override var near: Float
        get() = super.near
        set(value) {
            _near = value
            updateLensProjection()
        }

    // Far = 1 km
    private var _far = 1000.0f
    override var far: Float
        get() = super.far
        set(value) {
            _far = value
            updateLensProjection()
        }

    var viewSize: Size? = null
        set(value) {
            field = value
            updateLensProjection()
        }

    constructor(
        engine: Engine,
        entity: Entity = EntityManager.get().create(),
        /**
         * The parent node.
         *
         * If set to null, this node will not be attached.
         *
         * The local position, rotation, and scale of this node will remain the same.
         * Therefore, the world position, rotation, and scale of this node may be different after the
         * parent changes.
         */
        parent: Node? = null,
        camera: Camera.() -> Unit = {}
    ) : this(engine, entity, parent) {
        engine.createCamera(entity).apply(camera)
    }

    /**
     * Sets the projection matrix from the focal length.
     */
    fun updateLensProjection() {
        val viewSize = viewSize ?: return

        val aspect = viewSize.width.toDouble() / viewSize.height.toDouble()
        camera.setLensProjection(
            _focalLength,
            if (!aspect.isNaN()) aspect else 1.0,
            _near.toDouble(),
            _far.toDouble()
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
        screenPoint.x = screenPoint.x * viewSize!!.width
        screenPoint.y = screenPoint.y * viewSize!!.height

        // Invert Y because screen Y points down and Sceneform Y points up.
        screenPoint.y = viewSize!!.height - screenPoint.y
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
        y = viewSize!!.height - y

        // Normalize between -1 and 1.
        x = x / viewSize!!.width * 2.0f - 1.0f
        y = y / viewSize!!.height * 2.0f - 1.0f
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
}
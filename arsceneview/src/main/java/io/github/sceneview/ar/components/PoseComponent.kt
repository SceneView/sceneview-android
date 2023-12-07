package io.github.sceneview.ar.components

import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import io.github.sceneview.ar.arcore.createAnchor
import io.github.sceneview.ar.arcore.transform
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.components.TransformComponent

/**
 * Represents an immutable rigid transformation from one coordinate space to another.
 *
 * As provided from all ARCore APIs, Poses always describe the transformation from object's local
 * coordinate space to the **world coordinate space** (see below). That is, Poses from ARCore APIs
 * can be thought of as equivalent to OpenGL model matrices.
 *
 * The transformation is defined using a quaternion rotation about the origin followed by a
 * translation.
 *
 * Coordinate system is right-handed, like OpenGL conventions.
 *
 * Translation units are meters.
 *
 * ### World Coordinate Space
 *
 * As ARCore's understanding of the environment changes, it adjusts its model of the world to
 * keep things consistent. When this happens, the numerical location (coordinates) of the camera and
 * [Anchor]s can change significantly to maintain appropriate relative positions of the physical
 * locations they represent.
 *
 * These changes mean that every frame should be considered to be in a completely unique world
 * coordinate space. The numerical coordinates of anchors and the camera should never be used
 * outside the rendering frame during which they were retrieved. If a position needs to be
 * considered beyond the scope of a single rendering frame, either an anchor should be created or a
 * position relative to a nearby existing anchor should be used.
 */
interface PoseComponent : TransformComponent {

    /**
     * The position of the intersection between a ray and detected real-world geometry.
     *
     * The position is the location in space where the ray intersected the geometry.
     * The orientation is a best effort to face the user's device, and its exact definition differs
     * depending on the Trackable that was hit.
     *
     * - [Plane]: X+ is perpendicular to the cast ray and parallel to the plane, Y+ points along the
     * plane normal (up, for [Plane.Type.HORIZONTAL_UPWARD_FACING] planes), and Z+ is parallel to
     * the plane, pointing roughly toward the user's device.
     *
     * - [Point]: Attempt to estimate the normal of the surface centered around the hit test.
     * Surface normal estimation is most likely to succeed on textured surfaces and with camera
     * motion. If [Point.getOrientationMode] returns
     * [Point.OrientationMode.ESTIMATED_SURFACE_NORMAL], then X+ is perpendicular to the cast ray
     * and parallel to the physical surface centered around the hit test, Y+ points along the
     * estimated surface normal, and Z+ points roughly toward the user's device.
     * If [Point.getOrientationMode] returns [Point.OrientationMode.INITIALIZED_TO_IDENTITY], then
     * X+ is perpendicular to the cast ray and points right from the perspective of the user's
     * device, Y+ points up, and Z+ points roughly toward the user's device.
     *
     * - If you wish to retain the location of this pose beyond the duration of a single frame,
     * create an [Anchor] using [createAnchor] to save the pose in a physically consistent way.
     */
    val pose: Pose

//    /**
//     * Is the AR camera tracking.
//     *
//     * Used for visibility update.
//     */
//    open var cameraTrackingState = TrackingState.TRACKING
//        protected set(value) {
//            if (field != value) {
//                field = value
//                onCameraTrackingChanged(value)
//            }
//        }

//    /**
//     * Set the node to be visible only on those camera tracking states
//     */
//    var visibleCameraTrackingStates = setOf(TrackingState.TRACKING)
//        set(value) {
//            field = value
//            updateVisibility()
//        }

    fun setPose(pose: Pose) {
        worldTransform = pose.transform
        onPoseChanged(pose)
    }

    fun onPoseChanged(pose: Pose) {
    }

    /**
     * Defines a tracked location in the physical world at the actual [pose].
     *
     * See [Anchor] for more details.
     *
     * Anchors incur ongoing processing overhead within ARCore. To release unneeded anchors use
     * [Anchor.detach].
     *
     * @return the created anchor or null is it couldn't be created
     */
    fun createAnchor(session: Session) = runCatching { session.createAnchor(pose) }.getOrNull()

    /**
     * Defines a tracked [AnchorNode] in the physical world at the actual [pose].
     *
     * See [Anchor] for more details.
     *
     * Anchors incur ongoing processing overhead within ARCore. To release unneeded anchors use
     * [Anchor.detach].
     *
     * @return the created anchor or null is it couldn't be created
     */
    fun createAnchorNode(session: Session) = createAnchor(session)?.let { AnchorNode(engine, it) }

    open fun update(session: Session, frame: Frame) {
//        this.cameraTrackingState = frame.camera.trackingState
    }
}

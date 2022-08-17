package io.github.sceneview.ar.arcore

import android.view.MotionEvent
import com.google.ar.core.*
import io.github.sceneview.math.Position
import io.github.sceneview.utils.FrameTime
import kotlin.math.abs

/**
 * ### Captures the state and changes to the AR system from a call to [Session.update]
 */
data class ArFrame(
    val session: ArSession,
    val time: FrameTime,
    val frame: Frame
) {

    val camera: Camera by lazy { frame.camera }

    // TODO : Make a quick test with androidCameraTimestamp
    val updatedTrackables: List<Trackable> by lazy {
        frame.getUpdatedTrackables(Trackable::class.java).toList()
    }

    /**
     * ### Performs a ray cast to retrieve the hit trackables
     *
     * Performs a ray cast from the user's device in the direction of the given location in the
     * camera view. Intersections with detected scene geometry are returned, sorted by distance from
     * the device; the nearest intersection is returned first.
     *
     * When using:
     * - **Plane and/or Depth:** Significant geometric leeway is given when returning hit results.
     * For example, a plane hit may be generated if the ray came close, but did not actually hit
     * within the plane extents or plane bounds
     * [Plane.isPoseInExtents][com.google.ar.core.Plane.isPoseInExtents] and
     * [Plane.isPoseInPolygon][com.google.ar.core.Plane.isPoseInPolygon] can be used to determine
     * these cases). A point (point cloud) hit is generated when a point is roughly within one
     * finger-width of the provided screen coordinates.
     *
     * - **Instant Placement:** Ray cast can return a result before ARCore establishes full
     * tracking. The pose and apparent scale of attached objects depends on the
     * [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint] tracking method and the
     * provided approximateDistanceMeters. A discussion of the different tracking methods and the
     * effects of apparent object scale are described in
     * [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint].
     * This function will succeed only if
     * [Config.InstantPlacementMode][com.google.ar.core.Config.InstantPlacementMode] is
     * [com.google.ar.core.Config.InstantPlacementMode.LOCAL_Y_UP] in the ARCore session
     * configuration, the ARCore session tracking state is
     * [TrackingState.TRACKING][com.google.ar.core.TrackingState.TRACKING] }, and there are
     * sufficient feature points to track the point in screen space.
     *
     * - **[Session.Feature.FRONT_CAMERA][com.google.ar.core.Session.Feature.FRONT_CAMERA]:**
     * The returned hit result list will always be empty, as the camera is not
     * [TrackingState.TRACKING][com.google.ar.core.TrackingState.TRACKING]. Hit testing against
     * tracked faces is not currently supported.
     *
     * Note: In ARCore 1.24.0 or later on supported devices, if depth is enabled by calling
     * [com.google.ar.core.Config.setDepthMode] with the value
     * [Config.DepthMode.AUTOMATIC][com.google.ar.core.Config.DepthMode.AUTOMATIC], the returned
     * list includes [com.google.ar.core.DepthPoint] values sampled from the latest computed depth
     * image.
     *
     * @param xPx x view coordinate in pixels
     * *Default: The view width center*
     * @param yPx y view coordinate in pixelsls
     * *Default: The view height center*
     * @param approximateDistanceMeters the distance at which to create an
     * [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint]. This is only used while
     * the tracking method for the returned point is
     * [InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE]
     * [com.google.ar.core.InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE]
     * *Default: [ArSession.approximateDistance]*
     * @param plane enable plane results
     * *Default: [ArSession.planeFindingEnabled]*
     * @param depth enable depth results
     * *Default: [ArSession.depthEnabled]*
     * @param instantPlacement enable instant placement results
     * *Default: [ArSession.instantPlacementEnabled]*
     *
     * @return an ordered list of intersections with scene geometry, nearest hit first
     * In case of instant placement result, if successful a list containing a single
     * [HitResult][com.google.ar.core.HitResult], otherwise an empty list.
     * The [HitResult][com.google.ar.core.HitResult] will have a trackable
     * of type [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint]
     */
    @JvmOverloads
    fun hitTests(
        xPx: Float = session.displayWidth / 2.0f,
        yPx: Float = session.displayHeight / 2.0f,
        plane: Boolean = session.planeFindingEnabled,
        depth: Boolean = session.depthEnabled,
        instant: Boolean = session.instantPlacementEnabled,
        approximateDistance: Float = session.approximateDistance
    ): List<HitResult> {
        if (camera.isTracking) {
            if (plane || depth) {
                frame.hitTest(xPx, yPx).takeIf { it.isNotEmpty() }?.let {
                    return it
                }
            }
            if (instant) {
                return frame.hitTestInstantPlacement(xPx, yPx, approximateDistance)
            }
        }
        return listOf()
    }

    /**
     * @see hitTests
     * @see firstValid
     */
    @JvmOverloads
    fun hitTest(
        xPx: Float = session.displayWidth / 2.0f,
        yPx: Float = session.displayHeight / 2.0f,
        plane: Boolean = session.planeFindingEnabled,
        depth: Boolean = session.depthEnabled,
        instant: Boolean = session.instantPlacementEnabled,
        approximateDistance: Float = session.approximateDistance
    ): HitResult? {
        return hitTests(
            xPx = xPx,
            yPx = yPx,
            approximateDistance = approximateDistance,
            plane = plane,
            depth = depth,
            instant = instant
        ).firstValid(camera, plane, depth, instant)
    }

    fun hitTest(
        position: Position,
        plane: Boolean,
        depth: Boolean,
        instant: Boolean
    ) = hitTest(
        xPx = session.displayWidth / 2.0f * (1.0f + position.x),
        yPx = session.displayHeight / 2.0f * (1.0f - position.y),
        plane = plane,
        depth = depth,
        instant = instant,
        approximateDistance = abs(position.z)
    )

    /**
     * @see hitTests
     * @see firstValid
     */
    @JvmOverloads
    fun hitTest(
        motionEvent: MotionEvent,
        plane: Boolean = session.planeFindingEnabled,
        depth: Boolean = session.depthEnabled,
        instant: Boolean = session.instantPlacementEnabled,
        approximateDistance: Float = session.approximateDistance
    ): HitResult? = hitTests(
        xPx = motionEvent.x,
        yPx = motionEvent.y,
        plane = plane,
        depth = depth,
        instant = instant,
        approximateDistance = approximateDistance
    ).firstValid(frame.camera, plane, depth, instant)

    /**
     * ### Creates a new anchor at the hit location.
     *
     * See [hitTests] and [com.google.ar.core.HitResult.getHitPose] for details.
     * Anchors incur ongoing processing overhead within ARCore.
     * To release unneeded anchors use [Anchor#detach()] [com.google.ar.core.Anchor.detach].
     *
     * This method is a convenience alias for {@code
     * hitResult.getTrackable().createAnchor(hitResult.getHitPose())}
     *
     * @param xPx x view coordinate in pixels
     * *Default: The view width center*
     * @param yPx y view coordinate in pixelsls
     * *Default: The view height center*
     * @param approximateDistanceMeters the distance at which to create an
     * [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint]. This is only used while
     * the tracking method for the returned point is
     * [InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE]
     * [com.google.ar.core.InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE]
     * *Default: [ArSession.approximateDistance]*
     * @param plane enable plane results
     * *Default: [ArSession.planeFindingEnabled]*
     * @param depth enable depth results
     * *Default: [ArSession.depthEnabled]*
     * @param instantPlacement enable instant placement results
     * *Default: [ArSession.instantPlacementEnabled]*
     *
     * @see hitTests
     * @see #getHitPose()
     */
    fun createAnchor(
        xPx: Float = session.displayWidth / 2.0f,
        yPx: Float = session.displayHeight / 2.0f,
        plane: Boolean = session.planeFindingEnabled,
        depth: Boolean = session.depthEnabled,
        instant: Boolean = session.instantPlacementEnabled,
        approximateDistance: Float = session.approximateDistance
    ): Anchor? = hitTest(
        xPx = xPx,
        yPx = yPx,
        plane = plane,
        depth = depth,
        instant = instant,
        approximateDistance = approximateDistance
    )?.createAnchor()

    /**
     * ### Retrieve the frame tracked Planes
     */
    val updatedPlanes: List<Plane>
        get() = updatedTrackables.mapNotNull {
            it as? Plane
        }

    /**
     * ### Retrieve if the frame is currently tracking a plane
     *
     * @return true if the frame is tracking at least one plane
     */
    val isTrackingPlane: Boolean
        get() = updatedPlanes.any {
            it.trackingState == TrackingState.TRACKING
        }


    /**
     * ### Retrieve the frame tracked Augmented Images
     */
    val updatedAugmentedImages: List<AugmentedImage>
        get() = updatedTrackables.mapNotNull {
            it as? AugmentedImage
        }

    /**
     * ### Retrieve if the frame is currently tracking an Augmented Image
     *
     * @return true if the frame is fully tracking at least one Augmented Image
     */
    val isTrackingAugmentedImage: Boolean
        get() = updatedAugmentedImages.any {
            it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
                    && it.trackingState == TrackingState.TRACKING
        }

    /**
     * ### Retrieve the frame tracked Augmented Faces
     */
    val updatedAugmentedFaces: List<AugmentedFace>
        get() = updatedTrackables.mapNotNull {
            it as? AugmentedFace
        }

    /**
     * ### Retrieve if the frame is currently tracking an Augmented Face
     *
     * @return true if the frame is fully tracking at least one Augmented Face
     */
    val isTrackingAugmentedFace: Boolean
        get() = updatedAugmentedFaces.any {
            it.trackingState == TrackingState.TRACKING
        }

    fun intervalSeconds(arFrame: ArFrame?): Double = time.intervalSeconds(arFrame?.time)

    val fps: Double = time.fps

    fun fps(arFrame: ArFrame?): Double = time.fps(arFrame?.time)

    fun precision(arFrame: ArFrame?): Double = fps(arFrame) / fps
}

/**
 * ### Best HitResult within the list in precision terms
 *
 * Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, Depth Point,
 * or Instant Placement Point.
 */
fun Collection<HitResult>.firstValid(
    camera: Camera,
    plane: Boolean,
    depth: Boolean,
    instantPlacement: Boolean
): HitResult? {
    return firstOrNull { hitResult ->
        when (val trackable = hitResult.trackable!!) {
            is Plane -> plane && trackable.isPoseInPolygon(hitResult.hitPose) &&
                    hitResult.hitPose.calculateDistanceToPlane(camera.pose) > 0.0f
            is Point -> depth && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
            is InstantPlacementPoint -> instantPlacement
            is DepthPoint -> depth
            else -> false
        }
    }
}
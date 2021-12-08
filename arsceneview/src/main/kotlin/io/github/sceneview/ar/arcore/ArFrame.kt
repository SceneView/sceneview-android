package io.github.sceneview.ar.arcore

import android.view.MotionEvent
import com.google.ar.core.*
import com.google.ar.sceneform.FrameTime

/**
 * ### Captures the state and changes to the AR system from a call to [Session.update]
 */
data class ArFrame(
    val session: ArSession,
    val frameTime: FrameTime,
    val frame: Frame,
    val camera: Camera
) {

    // TODO : Make a quick test with androidCameraTimestamp
    val timestamp get() = frame.timestamp

    var updatedTrackables: List<Trackable> = listOf()

    init {
        updatedTrackables = frame.getUpdatedTrackables(Trackable::class.java).toList()
    }


    /**
     * ### Performs a ray cast to retrieve the hit trackables
     *
     * Performs a ray cast from the user's device in the direction of the given location in the
     * camera view. Intersections with detected scene geometry are returned, sorted by distance from
     * the device; the nearest intersection is returned first.
     *
     * Note: Significant geometric leeway is given when returning hit results. For example, a plane
     * hit may be generated if the ray came close, but did not actually hit within the plane extents
     * or plane bounds [Plane.isPoseInExtents][com.google.ar.core.Plane.isPoseInExtents] and
     * [Plane.isPoseInPolygon][com.google.ar.core.Plane.isPoseInPolygon] can be used to determine
     * these cases). A point (point cloud) hit is generated when a point is roughly within one
     * finger-width of the provided screen coordinates.
     *
     * Note: When using
     * [Session.Feature#FRONT_CAMERA][com.google.ar.core.Session.FeatureFRONT_CAMERA], the returned
     * hit result list will always be empty, as the camera is not
     * [TrackingState.TRACKING][com.google.ar.core.TrackingState.TRACKING]. Hit testing against
     * tracked faces is not currently supported.
     *
     * Note: In ARCore 1.24.0 or later on supported devices, if depth is enabled by calling
     * [com.google.ar.core.ConfigsetDepthMode] with the value
     * [Config.DepthMode.AUTOMATIC][com.google.ar.core.Config.DepthMode.AUTOMATIC], the returned
     * list includes [com.google.ar.core.DepthPoint] values sampled from the latest computed depth
     * image.
     *
     * @param xPx x view coordinate in pixels
     * *Default: The view width center*
     * @param yPx y view coordinate in pixelsls
     * *Default: The view height center*
     *
     * @return an ordered list of intersections with scene geometry, nearest hit first
     */
    @JvmOverloads
    fun hitTests(
        xPx: Float = session.displayWidth / 2.0f,
        yPx: Float = session.displayHeight / 2.0f,
    ): List<HitResult> = when {
        frame.camera.trackingState == TrackingState.TRACKING -> {
            frame.hitTest(xPx, yPx)
        }
        else -> listOf()
    }

    /**
     * ### Performs a ray cast that can return a result before ARCore establishes full tracking
     *
     * The pose and apparent scale of attached objects depends on the
     * [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint] tracking method and the
     * provided approximateDistanceMeters. A discussion of the different tracking methods and the
     * effects of apparent object scale are described in
     * [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint].
     *
     * This function will succeed only if
     * [Config.InstantPlacementMode][com.google.ar.core.Config.InstantPlacementMode] is
     * [com.google.ar.core.Config.InstantPlacementMode.LOCAL_Y_UP] in the ARCore session
     * configuration, the ARCore session tracking state is
     * [TrackingState.TRACKING][com.google.ar.core.TrackingState.TRACKING] }, and there are
     * sufficient feature points to track the point in screen space.
     *
     * @param xPx x screen coordinate in pixels
     * *Default: The view width center*
     * @param yPx y screen coordinate in pixels
     * *Default: The view height center*
     * @param approximateDistanceMeters the distance at which to create an
     * [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint]. This is only used while
     * the tracking method for the returned point is
     * [InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE]
     * [com.google.ar.core.InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE]
     * *Default: [io.github.sceneview.ar.ARCore][approximateDistanceMeters]*
     *
     * @return if successful a list containing a single [HitResult][com.google.ar.core.HitResult],
     * otherwise an empty list. The [HitResult][com.google.ar.core.HitResult] will have a trackable
     * of type [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint].
     */
    @JvmOverloads
    fun hitTestsInstantPlacement(
        xPx: Float = session.displayWidth / 2.0f,
        yPx: Float = session.displayHeight / 2.0f,
        approximateDistanceMeters: Float = session.approximateDistanceMeters
    ): List<HitResult> = when {
        session.instantPlacementEnabled -> {
            frame.hitTestInstantPlacement(xPx, yPx, approximateDistanceMeters)
        }
        else -> listOf()
    }

    /**
     * @see hitTests
     */
    @JvmOverloads
    fun hitTests(
        motionEvent: MotionEvent
    ): Collection<HitResult> = hitTests(motionEvent.x, motionEvent.y)

    /**
     * @see hitTestsInstantPlacement
     */
    @JvmOverloads
    fun hitTestsInstantPlacement(
        motionEvent: MotionEvent,
        approximateDistanceMeters: Float = session.approximateDistanceMeters
    ): Collection<HitResult> =
        hitTestsInstantPlacement(motionEvent.x, motionEvent.y, approximateDistanceMeters)

    /**
     * @see hitTests
     * @see firstValid
     */
    @JvmOverloads
    fun hitTest(
        xPx: Float,
        yPx: Float
    ): HitResult? = hitTests(xPx, yPx).firstValid(camera)

    /**
     * @see hitTestsInstantPlacement
     * @see firstValid
     */
    @JvmOverloads
    fun hitTestInstantPlacement(
        xPx: Float,
        yPx: Float,
        approximateDistanceMeters: Float = session.approximateDistanceMeters
    ): HitResult? = hitTestsInstantPlacement(xPx, yPx, approximateDistanceMeters).firstValid(camera)

    /**
     * @see hitTests
     * @see firstValid
     */
    @JvmOverloads
    fun hitTest(
        motionEvent: MotionEvent
    ): HitResult? = hitTests(motionEvent).firstValid(frame.camera)

    /**
     * @see hitTestsInstantPlacement
     * @see firstValid
     */
    @JvmOverloads
    fun hitTestInstantPlacement(
        motionEvent: MotionEvent,
        approximateDistanceMeters: Float = session.approximateDistanceMeters
    ): HitResult? = hitTestsInstantPlacement(motionEvent).firstValid(frame.camera)

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
     * The view width center is the default value
     * @param yPx y view coordinate in pixelsls
     * The view height center is the default value
     * @param approximateDistanceMeters only if Instant Placement is enabled
     * The distance at which to create an [InstantPlacementPoint].
     * This is only used while the tracking method for the returned point is
     * [InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE]
     *
     * @see hitTests
     * @see #getHitPose()
     */
    fun createAnchor(
        xPx: Float = session.displayWidth / 2.0f,
        yPx: Float = session.displayHeight / 2.0f,
        approximateDistanceMeters: Float = session.approximateDistanceMeters
    ): Anchor? = hitTest(xPx, yPx)?.createAnchor()

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
        get() = updatedPlanes.filter {
            it.trackingState == TrackingState.TRACKING
        }.isNotEmpty()


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
        get() = updatedAugmentedImages.filter {
            it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
                    && it.trackingState == TrackingState.TRACKING
        }.isNotEmpty()

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
        get() = updatedAugmentedFaces.filter {
            it.trackingState == TrackingState.TRACKING
        }.isNotEmpty()
}

/**
 * Tests to see if a motion event is touching any nodes within the scene, based on a ray hit test
 * whose origin is the screen position of the motion event, and outputs a PickHitResult containing
 * the node closest to the screen.
 *
 * @param motionEvent         the motion event to use for the test
 * @param selectableNodesOnly Filter the PickHitResult on only selectable nodes
 * @return the result includes the first node that was hit by the motion event (may be null), and
 * information about where the motion event hit the node in world-space
 */
fun Collection<HitResult>.firstValid(camera: Camera) = firstOrNull { hitResult ->
    when (val trackable = hitResult.trackable!!) {
        is Plane -> trackable.isPoseInPolygon(hitResult.hitPose) &&
                hitResult.hitPose.calculateDistanceToPlane(camera.pose) > 0
        // DepthPoints are only returned if Config.DepthMode is set to AUTOMATIC.
        is DepthPoint -> true
        is Point -> trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
        is InstantPlacementPoint -> true
        else -> false
    }
}
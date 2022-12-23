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
    val session: ArSessionOld,
    val time: FrameTime,
    val frame: Frame
) {

    val camera: Camera by lazy { frame.camera }

    // TODO : Make a quick test with androidCameraTimestamp
    val updatedTrackables: List<Trackable> by lazy {
        frame.getUpdatedTrackables(Trackable::class.java).toList()
    }

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
     * *Default: [ArSessionOld.approximateDistance]*
     * @param plane enable plane results
     * *Default: [ArSessionOld.planeFindingEnabled]*
     * @param depth enable depth results
     * *Default: [ArSessionOld.depthEnabled]*
     * @param instantPlacement enable instant placement results
     * *Default: [ArSessionOld.instantPlacementEnabled]*
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


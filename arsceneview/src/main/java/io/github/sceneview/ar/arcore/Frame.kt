package io.github.sceneview.ar.arcore

import com.google.ar.core.AugmentedFace
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.StreetscapeGeometry
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Ray
import io.github.sceneview.utils.fps
import io.github.sceneview.utils.intervalSeconds

/**
 * Similar to [Frame.hitTest], but takes an arbitrary ray in world space coordinates instead of a
 * screen-space point.
 *
 * Note: When using [Session.Feature.FRONT_CAMERA], the returned hit result list will always be
 * empty, as the camera is not [TrackingState.TRACKING]. Hit testing against tracked faces is not
 * currently supported.
 *
 * @param ray a ray containing an origin and direction in world space coordinates. Does not have to
 * be normalized.
 *
 * @return an ordered list of intersections with scene geometry, nearest hit first
 */
fun Frame.hitTest(ray: Ray): List<HitResult> {
    val origin = ray.origin.toFloatArray()
    val direction = ray.direction.toFloatArray()
    return hitTest(origin, origin.size, direction, direction.size)
}

/**
 * Returns the collection of updated trackables of the same type as [trackable] for this frame.
 */
inline fun <reified T : Trackable> Frame.hasUpdatedTrackable(trackable: T) =
    getUpdatedTrackables(T::class.java)

/**
 * Returns all updated [Trackable]s of any type for this frame.
 */
fun Frame.getUpdatedTrackables() = getUpdatedTrackables(Trackable::class.java)

/**
 * Retrieve the frame tracked Planes.
 */
fun Frame.getUpdatedPlanes() = getUpdatedTrackables(Plane::class.java)

/**
 * Retrieve if the frame is currently tracking a plane.
 *
 * @return true if the frame is tracking at least one plane.
 */
fun Frame.isTrackingPlane() = getUpdatedPlanes().any {
    it.trackingState == TrackingState.TRACKING
}

/**
 * Retrieve the frame tracked Augmented Images.
 */
fun Frame.getUpdatedAugmentedImages() = getUpdatedTrackables(AugmentedImage::class.java)

/**
 * Retrieve the frame tracked Augmented Faces.
 */
fun Frame.getUpdatedAugmentedFaces() = getUpdatedTrackables(AugmentedFace::class.java)

/**
 * Retrieve the frame tracked Streetscape Geometries.
 */
fun Frame.getUpdatedStreetscapeGeometries() = getUpdatedTrackables(StreetscapeGeometry::class.java)

/**
 * Returns the time interval in seconds between this frame and [other].
 * If [other] is `null`, returns the interval from timestamp 0.
 */
fun Frame.intervalSeconds(other: Frame?): Double = timestamp.intervalSeconds(other?.timestamp)

/**
 * Returns the estimated frames per second between this frame and [other].
 * If [other] is `null`, returns the FPS from timestamp 0.
 */
fun Frame.fps(other: Frame?): Double = timestamp.fps(other?.timestamp)
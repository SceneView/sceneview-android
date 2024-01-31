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

///**
// * Similar to [Frame.hitTest], but will take values from Android [MotionEvent].
// *
// * It is assumed that the [MotionEvent] is received from the same view that was used as the size for
// * [Session.setDisplayGeometry].
// *
// * Note: this method does not consider the [MotionEvent.getAction] of the [MotionEvent].
// * The caller must check for appropriate action, if needed, before calling this method.
// *
// * Note: When using [Session.Feature.FRONT_CAMERA], the returned hit result list will always be
// * empty, as the camera is not [TrackingState.TRACKING]. Hit testing against tracked faces is not
// * currently supported.
// *
// * @param motionEvent an event containing the x,y coordinates to hit test
// * @param plane enable plane results.
// * @param depth enable depth results.
// * @param instant enable instant placement results
// * @param instantDistance the distance at which to create an
// *
// * @return an ordered list of intersections with scene geometry, nearest hit first
// */
//fun Frame.hitTest(
//    motionEvent: MotionEvent,
//    plane: Boolean = true,
//    depth: Boolean = true,
//    instant: Boolean = true,
//    instantDistance: Float = kDefaultHitTestInstantDistance,
//    planePoseInPolygon: Boolean = true,
//    minCameraDistance: Float = 0.0f,
//    pointOrientationModes: Set<Point.OrientationMode> = setOf(
//        Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
//    )
//) = hitTest(
//    motionEvent.x, motionEvent.y, plane, depth, instant, instantDistance,
//    planePoseInPolygon, minCameraDistance, pointOrientationModes
//)
//
///**
// * Performs a ray cast to retrieve the hit trackables
// *
// * Performs a ray cast from the user's device in the direction of the given location in the
// * camera view. Intersections with detected scene geometry are returned, sorted by distance from
// * the device; the nearest intersection is returned first.
// *
// * When using:
// * - **Plane and/or Depth:** Significant geometric leeway is given when returning hit results.
// * For example, a plane hit may be generated if the ray came close, but did not actually hit
// * within the plane extents or plane bounds [Plane.isPoseInExtents] and [Plane.isPoseInPolygon]
// * can be used to determine these cases).
// * A point (point cloud) hit is generated when a point is roughly within one finger-width of the
// * provided screen coordinates.
// *
// * - **Instant Placement:** Ray cast can return a result before ARCore establishes full
// * tracking. The pose and apparent scale of attached objects depends on the[InstantPlacementPoint]
// * tracking method and the provided approximateDistanceMeters. A discussion of the different
// * tracking methods and the effects of apparent object scale are described in
// * [InstantPlacementPoint]
// * This function will succeed only if [Config.InstantPlacementMode] is
// * [Config.InstantPlacementMode.LOCAL_Y_UP] in the ARCore session configuration, the ARCore session
// * tracking state is [TrackingState.TRACKING], and there are sufficient feature points to track the
// * point in screen space.
// *
// * - **[Session.Feature.FRONT_CAMERA]:**
// * The returned hit result list will always be empty, as the camera is not [TrackingState.TRACKING].
// * Hit testing against tracked faces is not currently supported.
// *
// * Note: In ARCore 1.24.0 or later on supported devices, if depth is enabled by calling
// * [Config.setDepthMode] with the value [Config.DepthMode.AUTOMATIC], the returned list includes
// * [DepthPoint] values sampled from the latest computed depth
// * image.
// *
// * @param xPx x view coordinate in pixels
// * @param yPx y view coordinate in pixels
// * @param plane enable plane results.
// * @param depth enable depth results.
// * @param instant enable instant placement results
// * @param instantDistance the distance at which to create an
// *
// * @return an ordered list of intersections with scene geometry, nearest hit first
// */
//fun Frame.hitTest(
//    xPx: Float,
//    yPx: Float,
//    plane: Boolean = true,
//    depth: Boolean = true,
//    instant: Boolean = true,
//    instantDistance: Float = kDefaultHitTestInstantDistance,
//    planePoseInPolygon: Boolean = true,
//    minCameraDistance: Float = 0.0f,
//    pointOrientationModes: Set<Point.OrientationMode> = setOf(
//        Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
//    )
//): List<HitResult> {
//    var result = listOf<HitResult>()
//    if (camera.isTracking) {
//        if (plane || depth) {
//            result = hitTest(xPx, yPx)
//        }
//        if (result.isEmpty() && instant) {
//            result = hitTestInstantPlacement(xPx, yPx, instantDistance)
//        }
//    }
//    return result.filter { hitResult ->
//        hitResult.isValid(
//            plane = plane,
//            depthPoint = depth,
//            instant = instant,
//            camera = camera,
//            planePoseInPolygon = planePoseInPolygon,
//            minCameraDistance = minCameraDistance,
//            pointOrientationModes = pointOrientationModes
//        )
//    }
//}

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
    val direction = ray.origin.toFloatArray()
    return hitTest(origin, origin.size, direction, direction.size)
}
//
///**
// * Creates a new anchor at the hit location.
// *
// * See [hitTest] and [HitResult.getHitPose] for details.
// * Anchors incur ongoing processing overhead within ARCore.
// * To release unneeded anchors use [Anchor.detach()].
// *
// * This method is a convenience alias for [HitResult.createAnchor]
// *
// * @param xPx x view coordinate in pixels
// * @param yPx y view coordinate in pixels
// * @param plane enable plane results.
// * @param depth enable depth results.
// * @param instant enable instant placement results
// * @param instantDistance the distance at which to create an
// *
// * @see hitTest
// */
//fun Frame.createAnchor(
//    xPx: Float,
//    yPx: Float,
//    plane: Boolean = true,
//    depth: Boolean = true,
//    instant: Boolean = true,
//    instantDistance: Float = kDefaultHitTestInstantDistance,
//    planePoseInPolygon: Boolean = true,
//    minCameraDistance: Float = 0.0f,
//    pointOrientationModes: Set<Point.OrientationMode> = setOf(
//        Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
//    )
//) = runCatching {
//    hitTest(
//        xPx = xPx,
//        yPx = yPx,
//        plane = plane,
//        depth = depth,
//        instant = instant,
//        instantDistance = instantDistance,
//        planePoseInPolygon = planePoseInPolygon,
//        minCameraDistance = minCameraDistance,
//        pointOrientationModes = pointOrientationModes
//    ).firstOrNull()?.takeIf {
//        it.trackable.isTracking
//    }?.createAnchor()
//}.getOrNull()

/**
 * Retrieve all the frame tracked Planes.
 */
inline fun <reified T : Trackable> Frame.hasUpdatedTrackable(trackable: T) =
    getUpdatedTrackables(T::class.java)

/**
 * Retrieve all the frame tracked Planes.
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

fun Frame.intervalSeconds(other: Frame?): Double = timestamp.intervalSeconds(other?.timestamp)

fun Frame.fps(other: Frame?): Double = timestamp.fps(other?.timestamp)
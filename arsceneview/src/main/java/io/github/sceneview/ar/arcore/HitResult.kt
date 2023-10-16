package io.github.sceneview.ar.arcore

import com.google.ar.core.Camera
import com.google.ar.core.DepthPoint
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Point.OrientationMode

/**
 * Check for hit results first valid on provided criteria.
 *
 * Significant geometric leeway is given when returning hit results. For example, a plane
 * hit may be generated if the ray came close, but did not actually hit within the plane extents
 * or plane bounds [Plane.isPoseInExtents] and [Plane.isPoseInPolygon] can be used to determine
 * these cases).
 */
fun HitResult.isValid(
    plane: Boolean,
    depth: Boolean,
    instant: Boolean,
    camera: Camera,
    planePoseInPolygon: Boolean = true,
    minCameraDistance: Float = 0.0f,
    pointOrientationModes: Set<OrientationMode> = setOf(OrientationMode.ESTIMATED_SURFACE_NORMAL)
) = when (val trackable = trackable) {
    is Plane -> plane
            && (!planePoseInPolygon || trackable.isPoseInPolygon(hitPose))
            && hitPose.distanceToPlane(camera.pose) > minCameraDistance

    is Point -> depth
            && trackable.orientationMode in pointOrientationModes

    is DepthPoint -> depth
    is InstantPlacementPoint -> instant
    else -> false
}

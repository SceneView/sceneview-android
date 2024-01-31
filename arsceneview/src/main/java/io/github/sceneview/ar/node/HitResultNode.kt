package io.github.sceneview.ar.node

import com.google.android.filament.Engine
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.firstByTypeOrNull

/**
 * AR real time AR HitTest positioned node.
 *
 * This [PoseNode] follows the actual ARCore detected orientation and position at the provided
 * relative X, Y location in the [ARSceneView]
 *
 * Performs a ray cast from the user's device in the direction of the given location in the
 * camera view. Intersections with detected scene geometry are returned, sorted by distance from
 * the device; the nearest intersection is returned first.
 */
open class HitResultNode(
    engine: Engine,
    val hitTest: HitResultNode.(Frame) -> HitResult?
) : TrackableNode<Trackable>(engine) {

    /**
     * Make the node follow the camera/screen matching real world positions
     */
    var update: Boolean = true

    open var hitResult: HitResult? = null
        set(value) {
            field = value
            trackable = value?.trackable
            value?.hitPose?.let {
                pose = it
            }
        }

    init {
        isSmoothTransformEnabled = true
    }

    /**
     * Contruct an HitResultNode based on View coordinate hit test location
     *
     * @param xPx X view coordinate in pixels where the hit test should be done
     * @param yPx Y view coordinate in pixels where the hit test should be done
     * @param plane Enable plane results
     * @param depth Enable depth results
     * @param instant Enable instant placement results
     * @param instantDistance The distance at which to make the hit in case of instant result
     */
    constructor(
        engine: Engine,
        xPx: Float,
        yPx: Float,
        planeTypes: Set<Plane.Type> = Plane.Type.values().toSet(),
        point: Boolean = true,
        depthPoint: Boolean = true,
        instantPlacementPoint: Boolean = true,
        trackingStates: Set<TrackingState> = setOf(TrackingState.TRACKING),
        pointOrientationModes: Set<Point.OrientationMode> = setOf(Point.OrientationMode.ESTIMATED_SURFACE_NORMAL),
        planePoseInPolygon: Boolean = true,
        minCameraDistance: Pair<Camera, Float>? = null,
        predicate: ((HitResult) -> Boolean)? = null
    ) : this(
        engine = engine,
        hitTest = { frame ->
            frame.hitTest(xPx, yPx).firstByTypeOrNull(
                planeTypes, point, depthPoint, instantPlacementPoint, trackingStates,
                pointOrientationModes, planePoseInPolygon, minCameraDistance, predicate
            )
        }
    )

    override fun update(session: Session, frame: Frame) {
        if (update) {
            hitResult = hitTest(frame)
        }

        super.update(session, frame)
    }
}
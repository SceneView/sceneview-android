package io.github.sceneview.ar.node

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.instantPlacementEnabled
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.node.NodeParent

/**
 * ### Instant placed node
 *
 * This [Node] can be placed before ARCore establishes full tracking.
 * This [Node] is currently intended to be used with hit tests against horizontal surfaces
 *
 * You can:
 * - [anchor] this node at any time to make it fixed at the actual position with the +Y pointing
 * upward, against gravity.
 * This node will stop following the hitPostion to stay in place.
 * - [createAnchor] in order to extract a fixed/anchored copy of the actual.
 * This node will continue following the [com.google.ar.core.Camera]
 *
 * Hit tests may also be performed against surfaces with any orientation, however:
 * - The [InstantPlacementNode] will always have a pose with +Y pointing upward, against gravity.
 * - No guarantees are made with respect to orientation of +X and +Z. Specifically, a hittest
 * against a vertical surface, such as a wall, will not result in a pose that's in any way aligned
 * to the plane of the wall, other than +Y being up, against gravity.
 * - The [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint]'s tracking method may
 * never become [com.google.ar.core.InstantPlacementPoint.TrackingMethod.FULL_TRACKING] or may take
 * a long time to reach this state. The tracking method remains
 * [com.google.ar.core.InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE]
 * until a (tiny) horizontal plane is fitted at the point of the hit test.
 *
 * The pose and apparent scale of attached objects depends on the
 * [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint] tracking method and the
 * provided approximateDistanceMeters. A discussion of the different tracking methods and the
 * effects of apparent object scale are described in
 * [InstantPlacementPoint][com.google.ar.core.InstantPlacementPoint].
 */
open class InstantPlacementNode(
    position: Vector3 = defaultPosition,
    rotationQuaternion: Quaternion = defaultRotation,
    scales: Vector3 = defaultScales,
    parent: NodeParent? = null
) : ArNode(
    position = position,
    rotationQuaternion = rotationQuaternion,
    scales = scales,
    parent = parent
) {

    var lastValidHitResult: HitResult? = null

    var isTracking: Boolean = false
        internal set(value) {
            if (field != value) {
                field = value
                isVisible = value
                onTrackingChanged?.invoke(this, value)
            }
        }

    var onArFrameHitResult: ((node: InstantPlacementNode, hitResult: HitResult?, isTracking: Boolean) -> Unit)? =
        null
    var onTrackingChanged: ((node: InstantPlacementNode, isTracking: Boolean) -> Unit)? = null

    init {
        isVisible = false
    }

    constructor(
        context: Context,
        modelGlbFileLocation: String,
        coroutineScope: LifecycleCoroutineScope? = null,
        onModelLoaded: ((instance: RenderableInstance) -> Unit)? = null,
        onError: ((error: Exception) -> Unit)? = null,
        parent: NodeParent? = null,
        position: Vector3 = defaultPosition,
        rotationQuaternion: Quaternion = defaultRotation,
        scales: Vector3 = defaultScales,
    ) : this(position, rotationQuaternion, scales, parent) {
        loadModel(context, modelGlbFileLocation, coroutineScope, onModelLoaded, onError)
    }

    override fun onAttachToScene(sceneView: SceneView) {
        super.onAttachToScene(sceneView)

        (sceneView as? ArSceneView)?.configureSession { config ->
            config.instantPlacementEnabled = true
        }
    }

    override fun onArFrame(frame: ArFrame) {
        super.onArFrame(frame)

        if (anchor == null) {
            val hitResult = hitTest(frame)
            onArFrameHitResult(hitResult, hitResult?.trackable?.isTracking == true)
        }
        isTracking = pose != null || lastValidHitResult?.isTracking == true
    }

    open fun onArFrameHitResult(hitResult: HitResult?, isTracking: Boolean) {
        // Keep the last position when no more tracking result
        if (hitResult != null && isTracking) {
            lastValidHitResult = hitResult
            hitResult.hitPose?.let { hitPose ->
                pose = hitPose
            }
        }
        onArFrameHitResult?.invoke(this, hitResult, isTracking)
    }

    override fun createAnchor(): Anchor? {
        return super.createAnchor() ?: if (lastValidHitResult?.isTracking == true) {
            lastValidHitResult?.createAnchor()
        } else null
    }
}

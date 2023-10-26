package io.github.sceneview.ar.node

import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.NodeMotionEvent

/**
 * Construct a new AR placement Node
 */
open class AnchorNode(
    engine: Engine,
    anchor: Anchor,
    var onTrackingStateChanged: ((TrackingState) -> Unit)? = null,
    var onUpdated: ((Anchor) -> Unit)? = null
) : PoseNode(engine) {

    /**
     * Should the anchor automatically update the anchor new pose
     */
    var updateAnchorPose = true

    /**
     * The node world positioned anchor
     *
     * Describes a fixed location and orientation in the real world. To stay at a fixed location in
     * physical space, the numerical description of this position will update as ARCore's
     * understanding of the space improves.
     *
     * The nose uses [Anchor.getPose] to get the current numerical location of this anchor. This
     * location may change any time [Session.update] is called, but will never spontaneously change.
     *
     * mathematically:
     * `point_world = anchor.getPose().transformPoint(point_local);}`
     * `point_world = anchor.getPose().toMatrix() * point_local;`
     *
     * [Anchor]s are hashable and may for example be used as keys in [Map].
     *
     * Anchors incur ongoing processing overhead within ARCore. To release unneeded anchors use
     * [destroy].
     */
    var anchor: Anchor = anchor
        set(value) {
            field = value
            trackingState = value.trackingState
            pose = value.pose
        }

    /**
     * The anchor [TrackingState] of this Node.
     *
     * Updated on each frame
     */
    open var trackingState = anchor.trackingState
        get() = anchor.trackingState
        protected set(value) {
            if (field != value) {
                field = value
                updateVisibility()
                onTrackingStateChanged?.invoke(value)
            }
        }

    /**
     * Set the node to be visible only on those tracking states
     */
    var visibleTrackingStates = setOf(TrackingState.TRACKING)
        set(value) {
            field = value
            updateVisibility()
        }

    override var isVisible
        get() = super.isVisible && trackingState in visibleTrackingStates
        set(value) {
            super.isVisible = value
        }

    private var isMoving = false

    init {
        this.anchor = anchor
    }

    override fun update(session: Session, frame: Frame) {
        super.update(session, frame)

        if (isMoving) return

        if (anchor in frame.updatedAnchors) {
            trackingState = anchor.trackingState
            if (trackingState == TrackingState.TRACKING && updateAnchorPose) {
                pose = anchor.pose
            }
            onUpdated?.invoke(anchor)
        }
    }

    open fun detachAnchor() {
        anchor.detach()
    }

    override fun onMoveBegin(detector: MoveGestureDetector, e: NodeMotionEvent) {
        super.onMoveBegin(detector, e)

        if (isEditable && isPositionEditable) {
            detachAnchor()
            isMoving = true
        }
    }

    override fun onMoveEnd(detector: MoveGestureDetector, e: NodeMotionEvent) {
        super.onMoveEnd(detector, e)

        if (isEditable && isPositionEditable) {
            createAnchor()?.let {
                anchor = it
            }
            isMoving = false
        }
    }

    override fun destroy() {
        detachAnchor()

        super.destroy()
    }
}

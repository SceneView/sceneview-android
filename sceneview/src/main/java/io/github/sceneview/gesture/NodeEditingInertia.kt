package io.github.sceneview.gesture

import android.view.MotionEvent
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.degrees
import io.github.sceneview.node.Node
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln

/**
 * The coast of node editing — drag, twist and pinch keep going after the finger lifts, as set by
 * [Node.editingInertia]. Owned by [GestureDetector], ticked once per frame by the scene's render
 * loop through [GestureDetector.advanceInertia].
 *
 * A coast **replays the gesture** rather than integrating the node on its own:
 *
 * - a drag moves a synthetic pointer along the release velocity, in screen space, and hands it to
 *   the touched node's own `onMove` — the hit test, the parent delegation, an AR `PoseNode`'s
 *   plane hit and the app's `onMove` veto all run as for the finger. The node therefore stays on
 *   the surface it was dragged on, and the coast stops the first time that surface is missed;
 * - a twist / pinch feeds the editing node's `onRotate` / `onScale` the step the release velocity
 *   asks for, so the scale range and the app's callbacks still apply.
 *
 * The gesture's `…End` callbacks are held back until the coast is over, so whatever an app does
 * there (an AR re-anchor, a save) sees the node where it came to rest. A new touch-down
 * ([stopAll]) ends every coast on the spot and fires those callbacks then. The app-level
 * [GestureDetector.OnGestureListener] does not see the synthetic steps — its `onMove` /
 * `onRotate` / `onScale` read detector state that is frozen at release.
 */
internal class NodeEditingInertia(
    private val listener: () -> GestureDetector.OnGestureListener?,
) {
    private val move = InertiaTracker(
        dimensions = 2,
        minSpeed = MOVE_STOP_PX_PER_SECOND,
        maxSpeed = MOVE_MAX_PX_PER_SECOND,
        minReleaseSpeed = MOVE_RELEASE_PX_PER_SECOND,
    )
    private val rotate = InertiaTracker(
        dimensions = 1,
        minSpeed = ROTATE_STOP_RAD_PER_SECOND,
        maxSpeed = ROTATE_MAX_RAD_PER_SECOND,
        minReleaseSpeed = ROTATE_RELEASE_RAD_PER_SECOND,
    )
    private val scale = InertiaTracker(
        dimensions = 1,
        minSpeed = SCALE_STOP_PER_SECOND,
        maxSpeed = SCALE_MAX_PER_SECOND,
        minReleaseSpeed = SCALE_RELEASE_PER_SECOND,
    )
    private val step = FloatArray(2)

    private var moveX = 0f
    private var moveY = 0f
    private var moveCoast: MoveCoast? = null

    private var rotateAngle = 0f
    private var rotateCoast: Coast<RotateGestureDetector>? = null

    private var scaleLog = 0f
    private var scaleCoast: Coast<ScaleGestureDetector>? = null

    private class MoveCoast(
        val node: Node,
        val detector: MoveGestureDetector,
        val event: MotionEvent,
    )

    private class Coast<D>(
        /** The node the gesture was dispatched to, which receives the deferred `…End`. */
        val node: Node,
        /** The node that owns the edit — the first editable one up the chain. */
        val owner: Node,
        val detector: D,
        val event: MotionEvent,
    )

    /** `true` while any edit is still coasting: the render loop must keep ticking. */
    val isCoasting: Boolean
        get() = moveCoast != null || rotateCoast != null || scaleCoast != null

    /** A new touch: every coast stops where it is and its deferred `…End` callbacks fire. */
    fun stopAll() {
        finishMove()
        finishRotate()
        finishScale()
    }

    // ── Move ────────────────────────────────────────────────────────────────────────────────────

    fun onMoveBegin(e: MotionEvent) {
        finishMove()
        move.reset()
        onMoveSample(e)
    }

    fun onMoveSample(e: MotionEvent) {
        moveX = e.x
        moveY = e.y
        move.addSample(e.eventTime.seconds, moveX, moveY)
    }

    /**
     * The finger lifted a drag on [node]. Returns `true` when the drag coasts, in which case the
     * caller must NOT dispatch `onMoveEnd` — [finishMove] will, once the node is at rest.
     */
    fun onMoveEnd(detector: MoveGestureDetector, e: MotionEvent, node: Node?): Boolean {
        // A second finger landing also ends a move: the user is still touching, nothing coasts.
        if (node == null || e.actionMasked != MotionEvent.ACTION_UP) return false
        val owner = generateSequence(node) { it.parent }.firstOrNull { it.isPositionEditable }
            ?: return false
        move.inertia = owner.editingInertia
        if (!move.release(e.eventTime.seconds)) return false
        val synthetic = MotionEvent.obtain(e).apply {
            action = MotionEvent.ACTION_MOVE
            setLocation(moveX, moveY)
        }
        moveCoast = MoveCoast(node, detector, synthetic)
        return true
    }

    private fun advanceMove(deltaSeconds: Float) {
        val coast = moveCoast ?: return
        val going = move.advance(deltaSeconds, step)
        moveX += step[0]
        moveY += step[1]
        coast.event.setLocation(moveX, moveY)
        // The node's own drag path: a surface missed under the coasting point ends the coast.
        val moved = runCatching { coast.node.onMove(coast.detector, coast.event) }.getOrDefault(false)
        if (!going || !moved) finishMove()
    }

    private fun finishMove() {
        val coast = moveCoast ?: return
        moveCoast = null
        move.stop()
        try {
            coast.node.onMoveEnd(coast.detector, coast.event)
            listener()?.onMoveEnd(coast.detector, coast.event, coast.node)
        } finally {
            coast.event.recycle()
        }
    }

    // ── Rotate ──────────────────────────────────────────────────────────────────────────────────

    fun onRotateBegin(e: MotionEvent) {
        finishRotate()
        rotate.reset()
        rotateAngle = 0f
        rotate.addSample(e.eventTime.seconds, rotateAngle)
    }

    fun onRotateSample(detector: RotateGestureDetector, e: MotionEvent) {
        rotateAngle += wrapRadians(detector.currentAngle - detector.lastAngle)
        rotate.addSample(e.eventTime.seconds, rotateAngle)
    }

    /** As [onMoveEnd], for a twist. */
    fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent, node: Node?): Boolean {
        if (node == null || e.actionMasked == MotionEvent.ACTION_CANCEL) return false
        val owner = generateSequence(node) { it.parent }.firstOrNull { it.isRotationEditable }
            ?: return false
        rotate.inertia = owner.editingInertia
        if (!rotate.release(e.eventTime.seconds)) return false
        rotateCoast = Coast(node, owner, detector, MotionEvent.obtain(e))
        return true
    }

    private fun advanceRotate(deltaSeconds: Float) {
        val coast = rotateCoast ?: return
        val going = rotate.advance(deltaSeconds, step)
        // Same mapping as NodeGestureDelegate.onRotate: a screen-space angle turns the node about
        // its up axis, the other way round.
        val delta = Quaternion.fromAxisAngle(Y_AXIS, degrees(-step[0]))
        val turned = runCatching {
            coast.owner.onRotate(coast.detector, coast.event, delta)
        }.getOrDefault(false)
        if (!going || !turned) finishRotate()
    }

    private fun finishRotate() {
        val coast = rotateCoast ?: return
        rotateCoast = null
        rotate.stop()
        try {
            coast.node.onRotateEnd(coast.detector, coast.event)
            listener()?.onRotateEnd(coast.detector, coast.event, coast.node)
        } finally {
            coast.event.recycle()
        }
    }

    // ── Scale ───────────────────────────────────────────────────────────────────────────────────

    fun onScaleBegin(e: MotionEvent) {
        finishScale()
        scale.reset()
        scaleLog = 0f
        scale.addSample(e.eventTime.seconds, scaleLog)
    }

    fun onScaleSample(detector: ScaleGestureDetector, e: MotionEvent) {
        val factor = detector.scaleFactor
        if (factor > 0f && factor.isFinite()) scaleLog += ln(factor)
        scale.addSample(e.eventTime.seconds, scaleLog)
    }

    /** As [onMoveEnd], for a pinch. */
    fun onScaleEnd(detector: ScaleGestureDetector, e: MotionEvent, node: Node?): Boolean {
        if (node == null || e.actionMasked == MotionEvent.ACTION_CANCEL) return false
        val owner = generateSequence(node) { it.parent }.firstOrNull { it.isScaleEditable }
            ?: return false
        scale.inertia = owner.editingInertia
        if (!scale.release(e.eventTime.seconds)) return false
        scaleCoast = Coast(node, owner, detector, MotionEvent.obtain(e))
        return true
    }

    private fun advanceScale(deltaSeconds: Float) {
        val coast = scaleCoast ?: return
        val going = scale.advance(deltaSeconds, step)
        // A pinch that runs into the scale range stops there (the edit reports `applied = false`).
        val grown = runCatching {
            coast.owner.onScale(coast.detector, coast.event, exp(step[0]))
        }.getOrDefault(false)
        if (!going || !grown) finishScale()
    }

    private fun finishScale() {
        val coast = scaleCoast ?: return
        scaleCoast = null
        scale.stop()
        try {
            coast.node.onScaleEnd(coast.detector, coast.event)
            listener()?.onScaleEnd(coast.detector, coast.event, coast.node)
        } finally {
            coast.event.recycle()
        }
    }

    /** One frame of every coast in flight. */
    fun advance(deltaSeconds: Float) {
        advanceMove(deltaSeconds)
        advanceRotate(deltaSeconds)
        advanceScale(deltaSeconds)
    }

    private val Long.seconds: Double get() = this / 1000.0

    companion object {
        /** Slowest drag release that coasts, in pixels per second (≈ a list fling). */
        const val MOVE_RELEASE_PX_PER_SECOND = 150f
        const val MOVE_STOP_PX_PER_SECOND = 8f
        const val MOVE_MAX_PX_PER_SECOND = 4000f

        /** Slowest twist release that coasts, in radians per second (≈ 30°/s). */
        const val ROTATE_RELEASE_RAD_PER_SECOND = 0.5f
        const val ROTATE_STOP_RAD_PER_SECOND = 0.02f
        const val ROTATE_MAX_RAD_PER_SECOND = 4f * PI.toFloat()

        /** Slowest pinch release that coasts, in ln(scale) per second. */
        const val SCALE_RELEASE_PER_SECOND = 0.3f
        const val SCALE_STOP_PER_SECOND = 0.01f
        const val SCALE_MAX_PER_SECOND = 3f

        private val Y_AXIS = Float3(y = 1.0f)

        /** [angle] brought into `(−π, π]`, so an `atan2` wrap is not read as a full turn. */
        fun wrapRadians(angle: Float): Float {
            val twoPi = (2 * PI).toFloat()
            var a = angle % twoPi
            if (a > PI) a -= twoPi
            if (a <= -PI) a += twoPi
            return a
        }
    }
}

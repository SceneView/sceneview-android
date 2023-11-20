package io.github.sceneview.gesture

import android.content.Context
import android.view.MotionEvent
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.node.Node
import io.github.sceneview.utils.pickNode

/**
 * Detects various gestures and events using the supplied {@link MotionEvent}s.
 *
 * The gesture listener callback will notify users when a particular motion event has occurred.
 * This class should only be used with [MotionEvent]s reported via touch (don't use for trackball
 * events).
 *
 * Responds to Android touch events with listeners.
 */
open class GestureDetector(
    context: Context,
    var nodeSelector: (e: MotionEvent, (node: Node?) -> Unit) -> Unit
) {
    interface OnGestureListener {
        fun onDown(e: MotionEvent, node: Node?)
        fun onShowPress(e: MotionEvent, node: Node?)
        fun onSingleTapUp(e: MotionEvent, node: Node?)
        fun onScroll(e1: MotionEvent?, e2: MotionEvent, node: Node?, distance: Float2)
        fun onLongPress(e: MotionEvent, node: Node?)
        fun onFling(e1: MotionEvent?, e2: MotionEvent, node: Node?, velocity: Float2)
        fun onSingleTapConfirmed(e: MotionEvent, node: Node?)
        fun onDoubleTap(e: MotionEvent, node: Node?)
        fun onDoubleTapEvent(e: MotionEvent, node: Node?)
        fun onContextClick(e: MotionEvent, node: Node?)
        fun onMoveBegin(detector: MoveGestureDetector, e: MotionEvent, node: Node?)
        fun onMove(detector: MoveGestureDetector, e: MotionEvent, node: Node?)
        fun onMoveEnd(detector: MoveGestureDetector, e: MotionEvent, node: Node?)
        fun onRotateBegin(detector: RotateGestureDetector, e: MotionEvent, node: Node?)
        fun onRotate(detector: RotateGestureDetector, e: MotionEvent, node: Node?)
        fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent, node: Node?)
        fun onScaleBegin(detector: ScaleGestureDetector, e: MotionEvent, node: Node?)
        fun onScale(detector: ScaleGestureDetector, e: MotionEvent, node: Node?)
        fun onScaleEnd(detector: ScaleGestureDetector, e: MotionEvent, node: Node?)
    }

    open class SimpleOnGestureListener : OnGestureListener {
        override fun onDown(e: MotionEvent, node: Node?) {}
        override fun onShowPress(e: MotionEvent, node: Node?) {}
        override fun onSingleTapUp(e: MotionEvent, node: Node?) {}
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, node: Node?, distance: Float2) {}
        override fun onLongPress(e: MotionEvent, node: Node?) {}
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, node: Node?, velocity: Float2) {}
        override fun onSingleTapConfirmed(e: MotionEvent, node: Node?) {}
        override fun onDoubleTap(e: MotionEvent, node: Node?) {}
        override fun onDoubleTapEvent(e: MotionEvent, node: Node?) {}
        override fun onContextClick(e: MotionEvent, node: Node?) {}
        override fun onMoveBegin(detector: MoveGestureDetector, e: MotionEvent, node: Node?) {}
        override fun onMove(detector: MoveGestureDetector, e: MotionEvent, node: Node?) {}
        override fun onMoveEnd(detector: MoveGestureDetector, e: MotionEvent, node: Node?) {}
        override fun onRotateBegin(detector: RotateGestureDetector, e: MotionEvent, node: Node?) {}
        override fun onRotate(detector: RotateGestureDetector, e: MotionEvent, node: Node?) {}
        override fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent, node: Node?) {}
        override fun onScaleBegin(detector: ScaleGestureDetector, e: MotionEvent, node: Node?) {}
        override fun onScale(detector: ScaleGestureDetector, e: MotionEvent, node: Node?) {}
        override fun onScaleEnd(detector: ScaleGestureDetector, e: MotionEvent, node: Node?) {}
    }

    var listener: OnGestureListener? = null
    var touchedNode: Node? = null

    private var lastTouchEvent: MotionEvent? = null

    private val gestureDetector = android.view.GestureDetector(context,
        object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = super.onDown(e).also {
                touchedNode?.onDown(e)
                listener?.onDown(e, touchedNode)
            }

            override fun onShowPress(e: MotionEvent) = super.onShowPress(e).also {
                touchedNode?.onShowPress(e)
                listener?.onShowPress(e, touchedNode)
            }

            override fun onSingleTapUp(e: MotionEvent) = super.onSingleTapUp(e).also {
                touchedNode?.onSingleTapUp(e)
                listener?.onSingleTapUp(e, touchedNode)
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ) = super.onScroll(e1, e2, distanceX, distanceY).also {
                touchedNode?.onScroll(e1, e2, distanceX, distanceY)
                listener?.onScroll(e1, e2, touchedNode, Float2(distanceX, distanceY))
            }

            override fun onLongPress(e: MotionEvent) = super.onLongPress(e).also {
                touchedNode?.onLongPress(e)
                listener?.onLongPress(e, touchedNode)
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ) = super.onFling(e1, e2, velocityX, velocityY).also {
                touchedNode?.onFling(e1, e2, velocityX, velocityY)
                listener?.onFling(e1, e2, touchedNode, Float2(velocityX, velocityY))
            }

            override fun onSingleTapConfirmed(e: MotionEvent) = super.onSingleTapConfirmed(e).also {
                touchedNode?.onSingleTapConfirmed(e)
                listener?.onSingleTapConfirmed(e, touchedNode)
            }

            override fun onDoubleTap(e: MotionEvent) = super.onDoubleTap(e).also {
                touchedNode?.onDoubleTap(e)
                listener?.onDoubleTap(e, touchedNode)
            }

            override fun onDoubleTapEvent(e: MotionEvent) = super.onDoubleTapEvent(e).also {
                touchedNode?.onDoubleTapEvent(e)
                listener?.onDoubleTapEvent(e, touchedNode)
            }

            override fun onContextClick(e: MotionEvent) = super.onContextClick(e).also {
                touchedNode?.onContextClick(e)
                listener?.onContextClick(e, touchedNode)
            }
        }
    )

    private val moveGestureDetector = MoveGestureDetector(context,
        object : MoveGestureDetector.SimpleOnMoveListener {
            var moveBeginEvent: Pair<MotionEvent, Node?>? = null

            override fun onMoveBegin(detector: MoveGestureDetector, e: MotionEvent) =
                super.onMoveBegin(detector, e).also {
                    moveBeginEvent = e to touchedNode
                    touchedNode?.onMoveBegin(detector, e)
                    listener?.onMoveBegin(detector, e, touchedNode)
                }

            override fun onMove(detector: MoveGestureDetector, e: MotionEvent) =
                super.onMove(detector, e).also {
                    moveBeginEvent?.let { (e, node) ->
                        node?.onMove(detector, e)
                        listener?.onMove(detector, e, node)
                    }
                }

            override fun onMoveEnd(detector: MoveGestureDetector, e: MotionEvent) =
                super.onMoveEnd(detector, e).also {
                    moveBeginEvent?.let { (e, node) ->
                        node?.onMoveEnd(detector, e)
                        listener?.onMoveEnd(detector, e, node)
                    }
                    moveBeginEvent = null
                }
        }
    )

    private val rotateGestureDetector = RotateGestureDetector(context,
        object : RotateGestureDetector.SimpleOnRotateListener {
            var rotateBeginEvent: Pair<MotionEvent, Node?>? = null

            override fun onRotateBegin(detector: RotateGestureDetector, e: MotionEvent) =
                super.onRotateBegin(detector, e).also {
                    rotateBeginEvent = e to touchedNode
                    touchedNode?.onRotateBegin(detector, e)
                    listener?.onRotateBegin(detector, e, touchedNode)
                }

            override fun onRotate(detector: RotateGestureDetector, e: MotionEvent) =
                super.onRotate(detector, e).also {
                    rotateBeginEvent?.let { (e, node) ->
                        node?.onRotate(detector, e)
                        listener?.onRotate(detector, e, node)
                    }
                }

            override fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent) {
                rotateBeginEvent?.let { (e, node) ->
                    node?.onRotateEnd(detector, e)
                    listener?.onRotateEnd(detector, e, node)
                }
                rotateBeginEvent = null
            }
        })

    private val scaleGestureDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleListener {
            var scaleBeginEvent: Pair<MotionEvent, Node?>? = null

            override fun onScaleBegin(detector: ScaleGestureDetector, e: MotionEvent) =
                super.onScaleBegin(detector, e).also {
                    scaleBeginEvent = e to touchedNode
                    touchedNode?.onScaleBegin(detector, e)
                    listener?.onScaleBegin(detector, e, touchedNode)
                }

            override fun onScale(detector: ScaleGestureDetector, e: MotionEvent) =
                super.onScale(detector, e).also {
                    scaleBeginEvent?.let { (e, node) ->
                        node?.onScale(detector, e)
                        listener?.onScale(detector, e, node)
                    }
                }

            override fun onScaleEnd(detector: ScaleGestureDetector, e: MotionEvent) {
                scaleBeginEvent?.let { (e, node) ->
                    node?.onScaleEnd(detector, e)
                    listener?.onScaleEnd(detector, e, node)
                }
                scaleBeginEvent = null
            }
        }
    )

    fun onTouchEvent(event: MotionEvent): Boolean {
        lastTouchEvent = event
        nodeSelector(event) { node ->
            touchedNode = node
            gestureDetector.onTouchEvent(event)
            moveGestureDetector.onTouchEvent(event)
            rotateGestureDetector.onTouchEvent(event)
            scaleGestureDetector.onTouchEvent(event)
        }
        return true
    }
}

open class SelectedNodeGestureDetector(
    context: Context,
    var selectedNode: Node?
) : GestureDetector(context, { _, onCompleted ->
    onCompleted(selectedNode)
})

open class HitTestGestureDetector(
    context: Context,
    collisionSystem: CollisionSystem
) : GestureDetector(context, { e, onCompleted ->
    onCompleted(collisionSystem.hitTest(e).firstOrNull { it.node.isTouchable }?.node)
})

open class PickGestureDetector(
    context: Context,
    view: View,
    nodes: () -> List<Node>
) : GestureDetector(context, { e, onCompleted ->
    view.pickNode(e, nodes()) { node, _, _ ->
        onCompleted(node?.takeIf { it.isTouchable })
    }
})
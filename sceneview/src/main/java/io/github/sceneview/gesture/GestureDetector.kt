package io.github.sceneview.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.node.Node

/**
 * Responds to Android touch events with listeners and/or camera manipulator
 *
 * Camera supports one-touch orbit, two-touch pan, and pinch-to-zoom.
 */
open class GestureDetector(
    context: Context,
    collisionSystem: CollisionSystem,
    listener: OnGestureListener
) : GestureDetector(context,
    object : GestureDetector.SimpleOnGestureListener() {
        fun hitTest(e: MotionEvent) = hitTest(collisionSystem, e)

        override fun onDown(e: MotionEvent) = super.onDown(e).also {
            hitTest(e).let {
                it.node?.onDown(it)
                listener.onDown(it)
            }
        }

        override fun onShowPress(e: MotionEvent) = super.onShowPress(e).also {
            hitTest(e).let {
                it.node?.onShowPress(it)
                listener.onShowPress(it)
            }
        }

        override fun onSingleTapUp(e: MotionEvent) = super.onSingleTapUp(e).also {
            hitTest(e).let {
                it.node?.onSingleTapUp(it)
                listener.onSingleTapUp(it)
            }
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ) = super.onScroll(e1, e2, distanceX, distanceY).also {
            hitTest(e1 ?: e2).let { ne1 ->
                hitTest(e2).let { ne2 ->
                    ne1.node?.onScroll(ne1, ne2, distanceX, distanceY)
                    listener.onScroll(ne1, ne2, distanceX, distanceY)
                }
            }
        }

        override fun onLongPress(e: MotionEvent) = super.onLongPress(e).also {
            hitTest(e).let {
                it.node?.onLongPress(it)
                listener.onLongPress(it)
            }
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ) = super.onFling(e1, e2, velocityX, velocityY).also {
            hitTest(e1 ?: e2).let { ne1 ->
                hitTest(e2).let { ne2 ->
                    ne1.node?.onFling(ne1, ne2, velocityX, velocityY)
                    listener.onFling(ne1, ne2, velocityX, velocityY)
                }
            }
        }

        override fun onSingleTapConfirmed(e: MotionEvent) = super.onSingleTapConfirmed(e).also {
            hitTest(e).let {
                it.node?.onSingleTapConfirmed(it)
                listener.onSingleTapConfirmed(it)
            }
        }

        override fun onDoubleTap(e: MotionEvent) = super.onDoubleTap(e).also {
            hitTest(e).let {
                it.node?.onDoubleTap(it)
                listener.onDoubleTap(it)
            }
        }

        override fun onDoubleTapEvent(e: MotionEvent) = super.onDoubleTapEvent(e).also {
            hitTest(e).let {
                it.node?.onDoubleTapEvent(it)
                listener.onDoubleTapEvent(it)
            }
        }

        override fun onContextClick(e: MotionEvent) = super.onContextClick(e).also {
            hitTest(e).let {
                it.node?.onContextClick(it)
                listener.onContextClick(it)
            }
        }
    }) {

    interface OnGestureListener {
        var onDown: List<(e: NodeMotionEvent) -> Unit>
        var onShowPress: List<(e: NodeMotionEvent) -> Unit>
        var onSingleTapUp: List<(e: NodeMotionEvent) -> Unit>
        var onScroll: List<(e1: NodeMotionEvent, e2: NodeMotionEvent, distanceX: Float, distanceY: Float) -> Unit>
        var onLongPress: List<(e: NodeMotionEvent) -> Unit>
        var onFling: List<(e1: NodeMotionEvent, e2: NodeMotionEvent, velocityX: Float, velocityY: Float) -> Unit>
        var onSingleTapConfirmed: List<(e: NodeMotionEvent) -> Unit>
        var onDoubleTap: List<(e: NodeMotionEvent) -> Unit>
        var onDoubleTapEvent: List<(e: NodeMotionEvent) -> Unit>
        var onContextClick: List<(e: NodeMotionEvent) -> Unit>

        var onMove: List<(detector: MoveGestureDetector, e: NodeMotionEvent) -> Unit>
        var onMoveBegin: List<(detector: MoveGestureDetector, e: NodeMotionEvent) -> Unit>
        var onMoveEnd: List<(detector: MoveGestureDetector, e: NodeMotionEvent) -> Unit>

        var onRotate: List<(detector: RotateGestureDetector, e: NodeMotionEvent) -> Unit>
        var onRotateBegin: List<(detector: RotateGestureDetector, e: NodeMotionEvent) -> Unit>
        var onRotateEnd: List<(detector: RotateGestureDetector, e: NodeMotionEvent) -> Unit>

        var onScale: List<(detector: ScaleGestureDetector, e: NodeMotionEvent) -> Unit>
        var onScaleBegin: List<(detector: ScaleGestureDetector, e: NodeMotionEvent) -> Unit>
        var onScaleEnd: List<(detector: ScaleGestureDetector, e: NodeMotionEvent) -> Unit>

        fun onDown(e: NodeMotionEvent)
        fun onShowPress(e: NodeMotionEvent)
        fun onSingleTapUp(e: NodeMotionEvent)
        fun onScroll(e1: NodeMotionEvent, e2: NodeMotionEvent, distanceX: Float, distanceY: Float)
        fun onLongPress(e: NodeMotionEvent)
        fun onFling(e1: NodeMotionEvent, e2: NodeMotionEvent, velocityX: Float, velocityY: Float)
        fun onSingleTapConfirmed(e: NodeMotionEvent)
        fun onDoubleTap(e: NodeMotionEvent)
        fun onDoubleTapEvent(e: NodeMotionEvent)
        fun onContextClick(e: NodeMotionEvent)
        fun onMoveBegin(detector: MoveGestureDetector, e: NodeMotionEvent)
        fun onMove(detector: MoveGestureDetector, e: NodeMotionEvent)
        fun onMoveEnd(detector: MoveGestureDetector, e: NodeMotionEvent)
        fun onRotateBegin(detector: RotateGestureDetector, e: NodeMotionEvent)
        fun onRotate(detector: RotateGestureDetector, e: NodeMotionEvent)
        fun onRotateEnd(detector: RotateGestureDetector, e: NodeMotionEvent)
        fun onScaleBegin(detector: ScaleGestureDetector, e: NodeMotionEvent)
        fun onScale(detector: ScaleGestureDetector, e: NodeMotionEvent)
        fun onScaleEnd(detector: ScaleGestureDetector, e: NodeMotionEvent)
    }

    class SimpleOnGestureListener : OnGestureListener {

        override var onDown = listOf<(e: NodeMotionEvent) -> Unit>()
        override var onShowPress = listOf<(e: NodeMotionEvent) -> Unit>()
        override var onSingleTapUp = listOf<(e: NodeMotionEvent) -> Unit>()
        override var onScroll =
            listOf<(e1: NodeMotionEvent, e2: NodeMotionEvent, distanceX: Float, distanceY: Float) -> Unit>()
        override var onLongPress = listOf<(e: NodeMotionEvent) -> Unit>()
        override var onFling =
            listOf<(e1: NodeMotionEvent, e2: NodeMotionEvent, velocityX: Float, velocityY: Float) -> Unit>()
        override var onSingleTapConfirmed = listOf<(e: NodeMotionEvent) -> Unit>()
        override var onDoubleTap = listOf<(e: NodeMotionEvent) -> Unit>()
        override var onDoubleTapEvent = listOf<(e: NodeMotionEvent) -> Unit>()
        override var onContextClick = listOf<(e: NodeMotionEvent) -> Unit>()

        override var onMove = listOf<(detector: MoveGestureDetector, e: NodeMotionEvent) -> Unit>()
        override var onMoveBegin =
            listOf<(detector: MoveGestureDetector, e: NodeMotionEvent) -> Unit>()
        override var onMoveEnd =
            listOf<(detector: MoveGestureDetector, e: NodeMotionEvent) -> Unit>()

        override var onRotate =
            listOf<(detector: RotateGestureDetector, e: NodeMotionEvent) -> Unit>()
        override var onRotateBegin =
            listOf<(detector: RotateGestureDetector, e: NodeMotionEvent) -> Unit>()
        override var onRotateEnd =
            listOf<(detector: RotateGestureDetector, e: NodeMotionEvent) -> Unit>()

        override var onScale =
            listOf<(detector: ScaleGestureDetector, e: NodeMotionEvent) -> Unit>()
        override var onScaleBegin =
            listOf<(detector: ScaleGestureDetector, e: NodeMotionEvent) -> Unit>()
        override var onScaleEnd =
            listOf<(detector: ScaleGestureDetector, e: NodeMotionEvent) -> Unit>()

        override fun onDown(e: NodeMotionEvent) {
            onDown.forEach { it(e) }
        }

        override fun onShowPress(e: NodeMotionEvent) {
            onShowPress.forEach { it(e) }
        }

        override fun onSingleTapUp(e: NodeMotionEvent) {
            onSingleTapUp.forEach { it(e) }
        }

        override fun onScroll(
            e1: NodeMotionEvent,
            e2: NodeMotionEvent,
            distanceX: Float,
            distanceY: Float
        ) {
            onScroll.forEach { it(e1, e2, distanceX, distanceY) }
        }

        override fun onLongPress(e: NodeMotionEvent) {
            onLongPress.forEach { it(e) }
        }

        override fun onFling(
            e1: NodeMotionEvent,
            e2: NodeMotionEvent,
            velocityX: Float,
            velocityY: Float
        ) {
            onFling.forEach { it(e1, e2, velocityX, velocityY) }
        }

        override fun onSingleTapConfirmed(e: NodeMotionEvent) {
            onSingleTapConfirmed.forEach { it(e) }
        }

        override fun onDoubleTap(e: NodeMotionEvent) {
            onDoubleTap.forEach { it(e) }
        }

        override fun onDoubleTapEvent(e: NodeMotionEvent) {
            onDoubleTapEvent.forEach { it(e) }
        }

        override fun onContextClick(e: NodeMotionEvent) {
            onContextClick.forEach { it(e) }
        }

        override fun onMoveBegin(detector: MoveGestureDetector, e: NodeMotionEvent) {
            onMoveBegin.forEach { it(detector, e) }
        }

        override fun onMove(detector: MoveGestureDetector, e: NodeMotionEvent) {
            onMove.forEach { it(detector, e) }
        }

        override fun onMoveEnd(detector: MoveGestureDetector, e: NodeMotionEvent) {
            onMoveEnd.forEach { it(detector, e) }
        }

        override fun onRotateBegin(detector: RotateGestureDetector, e: NodeMotionEvent) {
            onRotateBegin.forEach { it(detector, e) }
        }

        override fun onRotate(detector: RotateGestureDetector, e: NodeMotionEvent) {
            onRotate.forEach { it(detector, e) }
        }

        override fun onRotateEnd(detector: RotateGestureDetector, e: NodeMotionEvent) {
            onRotateEnd.forEach { it(detector, e) }
        }

        override fun onScaleBegin(detector: ScaleGestureDetector, e: NodeMotionEvent) {
            onScaleBegin.forEach { it(detector, e) }
        }

        override fun onScale(detector: ScaleGestureDetector, e: NodeMotionEvent) {
            onScale.forEach { it(detector, e) }
        }

        override fun onScaleEnd(detector: ScaleGestureDetector, e: NodeMotionEvent) {
            onScaleEnd.forEach { it(detector, e) }
        }
    }

    val moveGestureDetector = MoveGestureDetector(context,
        object : MoveGestureDetector.SimpleOnMoveListener {
            var moveBeginEvent: NodeMotionEvent? = null

            fun hitTest(e: MotionEvent) = hitTest(collisionSystem, e)

            override fun onMoveBegin(detector: MoveGestureDetector, e: MotionEvent) =
                super.onMoveBegin(detector, e).also {
                    hitTest(e).let {
                        moveBeginEvent = it
                        it.node?.onMoveBegin(detector, it)
                        listener.onMoveBegin(detector, it)
                    }
                }

            override fun onMove(detector: MoveGestureDetector, e: MotionEvent) =
                super.onMove(detector, e).also {
                    moveBeginEvent?.let { NodeMotionEvent(e, it.node) }?.let { ne ->
                        ne.node?.onMove(detector, ne)
                        listener.onMove(detector, ne)
                    }
                }

            override fun onMoveEnd(detector: MoveGestureDetector, e: MotionEvent) {
                moveBeginEvent?.let { NodeMotionEvent(e, it.node) }?.let { ne ->
                    ne.node?.onMoveEnd(detector, ne)
                    listener.onMoveEnd(detector, ne)
                }
                moveBeginEvent = null
            }
        })

    val rotateGestureDetector = RotateGestureDetector(context,
        object : RotateGestureDetector.SimpleOnRotateListener {
            var rotateBeginEvent: NodeMotionEvent? = null

            fun hitTest(e: MotionEvent) = hitTest(collisionSystem, e)

            override fun onRotateBegin(detector: RotateGestureDetector, e: MotionEvent) =
                super.onRotateBegin(detector, e).also {
                    hitTest(e).let {
                        rotateBeginEvent = it
                        it.node?.onRotateBegin(detector, it)
                        listener.onRotateBegin(detector, it)
                    }
                }

            override fun onRotate(detector: RotateGestureDetector, e: MotionEvent) =
                super.onRotate(detector, e).also {
                    rotateBeginEvent?.let { NodeMotionEvent(e, it.node) }
                        ?.let { ne ->
                            ne.node?.onRotate(detector, ne)
                            listener.onRotate(detector, ne)
                        }
                }

            override fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent) {
                rotateBeginEvent?.let { NodeMotionEvent(e, it.node) }?.let { ne ->
                    ne.node?.onRotateEnd(detector, ne)
                    listener.onRotateEnd(detector, ne)
                }
                rotateBeginEvent = null
            }
        })

    val scaleGestureDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.OnScaleListener {
            var scaleBeginEvent: NodeMotionEvent? = null

            fun hitTest(e: MotionEvent) = hitTest(collisionSystem, e)

            override fun onScaleBegin(detector: ScaleGestureDetector, e: MotionEvent) {
                hitTest(e).let {
                    scaleBeginEvent = it
                    it.node?.onScaleBegin(detector, it)
                    listener.onScaleBegin(detector, it)
                }
            }

            override fun onScale(detector: ScaleGestureDetector, e: MotionEvent) {
                scaleBeginEvent?.let { NodeMotionEvent(e, it.node) }?.let { ne ->
                    ne.node?.onScale(detector, ne)
                    listener.onScale(detector, ne)
                }
            }

            override fun onScaleEnd(detector: ScaleGestureDetector, e: MotionEvent) {
                scaleBeginEvent?.let { NodeMotionEvent(e, it.node) }?.let { ne ->
                    ne.node?.onScaleEnd(detector, ne)
                    listener.onScaleEnd(detector, ne)
                }
                scaleBeginEvent = null
            }
        })

    var lastTouchEvent: MotionEvent? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event).also {
            lastTouchEvent = event
            moveGestureDetector.onTouchEvent(event)
            rotateGestureDetector.onTouchEvent(event)
            scaleGestureDetector.onTouchEvent(event)
        }
    }

    companion object {
        fun hitTest(collisionSystem: CollisionSystem, e: MotionEvent) =
            NodeMotionEvent(e, collisionSystem.hitTest(e).firstOrNull {
                it.node?.isTouchable == true
            }?.node)
    }
}

data class NodeMotionEvent(
    val motionEvent: MotionEvent,
    val node: Node? = null
)
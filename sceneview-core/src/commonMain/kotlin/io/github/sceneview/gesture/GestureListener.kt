package io.github.sceneview.gesture

import dev.romainguy.kotlin.math.Float2

/**
 * Cross-platform touch event abstraction.
 *
 * Wraps platform-specific touch events (Android MotionEvent, iOS UITouch)
 * into a common representation.
 */
data class TouchEvent(
    val x: Float,
    val y: Float,
    val action: TouchAction,
    val pointerCount: Int = 1
)

enum class TouchAction {
    DOWN,
    MOVE,
    UP,
    CANCEL
}

/**
 * Cross-platform gesture listener interface.
 *
 * Platform-specific gesture detectors translate native touch events
 * into these callbacks. The node parameter is the hit-tested scene node
 * (null if no node was hit).
 *
 * @param N The platform-specific node type.
 */
interface OnGestureListener<N> {
    fun onDown(event: TouchEvent, node: N?): Boolean = false
    fun onSingleTapUp(event: TouchEvent, node: N?): Boolean = false
    fun onSingleTapConfirmed(event: TouchEvent, node: N?): Boolean = false
    fun onDoubleTap(event: TouchEvent, node: N?): Boolean = false
    fun onLongPress(event: TouchEvent, node: N?) {}
    fun onScroll(event: TouchEvent, node: N?, distance: Float2): Boolean = false
    fun onFling(event: TouchEvent, node: N?, velocity: Float2): Boolean = false
    fun onMoveBegin(event: TouchEvent, node: N?): Boolean = false
    fun onMove(event: TouchEvent, node: N?): Boolean = false
    fun onMoveEnd(event: TouchEvent, node: N?) {}
    fun onRotateBegin(event: TouchEvent, node: N?): Boolean = false
    fun onRotate(event: TouchEvent, node: N?): Boolean = false
    fun onRotateEnd(event: TouchEvent, node: N?) {}
    fun onScaleBegin(event: TouchEvent, node: N?): Boolean = false
    fun onScale(event: TouchEvent, node: N?): Boolean = false
    fun onScaleEnd(event: TouchEvent, node: N?) {}
}

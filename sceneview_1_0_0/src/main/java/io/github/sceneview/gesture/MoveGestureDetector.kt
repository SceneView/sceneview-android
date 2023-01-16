package io.github.sceneview.gesture

import android.content.Context
import android.view.MotionEvent
import io.github.sceneview.gesture.MoveGestureDetector.OnMoveListener
import kotlin.math.pow

private const val MIN_DRAG_DISTANCE = 1000f

/**
 * Detects move transformation gestures using the supplied [MotionEvent]s
 *
 * The [OnMoveListener] callback will notify users when a particular gesture event has
 * occurred.
 *
 * This class should only be used with [MotionEvent]s reported via touch.
 *
 * To use this class:
 * - Create an instance of the [MoveGestureDetector] for your [android.view.View]
 * - In the [android.view.View.onTouchEvent] method ensure you call [onTouchEvent].
 * The methods defined in your callback will be executed when the events occur.
 *
 * @param context the application's context
 * @param listener the listener invoked for all the callbacks, this must not be null
 */
open class MoveGestureDetector(private val context: Context, private val listener: OnMoveListener) {

    /**
     * The listener for receiving notifications when gestures occur
     *
     * If you want to listen for the different gestures then implement this interface.
     *
     * An application will receive events in the following order:
     * - One [onMoveBegin]
     * - Zero or more [onMove]
     * - One [onMoveEnd]
     */
    interface OnMoveListener {
        /**
         * Responds to moving events for a gesture in progress
         *
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         * @return Whether or not the detector should consider this event as handled.
         * If an event was not handled, the detector will continue to accumulate movement until an
         * event is handled. This can be useful if an application, for example, only wants to update
         * rotation factors if the change is greater than 0.01.
         */
        fun onMove(detector: MoveGestureDetector, e: MotionEvent)

        /**
         * Responds to the beginning of a moving gesture
         *
         * Reported by new pointers going down.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         * @return Whether or not the detector should continue recognizing this gesture.
         * For example, if a gesture is beginning with a focal point outside of a region where it
         * makes sense, onRotateBegin() may return false to ignore the rest of the gesture.
         */
        fun onMoveBegin(detector: MoveGestureDetector, e: MotionEvent)

        /**
         * Responds to the end of a move gesture
         *
         * Reported by existing pointers going up.
         *
         * Once a scale has ended, [focusX] and [focusY] will return focal point of the pointers
         * remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         */
        fun onMoveEnd(detector: MoveGestureDetector, e: MotionEvent)
    }

    /**
     * A convenience class to extend when you only want to listen for a subset of
     * move-related events
     *
     * This implements all methods in [OnMoveListener] but does nothing.
     * [OnMoveListener.onMove] returns `false` so that a subclass can retrieve the accumulated
     * moving factor in an overridden [OnMoveListener.onMoveEnd].
     * [OnMoveListener.onMoveBegin] returns `true`.
     */
    interface SimpleOnMoveListener : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector, e: MotionEvent) {}
        override fun onMoveBegin(detector: MoveGestureDetector, e: MotionEvent) {}
        override fun onMoveEnd(detector: MoveGestureDetector, e: MotionEvent) {}
    }

    private var gestureInProgress = false
    private var detectionInProgress = false

    var lastDistanceX: Float? = null
    var lastDistanceY: Float? = null

    var firstMotionEvent: MotionEvent? = null
        private set
    var currentMotionEvent: MotionEvent? = null
        private set

    private fun update(event: MotionEvent) {
        if (firstMotionEvent == null) {
            firstMotionEvent = MotionEvent.obtain(event)
        }
        currentMotionEvent?.recycle()
        currentMotionEvent = MotionEvent.obtain(event)

        val firstEvent = firstMotionEvent ?: return
        val lastEvent = currentMotionEvent ?: return
        lastDistanceX = lastEvent.x - firstEvent.x
        lastDistanceY = lastEvent.y - firstEvent.y
    }

    private fun reset() {
        currentMotionEvent?.recycle()
        currentMotionEvent = null
        firstMotionEvent?.recycle()
        firstMotionEvent = null
        lastDistanceX = 0f
        lastDistanceY = 0f
        gestureInProgress = false
        detectionInProgress = false
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            if (gestureInProgress || detectionInProgress) {
                listener.onMoveEnd(this, event)
                reset()
                return true
            }
            return false
        }
        return if (gestureInProgress || detectionInProgress) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    update(event)
                    if (detectionInProgress) {
                        val distanceSquared =
                            (lastDistanceX ?: 0f).pow(2) + (lastDistanceY ?: 0f).pow(2)
                        if (distanceSquared >= MIN_DRAG_DISTANCE) {
                            detectionInProgress = false
                            gestureInProgress = true
                            listener.onMoveBegin(this, event)
                        } else {
                            return false
                        }
                    }
                    listener.onMove(this, event)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                    reset()
                    listener.onMoveEnd(this, event)
                    true
                }
                else -> false
            }
        } else {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    update(event)
                    detectionInProgress = true
                    false
                }
                else -> false
            }
        }
    }
}

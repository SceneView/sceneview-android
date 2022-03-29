package io.github.sceneview.interaction

import android.content.Context
import android.os.Handler
import android.view.MotionEvent
import io.github.sceneview.interaction.MoveGestureDetector.OnMoveGestureListener


/**
 * ### Detects move transformation gestures using the supplied [MotionEvent]s
 *
 * The [OnMoveGestureListener] callback will notify users when a particular gesture event has
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
 * @param handler the handler to use for running deferred listener events
 */
class MoveGestureDetector(
    val context: Context,
    var listener: OnMoveGestureListener,
    var handler: Handler? = null
) {

    /**
     * ### The listener for receiving notifications when gestures occur
     *
     * If you want to listen for all the different gestures then implement this interface.
     * If you only want to listen for a subset it might be easier to extend
     * [SimpleOnMoveGestureListener].
     *
     * An application will receive events in the following order:
     * - One [onMoveBegin]
     * - Zero or more [onMove]
     * - One [onMoveEnd]
     */
    interface OnMoveGestureListener {
        /**
         * ### Responds to moving events for a gesture in progress
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
        fun onMove(detector: MoveGestureDetector): Boolean

        /**
         * ### Responds to the beginning of a moving gesture
         *
         * Reported by new pointers going down.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         * @return Whether or not the detector should continue recognizing this gesture.
         * For example, if a gesture is beginning with a focal point outside of a region where it
         * makes sense, onRotateBegin() may return false to ignore the rest of the gesture.
         */
        fun onMoveBegin(detector: MoveGestureDetector): Boolean

        /**
         * ### Responds to the end of a move gesture
         *
         * Reported by existing pointers going up.
         *
         * Once a scale has ended, [focusX] and [focusY] will return focal point of the pointers
         * remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         */
        fun onMoveEnd(detector: MoveGestureDetector)
    }

    /**
     * ### A convenience class to extend when you only want to listen for a subset of
     * move-related events.
     *
     * This implements all methods in [OnMoveGestureListener] but does nothing.
     * [OnMoveGestureListener.onMove] returns `false` so that a subclass can retrieve the
     * accumulated moving factor in an overridden [OnMoveGestureListener.onMoveEnd].
     * [OnMoveGestureListener.onMoveBegin] returns `true`.
     */
    open class SimpleOnMoveGestureListener : OnMoveGestureListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector): Boolean {
            return true
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
        }
    }


    fun onTouchEvent(event: MotionEvent): Boolean {
        //TODO
        return true
    }
}
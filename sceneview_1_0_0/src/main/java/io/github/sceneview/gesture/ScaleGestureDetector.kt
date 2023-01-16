package io.github.sceneview.gesture

import android.content.Context
import android.view.MotionEvent

/**
 * Detects scaling transformation gestures using the supplied [MotionEvent]s
 *
 * The [android.view.ScaleGestureDetector.OnScaleGestureListener] callback will notify users when
 * a particular gesture event has occurred.
 *
 * This class should only be used with [MotionEvent]s reported via touch.
 *
 * To use this class:
 * - Create an instance of the {@code ScaleGestureDetector} for your [android.view.View]
 * - In the [android.view.View.onTouchEvent] method ensure you call [onTouchEvent].
 * The methods defined in your callback will be executed when the events occur.
 */
open class ScaleGestureDetector(context: Context, listener: OnScaleListener) :
    android.view.ScaleGestureDetector(
        context,
        object : OnScaleGestureListener {
            override fun onScaleBegin(detector: android.view.ScaleGestureDetector): Boolean {
                (detector as ScaleGestureDetector).let { listener.onScaleBegin(it, it.event) }
                // true means that we always start a gesture
                return true
            }

            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                (detector as ScaleGestureDetector).let { listener.onScale(it, it.event) }
                // true means that we work with increments rather than an accumulated value
                return true
            }

            override fun onScaleEnd(detector: android.view.ScaleGestureDetector) {
                (detector as ScaleGestureDetector).let { listener.onScaleEnd(it, it.event) }
            }
        }
    ) {

    lateinit var event: MotionEvent

    override fun onTouchEvent(event: MotionEvent): Boolean {
        this.event = event
        return super.onTouchEvent(event)
    }

    /**
     * The listener for receiving notifications when gestures occur
     *
     * If you want to listen for all the different gestures then implement this interface.
     *
     * An application will receive events in the following order:
     * - One [onScaleBegin]
     * - Zero or more [onScale]
     * - One [onScaleEnd]
     *
     */
    interface OnScaleListener {
        /**
         * Responds to the beginning of a scaling gesture. Reported by new pointers going down
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         */
        fun onScaleBegin(detector: ScaleGestureDetector, e: MotionEvent): Boolean

        /**
         * Responds to scaling events for a gesture in progress. Reported by pointer motion
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         */
        fun onScale(detector: ScaleGestureDetector, e: MotionEvent): Boolean

        /**
         * Responds to the end of a scale gesture. Reported by existing pointers going up
         *
         * Once a scale has ended, [ScaleGestureDetector.getFocusX] and
         * [ScaleGestureDetector.getFocusY] will return focal point of the pointers remaining on
         * the screen.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         */
        fun onScaleEnd(detector: ScaleGestureDetector, e: MotionEvent)
    }

    /**
     * A convenience class to extend when you only want to listen for a subset of  scale-related
     * events
     *
     * This implements all methods in [OnScaleListener] but does nothing.
     * [OnScaleListener.onScale] returns `false` so that a subclass can retrieve the accumulated
     * scale factor in an overridden [OnScaleListener.onScaleEnd].
     * [OnScaleListener.onScaleBegin] returns `true`.
     */
    interface SimpleOnScaleListener : OnScaleListener {
        override fun onScaleBegin(detector: ScaleGestureDetector, e: MotionEvent) = false
        override fun onScale(detector: ScaleGestureDetector, e: MotionEvent) = true
        override fun onScaleEnd(detector: ScaleGestureDetector, e: MotionEvent) {}
    }
}
package io.github.sceneview.gesture

import android.content.Context
import android.view.MotionEvent
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.degrees
import io.github.sceneview.gesture.RotateGestureDetector.OnRotateListener
import io.github.sceneview.math.Direction
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Detects rotation transformation gestures using the supplied [MotionEvent]s
 *
 * The [OnRotateListener] callback will notify users when a particular gesture event has
 * occurred
 *
 * This class should only be used with [MotionEvent]s reported via touch.
 *
 * To use this class:
 * - Create an instance of the [RotateGestureDetector] for your [android.view.View].
 * - In the [android.view.View.onTouchEvent] method ensure you call [onTouchEvent].
 * The methods defined in your callback will be executed when the events occur.
 *
 * @param context the application's context
 * @param listener the listener invoked for all the callbacks, this must not be null
 */
open class RotateGestureDetector(
    private val context: Context,
    private val listener: OnRotateListener
) {

    /**
     * The listener for receiving notifications when gestures occur
     *
     * If you want to listen for the different gestures then implement this interface.
     *
     * An application will receive events in the following order:
     * - One [onRotateBegin]
     * - Zero or more [onRotate]
     * - One [onRotateEnd]
     */
    interface OnRotateListener {
        /**
         * Responds to rotating events for a gesture in progress
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
        fun onRotate(detector: RotateGestureDetector, e: MotionEvent): Boolean

        /**
         * Responds to the beginning of a scaling gesture
         *
         * Reported by new pointers going down.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         * @return Whether or not the detector should continue recognizing this gesture.
         * For example, if a gesture is beginning with a focal point outside of a region where it
         * makes sense, onRotateBegin() may return false to ignore the rest of the gesture.
         */
        fun onRotateBegin(detector: RotateGestureDetector, e: MotionEvent): Boolean

        /**
         * Responds to the end of a scale gesture
         *
         * Reported by existing pointers going up.
         *
         * Once a scale has ended, [focusX] and [focusY] will return focal point of the pointers
         * remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         */
        fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent)
    }

    /**
     * A convenience class to extend when you only want to listen for a subset of
     * rotation-related events
     *
     * This implements all methods in [OnRotateListener] but does nothing.
     * [OnRotateListener.onRotate] returns `false` so that a subclass can retrieve the accumulated
     * rotation factor in an overridden [OnRotateListener.onRotateEnd].
     * [OnRotateListener.onRotateBegin] returns `true`.
     */
    interface SimpleOnRotateListener : OnRotateListener {
        override fun onRotate(detector: RotateGestureDetector, e: MotionEvent) = false
        override fun onRotateBegin(detector: RotateGestureDetector, e: MotionEvent) = true
        override fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent) {}
    }

    private var previousEvent: MotionEvent? = null
    private var currentEvent: MotionEvent? = null

    private var previousFingerDiffX = 0f
    private var previousFingerDiffY = 0f
    private var currentFingerDiffX = 0f
    private var currentFingerDiffY = 0f

    private var previousPressure = 0f
    private var currentPressure = 0f

    /**
     * `true` if a rotate gesture is in progress
     */
    var isGestureInProgress: Boolean = false
        private set

    /**
     * The X coordinate of the current gesture's focal point in pixels
     *
     * If a gesture is in progress, the focal point is between each of the pointers forming the
     * gesture.
     * If a gesture is ending, the focal point is the location of the remaining pointer on the
     * screen.
     */
    var focusX = 0f
        private set

    /**
     * The Y coordinate of the current gesture's focal point in pixels
     *
     * If a gesture is in progress, the focal point is between each of the pointers forming the
     * gesture.
     * If a gesture is ending, the focal point is the location of the remaining pointer on the
     * screen.
     */
    var focusY = 0f
        private set

    /**
     * The current distance in pixels between the two pointers forming the gesture in progress
     */
    var currentSpan = 0f
        private set

    /**
     * The previous distance in pixels between the two pointers forming the gesture in progress
     */
    var previousSpan = 0f
        private set

    /**
     * The average angle in radians between each of the pointers forming the gesture in progress
     * through the focal point
     */
    var currentAngle = 0f
        private set(value) {
            lastAngle = field
            field = value
        }

    /**
     * The previous average angle in radians between each of the pointers forming the gesture in
     * progress through the focal point.
     */
    var previousAngle = 0f
        private set

    var lastAngle = 0.0f

    val deltaRadians get() = currentAngle - lastAngle
    val deltaQuaternion
        get() = Quaternion.fromAxisAngle(Direction(y = 1.0f), degrees(-deltaRadians))

    /**
     * The rotation factor in degrees from the previous rotation event to the current event
     *
     * This value is defined as ([currentAngle] / [previousAngle]).
     */
    var rotation = 0f
        private set

    /**
     * The initial rotation threshold in degrees for detecting a gesture
     *
     * This value is selected to avoid conflicts with the [android.view.ScaleGestureDetector]
     */
    var rotationThreshold = 2f

    /**
     * The time difference in milliseconds between the previous accepted GestureDetector event
     * and the current GestureDetector event
     */
    var timeDelta = 0L
        private set

    /**
     * Accepts MotionEvents and dispatches events to a [OnRotateListener] when
     * appropriate
     *
     * Applications should pass a complete and consistent event stream to this method.
     * A complete and consistent event stream involves all MotionEvents from the initial
     * ACTION_DOWN to the final ACTION_UP or ACTION_CANCEL.
     *
     * @param event The event to process
     * @return `true` if the event was processed and the detector wants to receive the rest of the
     * MotionEvents in this event stream.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        val actionCode = event.action and MotionEvent.ACTION_MASK
        if (!isGestureInProgress) {
            when (actionCode) {
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount != 2) return false
                    if (previousEvent == null) {
                        previousEvent = MotionEvent.obtain(event)
                        return false
                    }
                    update(event)
                    if (rotation.absoluteValue > rotationThreshold) {
                        listener.onRotateBegin(this, event)
                        isGestureInProgress = true
                    } else {
                        return false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reset()
            }
        } else {
            when (actionCode) {
                MotionEvent.ACTION_POINTER_UP -> {
                    // Gesture ended
                    update(event)
                    // Set focus point to the remaining finger
                    val id = if (event.actionIndex == 0) 1 else 0
                    focusX = event.getX(id)
                    focusY = event.getY(id)

                    lastAngle = 0f
                    listener.onRotateEnd(this, event)
                    reset()
                }
                MotionEvent.ACTION_CANCEL -> {
                    lastAngle = 0f
                    listener.onRotateEnd(this, event)
                    reset()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount != 2) {
                        lastAngle = 0f
                        listener.onRotateEnd(this, event)
                        reset()
                        return false
                    }

                    update(event)
                    // Only accept the event if our relative pressure is within
                    // a certain limit. This can help filter shaky data as a
                    // finger is lifted.
                    if (currentPressure / previousPressure > PRESSURE_THRESHOLD) {
                        val updatePrevious = listener.onRotate(this, event)
                        if (updatePrevious) {
                            previousEvent?.recycle()
                            previousEvent = MotionEvent.obtain(event)
                        }
                    }
                }
            }
        }
        return true
    }

    private fun update(event: MotionEvent) {
        val previousEvent = previousEvent ?: return

        // Reset mCurrEvent
        currentEvent?.recycle()
        currentEvent = MotionEvent.obtain(event)

        // Previous
        previousFingerDiffX = previousEvent.getX(1) - previousEvent.getX(0)
        previousFingerDiffY = previousEvent.getY(1) - previousEvent.getY(0)

        // Current
        currentFingerDiffX = event.getX(1) - event.getX(0)
        currentFingerDiffY = event.getY(1) - event.getY(0)

        focusX = event.getX(0) + currentFingerDiffX * 0.5f
        focusY = event.getY(0) + currentFingerDiffY * 0.5f

        currentSpan =
            sqrt(currentFingerDiffX * currentFingerDiffX + currentFingerDiffY * currentFingerDiffY)
        previousSpan =
            sqrt(previousFingerDiffX * previousFingerDiffX + currentFingerDiffY * currentFingerDiffY)

        previousAngle = atan2(previousFingerDiffY, currentFingerDiffX)
        currentAngle = atan2(currentFingerDiffY, currentFingerDiffX)
        rotation = degrees(currentAngle - previousAngle)

        // Pressure
        currentPressure = event.getPressure(event.actionIndex)
        previousPressure = previousEvent.getPressure(previousEvent.actionIndex)
        // Delta time
        timeDelta = event.eventTime - previousEvent.eventTime
    }

    fun reset() {
        previousEvent?.recycle()
        previousEvent = null
        currentEvent?.recycle()
        currentEvent = null
        isGestureInProgress = false
    }

    companion object {
        /**
         * This value is the threshold ratio between our previous combined pressure and the current
         * combined pressure. We will only fire an onRotate event if the computed ratio between the
         * current and previous event pressures is greater than this value. When pressure decreases
         * rapidly between events the position values can often be imprecise, as it usually
         * indicates that the user is in the process of lifting a pointer off of the device.
         * Its value was tuned experimentally.
         */
        private const val PRESSURE_THRESHOLD = 0.67f
    }
}
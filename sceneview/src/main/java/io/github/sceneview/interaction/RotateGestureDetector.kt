package io.github.sceneview.interaction

import android.content.Context
import android.os.Handler
import android.view.MotionEvent
import io.github.sceneview.interaction.RotateGestureDetector.OnRotateGestureListener
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.sqrt


/**
 * ### Detects rotation transformation gestures using the supplied [MotionEvent]s
 *
 * The [OnRotateGestureListener] callback will notify users when a particular gesture event has
 * occurred.
 *
 * This class should only be used with [MotionEvent]s reported via touch.
 *
 * To use this class:
 * - Create an instance of the [RotateGestureDetector] for your [android.view.View]
 * - In the [android.view.View.onTouchEvent] method ensure you call [onTouchEvent].
 * The methods defined in your callback will be executed when the events occur.
 *
 * @param context the application's context
 * @param listener the listener invoked for all the callbacks, this must not be null
 * @param handler the handler to use for running deferred listener events
 */
class RotateGestureDetector(
    val context: Context,
    var listener: OnRotateGestureListener,
    var handler: Handler? = null
) {

    /**
     * ### The listener for receiving notifications when gestures occur
     *
     * If you want to listen for all the different gestures then implement this interface.
     * If you only want to listen for a subset it might be easier to extend
     * [SimpleOnRotateGestureListener].
     *
     * An application will receive events in the following order:
     * - One [onRotateBegin]
     * - Zero or more [onRotate]
     * - One [onRotateEnd]
     */
    interface OnRotateGestureListener {
        /**
         * ### Responds to rotating events for a gesture in progress
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
        fun onRotate(detector: RotateGestureDetector): Boolean

        /**
         * ### Responds to the beginning of a scaling gesture
         *
         * Reported by new pointers going down.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         * @return Whether or not the detector should continue recognizing this gesture.
         * For example, if a gesture is beginning with a focal point outside of a region where it
         * makes sense, onRotateBegin() may return false to ignore the rest of the gesture.
         */
        fun onRotateBegin(detector: RotateGestureDetector): Boolean

        /**
         * ### Responds to the end of a scale gesture
         *
         * Reported by existing pointers going up.
         *
         * Once a scale has ended, [focusX] and [focusY] will return focal point of the pointers
         * remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         */
        fun onRotateEnd(detector: RotateGestureDetector)
    }

    /**
     * ### A convenience class to extend when you only want to listen for a subset of
     * rotation-related events.
     *
     * This implements all methods in [OnRotateGestureListener] but does nothing.
     * [OnRotateGestureListener.onRotate] returns `false` so that a subclass can retrieve the
     * accumulated rotation factor in an overridden [OnRotateGestureListener.onRotateEnd].
     * [OnRotateGestureListener.onRotateBegin] returns `true`.
     */
    open class SimpleOnRotateGestureListener : OnRotateGestureListener {
        override fun onRotate(detector: RotateGestureDetector): Boolean {
            return false
        }

        override fun onRotateBegin(detector: RotateGestureDetector): Boolean {
            return true
        }

        override fun onRotateEnd(detector: RotateGestureDetector) {
        }
    }

    protected var previousEvent: MotionEvent? = null
    protected var currentEvent: MotionEvent? = null

    var currentPressure: Float? = null
    var previousPressure: Float? = null

    /**
     * ### `true` if a rotate gesture is in progress.
     */
    var isGestureInProgress: Boolean = false
        private set

    /**
     * ### The X coordinate of the current gesture's focal point in pixels
     *
     * If a gesture is in progress, the focal point is between each of the pointers forming the
     * gesture.
     * If a gesture is ending, the focal point is the location of the  remaining pointer on the
     * screen.
     * If [isGestureInProgress] would return false, the result of this function is null.
     */
    var focusX: Float? = null
        private set

    /**
     * ### The Y coordinate of the current gesture's focal point in pixels
     *
     * If a gesture is in progress, the focal point is between each of the pointers forming the
     * gesture.
     * If a gesture is ending, the focal point is the location of the  remaining pointer on the
     * screen.
     * If [isGestureInProgress] would return false, the result of this function is null.
     */
    var focusY: Float? = null
        private set

    private var previousFingerDiffX: Float? = null
    private var previousFingerDiffY: Float? = null
    private var currentFingerDiffX: Float? = null
    private var currentFingerDiffY: Float? = null

    /**
     * ### The current distance in pixels between the two pointers forming the gesture in progress
     */
    var currentSpan: Float? = null
        private set

    /**
     * ### The previous distance in pixels between the two pointers forming the gesture in progress
     */
    var previousSpan: Float? = null
        private set

    /**
     * ### The average angle in radians between each of the pointers forming the gesture in progress
     * through the focal point
     */
    var currentAngle = 0.0f
        private set

    /**
     * ### The previous average angle in radians between each of the pointers forming the gesture in
     * progress through the focal point.
     */
    var previousAngle = 0.0f
        private set

    /**
     * ### The rotation factor in degrees from the previous rotation event to the current event
     *
     * This value is defined as ([currentAngle] / [previousAngle]).
     */
    var rotation: Float? = null
        private set


    /**
     * ### The time difference in milliseconds between the previous accepted GestureDetector event
     * and the current GestureDetector event
     */
    var timeDelta: Long? = null
        private set

    /**
     * ### Accepts MotionEvents and dispatches events to a [OnRotateGestureListener] when
     * appropriate
     *
     * Applications should pass a complete and consistent event stream to this method.
     * A complete and consistent event stream involves all MotionEvents from the initial
     * ACTION_DOWN to the final ACTION_UP or ACTION_CANCEL.
     *
     * @param event The event to process
     * @return true if the event was processed and the detector wants to receive the rest of the
     * MotionEvents in this event stream.
     */
    open fun onTouchEvent(event: MotionEvent): Boolean {
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
                    if (currentAngle.absoluteValue > 0.1) {
                        listener.onRotateBegin(this)
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
                    // Set focus point to the remaining finger
                    val id = if ((event.action and MotionEvent.ACTION_POINTER_INDEX_MASK
                                shr MotionEvent.ACTION_POINTER_INDEX_SHIFT) == 0
                    ) 1 else 0
                    focusX = event.getX(id)
                    focusY = event.getY(id)

                    listener.onRotateEnd(this)
                    reset()
                }
                MotionEvent.ACTION_CANCEL -> {
                    listener.onRotateEnd(this)
                    reset()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount > 2) {
                        listener.onRotateEnd(this)
                        reset()
                        return false
                    }

                    update(event)
                    // Only accept the event if our relative pressure is within
                    // a certain limit. This can help filter shaky data as a
                    // finger is lifted.
                    if (currentPressure!! / previousPressure!! > PRESSURE_THRESHOLD) {
                        val updatePrevious = listener.onRotate(this)
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

        focusX = event.getX(0) + currentFingerDiffX!! * 0.5f
        focusY = event.getY(0) + currentFingerDiffY!! * 0.5f

        currentSpan =
            sqrt(currentFingerDiffX!! * currentFingerDiffX!! + currentFingerDiffY!! * currentFingerDiffY!!)
        previousSpan =
            sqrt(previousFingerDiffX!! * previousFingerDiffX!! + currentFingerDiffY!! * currentFingerDiffY!!)

        previousAngle = atan2(previousFingerDiffY!!, currentFingerDiffX!!)
        currentAngle = atan2(currentFingerDiffY!!, currentFingerDiffX!!)
        rotation = ((currentAngle - previousAngle) * 180.0f / Math.PI).toFloat()

        // Pressure
        currentPressure = event.getPressure(event.actionIndex)
        previousPressure = previousEvent.getPressure(previousEvent.actionIndex);
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
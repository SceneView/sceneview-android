package io.github.sceneview.gesture

import android.view.MotionEvent
import com.google.android.filament.utils.Float2
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.distance
import com.google.android.filament.utils.mix
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.lookAt
import io.github.sceneview.SceneView
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toFloat3

/**
 * Responds to Android touch events and manages a camera manipulator.
 * Supports one-touch orbit, two-touch pan, and pinch-to-zoom.
 *
 * Copied from filament-utils-android/src/main/java/com/google/android/filament/utils/GestureDetector.kt
 */
open class CameraGestureDetector(
    private val sceneView: SceneView,
    private val listener: OnCameraGestureListener
) {

    interface OnCameraGestureListener {
        /**
         * In MAP and ORBIT modes, dollys the camera along the viewing direction
         *
         * In FREE_FLIGHT mode, adjusts the move speed of the camera.
         *
         * @param x X-coordinate for point of interest in viewport space, ignored in FREE_FLIGHT
         * mode
         * @param y Y-coordinate for point of interest in viewport space, ignored in FREE_FLIGHT
         * mode
         * @param scrollDelta In MAP and ORBIT modes, negative means "zoom in", positive means
         * "zoom out"
         * In FREE_FLIGHT mode, negative means "slower", positive means "faster"
         */
        fun onScroll(x: Int, y: Int, scrollDelta: Float)

        /**
         * Start of a grabbing session (i.e. the user is dragging around in the viewport).
         *
         * In MAP mode, this starts a panning session.
         * In ORBIT mode, this starts either rotating or strafing.
         * In FREE_FLIGHT mode, this starts a nodal panning session.
         *
         * @param x X-coordinate for point of interest in viewport space
         * @param y Y-coordinate for point of interest in viewport space
         * @param strafe ORBIT mode only; if true, starts a translation rather than a rotation
         */
        fun onGrabBegin(x: Int, y: Int, strafe: Boolean)

        /**
         * Updated grabbing session
         *
         * This is called at least once between [onGrabBegin] / [onGrabEnd] to dirty the camera.
         */
        fun onGrabUpdate(x: Int, y: Int)

        /**
         * Ends a grabbing session.
         */
        fun onGrabEnd()
    }

    class SimpleOnCameraGestureListener : OnCameraGestureListener {
        override fun onScroll(x: Int, y: Int, scrollDelta: Float) {}
        override fun onGrabBegin(x: Int, y: Int, strafe: Boolean) {}
        override fun onGrabUpdate(x: Int, y: Int) {}
        override fun onGrabEnd() {}
    }

    private enum class Gesture { NONE, ORBIT, PAN, ZOOM }

    // Simplified memento of MotionEvent, minimal but sufficient for our purposes.
    private data class TouchPair(var pt0: Float2, var pt1: Float2, var count: Int) {
        constructor() : this(Float2(0f), Float2(0f), 0)
        constructor(me: MotionEvent, height: Int) : this() {
            if (me.pointerCount >= 1) {
                this.pt0 = Float2(me.getX(0), height - me.getY(0))
                this.pt1 = this.pt0
                this.count++
            }
            if (me.pointerCount >= 2) {
                this.pt1 = Float2(me.getX(1), height - me.getY(1))
                this.count++
            }
        }

        val separation get() = distance(pt0, pt1)
        val midpoint get() = mix(pt0, pt1, 0.5f)
        val x: Int get() = midpoint.x.toInt()
        val y: Int get() = midpoint.y.toInt()
    }

    private var currentGesture = Gesture.NONE
    private var previousTouch = TouchPair()
    private val tentativePanEvents = ArrayList<TouchPair>()
    private val tentativeOrbitEvents = ArrayList<TouchPair>()
    private val tentativeZoomEvents = ArrayList<TouchPair>()

    private val kGestureConfidenceCount = 2
    private val kPanConfidenceDistance = 10
    private val kZoomConfidenceDistance = 10
    private val kZoomSpeed = 1f / 10f

    var isPanEnabled: Boolean = true

    fun onTouchEvent(event: MotionEvent) {
        val touch = TouchPair(event, sceneView.height)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {

                // CANCEL GESTURE DUE TO UNEXPECTED POINTER COUNT

                if ((event.pointerCount != 1 && currentGesture == Gesture.ORBIT) ||
                    (event.pointerCount != 2 && currentGesture == Gesture.PAN) ||
                    (event.pointerCount != 2 && currentGesture == Gesture.ZOOM)
                ) {
                    endGesture()
                    return
                }

                // UPDATE EXISTING GESTURE

                if (currentGesture == Gesture.ZOOM) {
                    val d0 = previousTouch.separation
                    val d1 = touch.separation
                    listener.onScroll(touch.x, touch.y, (d0 - d1) * kZoomSpeed)
                    previousTouch = touch
                    return
                }

                if (currentGesture != Gesture.NONE) {
                    listener.onGrabUpdate(touch.x, touch.y)
                    return
                }

                // DETECT NEW GESTURE

                if (event.pointerCount == 1) {
                    tentativeOrbitEvents.add(touch)
                }

                if (event.pointerCount == 2) {
                    tentativePanEvents.add(touch)
                    tentativeZoomEvents.add(touch)
                }

                if (isOrbitGesture()) {
                    listener.onGrabBegin(touch.x, touch.y, false)
                    currentGesture = Gesture.ORBIT
                    return
                }

                if (isZoomGesture()) {
                    currentGesture = Gesture.ZOOM
                    previousTouch = touch
                    return
                }

                if (isPanGesture()) {
                    listener.onGrabBegin(touch.x, touch.y, true)
                    currentGesture = Gesture.PAN
                    return
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                endGesture()
            }
        }
    }

    private fun endGesture() {
        tentativePanEvents.clear()
        tentativeOrbitEvents.clear()
        tentativeZoomEvents.clear()
        currentGesture = Gesture.NONE
        listener.onGrabEnd()
    }

    private fun isOrbitGesture(): Boolean {
        return tentativeOrbitEvents.size > kGestureConfidenceCount
    }

    private fun isPanGesture(): Boolean {
        if (!isPanEnabled || tentativePanEvents.size <= kGestureConfidenceCount) {
            return false
        }
        val oldest = tentativePanEvents.first().midpoint
        val newest = tentativePanEvents.last().midpoint
        return distance(oldest, newest) > kPanConfidenceDistance
    }

    private fun isZoomGesture(): Boolean {
        if (tentativeZoomEvents.size <= kGestureConfidenceCount) {
            return false
        }
        val oldest = tentativeZoomEvents.first().separation
        val newest = tentativeZoomEvents.last().separation
        return kotlin.math.abs(newest - oldest) > kZoomConfidenceDistance
    }
}

val Manipulator.transform: Transform
    get() = Array(3) { FloatArray(3) }.apply {
        getLookAt(this[0], this[1], this[2])
    }.let { (eye, target, _) ->
        return lookAt(eye = eye.toFloat3(), target = target.toFloat3(), up = Float3(y = 1.0f))
    }
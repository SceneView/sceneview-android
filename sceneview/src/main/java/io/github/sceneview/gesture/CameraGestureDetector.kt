package io.github.sceneview.gesture

import android.view.MotionEvent
import com.google.android.filament.utils.Float2
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.distance
import com.google.android.filament.utils.mix
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform

/**
 * Pan fixed version of the mostly duplicated com.google.android.filament.utils.GestureDetector
 *
 * Responds to Android touch events and manages a camera manipulator.
 * Supports one-touch orbit, two-touch pan, and pinch-to-zoom.
 *
 * Copied from
 * filament-utils-android/src/main/java/com/google/android/filament/utils/GestureDetector.kt
 */
open class CameraGestureDetector(
    private val viewHeight: () -> Int,
    var cameraManipulator: CameraManipulator?,
) {
    /**
     * ## Deprecated: Use CameraGestureDetector.CameraManipulator
     *
     * Replace `manipulator = Manipulator.Builder().build()` with
     * `cameraManipulator = CameraGestureDetector.DefaultCameraManipulator(manipulator =
     * Manipulator.Builder().build())`
     */
    @Deprecated("Use CameraGestureDetector.CameraManipulator")
    constructor(
        viewHeight: () -> Int,
        manipulator: Manipulator?
    ): this(
        viewHeight,
        createDefaultCameraManipulator(manipulator)
    )

    interface CameraManipulator {
        fun setViewport(width: Int, height: Int)
        fun getTransform(): Transform
        fun grabBegin(x: Int, y: Int, strafe: Boolean)
        fun grabUpdate(x: Int, y: Int)
        fun grabEnd()
        fun scrollBegin(x: Int, y: Int, separation: Float)
        fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float)
        fun scrollEnd()
        fun update(deltaTime: Float)
    }

    /**
     * The first onTouch event will make the first manipulator build. So you can change the camera
     * position before any user gesture.
     *
     * Clients notify the camera manipulator of various mouse or touch events, then periodically
     * call its getLookAt() method so that they can adjust their camera(s). Three modes are
     * supported: ORBIT, MAP, and FREE_FLIGHT. To construct a manipulator instance, the desired mode
     * is passed into the create method.
     */
    open class DefaultCameraManipulator(
        protected val manipulator: Manipulator,
    ): CameraManipulator {
        private val kZoomSpeed = 1f / 10f

        constructor(
            orbitHomePosition: Position? = null,
            targetPosition: Position? = null
        ) : this(
            Manipulator.Builder()
                .apply {
                    orbitHomePosition?.let { orbitHomePosition(it) }
                    targetPosition?.let { targetPosition(it) }
                }
//                .viewport(min(width, 1), min(height, 1))
                .orbitSpeed(0.005f, 0.005f)
                .zoomSpeed(0.05f)
                .build(Manipulator.Mode.ORBIT),
        )

        override fun setViewport(width: Int, height: Int) {
            manipulator.setViewport(width, height)
        }

        override fun getTransform(): Transform {
            return manipulator.transform
        }

        override fun grabBegin(x: Int, y: Int, strafe: Boolean) {
            manipulator.grabBegin(x, y, strafe)
        }

        override fun grabUpdate(x: Int, y: Int) {
            manipulator.grabUpdate(x, y)
        }

        override fun grabEnd() {
            manipulator.grabEnd()
        }

        override fun scrollBegin(x: Int, y: Int, separation: Float) {}

        override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {
            manipulator.scroll(x, y, (prevSeparation - currSeparation) * kZoomSpeed)
        }

        override fun scrollEnd() {}

        override fun update(deltaTime: Float) {
            manipulator.update(deltaTime)
        }
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

    var isPanEnabled: Boolean = true

    fun onTouchEvent(event: MotionEvent) {
        val touch = TouchPair(event, viewHeight())
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
                    cameraManipulator?.scrollUpdate(touch.x, touch.y, d0, d1)
                    previousTouch = touch
                    return
                }

                if (currentGesture != Gesture.NONE) {
                    cameraManipulator?.grabUpdate(touch.x, touch.y)
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
                    cameraManipulator?.grabBegin(touch.x, touch.y, false)
                    currentGesture = Gesture.ORBIT
                    return
                }

                if (isZoomGesture()) {
                    cameraManipulator?.scrollBegin(touch.x, touch.y, touch.separation)
                    currentGesture = Gesture.ZOOM
                    previousTouch = touch
                    return
                }

                if (isPanGesture()) {
                    cameraManipulator?.grabBegin(touch.x, touch.y, true)
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
        cameraManipulator?.grabEnd()
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

    companion object {
        fun createDefaultCameraManipulator(
            manipulator: Manipulator? = null,
        ): DefaultCameraManipulator? {
            if (manipulator == null) {
                return null
            }

            return DefaultCameraManipulator(manipulator)
        }
    }
}
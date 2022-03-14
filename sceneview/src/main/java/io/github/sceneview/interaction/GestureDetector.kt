/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.sceneview.interaction;

import android.view.MotionEvent
import android.view.View
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.distance
import dev.romainguy.kotlin.math.mix
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Responds to Android touch events and manages a camera manipulator.
 * Supports one-touch orbit, two-touch pan, and pinch-to-zoom.
 */
class GestureDetector(
    private val view: View,
    private val manipulator: Manipulator,
    private val supportsTwist: Boolean = false
) {
    enum class Gesture { NONE, ORBIT, PAN, ZOOM, TWIST }

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
        set(value) {
            manipulator.gestureChanged(value)
            field = value
        }
    private var previousTouch = TouchPair()
    private val tentativePanEvents = ArrayList<TouchPair>()
    private val tentativeOrbitEvents = ArrayList<TouchPair>()
    private val tentativeZoomEvents = ArrayList<TouchPair>()
    private val tentativeTwistEvents = ArrayList<TouchPair>()

    private val kGestureConfidenceCount = 2
    private val kPanConfidenceDistance = 4
    private val kZoomConfidenceDistance = 10
    private val kTwistConfidenceRotation = 1.25f
    private val kZoomSpeed = 1f / 10f

    fun onTouchEvent(event: MotionEvent) {
        val touch = TouchPair(event, view.height)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {

                // CANCEL GESTURE DUE TO UNEXPECTED POINTER COUNT

                if ((event.pointerCount != 1 && currentGesture == Gesture.ORBIT) ||
                    (event.pointerCount != 2 && currentGesture == Gesture.PAN) ||
                    (event.pointerCount != 2 && currentGesture == Gesture.ZOOM) ||
                    (event.pointerCount != 2 && currentGesture == Gesture.TWIST)
                ) {
                    endGesture()
                    return
                }

                // UPDATE EXISTING GESTURE

                if (currentGesture == Gesture.ZOOM) {
                    val d0 = previousTouch.separation
                    val d1 = touch.separation
                    manipulator.scroll(touch.x, touch.y, (d0 - d1) * kZoomSpeed)
                    previousTouch = touch
                    return
                }

                if (currentGesture == Gesture.TWIST) {
                    val degree = calculateDeltaRotation(
                        touch.pt0,
                        touch.pt1,
                        previousTouch.pt0,
                        previousTouch.pt1
                    )
                    manipulator.rotate(degree)
                    previousTouch = touch
                    return
                }

                if (currentGesture != Gesture.NONE) {
                    manipulator.grabUpdate(touch.x, touch.y)
                    return
                }

                // DETECT NEW GESTURE

                if (event.pointerCount == 1) {
                    tentativeOrbitEvents.add(touch)
                }

                if (event.pointerCount == 2) {
                    tentativePanEvents.add(touch)
                    tentativeZoomEvents.add(touch)
                    tentativeTwistEvents.add(touch)
                }

                if (isOrbitGesture()) {
                    manipulator.grabBegin(touch.x, touch.y, false)
                    currentGesture = Gesture.ORBIT
                    return
                }

                if (supportsTwist && isTwistGesture()) {
                    currentGesture = Gesture.TWIST
                    manipulator.grabBegin(touch.x, touch.y, true)
                    return
                }

                if (isZoomGesture()) {
                    currentGesture = Gesture.ZOOM
                    previousTouch = touch
                    return
                }

                if (isPanGesture()) {
                    currentGesture = Gesture.PAN
                    manipulator.grabBegin(touch.x, touch.y, true)

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
        tentativeTwistEvents.clear()
        tentativeZoomEvents.clear()
        manipulator.grabEnd()
        currentGesture = Gesture.NONE
    }

    private fun isOrbitGesture(): Boolean {
        return tentativeOrbitEvents.size > kGestureConfidenceCount
    }

    private fun isPanGesture(): Boolean {
        if (tentativePanEvents.size <= kGestureConfidenceCount) {
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
        return abs(newest - oldest) > kZoomConfidenceDistance
    }

    private fun isTwistGesture(): Boolean {
        if (tentativeTwistEvents.size <= kGestureConfidenceCount) {
            return false
        }
        val oldest = tentativeTwistEvents.first()
        val newest = tentativeTwistEvents.last()

        return abs(calculateDeltaRotation(
            newest.pt0,
            newest.pt1,
            oldest.pt0,
            oldest.pt1
        )) > kTwistConfidenceRotation
    }

    private fun calculateDeltaRotation(
        currentPosition1: Float2,
        currentPosition2: Float2,
        previousPosition1: Float2,
        previousPosition2: Float2
    ): Float {
        val currentDirection2d = currentPosition1 - currentPosition2
        val previousDirection2d = previousPosition1 - previousPosition2
        val currentDirection = Vector3(currentDirection2d.x, currentDirection2d.y, 0f).normalized()
        val previousDirection =
            Vector3(previousDirection2d.x, previousDirection2d.y, 0f).normalized()
        val sign = sign(
            previousDirection.x * currentDirection.y - previousDirection.y * currentDirection.x
        )
        return Vector3.angleBetweenVectors(currentDirection, previousDirection) * sign
    }
}
package io.github.sceneview.ar.arcore

import com.google.ar.core.Camera
import com.google.ar.core.TrackingState
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toTransform

/**
 * Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
 */
val Camera.isTracking get() = trackingState == TrackingState.TRACKING

fun Camera.getProjectionTransform(near: Float, far: Float) = FloatArray(16).apply {
    getProjectionMatrix(this, 0, near, far)
}.toTransform()

val Camera.viewTransform: Transform
    get() = FloatArray(16).apply { getViewMatrix(this, 0) }.toTransform()
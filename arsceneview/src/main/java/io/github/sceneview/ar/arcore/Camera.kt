package io.github.sceneview.ar.arcore

import com.google.ar.core.Camera
import com.google.ar.core.TrackingState

/**
 * Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
 */
val Camera.isTracking get() = trackingState == TrackingState.TRACKING
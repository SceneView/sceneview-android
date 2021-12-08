package io.github.sceneview.ar.arcore

import com.google.ar.core.HitResult
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState

val Trackable.isTracking get() = trackingState == TrackingState.TRACKING

val HitResult.isTracking get() = trackable.isTracking
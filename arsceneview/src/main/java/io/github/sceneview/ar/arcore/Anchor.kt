package io.github.sceneview.ar.arcore

import com.google.ar.core.Anchor
import com.google.ar.core.TrackingState

val Anchor.isTracking get() = trackingState == TrackingState.TRACKING

val Anchor.transform get() = pose.transform
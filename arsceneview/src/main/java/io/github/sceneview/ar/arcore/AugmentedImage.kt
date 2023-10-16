package io.github.sceneview.ar.arcore

import com.google.ar.core.AugmentedImage
import com.google.ar.core.TrackingState

val AugmentedImage.isTracking
    get() = trackingState == TrackingState.TRACKING
            && trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
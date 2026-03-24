package io.github.sceneview.ar.arcore

import com.google.ar.core.AugmentedImage
import com.google.ar.core.TrackingState

/**
 * Whether this [AugmentedImage] is actively being tracked with full accuracy.
 *
 * Returns `true` only when both [AugmentedImage.getTrackingState] is [TrackingState.TRACKING]
 * and [AugmentedImage.getTrackingMethod] is [AugmentedImage.TrackingMethod.FULL_TRACKING].
 */
val AugmentedImage.isTracking
    get() = trackingState == TrackingState.TRACKING
            && trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
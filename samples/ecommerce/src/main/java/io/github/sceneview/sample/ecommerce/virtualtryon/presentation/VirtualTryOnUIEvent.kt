package io.github.sceneview.sample.ecommerce.virtualtryon.presentation

import com.google.android.filament.Engine
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason

sealed class VirtualTryOnUIEvent {
    object ModelPlaced : VirtualTryOnUIEvent()

    data class OnUserTap(val engine: Engine) : VirtualTryOnUIEvent()

    data class OnPlanesUpdated(val updatedPlanes: List<Plane>) : VirtualTryOnUIEvent()

    data class OnTrackingFailure(
        val trackingFailureReason:
        TrackingFailureReason?
    ) : VirtualTryOnUIEvent()

    data class FetchAsset(val productId: Int) : VirtualTryOnUIEvent()
}
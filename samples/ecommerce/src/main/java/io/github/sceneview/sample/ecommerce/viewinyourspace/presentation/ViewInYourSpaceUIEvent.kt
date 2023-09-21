package io.github.sceneview.sample.ecommerce.viewinyourspace.presentation

import com.google.ar.core.Plane

sealed class ViewInYourSpaceUIEvent {
    object ModelPlaced : ViewInYourSpaceUIEvent()
    data class OnPlanesUpdated(val updatedPlanes: List<Plane>) : ViewInYourSpaceUIEvent()

    data class FetchAsset(val productId: Int) : ViewInYourSpaceUIEvent()
}
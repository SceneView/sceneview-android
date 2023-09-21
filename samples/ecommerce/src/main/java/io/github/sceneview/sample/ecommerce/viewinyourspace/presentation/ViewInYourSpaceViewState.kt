package io.github.sceneview.sample.ecommerce.viewinyourspace.presentation

data class ViewInYourSpaceViewState(
    val modelPlaced: Boolean = false,
    val readyToPlaceModel: Boolean = false,
    val downloadingAsset: Boolean = false,
    val modelAsset: String? = null
)

package io.github.sceneview.sample.ecommerce.virtualtryon.presentation

import io.github.sceneview.ar.node.ArModelNode

data class VirtualTryOnViewState(
    val modelNode: ArModelNode? = null,
    val modelLoaded: Boolean = false,
    val modelPlaced: Boolean = false,
    val userTapped: Boolean = false,
    val planeDetected: Boolean = false,
    val trackingError: Boolean = false,
    val readyToPlaceModel: Boolean = false,
    val downloadingAsset: Boolean = false
)



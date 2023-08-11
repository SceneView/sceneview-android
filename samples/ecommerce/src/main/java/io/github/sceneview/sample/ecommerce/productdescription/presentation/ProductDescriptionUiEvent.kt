package io.github.sceneview.sample.ecommerce.productdescription.presentation

sealed class ProductDescriptionUiEvent {
    object OnAddToCartTap : ProductDescriptionUiEvent()
    object OnVirtualTryOnTap : ProductDescriptionUiEvent()
    data class FetchProductData(val productId: Int) : ProductDescriptionUiEvent()
}
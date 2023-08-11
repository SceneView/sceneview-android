package io.github.sceneview.sample.ecommerce.productdescription.presentation


// UI Actions are
sealed class ProductDescriptionUIAction {
    object NavigateToVirtualTryOnScreen : ProductDescriptionUIAction()
    object NavigateToAddToCartScreen : ProductDescriptionUIAction()
}

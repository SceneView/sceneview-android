package io.github.sceneview.sample.ecommerce.productdescription.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sceneview.sample.ecommerce.productdescription.data.ProductRepositoryImpl
import io.github.sceneview.sample.ecommerce.productdescription.domain.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    val productRepository: ProductRepository = ProductRepositoryImpl()

    private val _state: MutableStateFlow<ProductDescriptionViewState> =
        MutableStateFlow(ProductDescriptionViewState())
    val state: StateFlow<ProductDescriptionViewState> = _state


    fun dispatchEvent(event: ProductDescriptionUiEvent) {
        when (event) {
            is ProductDescriptionUiEvent.FetchProductData -> onFetchProductData(event.productId)
            is ProductDescriptionUiEvent.OnAddToCartTap -> setState(state.value.copy(showAddToCartToast = true))
            is ProductDescriptionUiEvent.OnVirtualTryOnTap -> setState(state.value.copy(goToVirtualTryOnPage = true))
            is ProductDescriptionUiEvent.ShownAddToCartToast -> setState(state.value.copy(showAddToCartToast = false))
            is ProductDescriptionUiEvent.NavigatedToVirtualTryOn -> setState(state.value.copy(goToVirtualTryOnPage = false))
        }

    }

    private fun onFetchProductData(productId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val product = productRepository.fetchProductMetadata(productId)
            setState(
                state.value.copy(product = product)
            )
        }
    }


    private fun setState(newState: ProductDescriptionViewState) {
        viewModelScope.launch {
            _state.emit(newState)
        }
    }
}
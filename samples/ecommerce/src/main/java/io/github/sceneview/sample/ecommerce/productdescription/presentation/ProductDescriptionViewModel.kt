package io.github.sceneview.sample.ecommerce.productdescription.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sceneview.sample.ecommerce.productdescription.data.ProductRepositoryImpl
import io.github.sceneview.sample.ecommerce.productdescription.domain.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductDescriptionViewModel : ViewModel() {

    val productRepository: ProductRepository = ProductRepositoryImpl()

    private val _state: MutableStateFlow<ProductDescriptionViewState> =
        MutableStateFlow(ProductDescriptionViewState())
    val state: StateFlow<ProductDescriptionViewState> = _state
    private val _uiAction: MutableStateFlow<ProductDescriptionUIAction?> = MutableStateFlow(
        null
    )
    val uiAction: StateFlow<ProductDescriptionUIAction?> = _uiAction


    fun dispatchEvent(event: ProductDescriptionUiEvent) {
        when (event) {
            is ProductDescriptionUiEvent.FetchProductData -> onFetchProductData(event.productId)
            is ProductDescriptionUiEvent.OnAddToCartTap -> setUiAction(ProductDescriptionUIAction.NavigateToAddToCartScreen)
            is ProductDescriptionUiEvent.OnVirtualTryOnTap -> setUiAction(ProductDescriptionUIAction.NavigateToVirtualTryOnScreen)
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

    private fun setUiAction(newUiAction: ProductDescriptionUIAction?) {
        viewModelScope.launch {
            _uiAction.emit(newUiAction)
        }
    }

    fun onConsumeUIAction() {
        viewModelScope.launch {
            _uiAction.emit(null)
        }
    }
}
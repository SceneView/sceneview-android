package io.github.sceneview.sample.ecommerce.virtualtryon.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Plane
import io.github.sceneview.sample.ecommerce.virtualtryon.data.ModelAssetRepositoryImpl
import io.github.sceneview.sample.ecommerce.virtualtryon.domain.ModelAssetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VirtualTryOnViewModel : ViewModel() {

    private val _state: MutableStateFlow<VirtualTryOnViewState> =
        MutableStateFlow(VirtualTryOnViewState())
    val state: StateFlow<VirtualTryOnViewState> = _state

    private val _uiAction: MutableStateFlow<VirtualTryOnUIAction?> = MutableStateFlow(
        null
    )
    val uiAction: StateFlow<VirtualTryOnUIAction?> = _uiAction

    private var remoteAsset: String? = null

    // Best practice is to use dependency injection to inject the repository
    private val repository: ModelAssetRepository = ModelAssetRepositoryImpl()


    fun dispatchEvent(event: VirtualTryOnUIEvent) {
        when (event) {
            is VirtualTryOnUIEvent.ModelPlaced -> onModelPlaced()
            is VirtualTryOnUIEvent.OnPlanesUpdated -> onPlanesUpdated(event.updatedPlanes)
            is VirtualTryOnUIEvent.FetchAsset -> onFetchAsset(event.productId)
        }
    }

    private fun onFetchAsset(productId: Int) {
        viewModelScope.launch {
            setState(state.value.copy(downloadingAsset = true))
            remoteAsset = repository.fetchAsset(productId)
            setState(state.value.copy(downloadingAsset = false, modelAsset = remoteAsset))
        }
    }

    private fun onPlanesUpdated(updatedPlanes: List<Plane>) {
        if (!state.value.readyToPlaceModel && updatedPlanes.isNotEmpty()) {
            // Only update once so that user sees that the model can be placed
            setState(
                state.value.copy(readyToPlaceModel = updatedPlanes.isNotEmpty())
            )
        }
    }

    private fun onModelPlaced() {
        setState(
            state.value.copy(
                modelPlaced = true,
            )
        )

        setUiAction(VirtualTryOnUIAction.ShowModalPlaced)
    }


    private fun setState(newState: VirtualTryOnViewState) {
        viewModelScope.launch {
            _state.emit(newState)
        }
    }

    private fun setUiAction(uiAction: VirtualTryOnUIAction) {
        viewModelScope.launch {
            _uiAction.emit(uiAction)
        }
    }

    fun onConsumedUiAction() {
        viewModelScope.launch {
            _uiAction.emit(null)
        }
    }
}
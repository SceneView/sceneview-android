package io.github.sceneview.sample.ecommerce.viewinyourspace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Plane
import io.github.sceneview.sample.ecommerce.viewinyourspace.data.ModelAssetRepositoryImpl
import io.github.sceneview.sample.ecommerce.viewinyourspace.domain.ModelAssetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ViewInYourSpaceViewModel : ViewModel() {

    private val _state: MutableStateFlow<ViewInYourSpaceViewState> =
        MutableStateFlow(ViewInYourSpaceViewState())
    val state: StateFlow<ViewInYourSpaceViewState> = _state

    private val _uiAction: MutableStateFlow<ViewInYourSpaceUIAction?> = MutableStateFlow(
        null
    )
    val uiAction: StateFlow<ViewInYourSpaceUIAction?> = _uiAction

    private var remoteAsset: String? = null

    // Best practice is to use dependency injection to inject the repository
    private val repository: ModelAssetRepository = ModelAssetRepositoryImpl()


    fun dispatchEvent(event: ViewInYourSpaceUIEvent) {
        when (event) {
            is ViewInYourSpaceUIEvent.ModelPlaced -> onModelPlaced()
            is ViewInYourSpaceUIEvent.OnPlanesUpdated -> onPlanesUpdated(event.updatedPlanes)
            is ViewInYourSpaceUIEvent.FetchAsset -> onFetchAsset(event.productId)
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

        setUiAction(ViewInYourSpaceUIAction.ShowModalPlaced)
    }


    private fun setState(newState: ViewInYourSpaceViewState) {
        viewModelScope.launch {
            _state.emit(newState)
        }
    }

    private fun setUiAction(uiAction: ViewInYourSpaceUIAction) {
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
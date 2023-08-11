package io.github.sceneview.sample.ecommerce.virtualtryon.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.filament.Engine
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.sample.ecommerce.virtualtryon.data.ModelAssetRepositoryImpl
import io.github.sceneview.sample.ecommerce.virtualtryon.domain.ModelAssetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VirtualTryOnViewModel : ViewModel() {

    private val _state: MutableStateFlow<VirtualTryOnViewState> =
        MutableStateFlow(VirtualTryOnViewState())
    val state: StateFlow<VirtualTryOnViewState> = _state

    private var remoteAsset: String? = null

    // Best practice is to use dependency injection to inject the repository
    private val repository: ModelAssetRepository = ModelAssetRepositoryImpl()


    fun dispatchEvent(event: VirtualTryOnUIEvent) {
        when (event) {
            is VirtualTryOnUIEvent.ModelPlaced -> onModelPlaced()
            is VirtualTryOnUIEvent.OnPlanesUpdated -> onPlanesUpdated(event.updatedPlanes)
            is VirtualTryOnUIEvent.OnUserTap -> onUserTap(event.engine)
            is VirtualTryOnUIEvent.OnTrackingFailure -> onTrackingFailure(event.trackingFailureReason)
            is VirtualTryOnUIEvent.FetchAsset -> onFetchAsset(event.productId)
        }
    }

    private fun onFetchAsset(productId: Int) {
        viewModelScope.launch {
            setState(state.value.copy(downloadingAsset = true))
            remoteAsset = repository.fetchAsset(productId)
            setState(state.value.copy(downloadingAsset = false))
        }
    }

    private fun onTrackingFailure(trackingFailureReason: TrackingFailureReason?) {
        if (trackingFailureReason != TrackingFailureReason.NONE) {
//            setState(
//
//            )
        }
    }

    private fun onUserTap(engine: Engine) {
        setState(
            state.value.copy(
                modelNode = ArModelNode(
                    engine,
                    PlacementMode.BEST_AVAILABLE
                ).apply {
                    remoteAsset?.let {
                        loadModelGlbAsync(
                            glbFileLocation = it,
                        )
                    }
                })
        )
    }

    private fun onPlanesUpdated(updatedPlanes: List<Plane>) {
        setState(
            state.value.copy(readyToPlaceModel = updatedPlanes.isNotEmpty())
        )
    }

    private fun onModelPlaced() {
        Log.e("test", "vic: onModelPlaced")
        setState(
            state.value.copy(
                modelPlaced = true,
                modelNode = null
            )
        )
    }


    private fun setState(newState: VirtualTryOnViewState) {
        viewModelScope.launch {
            _state.emit(newState)
        }
    }
}
package io.github.sceneview.sample.ecommerce.virtualtryon.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode


@Composable
fun VirtualTryOnScreen(
    productId: Int,
    virtualTryOnViewModel: VirtualTryOnViewModel
) {
    val nodes = remember { mutableStateListOf<ArNode>() }

    LaunchedEffect(Unit) {
        virtualTryOnViewModel.dispatchEvent(VirtualTryOnUIEvent.FetchAsset(productId))
    }
    var sceneview by remember { mutableStateOf<ArSceneView?>(null) }
    val viewState by virtualTryOnViewModel.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        if (viewState.downloadingAsset) {
            CircularProgressIndicator()
        }
        ARScene(
            modifier = Modifier.fillMaxSize(),
            nodes = nodes,
            planeRenderer = true,
            onCreate = { arSceneView ->
                // Apply your configuration
                sceneview = arSceneView
            },
            onSessionCreate = { session ->
                // Configure the ARCore session
            },
            onFrame = { arFrame ->
                // Update planes state to determine whether or not to UI message
                // WARNING: DO NOT PASS ARSCENEVIEW/ARFrame TO VIEWMODEL to avoid memory leaks
                virtualTryOnViewModel.dispatchEvent(VirtualTryOnUIEvent.OnPlanesUpdated(arFrame.updatedPlanes))
            },
            onTap = { hitResult ->
                // User tapped in the AR view
                sceneview?.let {
                    onUserTap(it, virtualTryOnViewModel)
                }
            },
            onTrackingFailureChanged = { trackingFailureReason ->
                virtualTryOnViewModel.dispatchEvent(VirtualTryOnUIEvent.OnTrackingFailure(trackingFailureReason))
            }
        )

        if (viewState.readyToPlaceModel) {
            Box(
                modifier = Modifier
                    .padding(50.dp)
                    .align(Alignment.Center)
                    .background(Color.Gray)
            ) {
                Text("Ready to place model!")
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(50.dp)
                    .align(Alignment.Center)
                    .background(Color.Gray)
            ) {
                Text("Move your phone to place model")
            }
        }

        viewState.modelNode?.let {
            sceneview?.planeRenderer?.isVisible = true
            sceneview?.addChild(it)
            sceneview?.selectedNode = it
            virtualTryOnViewModel.dispatchEvent(VirtualTryOnUIEvent.ModelPlaced)
        }
    }
}

fun onUserTap(sceneView: ArSceneView, virtualTryOnViewModel: VirtualTryOnViewModel) {
//    virtualTryOnViewModel.dispatchEvent()

}
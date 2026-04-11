package com.example.ok

import androidx.compose.runtime.Composable
import io.github.sceneview.Scene
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.loaders.rememberModelInstance

@Composable
fun ModelScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/chair.glb")
    Scene(engine = engine) {
        modelInstance?.let {
            ModelNode(modelInstance = it)
        }
    }
}

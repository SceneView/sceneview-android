package com.example.warn

import androidx.compose.runtime.Composable
import com.google.ar.sceneform.ux.ArFragment
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.TransformableNode
import io.github.sceneview.loaders.ModelLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

fun loadBad(scope: CoroutineScope, modelLoader: ModelLoader) {
    scope.launch(Dispatchers.IO) {
        modelLoader.createModelInstance("models/chair.glb")
    }
}

@Composable
fun BadLight(engine: Any) {
    LightNode(engine = engine, type = 0) {
        intensity(100_000f)
    }
    val t = TransformableNode(engine)
    val af = ArFragment()
}

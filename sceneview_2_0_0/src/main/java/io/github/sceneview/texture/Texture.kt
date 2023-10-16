package io.github.sceneview.texture

import com.google.android.filament.Texture
import io.github.sceneview.SceneView
import kotlinx.coroutines.*

fun Texture.Builder.build(sceneView: SceneView): Texture = build(sceneView.engine).also {
    sceneView.textures += it
}

inline fun <reified T : Texture.Builder> Deferred<T>.build(
    sceneView: SceneView,
    crossinline onResult: (Texture) -> Unit
): Job = CoroutineScope(Dispatchers.IO).launch {
    onResult(await().build(sceneView.engine))
}.also {
    sceneView.loadingJobs += it
}

fun SceneView.destroyTexture(texture: Texture) {
    engine.destroyTexture(texture)
    textures -= texture
}
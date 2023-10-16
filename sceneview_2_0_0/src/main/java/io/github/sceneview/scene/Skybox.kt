package io.github.sceneview.scene

import com.google.android.filament.Skybox
import io.github.sceneview.SceneView

fun Skybox.Builder.build(sceneView: SceneView) = build(sceneView.engine).also {
    sceneView.skyboxes += it
}

fun SceneView.destroySkybox(skybox: Skybox) {
    engine.destroySkybox(skybox)
    skyboxes -= skybox
}
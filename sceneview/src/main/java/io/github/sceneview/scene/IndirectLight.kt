package io.github.sceneview.scene

import com.google.android.filament.IndirectLight
import io.github.sceneview.SceneView

fun IndirectLight.Builder.build(sceneView: SceneView) = build(sceneView.engine).also {
    sceneView.indirectLights += it
}

fun SceneView.destroyIndirectLight(indirectLight: IndirectLight) {
    engine.destroyIndirectLight(indirectLight)
    indirectLights -= indirectLight
}
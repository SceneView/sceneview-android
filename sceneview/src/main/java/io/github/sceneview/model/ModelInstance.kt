package io.github.sceneview.model

import com.google.android.filament.gltfio.FilamentInstance
import io.github.sceneview.Filament.lightManager
import io.github.sceneview.Filament.renderableManager
import io.github.sceneview.light.Light
import io.github.sceneview.renderable.Renderable

typealias ModelInstance = FilamentInstance

val ModelInstance.renderables: List<Renderable>
    get() = entities.filter {
        renderableManager.hasComponent(it)
    }

val ModelInstance.lights: List<Light>
    get() = entities.filter {
        lightManager.hasComponent(it)
    }

val ModelInstance.model get() = asset

fun ModelInstance.getRenderableByName(name: String) = model.getRenderableByName(name)

fun ModelInstance.destroy() {
    runCatching {
        renderables.forEach { renderableManager.destroy(it) }
        lights.forEach { renderableManager.destroy(it) }
    }
}
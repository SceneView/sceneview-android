package io.github.sceneview.scene

import com.google.android.filament.Scene
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.KTXLoader
import io.github.sceneview.environment.Environment
import io.github.sceneview.light.Light
import io.github.sceneview.model.Model
import io.github.sceneview.model.allEntities

/**
 *
 * ### Defines the lighting environment and the skybox of the scene
 *
 * Environments are usually captured as high-resolution HDR equirectangular images and processed by
 * the cmgen tool to generate the data needed by IndirectLight.
 *
 * You can also process an hdr at runtime but this is more consuming.
 *
 * - Currently IndirectLight is intended to be used for "distant probes", that is, to represent
 * global illumination from a distant (i.e. at infinity) environment, such as the sky or distant
 * mountains.
 * Only a single IndirectLight can be used in a Scene. This limitation will be lifted in the future.
 *
 * - When added to a Scene, the Skybox fills all untouched pixels.
 *
 * @return the created directional light
 *
 * @see [KTXLoader.loadEnvironment]
 * @see [HDRLoader.loadEnvironment]
 */
fun Scene.setEnvironment(environment: Environment?) {
    if (indirectLight != environment?.indirectLight) {
        indirectLight = environment?.indirectLight
    }
    if (skybox != environment?.skybox) {
        skybox = environment?.skybox
    }
}

fun Scene.addModel(model: Model) {
    addEntities(model.allEntities)
}

fun Scene.removeModel(model: Model) {
    removeEntities(model.allEntities)
}

fun Scene.addLight(light: Light) {
    addEntity(light)
}

fun Scene.removeLight(light: Light) {
    removeEntity(light)
}

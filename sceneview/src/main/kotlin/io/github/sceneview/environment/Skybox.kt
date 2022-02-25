package io.github.sceneview.environment

import com.google.android.filament.Skybox
import io.github.sceneview.Filament

/**
 * @see Skybox.Builder.build
 */
fun Skybox.Builder.build(): Skybox = build(Filament.engine)

/**
 * Destroys a Skybox and frees all its associated resources.
 */
fun Skybox.destroy() {
    Filament.engine.destroySkybox(this)
}
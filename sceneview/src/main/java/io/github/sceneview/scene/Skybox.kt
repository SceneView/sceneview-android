package io.github.sceneview.scene

import com.google.android.filament.Skybox
import io.github.sceneview.Filament

fun Skybox.Builder.build(): Skybox = build(Filament.engine)

/**
 * Destroys a Skybox and frees all its associated resources.
 */
fun Skybox.destroy() {
    runCatching { Filament.engine.destroySkybox(this) }
}
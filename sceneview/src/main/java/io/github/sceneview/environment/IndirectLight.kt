package io.github.sceneview.environment

import com.google.android.filament.IndirectLight
import io.github.sceneview.Filament

/**
 * @see IndirectLight.Builder.build
 */
fun IndirectLight.Builder.build(): IndirectLight = build(Filament.engine)

/**
 * Destroys an IndirectLight and frees all its associated resources.
 */
fun IndirectLight.destroy() {
    Filament.engine.destroyIndirectLight(this)
}
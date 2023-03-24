package io.github.sceneview.light

import com.google.android.filament.IndirectLight
import io.github.sceneview.Filament

fun IndirectLight.Builder.build(): IndirectLight = build(Filament.engine)

/**
 * Destroys an IndirectLight and frees all its associated resources.
 */
fun IndirectLight.destroy() {
    runCatching { Filament.engine.destroyIndirectLight(this) }
}
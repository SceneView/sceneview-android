package io.github.sceneview.light

import androidx.lifecycle.Lifecycle
import com.google.android.filament.IndirectLight
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament

fun IndirectLight.Builder.build(lifecycle: Lifecycle): IndirectLight = build(Filament.engine)
    .also { indirectLight ->
        lifecycle.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { indirectLight.destroy() }
        })
    }

/**
 * Destroys an IndirectLight and frees all its associated resources.
 */
fun IndirectLight.destroy() {
    runCatching { Filament.engine.destroyIndirectLight(this) }
}
package io.github.sceneview.scene

import androidx.lifecycle.Lifecycle
import com.google.android.filament.Skybox
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament

fun Skybox.Builder.build(lifecycle: Lifecycle): Skybox = build(Filament.engine)
    .also { skybox ->
        lifecycle.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { skybox.destroy() }
        })
    }

/**
 * Destroys a Skybox and frees all its associated resources.
 */
fun Skybox.destroy() {
    Filament.engine.destroySkybox(this)
}
package io.github.sceneview.material

import androidx.lifecycle.Lifecycle
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament

fun Material.Builder.build(lifecycle: Lifecycle): Material = build(Filament.engine)
    .also { material ->
        lifecycle.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { material.destroy() }
        })
    }

fun Material.createInstance(lifecycle: Lifecycle): MaterialInstance =
    createInstance().also { materialInstance ->
        lifecycle.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { materialInstance.destroy() }
        })
    }

fun Material.destroy() {
    Filament.engine.destroyMaterial(this)
}
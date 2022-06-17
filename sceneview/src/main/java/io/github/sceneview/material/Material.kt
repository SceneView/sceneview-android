package io.github.sceneview.material

import androidx.lifecycle.Lifecycle
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament

fun Material.Builder.build(lifecycle: Lifecycle? = null): Material = build(Filament.engine)
    .also { material ->
        lifecycle?.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { material.destroy() }
        })
    }

fun Material.createInstance(lifecycle: Lifecycle? = null): MaterialInstance =
    createInstance().also { materialInstance ->
        lifecycle?.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { materialInstance.destroy() }
        })
    }

fun Material.destroy() {
    // TODO : We still have an issue because of material instances not destroyed before this call.
    //  It should be fixed when Material override is removed and we don't allow multiple instances
    //  anymore. Anyway Filament.destroyMaterials() might do the job at the end.
//    Filament.engine.destroyMaterial(this)
}
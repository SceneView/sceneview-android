package io.github.sceneview.material

import com.google.android.filament.Material
import io.github.sceneview.Filament

fun Material.destroy(removeFromCache: Boolean = true) {
    if (removeFromCache) {
        MaterialLoader.cache -= MaterialLoader.cache.filter { (_, material) -> material == this }.keys
    }
    Filament.engine.destroyMaterial(this)
}
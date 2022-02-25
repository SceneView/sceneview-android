package io.github.sceneview.material

import com.google.android.filament.Material
import io.github.sceneview.Filament

fun Material.destroy() {
    Filament.engine.destroyMaterial(this)
}
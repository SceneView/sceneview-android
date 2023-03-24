package io.github.sceneview.material

import com.google.android.filament.Material
import io.github.sceneview.Filament

fun Material.Builder.build(): Material = build(Filament.engine)

fun Material.destroy() {
    // TODO : We still have an issue because of material instances not destroyed before this call.
    //  It should be fixed when Material override is removed and we don't allow multiple instances
    //  anymore. Anyway Filament.destroyMaterials() might do the job at the end.
    runCatching { Filament.engine.destroyMaterial(this) }
}
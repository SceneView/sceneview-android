package io.github.sceneview.scene

import com.google.android.filament.ColorGrading
import com.google.android.filament.View
import io.github.sceneview.Filament

fun View.destroy() {
    runCatching { Filament.engine.destroyView(this) }
}

fun ColorGrading.Builder.build(): ColorGrading = build(Filament.engine)
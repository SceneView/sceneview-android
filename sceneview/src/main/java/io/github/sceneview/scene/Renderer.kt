package io.github.sceneview.scene

import com.google.android.filament.Renderer
import io.github.sceneview.Filament

fun Renderer.destroy() = Filament.engine.destroyRenderer(this)
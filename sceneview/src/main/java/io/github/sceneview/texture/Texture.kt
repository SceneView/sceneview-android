package io.github.sceneview.texture

import com.google.android.filament.Engine
import com.google.android.filament.Texture

fun <R> Texture.use(engine: Engine, block: (Texture) -> R): R = block(this).also {
    engine.destroyTexture(this)
}
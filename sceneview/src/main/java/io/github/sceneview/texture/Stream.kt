package io.github.sceneview.texture

import com.google.android.filament.Stream
import io.github.sceneview.Filament

fun <R> Stream.use(block: (Stream) -> R): R = block(this).also { destroy() }
fun Stream.Builder.build(): Stream = build(Filament.engine)

fun Stream.destroy() {
    runCatching { Filament.engine.destroyStream(this) }
}
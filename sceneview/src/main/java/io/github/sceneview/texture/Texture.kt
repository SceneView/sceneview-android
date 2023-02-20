package io.github.sceneview.texture

import androidx.annotation.IntRange
import androidx.annotation.Size
import com.google.android.filament.Stream
import com.google.android.filament.Texture
import io.github.sceneview.Filament

fun <R> Texture.use(block: (Texture) -> R): R = block(this).also { destroy() }
fun Texture.Builder.build(): Texture = build(Filament.engine)

fun Texture.setExternalStream(stream: Stream) = setExternalStream(Filament.engine, stream)
fun Texture.setImage(level: Int, buffer: Texture.PixelBufferDescriptor) =
    setImage(Filament.engine, level, buffer)

fun Texture.setImage(
    @IntRange(from = 0) level: Int,
    buffer: Texture.PixelBufferDescriptor,
    @Size(min = 6) faceOffsetsInBytes: IntArray
) = setImage(Filament.engine, level, buffer, faceOffsetsInBytes)

/**
 * Destroys a Texture and frees all its associated resources.
 */
fun Texture.destroy() {
    runCatching { Filament.engine.destroyTexture(this) }
}
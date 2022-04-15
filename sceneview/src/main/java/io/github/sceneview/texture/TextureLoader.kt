package io.github.sceneview.texture

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.Lifecycle
import com.google.android.filament.Texture
import com.google.android.filament.android.TextureHelper
import io.github.sceneview.Filament
import io.github.sceneview.utils.useFileBufferNotNull

/**
 * Based on the [com.google.android.filament.textured.loadTexture]
 */
object TextureLoader {

    suspend fun loadTexture(
        context: Context,
        lifecycle: Lifecycle,
        textureFileLocation: String,
        type: TextureType
    ): Texture? =
        context.useFileBufferNotNull(textureFileLocation) { buffer ->
            val bitmap = BitmapFactory.decodeByteArray(
                buffer.array(),
                0,
                buffer.capacity(),
                BitmapFactory.Options().apply {
                    // Color is the only type of texture we want to pre-multiply with the alpha
                    // channel. Pre-multiplication is the default behavior, so we need to turn it
                    // off here
                    inPremultiplied = type == TextureType.COLOR
                })
            Texture.Builder()
                .width(bitmap.width)
                .height(bitmap.height)
                .sampler(Texture.Sampler.SAMPLER_2D)
                .format(internalFormat(type))
                // This tells Filament to figure out the number of mip levels
                .levels(0xff)
                .build(lifecycle).apply {
                    // TextureHelper offers a method that skips the copy of the bitmap into a
                    // ByteBuffer
                    TextureHelper.setBitmap(Filament.engine, this, 0, bitmap)
                    generateMipmaps(Filament.engine)
                }
        }

    private fun internalFormat(type: TextureType) = when (type) {
        TextureType.COLOR -> Texture.InternalFormat.SRGB8_A8
        TextureType.NORMAL -> Texture.InternalFormat.RGBA8
        TextureType.DATA -> Texture.InternalFormat.RGBA8
    }

    enum class TextureType {
        COLOR,
        NORMAL,
        DATA
    }
}
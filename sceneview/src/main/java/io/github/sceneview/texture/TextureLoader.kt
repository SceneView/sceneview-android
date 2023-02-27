package io.github.sceneview.texture

import android.content.Context
import android.graphics.BitmapFactory
import com.google.android.filament.Texture
import com.google.android.filament.android.TextureHelper
import io.github.sceneview.Filament
import io.github.sceneview.utils.useFileBufferNotNull
import io.github.sceneview.utils.useLocalFileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Based on the [com.google.android.filament.textured.loadTexture]
 */
object TextureLoader {

    suspend fun loadImageTexture(
        context: Context,
        imageFileLocation: String,
        type: TextureType = TextureType.COLOR
    ): Texture? =
        context.useFileBufferNotNull(imageFileLocation) { buffer ->
            withContext(Dispatchers.Main) {
                createImageTexture(buffer, type)
            }
        }

    fun createImageTexture(
        context: Context,
        imageFileLocation: String,
        type: TextureType = TextureType.COLOR
    ): Texture = context.useLocalFileBuffer(imageFileLocation) { buffer ->
        createImageTexture(buffer!!, type)
    }

    fun createImageTexture(
        imageBuffer: ByteBuffer,
        type: TextureType = TextureType.COLOR,
    ): Texture {
        val bitmap = BitmapFactory.decodeByteArray(
            imageBuffer.array(),
            0,
            imageBuffer.capacity(),
            BitmapFactory.Options().apply {
                // Color is the only type of texture we want to pre-multiply with the alpha
                // channel. Pre-multiplication is the default behavior, so we need to turn it
                // off here
                inPremultiplied = type == TextureType.COLOR
            })
        return Texture.Builder()
            .width(bitmap.width)
            .height(bitmap.height)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(internalFormat(type))
            // This tells Filament to figure out the number of mip levels
            .levels(0xff)
            .build().apply {
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
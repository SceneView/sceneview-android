package io.github.sceneview.texture

import android.content.Context
import android.graphics.BitmapFactory
import com.google.android.filament.Texture
import com.google.android.filament.android.TextureHelper
import io.github.sceneview.Filament
import io.github.sceneview.utils.fileBuffer

/**
 * Based on the [com.google.android.filament.textured.loadTexture]
 */
object TextureLoader {

    suspend fun loadTexture(context: Context, textureFileLocation: String, type: TextureType): Texture? {
        val options = BitmapFactory.Options()
        // Color is the only type of texture we want to pre-multiply with the alpha channel
        // Pre-multiplication is the default behavior, so we need to turn it off here
        options.inPremultiplied = type == TextureType.COLOR

        val byteBuffer = context.fileBuffer(textureFileLocation) ?: return null

        val bitmap = BitmapFactory.decodeByteArray(byteBuffer.array(), 0, byteBuffer.capacity(), options)

        val texture = Texture.Builder()
            .width(bitmap.width)
            .height(bitmap.height)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(internalFormat(type))
            // This tells Filament to figure out the number of mip levels
            .levels(0xff)
            .build(Filament.engine)

        // TextureHelper offers a method that skips the copy of the bitmap into a ByteBuffer
        TextureHelper.setBitmap(Filament.engine, texture, 0, bitmap)

        texture.generateMipmaps(Filament.engine)

        return texture
    }

    private fun internalFormat(type: TextureType) = when (type) {
        TextureType.COLOR  -> Texture.InternalFormat.SRGB8_A8
        TextureType.NORMAL -> Texture.InternalFormat.RGBA8
        TextureType.DATA   -> Texture.InternalFormat.RGBA8
    }

    enum class TextureType {
        COLOR,
        NORMAL,
        DATA
    }
}
package io.github.sceneview.texture

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.android.filament.android.TextureHelper
import com.google.android.filament.utils.TextureType
import io.github.sceneview.utils.readFileBuffer

class ImageTexture private constructor(val bitmap: Bitmap, texture: Texture) {
    class Builder : Texture.Builder() {
        private lateinit var bitmap: Bitmap

        fun bitmap(
            bitmap: Bitmap,
            type: TextureType = TextureType.COLOR
        ) = apply {
            width(bitmap.width)
            height(bitmap.height)
            sampler(Texture.Sampler.SAMPLER_2D)
                .format(internalFormat(type))
            // This tells Filament to figure out the number of mip levels
            levels(0xff)
            this.bitmap = bitmap
        }

        fun image(
            assets: AssetManager,
            fileLocation: String,
            type: TextureType = TextureType.COLOR
        ) = apply {
            val buffer = assets.readFileBuffer(fileLocation)
            bitmap(
                BitmapFactory.decodeByteArray(
                    buffer.array(),
                    0,
                    buffer.capacity(),
                    BitmapFactory.Options().apply {
                        // Color is the only type of texture we want to pre-multiply with the alpha
                        // channel. Pre-multiplication is the default behavior, so we need to turn it
                        // off here
                        inPremultiplied = type == TextureType.COLOR
                    })
            )
        }

        fun image(
            resources: Resources,
            resourceId: Int,
            type: TextureType = TextureType.COLOR
        ) = bitmap(
            BitmapFactory.decodeResource(
                resources,
                resourceId,
                BitmapFactory.Options().apply {
                    // Color is the only type of texture we want to pre-multiply with the alpha
                    // channel. Pre-multiplication is the default behavior, so we need to turn it
                    // off here
                    inPremultiplied = type == TextureType.COLOR
                })
        )

        override fun build(engine: Engine): Texture = super.build(engine).apply {
            // TextureHelper offers a method that skips the copy of the bitmap into a
            // ByteBuffer
            TextureHelper.setBitmap(engine, this, 0, bitmap)
            generateMipmaps(engine)
        }
    }

    companion object {
        private fun internalFormat(type: TextureType) = when (type) {
            TextureType.COLOR -> Texture.InternalFormat.SRGB8_A8
            TextureType.NORMAL -> Texture.InternalFormat.RGBA8
            TextureType.DATA -> Texture.InternalFormat.RGBA8
        }
    }
}
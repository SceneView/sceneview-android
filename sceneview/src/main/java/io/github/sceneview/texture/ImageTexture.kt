package io.github.sceneview.texture

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.android.filament.android.TextureHelper
import com.google.android.filament.utils.TextureType
import io.github.sceneview.utils.readFileBuffer

class ImageTexture {
    class Builder : Texture.Builder() {
        private lateinit var bitmap: Bitmap

        init {
            sampler(Texture.Sampler.SAMPLER_2D)
            // This tells Filament to figure out the number of mip levels
            levels(0xff)
        }

        fun type(type: TextureType) = apply {
            format(
                when (type) {
                    TextureType.COLOR -> Texture.InternalFormat.SRGB8_A8
                    TextureType.NORMAL -> Texture.InternalFormat.RGBA8
                    TextureType.DATA -> Texture.InternalFormat.RGBA8
                }
            )
        }

        fun bitmap(bitmap: Bitmap, type: TextureType = DEFAULT_TYPE) = apply {
            width(bitmap.width)
            height(bitmap.height)
            type(type)
            this.bitmap = bitmap
        }

        fun bitmap(
            assets: AssetManager,
            fileLocation: String,
            type: TextureType = DEFAULT_TYPE
        ) = bitmap(getBitmap(assets, fileLocation, type), type)

        fun bitmap(
            context: Context,
            @DrawableRes drawableResId: Int,
            type: TextureType = DEFAULT_TYPE
        ) = bitmap(getBitmap(context, drawableResId, type), type)

        override fun build(engine: Engine): Texture = super.build(engine).apply {
            // TextureHelper offers a method that skips the copy of the bitmap into a ByteBuffer
            setBitmap(engine, bitmap)
            generateMipmaps(engine)
        }
    }

    companion object {
        val DEFAULT_TYPE = TextureType.COLOR

        fun getBitmap(
            assets: AssetManager,
            fileLocation: String,
            type: TextureType = DEFAULT_TYPE
        ): Bitmap {
            val buffer = assets.readFileBuffer(fileLocation)
            return BitmapFactory.decodeByteArray(
                buffer.array(),
                0,
                buffer.capacity(),
                BitmapFactory.Options().apply {
                    // Color is the only type of texture we want to pre-multiply with the alpha
                    // channel. Pre-multiplication is the default behavior, so we need to turn it
                    // off here
                    inPremultiplied = type == TextureType.COLOR
                })
        }

        fun getBitmap(
            context: Context,
            @DrawableRes drawableResId: Int,
            type: TextureType = DEFAULT_TYPE
        ) = BitmapFactory.decodeResource(
            context.resources,
            drawableResId,
            BitmapFactory.Options().apply {
                // Color is the only type of texture we want to pre-multiply with the alpha
                // channel.
                // Pre-multiplication is the default behavior, so we need to turn it off here
                inPremultiplied = type == TextureType.COLOR
            })
    }
}

fun Texture.setBitmap(
    engine: Engine,
    assets: AssetManager,
    fileLocation: String,
    type: TextureType = ImageTexture.DEFAULT_TYPE,
    @IntRange(from = 0) level: Int = 0
) = setBitmap(engine, ImageTexture.getBitmap(assets, fileLocation, type), level)

fun Texture.setBitmap(
    engine: Engine,
    context: Context,
    @DrawableRes drawableResId: Int,
    type: TextureType = ImageTexture.DEFAULT_TYPE,
    @IntRange(from = 0) level: Int = 0
) = setBitmap(engine, ImageTexture.getBitmap(context, drawableResId, type), level)

fun Texture.setBitmap(engine: Engine, bitmap: Bitmap, @IntRange(from = 0) level: Int = 0) =
    TextureHelper.setBitmap(
        engine,
        this,
        // This tells Filament to figure out the number of mip levels
        level,
        bitmap
    )
package io.github.sceneview.material

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.utils.TextureType
import io.github.sceneview.safeDestroyTexture
import io.github.sceneview.texture.ImageTexture
import io.github.sceneview.texture.setBitmap

class ImageMaterial internal constructor(
    val engine: Engine,
    val instance: MaterialInstance,
    bitmap: Bitmap,
    type: TextureType = TextureType.COLOR
) {
    val texture = ImageTexture.Builder()
        .bitmap(bitmap, type)
        .build(engine)

    init {
        instance.setTexture(texture)
    }

    fun setBitmap(
        engine: Engine,
        assets: AssetManager,
        fileLocation: String,
        type: TextureType = TextureType.COLOR,
        @IntRange(from = 0) level: Int = 0
    ) = texture.setBitmap(engine, ImageTexture.getBitmap(assets, fileLocation, type), level)

    fun setBitmap(
        engine: Engine,
        context: Context,
        @DrawableRes drawableResId: Int,
        type: TextureType = TextureType.COLOR,
        @IntRange(from = 0) level: Int = 0
    ) = texture.setBitmap(engine, ImageTexture.getBitmap(context, drawableResId, type), level)

    fun setBitmap(engine: Engine, bitmap: Bitmap, @IntRange(from = 0) level: Int = 0) =
        texture.setBitmap(engine, bitmap, level)

    fun destroy() {
        engine.safeDestroyTexture(texture)
    }
}
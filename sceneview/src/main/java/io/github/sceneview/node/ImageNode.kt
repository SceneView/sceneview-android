package io.github.sceneview.node

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.google.android.filament.RenderableManager
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.utils.TextureType
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.geometries.Plane
import io.github.sceneview.geometries.UvScale
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.safeDestroyTexture
import io.github.sceneview.texture.ImageTexture
import io.github.sceneview.texture.TextureSampler2D
import io.github.sceneview.texture.setBitmap

open class ImageNode private constructor(
    val materialLoader: MaterialLoader,
    bitmap: Bitmap,
    val texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D(),
    /**
     * `null` to adjust size on the normalized image size
     */
    val size: Size? = null,
    center: Position = Plane.DEFAULT_CENTER,
    normal: Direction = Plane.DEFAULT_NORMAL,
    uvScale: UvScale = UvScale(1.0f),
    builderApply: RenderableManager.Builder.() -> Unit = {}
) : PlaneNode(
    engine = materialLoader.engine,
    size = size ?: normalize(Size(bitmap.width.toFloat(), bitmap.height.toFloat())),
    center = center,
    normal = normal,
    uvScale = uvScale,
    materialInstance = materialLoader.createImageInstance(texture, textureSampler),
    builderApply = builderApply
) {
    var bitmap = bitmap
        set(value) {
            field = value
            texture.setBitmap(engine, value)
            if (size == null) {
                updateGeometry(
                    size = normalize(Size(value.width.toFloat(), value.height.toFloat()))
                )
            }
        }

    constructor(
        materialLoader: MaterialLoader,
        bitmap: Bitmap,
        /**
         * `null` to adjust size on the normalized image size
         */
        size: Size? = null,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        type: TextureType = ImageTexture.DEFAULT_TYPE,
        textureSampler: TextureSampler = TextureSampler2D(),
        builderApply: RenderableManager.Builder.() -> Unit = {},
        textureBuilderApply: ImageTexture.Builder.() -> Unit = {}
    ) : this(
        materialLoader = materialLoader,
        bitmap = bitmap,
        texture = ImageTexture.Builder()
            .bitmap(bitmap)
            .type(type)
            .apply(textureBuilderApply)
            .build(materialLoader.engine),
        textureSampler = textureSampler,
        size = size,
        center = center,
        normal = normal,
        uvScale = uvScale,
        builderApply = builderApply
    )

    constructor(
        materialLoader: MaterialLoader,
        /**
         * `null` to adjust size on the normalized image size
         */
        size: Size? = null,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        imageFileLocation: String,
        type: TextureType = ImageTexture.DEFAULT_TYPE,
        textureSampler: TextureSampler = TextureSampler2D(),
        builderApply: RenderableManager.Builder.() -> Unit = {},
        textureBuilderApply: ImageTexture.Builder.() -> Unit = {}
    ) : this(
        materialLoader = materialLoader,
        bitmap = ImageTexture.getBitmap(materialLoader.assets, imageFileLocation),
        size = size,
        center = center,
        normal = normal,
        uvScale = uvScale,
        type = type,
        textureSampler = textureSampler,
        builderApply = builderApply,
        textureBuilderApply = textureBuilderApply
    )

    constructor(
        materialLoader: MaterialLoader,
        /**
         * `null` to adjust size on the normalized image size
         */
        size: Size? = null,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        @DrawableRes imageResId: Int,
        type: TextureType = ImageTexture.DEFAULT_TYPE,
        textureSampler: TextureSampler = TextureSampler2D(),
        builderApply: RenderableManager.Builder.() -> Unit = {},
        textureBuilderApply: ImageTexture.Builder.() -> Unit = {}
    ) : this(
        materialLoader = materialLoader,
        bitmap = ImageTexture.getBitmap(materialLoader.context, imageResId),
        size = size,
        center = center,
        normal = normal,
        uvScale = uvScale,
        type = type,
        textureSampler = textureSampler,
        builderApply = builderApply,
        textureBuilderApply = textureBuilderApply
    )

    fun setBitmap(fileLocation: String, type: TextureType = ImageTexture.DEFAULT_TYPE) {
        bitmap = ImageTexture.getBitmap(materialLoader.assets, fileLocation, type)
    }

    fun setBitmap(@DrawableRes drawableResId: Int, type: TextureType = ImageTexture.DEFAULT_TYPE) {
        bitmap = ImageTexture.getBitmap(materialLoader.context, drawableResId, type)
    }

    override fun destroy() {
        super.destroy()
        engine.safeDestroyTexture(texture)
    }
}
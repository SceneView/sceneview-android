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
    parent: Node? = null,
    builder: RenderableManager.Builder.() -> Unit = {}
) : PlaneNode(
    materialLoader.engine,
    size ?: normalize(Size(bitmap.width.toFloat(), bitmap.height.toFloat())),
    center, normal, uvScale,
    materialLoader.createImageInstance(texture, textureSampler),
    parent, builder
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
        parent: Node? = null,
        builder: RenderableManager.Builder.() -> Unit = {},
        textureBuilder: ImageTexture.Builder.() -> Unit = {}
    ) : this(
        materialLoader, bitmap,
        ImageTexture.Builder()
            .bitmap(bitmap)
            .type(type)
            .apply(textureBuilder)
            .build(materialLoader.engine),
        textureSampler, size, center, normal, uvScale, parent, builder
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
        parent: Node? = null,
        builder: RenderableManager.Builder.() -> Unit = {},
        textureBuilder: ImageTexture.Builder.() -> Unit = {}
    ) : this(
        materialLoader,
        ImageTexture.getBitmap(materialLoader.assets, imageFileLocation),
        size, center, normal, uvScale, type, textureSampler, parent, builder, textureBuilder
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
        parent: Node? = null,
        builder: RenderableManager.Builder.() -> Unit = {},
        textureBuilder: ImageTexture.Builder.() -> Unit = {}
    ) : this(
        materialLoader,
        ImageTexture.getBitmap(materialLoader.context, imageResId),
        size, center, normal, uvScale, type, textureSampler, parent, builder, textureBuilder
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
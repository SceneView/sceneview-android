package io.github.sceneview.node

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.RenderableManager
import com.google.android.filament.Texture
import com.google.android.filament.utils.TextureType
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.Entity
import io.github.sceneview.geometries.Plane
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.safeDestroyTexture
import io.github.sceneview.texture.ImageTexture
import io.github.sceneview.texture.setBitmap

open class ImageNode private constructor(
    engine: Engine,
    val materialLoader: MaterialLoader,
    entity: Entity = EntityManager.get().create(),
    parent: Node? = null,
    bitmap: Bitmap,
    val texture: Texture,
    /**
     * `null` to adjust size on the normalized image size
     */
    val size: Size? = null,
    center: Position = Plane.DEFAULT_CENTER,
    normal: Direction = Plane.DEFAULT_NORMAL,
    builder: RenderableManager.Builder.() -> Unit = {}
) : PlaneNode(
    engine, entity, parent,
    size ?: normalize(Size(bitmap.width.toFloat(), bitmap.height.toFloat())),
    center, normal,
    materialLoader.createImageInstance(texture),
    builder
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
        engine: Engine,
        materialLoader: MaterialLoader,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        bitmap: Bitmap,
        /**
         * `null` to adjust size on the normalized image size
         */
        size: Size? = null,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        type: TextureType = ImageTexture.DEFAULT_TYPE,
        builder: RenderableManager.Builder.() -> Unit = {},
        textureBuilder: ImageTexture.Builder.() -> Unit = {}
    ) : this(
        engine, materialLoader, entity, parent,
        bitmap,
        ImageTexture.Builder()
            .bitmap(bitmap)
            .type(type)
            .apply(textureBuilder)
            .build(engine),
        size, center, normal, builder
    )

    constructor(
        engine: Engine,
        materialLoader: MaterialLoader,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        /**
         * `null` to adjust size on the normalized image size
         */
        size: Size? = null,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        imageFileLocation: String,
        type: TextureType = ImageTexture.DEFAULT_TYPE,
        builder: RenderableManager.Builder.() -> Unit = {},
        textureBuilder: ImageTexture.Builder.() -> Unit = {}
    ) : this(
        engine, materialLoader, entity, parent,
        ImageTexture.getBitmap(materialLoader.assets, imageFileLocation),
        size, center, normal, type, builder, textureBuilder
    )

    constructor(
        engine: Engine,
        materialLoader: MaterialLoader,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        /**
         * `null` to adjust size on the normalized image size
         */
        size: Size? = null,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        @DrawableRes imageResId: Int,
        type: TextureType = ImageTexture.DEFAULT_TYPE,
        builder: RenderableManager.Builder.() -> Unit = {},
        textureBuilder: ImageTexture.Builder.() -> Unit = {}
    ) : this(
        engine, materialLoader, entity, parent,
        ImageTexture.getBitmap(materialLoader.context, imageResId),
        size, center, normal, type, builder, textureBuilder
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
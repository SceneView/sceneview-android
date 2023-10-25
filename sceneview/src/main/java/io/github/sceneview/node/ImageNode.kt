package io.github.sceneview.node

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.google.android.filament.RenderableManager
import com.google.android.filament.utils.TextureType
import io.github.sceneview.geometries.Plane
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.material.ImageMaterial
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

open class ImageNode(
    imageMaterial: ImageMaterial,
    size: Size = Plane.DEFAULT_SIZE,
    center: Position = Plane.DEFAULT_CENTER,
    normal: Direction = Plane.DEFAULT_NORMAL,
    /**
     * The parent node.
     *
     * If set to null, this node will not be attached.
     *
     * The local position, rotation, and scale of this node will remain the same.
     * Therefore, the world position, rotation, and scale of this node may be different after the
     * parent changes.
     */
    parent: Node? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : PlaneNode(
    engine = imageMaterial.engine,
    size = size,
    center = center,
    normal = normal,
    materialInstances = { imageMaterial.instance },
    parent = parent,
    renderableApply = renderableApply
) {
    constructor(
        materialLoader: MaterialLoader,
        bitmap: Bitmap,
        type: TextureType = TextureType.COLOR,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        /**
         * The parent node.
         *
         * If set to null, this node will not be attached.
         *
         * The local position, rotation, and scale of this node will remain the same.
         * Therefore, the world position, rotation, and scale of this node may be different after the
         * parent changes.
         */
        parent: Node? = null,
        renderableApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        imageMaterial = materialLoader.createImageMaterial(bitmap, type),
        size = size,
        center = center,
        normal = normal,
        parent = parent,
        renderableApply = renderableApply
    )

    constructor(
        materialLoader: MaterialLoader,
        assets: AssetManager,
        fileLocation: String,
        type: TextureType = TextureType.COLOR,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        /**
         * The parent node.
         *
         * If set to null, this node will not be attached.
         *
         * The local position, rotation, and scale of this node will remain the same.
         * Therefore, the world position, rotation, and scale of this node may be different after the
         * parent changes.
         */
        parent: Node? = null,
        renderableApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        imageMaterial = materialLoader.createImageMaterial(assets, fileLocation, type),
        size = size,
        center = center,
        normal = normal,
        parent = parent,
        renderableApply = renderableApply
    )

    constructor(
        materialLoader: MaterialLoader,
        context: Context,
        @DrawableRes drawableResId: Int,
        type: TextureType = TextureType.COLOR,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        /**
         * The parent node.
         *
         * If set to null, this node will not be attached.
         *
         * The local position, rotation, and scale of this node will remain the same.
         * Therefore, the world position, rotation, and scale of this node may be different after the
         * parent changes.
         */
        parent: Node? = null,
        renderableApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        imageMaterial = materialLoader.createImageMaterial(context, drawableResId, type),
        size = size,
        center = center,
        normal = normal,
        parent = parent,
        renderableApply = renderableApply
    )
}
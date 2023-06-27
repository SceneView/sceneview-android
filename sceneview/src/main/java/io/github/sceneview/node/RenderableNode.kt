package io.github.sceneview.node

import androidx.annotation.IntRange
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import dev.romainguy.kotlin.math.*
import io.github.sceneview.SceneView
import io.github.sceneview.math.*
import io.github.sceneview.model.*
import io.github.sceneview.renderable.*

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a renderable model for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the [SceneView]
 * .
 */
open class RenderableNode(engine: Engine, open val renderables: List<Renderable> = listOf()) :
    Node(engine) {

    val renderable get() = renderables.firstOrNull()
    val materialInstances get() = renderables.map { it.getMaterial() }

    constructor(engine: Engine, renderable: Renderable) : this(engine, listOf(renderable))

    /**
     * @see RenderableManager.setPriority
     */
    fun setPriority(@IntRange(from = 0, to = 7) priority: Int) =
        renderables.forEach { it.setPriority(priority) }

    /**
     * @see RenderableManager.setMaterialInstanceAt
     */
    fun setMaterialInstance(
        materialInstance: MaterialInstance,
        @IntRange(from = 0) primitiveIndex: Int = 0
    ) = renderables.forEach { it.setMaterialInstance(materialInstance, primitiveIndex) }

    /**
     * @see RenderableManager.setCastShadows
     */
    fun setCastShadows(enabled: Boolean) = renderables.forEach { it.setCastShadows(enabled) }

    /**
     * @see RenderableManager.setReceiveShadows
     */
    fun setReceiveShadows(enabled: Boolean) = renderables.forEach { it.setReceiveShadows(enabled) }

    /**
     * @see RenderableManager.setCulling
     */
    fun setCulling(enabled: Boolean) = renderables.forEach { it.setCulling(enabled) }

    /**
     * @see RenderableManager.setBlendOrder
     */
    fun setBlendOrder(
        @IntRange(from = 0, to = 65535) blendOrder: Int,
        @IntRange(from = 0) primitiveIndex: Int = 0
    ) = renderables.forEach { it.setBlendOrder(blendOrder, primitiveIndex) }

    /**
     * @see RenderableManager.setGlobalBlendOrderEnabledAt
     */
    fun setGlobalBlendOrderEnabled(enabled: Boolean, @IntRange(from = 0) primitiveIndex: Int = 0) =
        renderables.forEach { it.setGlobalBlendOrderEnabled(enabled, primitiveIndex) }

    /**
     * @see RenderableManager.setScreenSpaceContactShadows
     */
    fun setScreenSpaceContactShadows(enabled: Boolean) =
        renderables.forEach {
            it.setScreenSpaceContactShadows(enabled)
        }
}
package io.github.sceneview.model

import androidx.annotation.IntRange
import com.google.android.filament.Camera
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.gltfio.FilamentInstance
import io.github.sceneview.Entity
import io.github.sceneview.EntityInstance
import io.github.sceneview.node.Node

typealias ModelInstance = FilamentInstance

val ModelInstance.model get() = asset
val ModelInstance.engine get() = model.engine
val ModelInstance.renderableManager get() = engine.renderableManager
val ModelInstance.lightManager get() = engine.lightManager

val ModelInstance.renderableEntities: List<Entity>
    get() = entities.filter {
        renderableManager.hasComponent(it)
    }
val ModelInstance.renderableInstances: List<EntityInstance>
    get() = renderableEntities.map { renderableManager.getInstance(it) }

val ModelInstance.lightEntities: List<Entity>
    get() = entities.filter {
        lightManager.hasComponent(it)
    }
val ModelInstance.lightEntityInstances: List<EntityInstance>
    get() = lightEntities.map { lightManager.getInstance(it) }

val ModelInstance.camerasEntities: List<Entity>
    get() = entities.filter {
        engine.getCameraComponent(it) != null
    }
val ModelInstance.cameras: List<Camera>
    get() = entities.map { engine.getCameraComponent(it) }.filterNotNull()

/**
 * Attaches the given skin to the given node, which must have an associated mesh with
 * BONE_INDICES and BONE_WEIGHTS attributes.
 *
 * This is a no-op if the given skin index or target is invalid.
 */
fun ModelInstance.attachSkin(target: Node, @IntRange(from = 0) skinIndex: Int = 0) =
    attachSkin(skinIndex, target.entity)

/**
 * Attaches the given skin to the given node, which must have an associated mesh with
 * BONE_INDICES and BONE_WEIGHTS attributes.
 *
 * This is a no-op if the given skin index or target is invalid.
 */
fun ModelInstance.detachSkin(target: Node, @IntRange(from = 0) skinIndex: Int = 0) =
    detachSkin(skinIndex, target.entity)

/**
 * Gets the immutable number of primitives in the renderables.
 */
val ModelInstance.renderablesPrimitiveCounts: Map<EntityInstance, Int>
    get() = renderableInstances.associateWith {
        renderableManager.getPrimitiveCount(it)
    }

/**
 * Changes the material instances binding for the given primitives.
 *
 * @see RenderableManager.getMaterialInstanceAt
 */
var ModelInstance.materialInstances: Map<EntityInstance, List<MaterialInstance>>
    get() = renderablesPrimitiveCounts.mapValues { (renderableInstance, primitiveCount) ->
        List(primitiveCount) { index ->
            renderableManager.getMaterialInstanceAt(renderableInstance, index)
        }
    }
    set(value) {
        value.forEach { (renderableInstance, materialInstances) ->
            materialInstances.forEachIndexed { primitiveIndex, materialInstance ->
                renderableManager.setMaterialInstanceAt(
                    renderableInstance,
                    primitiveIndex,
                    materialInstance
                )
            }
        }
    }

/**
 * Updates the vertex morphing weights on a renderable, all zeroes by default.
 *
 * The renderable must be built with morphing enabled. In legacy morphing mode, only the first 4
 * weights are considered.
 *
 * @see RenderableManager.setMorphWeights
 */
fun ModelInstance.setMorphWeights(
    weights: FloatArray,
    @IntRange(from = 0) offset: Int = weights.size
) = renderableInstances.forEach { renderableManager.setMorphWeights(it, weights, offset) }

/**
 * Changes the visibility bits.
 *
 * @see RenderableManager.setLayerMask
 */
fun ModelInstance.setLayerMask(
    @IntRange(from = 0, to = 255) select: Int,
    @IntRange(from = 0, to = 255) value: Int
) = renderableInstances.forEach { renderableManager.setLayerMask(it, select, value) }

/**
 * Changes the coarse-level draw ordering.
 *
 * @see RenderableManager.setPriority
 */
fun ModelInstance.setPriority(@IntRange(from = 0, to = 7) priority: Int) =
    renderableInstances.forEach { renderableManager.setPriority(it, priority) }

/**
 * Changes whether or not frustum culling is on.
 *
 * @see RenderableManager.setCulling
 */
fun ModelInstance.setCulling(enabled: Boolean) =
    renderableInstances.forEach { renderableManager.setCulling(it, enabled) }

/**
 * Changes whether or not the renderable casts shadows.
 *
 * @see RenderableManager.isShadowCaster
 */
var ModelInstance.isShadowCaster: Boolean
    get() = renderableInstances.all { renderableManager.isShadowCaster(it) }
    set(value) = renderableInstances.forEach { renderableManager.setCastShadows(it, value) }

/**
 * Changes whether or not the renderable can receive shadows.
 *
 * @see RenderableManager.isShadowReceiver
 */
var ModelInstance.isShadowReceiver: Boolean
    get() = renderableInstances.all { renderableManager.isShadowReceiver(it) }
    set(value) = renderableInstances.forEach { renderableManager.setReceiveShadows(it, value) }

/**
 * Changes whether or not the renderable can use screen-space contact shadows.
 *
 * @see RenderableManager.setScreenSpaceContactShadows
 */
fun ModelInstance.setScreenSpaceContactShadows(enabled: Boolean) =
    renderableInstances.forEach { renderableManager.setScreenSpaceContactShadows(it, enabled) }

/**
 * Changes the drawing order for blended primitives.
 *
 * The drawing order is either global or local (default) to this Renderable.
 * In either case, the Renderable priority takes precedence.
 *
 * @param blendOrder draw order number (0 by default). Only the lowest 15 bits are used.
 *
 * @see RenderableManager.setBlendOrderAt
 */
fun ModelInstance.setBlendOrder(@IntRange(from = 0, to = 65535) blendOrder: Int) {
    renderablesPrimitiveCounts.forEach { (renderableInstance, primitiveCount) ->
        for (primitiveIndex in 0 until primitiveCount) {
            renderableManager.setBlendOrderAt(renderableInstance, primitiveIndex, blendOrder)
        }
    }
}

/**
 * Changes whether the blend order is global or local to this Renderable (by default).
 *
 * @param enabled true for global, false for local blend ordering.
 *
 * @see RenderableManager.setGlobalBlendOrderEnabled
 */
fun ModelInstance.setGlobalBlendOrderEnabled(enabled: Boolean) {
    renderablesPrimitiveCounts.forEach { (renderableInstance, primitiveCount) ->
        for (primitiveIndex in 0 until primitiveCount) {
            renderableManager.setGlobalBlendOrderEnabledAt(
                renderableInstance,
                primitiveIndex,
                enabled
            )
        }
    }
}
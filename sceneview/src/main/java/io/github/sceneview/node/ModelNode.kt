package io.github.sceneview.node

import androidx.annotation.IntRange
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.max
import io.github.sceneview.Entity
import io.github.sceneview.components.RenderableComponent
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.model.Model
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.model.camerasEntities
import io.github.sceneview.model.collisionShape
import io.github.sceneview.model.engine
import io.github.sceneview.model.getAnimationIndex
import io.github.sceneview.model.lightEntities
import io.github.sceneview.model.model
import io.github.sceneview.model.renderableEntities
import io.github.sceneview.utils.intervalSeconds

/**
 * Create the ModelNode from a loaded model instance.
 *
 * @see ModelLoader
 */
open class ModelNode(engine: Engine) : Node(engine) {

    interface ChildNode {
        val entity: Entity
        val modelInstance: ModelInstance

        val model: Model get() = modelInstance.asset

        fun getName() = model.getName(entity)

        /**
         * Gets the glTF extras string for the asset or a specific node.
         */
        val extras: String? get() = model.getExtras(entity)

        /**
         * Gets the names of all morph targets in the given entity.
         */
        val morphTargetNames: List<String> get() = model.getMorphTargetNames(entity).toList()
    }

    class RootNode internal constructor(
        override var modelInstance: ModelInstance,
        entity: Entity
    ) : io.github.sceneview.node.RenderableNode(engine = modelInstance.engine, entity = entity),
        ChildNode

    class RenderableNode internal constructor(
        override val modelInstance: ModelInstance,
        entity: Entity
    ) : io.github.sceneview.node.RenderableNode(
        engine = modelInstance.engine,
        entity = entity
    ), ChildNode {
        override var isTouchable = true
        override var isEditable = false
    }

    class LightNode internal constructor(
        override val modelInstance: ModelInstance,
        entity: Entity
    ) : io.github.sceneview.node.LightNode(
        engine = modelInstance.engine,
        entity = entity
    ), ChildNode

    class CameraNode internal constructor(
        override val modelInstance: ModelInstance,
        entity: Entity,
        override var viewSize: android.util.Size = android.util.Size(0, 0),
    ) : io.github.sceneview.node.CameraNode(
        engine = modelInstance.engine,
        entity = entity,
        viewSize = viewSize
    ), ChildNode

    data class PlayingAnimation(val startTime: Long = System.nanoTime(), val loop: Boolean = true)

    /**
     * The [ModelInstance] to display.
     *
     * The renderable is usually a 3D model.
     * @see ModelLoader
     */
    var modelInstance: ModelInstance? = null
        set(value) {
            field = value
            animator = value?.animator
            collisionShape = value?.model?.collisionShape
            rootNode = value?.let { RootNode(it, it.root) }
            renderableNodes =
                value?.renderableEntities?.map { RenderableNode(value, it) } ?: listOf()
            lightNodes = value?.lightEntities?.map { LightNode(value, it) } ?: listOf()
            cameraNodes = value?.camerasEntities?.map { CameraNode(value, it) } ?: listOf()
            playingAnimations.clear()
        }

    /**
     * The source [Model] ([FilamentAsset]) from the [ModelInstance].
     */
    val model get() = modelInstance?.model

    var rootNode: RootNode? = null
        private set(value) {
            field?.parent = null
            field = value
//            value?.transform = rootTransform
            value?.parent = this@ModelNode
        }
    var renderableNodes = listOf<RenderableNode>()
        private set(value) {
            field.forEach { it.parent = null }
            field = value
            value.forEach { it.parent = rootNode }
        }
    var lightNodes = listOf<LightNode>()
        private set(value) {
            field.forEach { it.parent = null }
            field = value
            value.forEach { it.parent = rootNode }
        }
    var cameraNodes = listOf<CameraNode>()
        private set(value) {
            field.forEach { it.parent = null }
            field = value
            value.forEach { it.parent = rootNode }
        }

    /**
     * Gets the bounding box computed from the supplied min / max values in glTF accessors.
     *
     * This does not return a bounding box over all FilamentInstance, it's just a straightforward
     * AAAB that can be determined at load time from the asset data.
     */
    val boundingBox get() = model?.boundingBox ?: Box()

    /**
     * Retrieves the [Animator] for this instance.
     */
    var animator: Animator? = null//modelInstance?.animator

    /**
     * The number of animation definitions in the glTF asset.
     */
    val animationCount get() = animator?.animationCount ?: 0

    var playingAnimations = mutableMapOf<Int, PlayingAnimation>()

    /**
     * Gets the skin count of this instance.
     */
    val skinCount: Int get() = modelInstance?.skinCount ?: 0

    /**
     * Returns the names of all material variants.
     */
    val materialVariantNames: List<String>
        get() = modelInstance?.materialVariantNames?.toList() ?: listOf()

    /**
     * Gets the skin name at skin index in this instance.
     */
    val skinNames: List<String> get() = modelInstance?.skinNames?.toList() ?: listOf()

    /**
     * Changes whether or not the renderable casts shadows.
     *
     * @see RenderableComponent.isShadowCaster
     */
    var isShadowCaster: Boolean
        get() = renderableNodes.all { it.isShadowCaster }
        set(value) = renderableNodes.forEach { it.isShadowCaster = value }

    /**
     * Changes whether or not the renderable can receive shadows.
     *
     * @see RenderableComponent.isShadowReceiver
     */
    var isShadowReceiver: Boolean
        get() = renderableNodes.all { it.isShadowReceiver }
        set(value) = renderableNodes.forEach { it.isShadowReceiver = value }

    override var isTouchable = true

    /**
     * Construct a Model Node with a model instance
     *
     * @see setModelInstance
     */
    constructor(
        modelInstance: ModelInstance,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null
    ) : this(modelInstance.model.engine) {
        setModelInstance(modelInstance, autoAnimate, scaleToUnits, centerOrigin)
    }

    /**
     * Set the [ModelInstance] to display.
     *
     * @param modelInstance The renderable is usually a 3D model.
     * @param autoAnimate Plays the animations automatically if the model has one.
     * @param scaleToUnits Scale the model to fit a unit cube. Default `null` to keep model original
     * size.
     * @param centerOrigin Center the model origin to this unit cube position.
     *
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    fun setModelInstance(
        modelInstance: ModelInstance,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null
    ) {
        this.modelInstance = modelInstance

        if (autoAnimate && (animator?.animationCount ?: 0) > 0) {
            playAnimation(0)
        }
        scaleToUnits?.let { scaleModel(it) }
        centerOrigin?.let { centerModel(it) }
    }

    override fun onFrame(frameTimeNanos: Long) {
        super.onFrame(frameTimeNanos)

        model?.popRenderable()

        playingAnimations.forEach { (index, animation) ->
            animator?.let { animator ->
                val elapsedTimeSeconds = frameTimeNanos.intervalSeconds(animation.startTime)
                animator.applyAnimation(index, elapsedTimeSeconds.toFloat())

                if (!animation.loop && elapsedTimeSeconds >= animator.getAnimationDuration(index)) {
                    playingAnimations.remove(index)
                }
            }
        }
        animator?.updateBoneMatrices()
    }

    /**
     * Sets up a root transform on the current model to make it fit into a unit cube.
     *
     * @param units the number of units of the cube to scale into.
     */
    fun scaleModel(units: Float = 1.0f) {
        val halfExtent = boundingBox.halfExtent.let { v ->
            Float3(v[0], v[1], v[2])
        }
        rootNode?.scale = Scale(units / (max(halfExtent) * 2.0f))
    }

    /**
     * Sets up a root transform on the current model to make it centered.
     *
     * @param origin Coordinate inside the model unit cube from where it is centered
     * - defaults to [0, 0, 0] will center the model on its center
     * - center horizontal | bottom aligned = [0, -1, 0]
     * - left | top aligned: [-1, 1, 0]
     */
    fun centerModel(origin: Position = Position(x = 0.0f, y = 0.0f, z = 0.0f)) {
        val center = boundingBox.center.let { v -> Float3(v[0], v[1], v[2]) }
        val halfExtent = boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
        rootNode?.position = -(center + halfExtent * origin) * (rootNode?.scale ?: Scale(1.0f))
    }

    /**
     * Applies rotation, translation, and scale to entities that have been targeted by the given
     * animation definition. Uses `TransformManager`.
     *
     * @param animationIndex Zero-based index for the `animation` of interest.
     *
     * @see Animator.getAnimationCount
     */
    fun playAnimation(animationIndex: Int, loop: Boolean = true) {
        if (animationIndex < animationCount) {
            playingAnimations[animationIndex] = PlayingAnimation(loop = loop)
        }
    }

    /**
     * @see playAnimation
     * @see Animator.getAnimationName
     */
    fun playAnimation(animationName: String, loop: Boolean = true) {
        animator?.getAnimationIndex(animationName)?.let { playAnimation(it, loop) }
    }

    fun stopAnimation(animationIndex: Int) {
        playingAnimations.remove(animationIndex)
    }

    fun stopAnimation(animationName: String) {
        animator?.getAnimationIndex(animationName)?.let { stopAnimation(it) }
    }

    /**
     * Attaches the given skin to the given node, which must have an associated mesh with
     * BONE_INDICES and BONE_WEIGHTS attributes.
     *
     * This is a no-op if the given skin index or target is invalid.
     */
    fun attachSkin(target: Node, @IntRange(from = 0) skinIndex: Int = 0) =
        modelInstance?.attachSkin(skinIndex, target.entity)

    /**
     * Attaches the given skin to the given node, which must have an associated mesh with
     * BONE_INDICES and BONE_WEIGHTS attributes.
     *
     * This is a no-op if the given skin index or target is invalid.
     */
    fun detachSkin(target: Node, @IntRange(from = 0) skinIndex: Int = 0) =
        modelInstance?.detachSkin(skinIndex, target.entity)

    /**
     * Gets the joint count at skin index in this instance.
     */
    open fun getJointCount(@IntRange(from = 0) skinIndex: Int = 0): Int =
        modelInstance?.getJointCountAt(skinIndex) ?: 0

    /**
     * Changes the material instances binding for the given primitives.
     *
     * @see RenderableComponent.materialInstances
     */
    var materialInstances: List<List<MaterialInstance>>
        get() = renderableNodes.map { it.materialInstances }
        set(value) {
            value.forEachIndexed { index, materialInstances ->
                renderableNodes[index].materialInstances = materialInstances
            }
        }

    /**
     * Changes the material instance binding for all primitives.
     *
     * @see RenderableComponent.setMaterialInstances
     */
    fun setMaterialInstance(materialInstance: MaterialInstance) {
        renderableNodes.forEach { it.setMaterialInstances(materialInstance) }
    }

    /**
     * Updates the vertex morphing weights on a renderable, all zeroes by default.
     *
     * The renderable must be built with morphing enabled. In legacy morphing mode, only the first
     * 4 weights are considered.
     *
     * @see RenderableComponent.setMorphWeights
     */
    fun setMorphWeights(weights: FloatArray, @IntRange(from = 0) offset: Int = weights.size) =
        renderableNodes.forEach { it.setMorphWeights(weights, offset) }

    /**
     * Changes the visibility.
     *
     * @see RenderableManager.setLayerMask
     */
    fun setLayerVisible(visible: Boolean) = renderableNodes.forEach { it.setLayerVisible(visible) }

    /**
     * Changes the visibility bits.
     *
     * @see RenderableComponent.setLayerMask
     */
    fun setLayerMask(
        @IntRange(from = 0, to = 255) select: Int,
        @IntRange(from = 0, to = 255) value: Int
    ) = renderableNodes.forEach { it.setLayerMask(select, value) }

    /**
     * Changes the coarse-level draw ordering.
     *
     * @see RenderableComponent.setPriority
     */
    fun setPriority(@IntRange(from = 0, to = 7) priority: Int) =
        renderableNodes.forEach { it.setPriority(priority) }

    /**
     * Changes whether or not frustum culling is on.
     *
     * @see RenderableComponent.setCulling
     */
    fun setCulling(enabled: Boolean) = renderableNodes.forEach { it.setCulling(enabled) }

    /**
     * Changes whether or not the renderable can use screen-space contact shadows.
     *
     * @see RenderableComponent.setScreenSpaceContactShadows
     */
    fun setScreenSpaceContactShadows(enabled: Boolean) =
        renderableNodes.forEach { it.setScreenSpaceContactShadows(enabled) }

    /**
     * Changes the drawing order for blended primitives.
     *
     * The drawing order is either global or local (default) to this Renderable.
     * In either case, the Renderable priority takes precedence.
     *
     * @param blendOrder draw order number (0 by default). Only the lowest 15 bits are used.
     *
     * @see RenderableComponent.setBlendOrder
     */
    fun setBlendOrder(@IntRange(from = 0, to = 65535) blendOrder: Int) {
        renderableNodes.forEach { it.setBlendOrder(blendOrder) }
    }

    /**
     * Changes whether the blend order is global or local to this Renderable (by default).
     *
     * @param enabled true for global, false for local blend ordering.
     *
     * @see RenderableComponent.setGlobalBlendOrderEnabled
     */
    fun setGlobalBlendOrderEnabled(enabled: Boolean) {
        renderableNodes.forEach { it.setGlobalBlendOrderEnabled(enabled) }
    }
}

inline operator fun <reified T : ModelNode.ChildNode> List<T>.get(name: String): T? =
    firstOrNull { it.getName() == name }

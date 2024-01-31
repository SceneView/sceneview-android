package io.github.sceneview.nodes

import androidx.annotation.IntRange
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.gltfio.Animator
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.components.CameraComponent
import io.github.sceneview.components.LightComponent
import io.github.sceneview.components.RenderableComponent
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.managers.NodeManager
import io.github.sceneview.model.Model
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.model.getAnimationIndex
import io.github.sceneview.utils.FrameTime

/**
 * Create the ModelNode from a loaded model instance
 *
 * @param engine [SceneView.engine] or your own shared [Engine.create]
 *
 * @see ModelLoader.createModel
 * @see ModelLoader.loadModel
 */
open class ModelNode private constructor(
    engine: Engine,
    nodeManager: NodeManager,
    val modelInstance: ModelInstance
) : Node(
    engine,
    nodeManager,
    modelInstance.root,
    true,
    true,
    listOf(modelInstance.root) + modelInstance.entities.toList()
) {
    data class PlayingAnimation(val startTime: Long = System.nanoTime(), val loop: Boolean = true)

    val model: Model get() = modelInstance.asset
    val entities: List<Entity> get() = modelInstance.entities.toList()

    val nodes: List<ChildNode>
    val renderableNodes: List<RenderableNode> get() = nodes.mapNotNull { it as? RenderableNode }
    val lightNodes: List<LightNode> get() = nodes.mapNotNull { it as? LightNode }
    val cameraNodes: List<CameraNode> get() = nodes.mapNotNull { it as? CameraNode }

    /**
     * Retrieved the [Animator] for the model instance
     */
    val animator: Animator = modelInstance.animator

    /**
     * The number of animation definitions in the glTF asset
     */
    val animationCount get() = animator.animationCount
    var playingAnimations = mutableMapOf<Int, PlayingAnimation>()

    init {
        // We use allChildEntities instead of entities because of this:
        // https://github.com/google/filament/discussions/6113
        nodes = entities.map { childEntity ->
            when {
                engine.renderableManager.hasComponent(childEntity) -> RenderableNode(
                    engine,
                    nodeManager,
                    modelInstance,
                    childEntity
                )
                engine.lightManager.hasComponent(childEntity) -> LightNode(
                    engine,
                    nodeManager,
                    modelInstance,
                    childEntity
                )
                engine.getCameraComponent(childEntity) != null -> CameraNode(
                    engine,
                    nodeManager,
                    modelInstance,
                    childEntity
                )
                else -> ChildNode(engine, nodeManager, modelInstance, childEntity)
            }
        }

        //TODO: Used by Filament ModelViewer, see if it's useful
//        setScreenSpaceContactShadows(false)
//        setCulling(true)
    }

    /**
     * Create the ModelNode from a loaded model
     *
     * @see ModelLoader.loadModel
     * @see ModelLoader.createModel
     */
    constructor(engine: Engine, nodeManager: NodeManager, model: Model) : this(
        engine,
        nodeManager,
        model.instance
    ) {
        runCatching { model.releaseSourceData() }
    }

    /**
     * Create the ModelNode from a loaded model instance
     *
     * @see ModelLoader.loadModel
     * @see ModelLoader.createModel
     */
    constructor(sceneView: SceneView, modelInstance: ModelInstance) : this(
        sceneView.engine,
        sceneView.nodeManager,
        modelInstance
    )

    /**
     * Create the ModelNode from a loaded model
     *
     * @see [ModelLoader.loadInstancedModel]
     * @see [ModelLoader.createInstance]
     */
    constructor(sceneView: SceneView, model: Model) : this(
        sceneView.engine,
        sceneView.nodeManager,
        model
    )

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)

        animator.apply {
            playingAnimations.forEach { (index, animation) ->
                val elapsedTimeSeconds = frameTime.intervalSeconds(animation.startTime)
                applyAnimation(index, elapsedTimeSeconds.toFloat())

                if (!animation.loop && elapsedTimeSeconds >= getAnimationDuration(index)) {
                    playingAnimations.remove(index)
                }
            }
            updateBoneMatrices()
        }
    }

    /**
     * Applies rotation, translation, and scale to entities that have been targeted by the given
     * animation definition. Uses `TransformManager`.
     *
     * @param animationIndex Zero-based index for the `animation` of interest.
     *
     * @see animationCount
     */
    fun playAnimation(animationIndex: Int = 0, loop: Boolean = true) {
        if (animationIndex < animationCount) {
            playingAnimations[animationIndex] = PlayingAnimation(loop = loop)
        }
    }

    /**
     * @see playAnimation
     * @see Animator.getAnimationName
     */
    fun playAnimation(animationName: String, loop: Boolean = true) {
        animator.getAnimationIndex(animationName)?.let { playAnimation(it, loop) }
    }

    fun stopAnimation(animationIndex: Int = 0) {
        playingAnimations.remove(animationIndex)
    }

    fun stopAnimation(animationName: String) {
        animator.getAnimationIndex(animationName)?.let { stopAnimation(it) }
    }

    /**
     * Gets the skin count of this instance.
     */
    val skinCount: Int get() = modelInstance.skinCount

    /**
     * Gets the skin name at skin index in this instance.
     */
    val skinNames: List<String> get() = modelInstance.skinNames.toList()

    /**
     * Attaches the given skin to the given node, which must have an associated mesh with
     * BONE_INDICES and BONE_WEIGHTS attributes.
     *
     * This is a no-op if the given skin index or target is invalid.
     */
    fun attachSkin(target: Node, @IntRange(from = 0) skinIndex: Int = 0) =
        modelInstance.attachSkin(skinIndex, target.entity)

    /**
     * Attaches the given skin to the given node, which must have an associated mesh with
     * BONE_INDICES and BONE_WEIGHTS attributes.
     *
     * This is a no-op if the given skin index or target is invalid.
     */
    fun detachSkin(target: Node, @IntRange(from = 0) skinIndex: Int = 0) =
        modelInstance.detachSkin(skinIndex, target.entity)

    /**
     * Gets the joint count at skin index in this instance.
     */
    open fun getJointCount(@IntRange(from = 0) skinIndex: Int = 0): Int =
        modelInstance.getJointCountAt(skinIndex)

    /**
     * Gets joints at skin index in this instance.
     */
    fun getJoints(@IntRange(from = 0) skinIndex: Int = 0): List<Node> =
        modelInstance.getJointsAt(skinIndex).map { nodeManager.getNode(it) }.filterNotNull()

    /**
     * Changes the material instances binding for the given primitives
     *
     * @see RenderableComponent.materialInstances
     */
    var materialInstances: List<MaterialInstance>
        get() = renderableNodes.flatMap { it.materialInstances }
        // TODO: Is it the same:
        // modelInstance.materialInstances.toList()
        set(value) {
            var start = 0
            renderableNodes.forEach {
                it.materialInstances = value.subList(start, start + it.primitiveCount)
                start += it.primitiveCount
            }
        }

    /**
     * Changes the material instance binding for all primitives
     *
     * @see RenderableComponent.setMaterialInstance
     */
    fun setMaterialInstance(materialInstance: MaterialInstance) {
        renderableNodes.forEach { it.setMaterialInstance(materialInstance) }
    }

    /**
     * Returns the names of all material variants.
     */
    val materialVariantNames: List<String> get() = modelInstance.materialVariantNames.toList()

    /**
     * Updates the vertex morphing weights on a renderable, all zeroes by default
     *
     * The renderable must be built with morphing enabled. In legacy morphing mode, only the
     * first 4 weights are considered.
     *
     * @see RenderableComponent.setMorphWeights
     */
    fun setMorphWeights(weights: FloatArray, @IntRange(from = 0) offset: Int = weights.size) =
        renderableNodes.forEach { it.setMorphWeights(weights, offset) }

    /**
     * Changes the visibility bits
     *
     * @see RenderableComponent.setLayerMask
     */
    fun setLayerMask(
        @IntRange(from = 0, to = 255) select: Int,
        @IntRange(from = 0, to = 255) value: Int
    ) = renderableNodes.forEach { it.setLayerMask(select, value) }

    /**
     * Changes the coarse-level draw ordering
     *
     * @see RenderableComponent.setPriority
     */
    fun setPriority(@IntRange(from = 0, to = 7) priority: Int) =
        renderableNodes.forEach { it.setPriority(priority) }

    /**
     * Changes whether or not frustum culling is on
     *
     * @see RenderableComponent.setCulling
     */
    fun setCulling(enabled: Boolean) = renderableNodes.forEach { it.setCulling(enabled) }

    /**
     * Changes whether or not the renderable casts shadows
     *
     * @see RenderableComponent.isShadowCaster
     */
    var isShadowCaster: Boolean
        get() = renderableNodes.all { it.isShadowCaster }
        set(value) = renderableNodes.forEach { it.isShadowCaster = value }

    /**
     * Changes whether or not the renderable can receive shadows
     *
     * @see RenderableComponent.isShadowReceiver
     */
    var isShadowReceiver: Boolean
        get() = renderableNodes.all { it.isShadowReceiver }
        set(value) = renderableNodes.forEach { it.isShadowReceiver = value }

    /**
     * Changes whether or not the renderable can use screen-space contact shadows
     *
     * @see RenderableComponent.setScreenSpaceContactShadows
     */
    fun setScreenSpaceContactShadows(enabled: Boolean) =
        renderableNodes.forEach { it.setScreenSpaceContactShadows(enabled) }

    /**
     * Changes the drawing order for blended primitives
     *
     * The drawing order is either global or local (default) to this Renderable.
     * In either case, the Renderable priority takes precedence.
     *
     * @param blendOrder draw order number (0 by default). Only the lowest 15 bits are used
     *
     * @see RenderableComponent.setBlendOrder
     */
    fun setBlendOrder(@IntRange(from = 0, to = 65535) blendOrder: Int) {
        renderableNodes.forEach { it.setBlendOrder(blendOrder) }
    }

    /**
     * Changes whether the blend order is global or local to this Renderable (by default)
     *
     * @param enabled true for global, false for local blend ordering
     *
     * @see RenderableComponent.setGlobalBlendOrderEnabled
     */
    fun setGlobalBlendOrderEnabled(enabled: Boolean) {
        renderableNodes.forEach { it.setGlobalBlendOrderEnabled(enabled) }
    }

    override fun getBoundingBox() = model.boundingBox

    open class ChildNode internal constructor(
        engine: Engine,
        nodeManager: NodeManager,
        val modelInstance: ModelInstance,
        entity: Entity
    ) : Node(engine, nodeManager, entity, false, false) {
        val model: Model get() = modelInstance.asset
        val name: String? get() = model.getName(entity)

        /**
         * Gets the glTF extras string for the asset or a specific node.
         *
         * null if it does not exist.
         */
        val extras: String? get() = model.getExtras(entity)

        /**
         * Gets the names of all morph targets in the given entity.
         */
        val morphTargetNames: List<String> get() = model.getMorphTargetNames(entity).toList()
    }

    class RenderableNode internal constructor(
        engine: Engine,
        nodeManager: NodeManager,
        modelInstance: ModelInstance,
        entity: Entity
    ) : ChildNode(engine, nodeManager, modelInstance, entity),
        RenderableComponent

    class LightNode internal constructor(
        engine: Engine,
        nodeManager: NodeManager,
        modelInstance: ModelInstance,
        entity: Entity
    ) : ChildNode(engine, nodeManager, modelInstance, entity),
        LightComponent

    class CameraNode internal constructor(
        engine: Engine,
        nodeManager: NodeManager,
        modelInstance: ModelInstance,
        entity: Entity
    ) : ChildNode(engine, nodeManager, modelInstance, entity),
        CameraComponent
}

inline operator fun <reified T : ModelNode.ChildNode> List<T>.get(name: String): T? =
    firstOrNull { it.name == name }
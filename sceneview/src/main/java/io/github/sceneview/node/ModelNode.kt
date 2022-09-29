package io.github.sceneview.node

import androidx.annotation.IntRange
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.gltfio.Animator
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.components.CameraComponent
import io.github.sceneview.components.LightComponent
import io.github.sceneview.components.RenderableComponent
import io.github.sceneview.components.TransformComponent
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.managers.NodeManager
import io.github.sceneview.model.Model
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.model.getAnimationIndex
import io.github.sceneview.utils.FrameTime

/**
 * Create the ModelNode from a loaded model
 *
 * @param engine [SceneView.engine] or your own shared [Engine.create]
 *
 * @see ModelLoader.createModel
 * @see ModelLoader.loadModel
 */
open class ModelNode private constructor(
    engine: Engine,
    nodeManager: NodeManager,
    val model: Model,
    val animator: Animator,
    rootEntity: Entity,
    entities: List<Entity>,
    val renderableEntities: List<Entity>,
    val lightEntities: List<Entity>,
    val cameraEntities: List<Entity>,
) : Node(
    engine,
    nodeManager,
    rootEntity,
    listOf(rootEntity) + entities,
    isSelectable = true
), TransformComponent {

    data class PlayingAnimation(val startTime: Long = System.nanoTime(), val loop: Boolean = true)

    val renderableNodes: List<RenderableNode> get() = allChildNodes.mapNotNull { it as? RenderableNode }
    val lightNodes: List<LightNode> get() = allChildNodes.mapNotNull { it as? LightNode }
    val cameraNodes: List<CameraNode> get() = allChildNodes.mapNotNull { it as? CameraNode }

    /**
     * The number of animation definitions in the glTF asset
     */
    val animationCount get() = animator.animationCount
    var playingAnimations = mutableMapOf<Int, PlayingAnimation>()

    final override val allChildEntities: List<Entity>
        get() = super<TransformComponent>.allChildEntities

    init {
        // We use allChildEntities instead of entities because of this:
        // https://github.com/google/filament/discussions/6113
        allChildEntities.forEach { childEntity ->
            val childNode = when (childEntity) {
                in renderableEntities -> RenderableNode(
                    model,
                    childEntity
                ).apply { setEditable(true) }
                in lightEntities -> LightNode(model, childEntity)
                in cameraEntities -> CameraNode(model, childEntity)
                else -> ChildNode(model, childEntity)
            }
            nodeManager.addComponent(childEntity, childNode)
        }

        //TODO: Used by Filament ModelViewer, see if it's useful
        setScreenSpaceContactShadows(true)

        model.releaseSourceData()
    }

    /**
     * Create the ModelNode from a loaded model
     *
     * @see ModelLoader.loadModel
     * @see ModelLoader.createModel
     */
    constructor(
        engine: Engine,
        nodeManager: NodeManager,
        model: Model
    ) : this(
        engine,
        nodeManager,
        model,
        model.animator,
        model.root,
        model.entities.toList(),
        model.renderableEntities.toList(),
        model.lightEntities.toList(),
        model.cameraEntities.toList()
    )

    /**
     * Create the ModelNode from a loaded model instance
     *
     * @see ModelLoader.loadInstancedModel
     * @see ModelLoader.createInstance
     */
    constructor(engine: Engine, nodeManager: NodeManager, modelInstance: ModelInstance) : this(
        engine,
        nodeManager,
        modelInstance.asset,
        modelInstance.animator,
        modelInstance.root,
        modelInstance.entities.toList(),
        modelInstance.entities.filter { engine.renderableManager.hasComponent(it) },
        modelInstance.entities.filter { engine.lightManager.hasComponent(it) },
        modelInstance.entities.filter { engine.getCameraComponent(it) != null }
    )

    /**
     * Create the ModelNode from a loaded model
     *
     * @see ModelLoader.loadModel
     * @see ModelLoader.createModel
     */
    constructor(sceneView: SceneView, model: Model) : this(
        sceneView.engine,
        sceneView.nodeManager,
        model
    )

    /**
     * Create the ModelNode from a loaded model instance
     *
     * @see [ModelLoader.loadInstancedModel]
     * @see [ModelLoader.createInstance]
     */
    constructor(sceneView: SceneView, modelInstance: ModelInstance) : this(
        sceneView.engine,
        sceneView.nodeManager,
        modelInstance
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
        if (animationIndex <= animationCount) {
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
     * Updates the vertex morphing weights on a renderable, all zeroes by default
     *
     * The renderable must be built with morphing enabled. In legacy morphing mode, only the
     * first 4 weights are considered.
     *
     * @see RenderableComponent.setMorphWeights
     */
    fun setMorphWeights(weights: FloatArray, @IntRange(from = 0) offset: Int) =
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
     * Changes the material instances binding for the given primitives
     *
     * @see RenderableComponent.materialInstances
     */
    var materialInstances: List<MaterialInstance>
        get() = renderableNodes.flatMap { it.materialInstances }
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

    open inner class ChildNode internal constructor(model: Model, entity: Entity) :
        Node(engine, nodeManager, entity) {
        val name: String? = model.getName(entity)
        val extras: String? = model.getExtras(entity)
    }

    inner class RenderableNode internal constructor(model: Model, entity: Entity) :
        ChildNode(model, entity), RenderableComponent

    inner class LightNode internal constructor(model: Model, entity: Entity) :
        ChildNode(model, entity), LightComponent

    inner class CameraNode internal constructor(model: Model, entity: Entity) :
        ChildNode(model, entity), CameraComponent
}
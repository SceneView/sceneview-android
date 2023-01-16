package io.github.sceneview.node

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.filament.gltfio.Animator
import dev.romainguy.kotlin.math.*
import io.github.sceneview.SceneView
import io.github.sceneview.gesture.NodeMotionEvent
import io.github.sceneview.math.*
import io.github.sceneview.model.*
import io.github.sceneview.renderable.Renderable
import io.github.sceneview.utils.FrameTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a renderable model for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the [SceneView]
 * .
 */
open class ModelNode : RenderableNode {

    data class PlayingAnimation(val startTime: Long = System.nanoTime(), val loop: Boolean = true)

    /**
     * ### The node model origin (center)
     *
     * A node's content origin is the transformation between its coordinate space and that used by
     * its [position]. The default origin is zero vector, specifying that the node's position
     * locates the origin of its coordinate system relatively to that center point.
     *
     * Changing the origin transform alters these behaviors in many useful ways.
     * You can offset the node's contents relative to its position.
     * For example, by setting the pivot to a translation transform you can position a node
     * containing a sphere geometry relative to where the sphere would rest on a floor instead of
     * relative to its center.
     */
    open var modelPosition = DEFAULT_MODEL_POSITION

    /**
     * TODO: Doc
     */
    var modelQuaternion: Quaternion = DEFAULT_MODEL_QUATERNION

    /**
     * ### The node model orientation
     *
     * A node's content origin is the transformation between its coordinate space and that used by
     * its [quaternion]. The default origin is zero vector, specifying that the node's orientation
     * locates the origin of its rotation relatively to that center point.
     *
     * `[0..360]`
     *
     * Changing the origin transform alters these behaviors in many useful ways.
     * You can move the node's axis of rotation.
     * For example, with a translation transform you can cause a node to revolve around a faraway
     * point instead of rotating around its center, and with a rotation transform you can tilt the
     * axis of rotation.
     */
    open var modelRotation: Rotation
        get() = modelQuaternion.toEulerAngles()
        set(value) {
            modelQuaternion = Quaternion.fromEuler(value)
        }

    /**
     * ### The node model scale
     */
    open var modelScale = DEFAULT_MODEL_SCALE

    open var modelTransform: Transform = Transform(modelPosition, modelQuaternion, modelScale)
        get() = field.takeIf {
            it.position == modelPosition && it.quaternion == modelQuaternion && it.scale == modelScale
        } ?: Transform(modelPosition, modelQuaternion, modelScale)
        set(value) {
            if (field != value) {
                field = value
                modelPosition = value.position
                modelQuaternion = value.quaternion
                modelScale = value.scale
            }
        }

    /**
     * ### The [Model] to display.
     *
     * The renderable is usually a 3D model.
     * If null, this node's current renderable will be removed.
     */
    open var model: Model? = null
        set(value) {
            if (field != value) {
                field?.destroy()
                field = value
                animator = value?.animator
                sceneEntities = value?.entities ?: intArrayOf()

                onModelChanged(value)
            }
        }

    var animator: Animator? = null
    var playingAnimations = mutableMapOf<Int, PlayingAnimation>()

    var onModelLoaded = mutableListOf<(model: Model) -> Unit>()
    var onModelChanged = mutableListOf<(model: Model?) -> Unit>()
    var onModelError: ((exception: Exception) -> Unit)? = null

    override val transformEntity: Int?
        get() = model?.root

    override val renderables: List<Renderable>
        get() = model?.renderableEntities?.toList() ?: listOf()

    val renderableNames: List<String>
        get() = model?.renderableNames?.toList() ?: listOf()

    override val worldTransform: Transform
        get() = super.worldTransform * modelTransform

    /**
     * ### Construct a [ModelNode] with it Position, Rotation and Scale
     *
     * @param position See [Node.position]
     * @param rotation See [Node.rotation]
     * @param scale See [Node.scale]
     */
    @JvmOverloads
    constructor(
        position: Position = DEFAULT_POSITION,
        rotation: Rotation = DEFAULT_ROTATION,
        scale: Scale = DEFAULT_SCALE
    ) : super(position, rotation, scale)

    /**
     * ### Create the Node and load a monolithic binary glTF and add it to the Node
     *
     * @param lifecycle Provide your lifecycle in order to load your model instantly and to destroy
     * it (and its resources) when the lifecycle goes to destroy state.
     * Passing null means the model loading will be done when the Node is added to the SceneView and
     * the destroy will be done when the SceneView is detached.
     * Otherwise the model loading is done when the parent [SceneView] is attached because it needs
     * a [kotlinx.coroutines.CoroutineScope] to load and resources will be destroyed when the
     * [SceneView] is.
     * You are responsible of manually destroy this [Node] only if you don't provide lifecycle and
     * the node is never attached to a [SceneView]
     * @param modelGlbFileLocation the model glb/gltf file location:
     * ```
     * - A relative asset file location *models/mymodel.glb*
     * - An Android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * ```
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param scaleUnits Scale the model to fit a unit cube. Default `null` to keep model original
     * size
     * @param centerOrigin Center point origin position within the model:
     * Float cube position values between -1.0 and 1.0 corresponding to percents from
     * model sizes.
     * - `null` = Keep the origin point where it was at the model export time
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top
     * - ...
     * ```
     * @param onError An exception has been thrown during model loading
     * @param onLoaded Called when the model loading finished so you can change its properties
     * (material, texture,...)
     *
     * @see loadModel
     */
    constructor(
        context: Context,
        lifecycle: Lifecycle? = null,
        modelGlbFileLocation: String,
        autoAnimate: Boolean = true,
        scaleUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((model: Model) -> Unit)? = null
    ) : this() {
        loadModelGlbAsync(
            context,
            lifecycle,
            modelGlbFileLocation,
            autoAnimate,
            scaleUnits,
            centerOrigin,
            onError,
            onLoaded
        )
    }

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)

        animator?.apply {
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

    open fun onModelLoaded(model: Model) {
        onModelLoaded.forEach { it(model) }
    }

    open fun onModelChanged(mode: Model?) {
        // Refresh the collider to ensure it is using the correct collision shape now
        // that the renderable has changed.
        onTransformChanged()

        onModelChanged.forEach { it(model) }
    }

    open fun onModelError(exception: Exception) {
        onModelError?.invoke(exception)
    }

    override fun onRotate(e: NodeMotionEvent, rotationDelta: Quaternion) {
        modelQuaternion *= rotationDelta
    }

    override fun onScale(e: NodeMotionEvent, scaleFactor: Float) {
        modelScale = clamp(modelScale * scaleFactor, minEditableScale, maxEditableScale)
    }

    fun doOnModelLoaded(action: (model: Model) -> Unit) {
        if (model != null) {
            action(model!!)
        } else {
            onModelLoaded += object : (Model) -> Unit {
                override fun invoke(model: Model) {
                    onModelLoaded -= this
                    action(model)
                }
            }
        }
    }

    /**
     * ### Loads a monolithic binary glTF and add it to the Node
     *
     * @param lifecycle Provide your lifecycle in order to load your model instantly and to destroy
     * it (and its resources) when the lifecycle goes to destroy state.
     * Otherwise the model loading is done when the parent [SceneView] is attached because it needs
     * a [kotlinx.coroutines.CoroutineScope] to load and resources will be destroyed when the
     * [SceneView] is.
     * You are responsible of manually destroy this [Node] only if you don't provide lifecycle and
     * the node is never attached to a [SceneView]
     * @param glbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param scaleToUnits Scale the model to fit a unit cube. Default `null` to keep model original
     * size
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    suspend fun loadModelGlb(
        context: Context,
        glbFileLocation: String,
        lifecycle: Lifecycle,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null
    ): Model? {
        return try {
            GLBLoader.loadModel(context, glbFileLocation, lifecycle)?.also { model ->
                withContext(Dispatchers.Main) {
                    setModel(model, autoAnimate, scaleToUnits, centerOrigin)
                    onModelLoaded(model)
                }
            }
        } catch (error: Exception) {
            onModelError(error)
            onError?.invoke(error)
            null
        }
    }

    /**
     * ### Loads a monolithic binary glTF and add it to the Node
     *
     * @param lifecycle Provide your lifecycle in order to load your model instantly and to destroy
     * it (and its resources) when the lifecycle goes to destroy state.
     * Otherwise the model loading is done when the parent [SceneView] is attached because it needs
     * a [kotlinx.coroutines.CoroutineScope] to load and resources will be destroyed when the
     * [SceneView] is.
     * You are responsible of manually destroy this [Node] only if you don't provide lifecycle and
     * the node is never attached to a [SceneView]
     * @param glbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param scaleToUnits Scale the model to fit a unit cube. Default `null` to keep model original
     * size
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    fun loadModelGlbAsync(
        context: Context,
        lifecycle: Lifecycle? = null,
        glbFileLocation: String,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((model: Model) -> Unit)? = null
    ): ModelNode {
        if (lifecycle != null) {
            lifecycle.coroutineScope.launchWhenCreated {
                loadModelGlb(
                    context,
                    glbFileLocation,
                    lifecycle,
                    autoAnimate,
                    scaleToUnits,
                    centerOrigin,
                    onError
                )?.let { onLoaded?.invoke(it) }
            }
        } else {
            doOnAttachedToScene { sceneView ->
                loadModelGlbAsync(
                    context, sceneView.lifecycle, glbFileLocation, autoAnimate, scaleToUnits,
                    centerOrigin, onError, onLoaded
                )
            }
        }
        return this
    }

    /**
     * ### Loads a monolithic binary glTF and add it to the Node
     *
     * @param lifecycle Provide your lifecycle in order to load your model instantly and to destroy
     * it (and its resources) when the lifecycle goes to destroy state.
     * Otherwise the model loading is done when the parent [SceneView] is attached because it needs
     * a [kotlinx.coroutines.CoroutineScope] to load and resources will be destroyed when the
     * [SceneView] is.
     * You are responsible of manually destroy this [Node] only if you don't provide lifecycle and
     * the node is never attached to a [SceneView]
     * @param gltfFileLocation the gltf file location:
     * - A relative asset file location *models/mymodel.gltf*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.gltf*
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param scaleToUnits Scale the model to fit a unit cube. Default `null` to keep model original
     * size
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    suspend fun loadModelGltf(
        context: Context,
        gltfFileLocation: String,
        resourceLocationResolver: (String) -> String = { resourceFileName: String ->
            "${gltfFileLocation.substringBeforeLast("/")}/$resourceFileName"
        },
        lifecycle: Lifecycle,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null
    ): Model? {
        return try {
            GLTFLoader.loadModel(context, gltfFileLocation, resourceLocationResolver, lifecycle)
                ?.also { model ->
                    withContext(Dispatchers.Main) {
                        setModel(model, autoAnimate, scaleToUnits, centerOrigin)
                        onModelLoaded(model)
                    }
                }
        } catch (error: Exception) {
            onModelError(error)
            onError?.invoke(error)
            null
        }
    }

    /**
     * ### Loads a monolithic binary glTF and add it to the Node
     *
     * @param lifecycle Provide your lifecycle in order to load your model instantly and to destroy
     * it (and its resources) when the lifecycle goes to destroy state.
     * Otherwise the model loading is done when the parent [SceneView] is attached because it needs
     * a [kotlinx.coroutines.CoroutineScope] to load and resources will be destroyed when the
     * [SceneView] is.
     * You are responsible of manually destroy this [Node] only if you don't provide lifecycle and
     * the node is never attached to a [SceneView]
     * @param gltfFileLocation the gltf file location:
     * - A relative asset file location *models/mymodel.gltf*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.gltf*
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param scaleToUnits Scale the model to fit a unit cube. Default `null` to keep model original
     * size
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    fun loadModelGltfAsync(
        context: Context,
        lifecycle: Lifecycle? = null,
        gltfFileLocation: String,
        resourceLocationResolver: (String) -> String = { resourceFileName: String ->
            "${gltfFileLocation.substringBeforeLast("/")}/$resourceFileName"
        },
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((model: Model) -> Unit)? = null
    ): ModelNode {
        if (lifecycle != null) {
            lifecycle.coroutineScope.launchWhenCreated {
                loadModelGltf(
                    context,
                    gltfFileLocation,
                    resourceLocationResolver,
                    lifecycle,
                    autoAnimate,
                    scaleToUnits,
                    centerOrigin,
                    onError
                )?.let { onLoaded?.invoke(it) }
            }
        } else {
            doOnAttachedToScene { sceneView ->
                loadModelGltfAsync(
                    context,
                    sceneView.lifecycle,
                    gltfFileLocation,
                    resourceLocationResolver,
                    autoAnimate,
                    scaleToUnits,
                    centerOrigin,
                    onError,
                    onLoaded
                )
            }
        }
        return this
    }

    /**
     * ### Set the node model
     *
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param scaleToUnits Scale the model to fit a unit cube. Default `null` to keep model original
     * size
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    open fun setModel(
        model: Model?,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
    ) {
        this.model = model
        if (model != null) {
            if (autoAnimate && model.animator.animationCount > 0) {
                playAnimation(0)
            }
            scaleToUnits?.let { scaleModel(it) }
            centerOrigin?.let { centerModel(it) }
        }
    }

    fun getRenderableByName(name: String) = model?.getRenderableByName(name)

    /**
     * ### Sets up a root transform on the current model to make it fit into a unit cube
     *
     * @param units the number of units of the cube to scale into.
     */
    fun scaleModel(units: Float = 1.0f) {
        doOnModelLoaded { model ->
            val halfExtent = model.boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
            modelScale = Scale(units / (max(halfExtent) * 2.0f))
        }
    }

    /**
     * ### Sets up a root transform on the current model to make it centered
     *
     * @param origin Coordinate inside the model unit cube from where it is centered
     * - defaults to [0, 0, 0] will center the model on its center
     * - center horizontal | bottom aligned = [0, -1, 0]
     * - left | top aligned: [-1, 1, 0]
     */
    fun centerModel(origin: Position = Position(x = 0.0f, y = 0.0f, z = 0.0f)) {
        doOnModelLoaded { model ->
            val center = model.boundingBox.center.let { v -> Float3(v[0], v[1], v[2]) }
            val halfExtent = model.boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
            modelPosition = -(center + halfExtent * origin) * modelScale
        }
    }

    /**
     * ### Applies rotation, translation, and scale to entities that have been targeted by the given
     * animation definition. Uses `TransformManager`.
     *
     * @param animationIndex Zero-based index for the `animation` of interest.
     *
     * @see Animator.getAnimationCount
     */
    fun playAnimation(animationIndex: Int, loop: Boolean = true) {
        playingAnimations[animationIndex] = PlayingAnimation(loop = loop)
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

    /** ### Detach and destroy the node */
    override fun destroy() {
        model?.destroy()
        super.destroy()
    }

    override fun clone() = copy(ModelNode())

    open fun copy(toNode: ModelNode = ModelNode()): ModelNode = toNode.apply {
        super.copy(toNode)
        setModel(this@ModelNode.model)
    }

    companion object {
        val DEFAULT_MODEL_POSITION = Position(x = 0.0f, y = 0.0f, z = 0.0f)
        val DEFAULT_MODEL_QUATERNION = Quaternion()
        val DEFAULT_MODEL_ROTATION = DEFAULT_MODEL_QUATERNION.toEulerAngles()
        val DEFAULT_MODEL_SCALE = Scale(1.0f)
    }
}

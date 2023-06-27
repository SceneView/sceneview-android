package io.github.sceneview.node

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.EntityInstance
import com.google.android.filament.TransformManager
import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import com.google.ar.sceneform.math.Matrix
import dev.romainguy.kotlin.math.*
import io.github.sceneview.SceneView
import io.github.sceneview.gesture.NodeMotionEvent
import io.github.sceneview.math.*
import io.github.sceneview.model.*
import io.github.sceneview.renderable.Renderable
import io.github.sceneview.transform.*
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
open class ModelNode(engine: Engine) : RenderableNode(engine) {

    data class PlayingAnimation(val startTime: Long = System.nanoTime(), val loop: Boolean = true)

    open var modelRootEntity: Int? = null
        @Entity
        set(value) {
            modelRootInstance?.let {
                if (transformManager.getParentOrNull(it) == transformEntity) {
                    transformManager.setParent(it, null)
                }
            }
            field = value
            modelRootInstance?.let {
                transformManager.setParent(it, transformInstance)
            }
            modelTransform = _modelTransform
        }

    val modelRootInstance: Int?
        @EntityInstance
        get() = modelRootEntity?.let { transformManager.getInstance(it) }

    /**
     * The node model origin (center)
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
    open var modelPosition: Position
        get() = modelTransform.position
        set(value) {
            modelTransform = Transform(value, modelQuaternion, modelScale)
        }

    /**
     * Model quaternion rotation.
     *
     * @see modelTransform
     */
    var modelQuaternion: Quaternion
        get() = modelTransform.quaternion
        set(value) {
            modelTransform = Transform(modelPosition, value, modelScale)
        }

    /**
     * The node model orientation
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
     * The model scale
     */
    open var modelScale: Scale
        get() = modelTransform.scale
        set(value) {
            modelTransform = Transform(modelPosition, modelQuaternion, value)
        }

    private var _modelTransform = Transform()

    /**
     * Local transform of the model transform component (i.e. relative to the parent).
     *
     * @see TransformManager.getTransform
     * @see TransformManager.setTransform
     */
    var modelTransform: Transform
        get() = _modelTransform
        set(value) {
            _modelTransform = value
            modelRootInstance?.let {
                transformManager.setTransform(it, value)
            }
        }

    val modelWorldTransform: Transform
        get() = modelRootInstance?.let {
            transformManager.getWorldTransform(it)
        } ?: worldTransform

    /**
     * The source [Model] ([FilamentAsset]) from the [ModelInstance] [ModelInstance]
     */
    open val model get() = modelInstance?.model

    /**
     * The [ModelInstance] to display.
     *
     * The renderable is usually a 3D model.
     * If null, this node's current renderable will be removed.
     */
    open var modelInstance: ModelInstance? = null
        set(value) {
            if (field != value) {
//                field?.destroy()
                field = value
                animator = value?.animator
                modelRootEntity = value?.root
                sceneEntities = value?.entities ?: intArrayOf()

                onModelChanged(modelInstance)
            }
        }

    var animator: Animator? = null
    var playingAnimations = mutableMapOf<Int, PlayingAnimation>()

    var onModelLoaded = mutableListOf<(modelInstance: ModelInstance) -> Unit>()
    var onModelChanged = mutableListOf<() -> Unit>()
    var onModelError: ((exception: Exception) -> Unit)? = null

    override val renderables: List<Renderable>
        get() = modelInstance?.renderables?.toList() ?: listOf()

    val lights: List<Renderable>
        get() = modelInstance?.lights?.toList() ?: listOf()

    val renderableNames: List<String>
        get() = modelInstance?.model?.renderableNames?.toList() ?: listOf()

    private var loadedModel: Model? = null

    /**
     * ### Create the Node and load a monolithic binary glTF and add it to the Node
     *
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
        engine: Engine,
        modelGlbFileLocation: String,
        autoAnimate: Boolean = true,
        scaleUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((modelInstance: ModelInstance) -> Unit)? = null
    ) : this(engine) {
        loadModelGlbAsync(
            modelGlbFileLocation,
            autoAnimate,
            scaleUnits,
            centerOrigin,
            onError,
            onLoaded
        )
    }

    /**
     * ### Create the Node and load a monolithic binary glTF and add it to the Node
     *
     * @param modelInstance the model instance
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
    constructor(
        engine: Engine,
        modelInstance: ModelInstance,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null
    ) : this(engine) {
        setModelInstance(modelInstance, autoAnimate, scaleToUnits, centerOrigin)
    }

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)

        model?.let { it.popRenderable() }

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

    open fun onModelLoaded(modelInstance: ModelInstance) {
        onModelLoaded.forEach { it(modelInstance) }
    }

    open fun onModelChanged(modelInstance: ModelInstance?) {
        collisionShape = modelInstance?.model?.collisionShape

        // Refresh the collider to ensure it is using the correct collision shape now
        // that the renderable has changed.
        onTransformChanged()

        onModelChanged.forEach { it() }
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

    fun doOnModelLoaded(action: (modelInstance: ModelInstance) -> Unit) {
        if (modelInstance != null) {
            action(modelInstance!!)
        } else {
            onModelLoaded += object : (ModelInstance) -> Unit {
                override fun invoke(modelInstance: ModelInstance) {
                    onModelLoaded -= this
                    action(modelInstance)
                }
            }
        }
    }

    /**
     * Loads a monolithic binary glTF and add it to the Node
     *
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
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null
    ): ModelInstance? {
        return try {
            GLBLoader.loadModel(context, glbFileLocation)?.also { model ->
                loadedModel = model
                withContext(Dispatchers.Main) {
                    setModelInstance(model.instance, autoAnimate, scaleToUnits, centerOrigin)
                    onModelLoaded(model.instance)
                }
            }?.instance
        } catch (error: Exception) {
            onModelError(error)
            onError?.invoke(error)
            null
        }
    }

    /**
     * Loads a monolithic binary glTF and add it to the Node
     *
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
        glbFileLocation: String,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((modelInstance: ModelInstance) -> Unit)? = null
    ): ModelNode {
        doOnAttachedToScene { sceneView ->
            sceneView.coroutineScope?.launchWhenCreated {
                loadModelGlb(
                    sceneView.context,
                    glbFileLocation,
                    autoAnimate,
                    scaleToUnits,
                    centerOrigin,
                    onError
                )?.also {
                    onLoaded?.invoke(it)
                }
            }
        }
        return this
    }

    /**
     * Loads a monolithic binary glTF and add it to the Node
     *
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
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null
    ): Model? {
        return try {
            GLTFLoader.loadModel(context, gltfFileLocation, resourceLocationResolver)
                ?.also { model ->
                    loadedModel = model
                    withContext(Dispatchers.Main) {
                        setModelInstance(model.instance, autoAnimate, scaleToUnits, centerOrigin)
                        onModelLoaded(model.instance)
                    }
                }
        } catch (error: Exception) {
            onModelError(error)
            onError?.invoke(error)
            null
        }
    }

    /**
     * Loads a monolithic binary glTF and add it to the Node
     *
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
        doOnAttachedToScene { sceneView ->
            sceneView.coroutineScope?.launchWhenCreated {
                loadModelGltf(
                    context,
                    gltfFileLocation,
                    resourceLocationResolver,
                    autoAnimate,
                    scaleToUnits,
                    centerOrigin,
                    onError
                )?.also {
                    onLoaded?.invoke(it)
                }
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
    open fun setModelInstance(
        modelInstance: ModelInstance?,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
    ) {
        this.modelInstance = modelInstance
        if (modelInstance != null) {
            if (autoAnimate && modelInstance.animator.animationCount > 0) {
                playAnimation(0)
            }
            scaleToUnits?.let { scaleModel(it) }
            centerOrigin?.let { centerModel(it) }
        }
    }

    /**
     * ### Sets up a root transform on the current model to make it fit into a unit cube
     *
     * @param units the number of units of the cube to scale into.
     */
    fun scaleModel(units: Float = 1.0f) {
        doOnModelLoaded { modelInstance ->
            val halfExtent = modelInstance.model.boundingBox.halfExtent.let { v ->
                Float3(v[0], v[1], v[2])
            }
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
        doOnModelLoaded { modelInstance ->
            val center =
                modelInstance.model.boundingBox.center.let { v -> Float3(v[0], v[1], v[2]) }
            val halfExtent =
                modelInstance.model.boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
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

    override fun getTransformationMatrix(): Matrix {
        return Matrix(modelWorldTransform.toColumnsFloatArray())
    }

    /** ### Detach and destroy the node */
    override fun destroy() {
        loadedModel?.destroy()
        super.destroy()
    }

    override fun clone() = copy(ModelNode(engine))

    open fun copy(toNode: ModelNode = ModelNode(engine)): ModelNode = toNode.apply {
        super.copy(toNode)
        setModelInstance(this@ModelNode.modelInstance)
    }
}

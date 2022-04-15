package io.github.sceneview.node

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.coroutineScope
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.ar.sceneform.utilities.ChangeId
import dev.romainguy.kotlin.math.*
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform
import io.github.sceneview.model.GLBLoader
import io.github.sceneview.utils.FrameTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias OnModelLoaded = (modelInstance: RenderableInstance) -> Unit

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a renderable for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
open class ModelNode : Node {

    companion object {
        val DEFAULT_MODEL_POSITION = Position(x = 0.0f, y = 0.0f, z = 0.0f)
        val DEFAULT_MODEL_QUATERNION = Quaternion()
        val DEFAULT_MODEL_ROTATION = DEFAULT_MODEL_QUATERNION.toEulerAngles()
        val DEFAULT_MODEL_SCALE = Scale(1.0f)
    }

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

    open var modelTransform: Transform
        get() = translation(modelPosition) * rotation(modelQuaternion) * scale(modelScale)
        set(value) {
            modelPosition = Position(value.position)
            modelQuaternion = rotation(value).toQuaternion()
            modelScale = Scale(value.scale)
        }

    override val worldTransform: Mat4
        get() = super.worldTransform * modelTransform

    /**
     * ### Loads a monolithic binary glTF and add it to the Node
     *
     * The glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     *
     * The load is done instantly if the node is already attached to the SceneView.
     * Else, it will be loaded when SceneView is attached because it needs a
     * [LifecycleCoroutineScope] and [Context] to load
     *
     * @see loadModel
     */
    var modelFileLocation: String? = null
        set(value) {
            if (field != value) {
                field = value
                if (value != null) {
                    doOnAttachedToScene { sceneView: SceneView ->
                        loadModelAsync(sceneView.context, sceneView.lifecycle, value)
                    }
                } else {
                    setModel(null)
                }
            }
        }

    // Rendering fields.
    private var renderableId: Int = ChangeId.EMPTY_ID

    /**
     * ### The [RenderableInstance] to display.
     *
     * If [collisionShape] is not set, then [Renderable.getCollisionShape] is used to detect
     * collisions for this [Node].
     *
     * The renderable is usually a 3D model.
     * If null, this node's current renderable will be removed.
     */
    var modelInstance: RenderableInstance? = null
        set(value) {
            if (field != value) {
                field?.renderer = null
                field?.destroy()
                field = value
                value?.renderer = if (shouldBeRendered) renderer else null
                onModelChanged()
            }
        }

    val model: Renderable?
        get() = modelInstance?.renderable

    override var isRendered: Boolean
        get() = super.isRendered
        set(value) {
            modelInstance?.renderer = if (value) renderer else null
            super.isRendered = value
        }

    var onModelLoaded = mutableListOf<OnModelLoaded>()
    var onModelError: ((exception: Exception) -> Unit)? = null

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
     * TODO : Doc
     */
    constructor(modelInstance: RenderableInstance) : this() {
        this.modelInstance = modelInstance
    }

    override fun onFrame(frameTime: FrameTime) {
        if (isRendered) {
            // TODO : Remove the renderable.id thing when Renderable is kotlined
            // Update state when the renderable has changed.
            model?.let { model ->
                if (model.id.checkChanged(renderableId)) {
                    onModelChanged()
                }
            }
        }
        super.onFrame(frameTime)
    }

    open fun onModelLoaded(modelInstance: RenderableInstance) {
        onModelLoaded.forEach { it(modelInstance) }
    }

    open fun onModelError(exception: Exception) {
        onModelError?.invoke(exception)
    }

    /**
     * ### The transformation of the [Node] has changed
     *
     * If node A's position is changed, then that will trigger [onTransformChanged] to be
     * called for all of it's descendants.
     */
    open fun onModelChanged() {
        // Refresh the collider to ensure it is using the correct collision shape now
        // that the renderable has changed.
        onTransformChanged()

        collisionShape = model?.collisionShape
        // TODO : Clean when Renderable is kotlined
        renderableId = model?.id?.get() ?: ChangeId.EMPTY_ID
    }

    fun doOnModelLoaded(action: (modelInstance: RenderableInstance) -> Unit) {
        if (modelInstance != null) {
            action(modelInstance!!)
        } else {
            onModelLoaded += object : OnModelLoaded {
                override fun invoke(modelInstance: RenderableInstance) {
                    onModelLoaded -= this
                    action(modelInstance)
                }
            }
        }
    }

    /**
     * ### Loads a monolithic binary glTF and add it to the Node
     *
     * @param glbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param autoScale Scale the model to fit a unit cube
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    suspend fun loadModel(
        context: Context,
        lifecycle: Lifecycle,
        glbFileLocation: String,
        autoAnimate: Boolean = true,
        autoScale: Boolean = false,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null
    ): RenderableInstance? {
        return try {
            val model = GLBLoader.loadModel(context, lifecycle, glbFileLocation)
            withContext(Dispatchers.Main) {
                setModel(model, autoAnimate, autoScale, centerOrigin)?.also {
                    onModelLoaded(it)
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
     * @param glbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param coroutineScope your Activity or Fragment coroutine scope if you want to preload the
     * 3D model before the node is attached to the [SceneView]
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param autoScale Scale the model to fit a unit cube
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    fun loadModelAsync(
        context: Context,
        lifecycle: Lifecycle? = null,
        glbFileLocation: String,
        autoAnimate: Boolean = true,
        autoScale: Boolean = false,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((instance: RenderableInstance) -> Unit)? = null
    ) {
        if (lifecycle != null) {
            lifecycle.coroutineScope.launchWhenCreated {
                loadModel(
                    context,
                    lifecycle,
                    glbFileLocation,
                    autoAnimate,
                    autoScale,
                    centerOrigin,
                    onError
                )?.let { onLoaded?.invoke(it) }
            }
        } else {
            doOnAttachedToScene { sceneView ->
                loadModelAsync(
                    context, sceneView.lifecycle, glbFileLocation, autoAnimate, autoScale,
                    centerOrigin, onError, onLoaded
                )
            }
        }
    }

    /**
     * ### Set the node model
     *
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param autoScale Scale the model to fit a unit cube
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    @JvmOverloads
    open fun setModel(
        renderable: Renderable?,
        autoAnimate: Boolean = true,
        autoScale: Boolean = false,
        centerOrigin: Position? = null,
    ): RenderableInstance? {
        modelInstance = renderable?.createInstance(this)
        modelInstance?.let { modelInstance ->
            if (autoAnimate && modelInstance.animationCount > 0) {
                modelInstance.animate(true)?.start()
            }
            if (autoScale) {
                scaleModel()
            }
            centerOrigin?.let { centerModel(it) }
        }
        return modelInstance
    }

    /**
     * ### Sets up a root transform on the current model to make it fit into a unit cube
     *
     * @param units the number of units of the cube to scale into.
     */
    fun scaleModel(units: Float = 1.0f) {
        doOnModelLoaded { modelInstance ->
            modelInstance.filamentAsset?.let { asset ->
                val halfExtent = asset.boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
                modelScale = Scale(units / max(halfExtent))
            }
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
            modelInstance.filamentAsset?.let { asset ->
                val center = asset.boundingBox.center.let { v -> Float3(v[0], v[1], v[2]) }
                val halfExtent = asset.boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
                modelPosition = -(center + halfExtent * origin) * modelScale
            }
        }
    }

    /** ### Detach and destroy the node */
    override fun destroy() {
        modelInstance?.destroy()
        super.destroy()
    }

    override fun clone() = copy(ModelNode())

    open fun copy(toNode: ModelNode = ModelNode()): ModelNode = toNode.apply {
        super.copy(toNode)
        setModel(this@ModelNode.model)
    }
}

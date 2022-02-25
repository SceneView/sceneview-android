package io.github.sceneview.node

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.ar.sceneform.FrameTime
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
    var glbFileLocation: String? = null
        set(value) {
            if (field != value) {
                field = value
                if (value != null) {
                    doOnAttachedToScene { sceneView: SceneView ->
                        loadModel(sceneView.context, value, sceneView.lifecycleScope)
                    }
                } else {
                    setRenderable(null)
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
    var renderableInstance: RenderableInstance? = null
        set(value) {
            if (field != value) {
                field?.renderer = null
                field?.destroy()
                field = value
                value?.renderer = if (shouldBeRendered) renderer else null
                onRenderableChanged()
            }
        }

    val renderable: Renderable?
        get() = renderableInstance?.renderable

    override var isRendered: Boolean
        get() = super.isRendered
        set(value) {
            renderableInstance?.renderer = if (value) renderer else null
            super.isRendered = value
        }

    var onModelLoaded: ((renderableInstance: RenderableInstance) -> Unit)? = null
    var onError: ((exception: Exception) -> Unit)? = null

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
     * - null = Keep the original model center point
     * - (0, -1, 0) = Center the model horizontally and vertically
     * - (0, -1, 0) = center horizontal | bottom aligned
     * - (-1, 1, 0) = left | top aligned
     * - ...
     *
     * @see loadModel
     */
    constructor(
        context: Context,
        glbFileLocation: String,
        coroutineScope: LifecycleCoroutineScope? = null,
        autoAnimate: Boolean = true,
        autoScale: Boolean = true,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onModelLoaded: ((instance: RenderableInstance) -> Unit)? = null
    ) : this() {
        loadModel(
            context = context,
            glbFileLocation = glbFileLocation,
            coroutineScope = coroutineScope,
            autoAnimate = autoAnimate,
            autoScale = autoScale,
            centerOrigin = centerOrigin,
            onError = onError,
            onLoaded = onModelLoaded
        )
    }

    /**
     * TODO : Doc
     */
    constructor(renderableInstance: RenderableInstance) : this() {
        this.renderableInstance = renderableInstance
    }

    override fun onFrame(frameTime: FrameTime) {
        if (isRendered) {
            // TODO : Remove the renderable.id thing when Renderable is kotlined
            // Update state when the renderable has changed.
            renderable?.let { renderable ->
                if (renderable.id.checkChanged(renderableId)) {
                    onRenderableChanged()
                }
            }
        }
        super.onFrame(frameTime)
    }

    open fun onModelLoaded(renderableInstance: RenderableInstance) {
        onModelLoaded?.invoke(renderableInstance)
    }

    open fun onError(exception: Exception) {
        onError?.invoke(exception)
    }

    /**
     * ### The transformation of the [Node] has changed
     *
     * If node A's position is changed, then that will trigger [onTransformChanged] to be
     * called for all of it's descendants.
     */
    open fun onRenderableChanged() {
        // Refresh the collider to ensure it is using the correct collision shape now
        // that the renderable has changed.
        onTransformChanged()

        collisionShape = renderable?.collisionShape
        // TODO : Clean when Renderable is kotlined
        renderableId = renderable?.id?.get() ?: ChangeId.EMPTY_ID
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
     * - null = Keep the original model center point
     * - (0, -1, 0) = Center the model horizontally and vertically
     * - (0, -1, 0) = center horizontal | bottom aligned
     * - (-1, 1, 0) = left | top aligned
     * - ...
     */
    fun loadModel(
        context: Context,
        glbFileLocation: String,
        coroutineScope: LifecycleCoroutineScope? = null,
        autoAnimate: Boolean = true,
        autoScale: Boolean = false,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((instance: RenderableInstance) -> Unit)? = null
    ) {
        if (coroutineScope != null) {
            coroutineScope.launchWhenCreated {
                try {
                    val instance =
                        setRenderable(GLBLoader.loadModel(context, glbFileLocation))?.apply {
                            if (autoAnimate && animationCount > 0) {
                                animate(true)?.start()
                            }
                            if (autoScale) {
                                scaleModel()
                            }
                            centerOrigin?.let { centerModel(it) }
                        }
                    onModelLoaded(instance!!)
                    onLoaded?.invoke(instance)
                } catch (error: Exception) {
                    onError(error)
                    onError?.invoke(error)
                }
            }
        } else {
            doOnAttachedToScene { scene ->
                loadModel(
                    context = context,
                    glbFileLocation = glbFileLocation,
                    coroutineScope = scene.lifecycleScope,
                    autoAnimate = autoAnimate,
                    autoScale = autoScale,
                    centerOrigin = centerOrigin,
                    onError = onError,
                    onLoaded = onLoaded
                )
            }
        }
    }

    open fun setRenderable(renderable: Renderable?): RenderableInstance? {
        renderableInstance = renderable?.createInstance(this)
        return renderableInstance
    }

    /**
     * ### Sets up a root transform on the current model to make it fit into a unit cube
     *
     * @param units the number of units of the cube to scale into.
     */
    fun scaleModel(units: Float = 1.0f) {
        renderableInstance?.filamentAsset?.let { asset ->
            val halfExtent = asset.boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
            modelScale = Scale(units / max(halfExtent))
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
    fun centerModel(origin: Position = Position(x = 0f, y = 0.0f, z = 0.0f)) {
        renderableInstance?.filamentAsset?.let { asset ->
            val center = asset.boundingBox.center.let { v -> Float3(v[0], v[1], v[2]) }
            val halfExtent = asset.boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
            modelPosition = -(center + halfExtent * origin) * modelScale
        }
    }

    /** ### Detach and destroy the node */
    override fun destroy() {
        super.destroy()
        renderableInstance?.destroy()
    }

    override fun clone() = copy(ModelNode())

    open fun copy(toNode: ModelNode = ModelNode()): ModelNode = toNode.apply {
        super.copy(toNode)
        setRenderable(this@ModelNode.renderable)
    }
}

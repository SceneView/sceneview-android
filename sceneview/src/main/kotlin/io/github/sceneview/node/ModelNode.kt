package io.github.sceneview.node

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.utilities.ChangeId
import io.github.sceneview.SceneView
import io.github.sceneview.model.GlbLoader

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a renderable for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
open class ModelNode() : Node() {

    /**
     * ### The node model origin (center)
     *
     * @see contentPosition
     */
    var modelPosition
        get() = contentPosition
        set(value) {
            contentPosition = value
        }

    /**
     * ### The node model orientation
     *
     * @see contentRotation
     */
    open var modelRotation
        get() = contentRotation
        set(value) {
            contentRotation = value
        }

    /**
     * ### The node model scale
     *
     * @see contentScale
     */
    open var modelScale
        get() = contentScale
        set(value) {
            contentScale = value
        }

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
     * TODO : Doc
     */
    constructor(renderableInstance: RenderableInstance) : this() {
        this.renderableInstance = renderableInstance
    }

    /**
     * ### Loads a monolithic binary glTF and add it to the Node
     *
     * @param modelLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param coroutineScope your Activity or Fragment coroutine scope if you want to preload the
     * 3D model before the node is attached to the [SceneView]
     * @param animate Plays the animations automatically if the model has one
     *
     * @see loadModel
     */
    constructor(
        context: Context,
        modelLocation: String,
        coroutineScope: LifecycleCoroutineScope? = null,
        animate: Boolean = true,
        onModelLoaded: ((instance: RenderableInstance) -> Unit)? = null,
        onError: ((error: Exception) -> Unit)? = null
    ) : this() {
        loadModel(context, modelLocation, coroutineScope, animate, onModelLoaded, onError)
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
     * @param animated Plays the animations automatically if the model has one
     */
    fun loadModel(
        context: Context,
        glbFileLocation: String,
        coroutineScope: LifecycleCoroutineScope? = null,
        animated: Boolean = true,
        onLoaded: ((instance: RenderableInstance) -> Unit)? = null,
        onError: ((error: Exception) -> Unit)? = null
    ) {
        this.glbFileLocation = glbFileLocation
        if (coroutineScope != null) {
            coroutineScope.launchWhenCreated {
                try {
                    val instance =
                        setRenderable(GlbLoader.loadModel(context, glbFileLocation))?.apply {
                            if (animated && animationCount > 0) {
                                animate(true)?.start()
                            }
                        }
                    onLoaded?.invoke(instance!!)
                    onModelLoaded(instance!!)
                } catch (error: Exception) {
                    onError?.invoke(error)
                    onError(error)
                }
            }
        } else {
            doOnAttachedToScene { scene ->
                loadModel(
                    context,
                    glbFileLocation,
                    scene.lifecycleScope,
                    animated,
                    onLoaded,
                    onError
                )
            }
        }
    }

    open fun setRenderable(renderable: Renderable?): RenderableInstance? {
        renderableInstance = renderable?.createInstance(this)
        return renderableInstance
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

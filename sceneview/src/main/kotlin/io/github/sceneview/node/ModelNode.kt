package io.github.sceneview.node

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.PickHitResult
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.utilities.ChangeId
import io.github.sceneview.model.GlbLoader
import io.github.sceneview.model.await

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a renderable for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
open class ModelNode(
    position: Vector3 = defaultPosition,
    rotationQuaternion: Quaternion = defaultRotation,
    scales: Vector3 = defaultScales,
    parent: NodeParent? = null
) : Node(position, rotationQuaternion, scales, parent) {

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
    var onViewLoaded: ((renderableInstance: RenderableInstance, view: View) -> Unit)? = null
    var onError: ((exception: Exception) -> Unit)? = null

    constructor(
        renderableInstance: RenderableInstance,
        parent: NodeParent? = null,
        position: Vector3 = defaultPosition,
        rotationQuaternion: Quaternion = defaultRotation,
        scales: Vector3 = defaultScales
    ) : this(position, rotationQuaternion, scales) {
        this.renderableInstance = renderableInstance
    }

    constructor(
        context: Context,
        modelGlbFileLocation: String,
        coroutineScope: LifecycleCoroutineScope? = null,
        onModelLoaded: ((instance: RenderableInstance) -> Unit)? = null,
        onError: ((error: Exception) -> Unit)? = null,
        parent: NodeParent? = null,
        position: Vector3 = defaultPosition,
        rotationQuaternion: Quaternion = defaultRotation,
        scales: Vector3 = defaultScales,
    ) : this(position, rotationQuaternion, scales, parent) {
        setModel(context, modelGlbFileLocation, coroutineScope, onModelLoaded, onError)
    }

    constructor(
        context: Context,
        viewLayoutResId: Int,
        coroutineScope: LifecycleCoroutineScope? = null,
        onViewLoaded: ((instance: RenderableInstance, view: View) -> Unit)? = null,
        onError: ((error: Exception) -> Unit)? = null,
        parent: NodeParent? = null,
        position: Vector3 = defaultPosition,
        rotationQuaternion: Quaternion = defaultRotation,
        scales: Vector3 = defaultScales
    ) : this(position, rotationQuaternion, scales, parent) {
        setView(context, viewLayoutResId, coroutineScope, onViewLoaded, onError)
    }

    constructor(node: ModelNode) : this(
        position = node.position,
        rotationQuaternion = node.rotationQuaternion,
        scales = node.scales
    ) {
        setRenderable(node.renderable)
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

    open fun onViewLoaded(renderableInstance: RenderableInstance, view: View) {
        onViewLoaded?.invoke(renderableInstance, view)
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

    fun setModel(
        context: Context,
        glbFileLocation: String,
        coroutineScope: LifecycleCoroutineScope? = null,
        onLoaded: ((instance: RenderableInstance) -> Unit)? = null,
        onError: ((error: Exception) -> Unit)? = null
    ) {
        if (coroutineScope != null) {
            coroutineScope.launchWhenCreated {
                try {
                    val instance = setRenderable(GlbLoader.loadModel(context, glbFileLocation))
                    onLoaded?.invoke(instance!!)
                    onModelLoaded(instance!!)
                } catch (error: Exception) {
                    onError?.invoke(error)
                    onError(error)
                }
            }
        } else {
            doOnAttachedToScene { scene ->
                setModel(context, glbFileLocation, scene.lifecycleScope, onLoaded, onError)
            }
        }
    }

    fun setView(
        context: Context,
        layoutResId: Int,
        coroutineScope: LifecycleCoroutineScope? = null,
        onLoaded: ((instance: RenderableInstance, view: View) -> Unit)? = null,
        onError: ((error: Exception) -> Unit)? = null
    ) {
        if (coroutineScope != null) {
            coroutineScope.launchWhenCreated {
                try {
                    val renderable = ViewRenderable.builder()
                        .setView(context, layoutResId)
                        .await()
                    val view = renderable.view
                    val instance = setRenderable(renderable)
                    onLoaded?.invoke(instance!!, view)
                    onViewLoaded(instance!!, view)
                } catch (e: java.lang.Exception) {
                    onError?.invoke(e)
                }
            }
        } else {
            doOnAttachedToScene { scene ->
                setView(context, layoutResId, scene.lifecycleScope, onLoaded, onError)
            }
        }
    }

    open fun setRenderable(renderable: Renderable?): RenderableInstance? =
        renderable?.createInstance(this)?.also {
            renderableInstance = it
        }

    /**
     * ### Calls onTouchEvent if the node is active
     *
     * Used by TouchEventSystem to dispatch touch events.
     *
     * @param pickHitResult Represents the node that was touched, and information about where it was
     * touched. On ACTION_DOWN events, [PickHitResult.getNode] will always be this node or
     * one of its children. On other events, the touch may have moved causing the
     * [PickHitResult.getNode] to change (or possibly be null).
     *
     * @param motionEvent   The motion event.
     * @return true if the event was handled, false otherwise.
     */
    override fun dispatchTouchEvent(
        pickHitResult: PickHitResult,
        motionEvent: MotionEvent
    ): Boolean {
        if (isRendered && renderable is ViewRenderable) {
            if ((renderable as ViewRenderable).dispatchTouchEventToView(this, motionEvent)) {
                return true
            }
        }
        return super.dispatchTouchEvent(pickHitResult, motionEvent)
    }

    /** ### Detach and destroy the node */
    override fun destroy() {
        super.destroy()
        renderableInstance?.destroy()
    }

    fun copy(toNode: ModelNode = ModelNode()): ModelNode = toNode.apply {
        super.copy(toNode)
        setRenderable(this@ModelNode.renderable)
    }
}

package io.github.sceneview.node

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.filament.Engine
import com.google.ar.sceneform.PickHitResult
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.utilities.ChangeId
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.model.await
import io.github.sceneview.utils.FrameTime

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a View for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
open class ViewNode(engine: Engine) : Node(engine) {

    companion object {
        val DEFAULT_POSITION = Position(x = 0.0f, y = 0.0f, z = -0.1f)
        val DEFAULT_QUATERNION = Quaternion()
        val DEFAULT_ROTATION = DEFAULT_QUATERNION.toEulerAngles()
        val DEFAULT_SCALE = Scale(1.0f)
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
                renderable?.detachView()
                field?.destroy()
                field = value
                sceneEntities = value?.let { intArrayOf(it.renderedEntity) } ?: intArrayOf()
                sceneView?.let { renderable?.attachView(it.viewAttachmentManager) }
                onRenderableChanged()
            }
        }

    val renderable: ViewRenderable?
        get() = renderableInstance?.renderable as? ViewRenderable

    var onViewLoaded: ((renderableInstance: RenderableInstance, view: View) -> Unit)? = null
    var onError: ((exception: Exception) -> Unit)? = null

    /**
     * TODO : Doc
     */
    constructor(engine: Engine, renderableInstance: RenderableInstance) : this(engine) {
        this.renderableInstance = renderableInstance
    }

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)

        if (isAttached) {
            renderableInstance?.prepareForDraw(sceneView)

            // TODO : Remove the renderable.id thing when Renderable is kotlined
            // Update state when the renderable has changed.
            renderable?.let { renderable ->
                if (renderable.id.checkChanged(renderableId)) {
                    onRenderableChanged()
                }
            }
            renderableInstance?.let { renderableInstance ->
                renderableInstance.setModelMatrix(
                    transformManager,
                    renderableInstance.worldModelMatrix.data
                )
            }
        }
    }

    open fun onViewLoaded(renderableInstance: RenderableInstance, view: View) {
        onViewLoaded?.invoke(renderableInstance, view)
    }

    override fun onAttachedToScene(sceneView: SceneView) {
        super.onAttachedToScene(sceneView)

        renderable?.attachView(sceneView.viewAttachmentManager)
    }

    override fun onDetachedFromScene(sceneView: SceneView) {
        super.onDetachedFromScene(sceneView)

        renderable?.detachView()
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
     * TODO : Doc
     *
     * @param coroutineScope your Activity or Fragment coroutine scope if you want to preload the
     * View before the node is attached to the [SceneView]
     */
    fun loadView(
        context: Context,
        lifecycle: Lifecycle? = null,
        layoutResId: Int,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((instance: RenderableInstance, view: View) -> Unit)? = null
    ) {
        if (lifecycle != null) {
            lifecycle.coroutineScope.launchWhenCreated {
                try {
                    val renderable = ViewRenderable.builder()
                        .setView(context, layoutResId)
                        .await()
                    val view = renderable.view
                    val instance = setRenderable(renderable)
                    onLoaded?.invoke(instance!!, view)
                    onViewLoaded(instance!!, view)
                } catch (error: java.lang.Exception) {
                    onError?.invoke(error)
                    onError(error)
                }
            }
        } else {
            doOnAttachedToScene { scene ->
                loadView(context, scene.lifecycle, layoutResId, onError, onLoaded)
            }
        }
    }

    // TODO: Add a method for setting the View from code and support setting the view sizer and alignment

    open fun setRenderable(renderable: ViewRenderable?): RenderableInstance? {
        renderableInstance = renderable?.createInstance(this)
        return renderableInstance
    }

    // TODO: Replace this method with the new system based on Filament picking
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
    /*override fun dispatchTouchEvent(
        pickHitResult: PickHitResult,
        motionEvent: MotionEvent
    ): Boolean {
        if (isRendered) {
            if (renderable?.dispatchTouchEventToView(this, motionEvent) == true) {
                return true
            }
        }
        return super.dispatchTouchEvent(pickHitResult, motionEvent)
    }*/

    /** ### Detach and destroy the node */
    override fun destroy() {
        renderableInstance?.destroy()

        super.destroy()
    }

    override fun clone() = copy(ViewNode(engine))

    fun copy(toNode: ViewNode = ViewNode(engine)): ViewNode = toNode.apply {
        super.copy(toNode)
        setRenderable(this@ViewNode.renderable)
    }
}
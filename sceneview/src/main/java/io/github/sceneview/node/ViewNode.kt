package io.github.sceneview.node

import android.content.Context
import android.view.View
import com.google.android.filament.Engine
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import com.google.ar.sceneform.rendering.ViewRenderable
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.collision.ChangeId
import io.github.sceneview.collision.Matrix
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale

/**
 * A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a View for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
open class ViewNode(
    engine: Engine,
    val modelLoader: ModelLoader,
    val viewAttachmentManager: ViewAttachmentManager,
) : Node(engine) {

    companion object {
        val DEFAULT_POSITION = Position(x = 0.0f, y = 0.0f, z = -0.1f)
        val DEFAULT_QUATERNION = Quaternion()
        val DEFAULT_ROTATION = DEFAULT_QUATERNION.toEulerAngles()
        val DEFAULT_SCALE = Scale(1.0f)
    }

    // Rendering fields.
    private var renderableId: Int = ChangeId.EMPTY_ID

    /**
     * The [RenderableInstance] to display.
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
//                field?.destroy()
                field = value
                renderable?.attachView(viewAttachmentManager)
                onRenderableChanged()
            }
        }

    override val sceneEntities
        get() = renderableInstance?.let { listOf(it.renderedEntity) } ?: listOf()

    val renderable: ViewRenderable?
        get() = renderableInstance?.renderable as? ViewRenderable

    var onViewLoaded: ((renderableInstance: RenderableInstance, view: View) -> Unit)? = null
    var onError: ((exception: Exception) -> Unit)? = null

    // Reuse this to limit frame instantiations
    private val _transformationMatrixInverted =
        Matrix()
    open val transformationMatrixInverted: Matrix
        get() = _transformationMatrixInverted.apply {
            Matrix.invert(transformationMatrix, this)
        }

    override fun onFrame(frameTimeNanos: Long) {
        super.onFrame(frameTimeNanos)

        renderableInstance?.prepareForDraw(engine)

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

    open fun onViewLoaded(renderableInstance: RenderableInstance, view: View) {
        onViewLoaded?.invoke(renderableInstance, view)
    }

    open fun onError(exception: Exception) {
        onError?.invoke(exception)
    }

    /**
     * The transformation of the [Node] has changed.
     *
     * If node A's position is changed, then that will trigger [onTransformChanged] to be called for
     * all of it's descendants.
     */
    open fun onRenderableChanged() {
        // Refresh the collider to ensure it is using the correct collision shape now
        // that the renderable has changed.
        onTransformChanged()

        collisionShape = renderable?.collisionShape
        // TODO : Clean when Renderable is kotlined
        renderableId = renderable?.id?.get() ?: ChangeId.EMPTY_ID

        updateVisibility()
    }

    fun loadView(
        context: Context,
        layoutResId: Int,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((instance: RenderableInstance, view: View) -> Unit)? = null
    ) {
        try {
            ViewRenderable.builder()
                .setView(context, layoutResId)
                .build(engine).thenAccept { renderable ->
                    val view = renderable.view
                    val instance = setRenderable(renderable)
                    onLoaded?.invoke(instance!!, view)
                    onViewLoaded(instance!!, view)
                }
        } catch (error: java.lang.Exception) {
            onError?.invoke(error)
            onError(error)
        }
    }

    // TODO: Add a method for setting the View from code and support setting the view sizer and alignment

    open fun setRenderable(renderable: ViewRenderable?): RenderableInstance? {
        renderable?.attachView(viewAttachmentManager)
        renderableInstance = renderable?.createInstance(
            engine,
            modelLoader.assetLoader,
            modelLoader.resourceLoader,
            this
        )
        return renderableInstance
    }

    override fun destroy() {
        renderable?.detachView()
        renderableInstance?.destroy()

        super.destroy()
    }
}
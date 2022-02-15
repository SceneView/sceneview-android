package io.github.sceneview.ar

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.lifecycle.lifecycleScope
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.model.await

/**
 * A SurfaceView that integrates with ARCore and renders a scene.
 */
// TODO: Use it for more actual ArNode and use view layout parameters for placement and glb file
open class ArModelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ArSceneView(
    context,
    attrs,
    defStyleAttr,
    defStyleRes
) {

    val modelNode = ArModelNode()

    /**
     * TODO: Doc
     *
     * @see io.github.sceneview.ar.node.ArModelNode.glbFileLocation
     */
    var modelLocation: String?
        get() = modelNode.glbFileLocation
        set(value) {
            modelNode.glbFileLocation = value
        }

    /**
     * TODO: Doc
     *
     * @see io.github.sceneview.ar.node.ArModelNode.placementMode
     */
    var placementMode: PlacementMode
        get() = modelNode.placementMode
        set(value) {
            modelNode.placementMode = value
        }

    /**
     * Loads a monolithic binary glTF and add it to the fragment when the user tap on a detected
     * plane surface.
     *
     * Plays the animations automatically if the model has one.
     *
     * @param glbFileLocation Glb file source location can be come from the asset folder ("model.glb")
     * or an http source ("http://domain.com/model.glb")
     * @param onModelLoaded Called when an ARCore plane is tapped and the model is added to the
     * scene. The callback will only be invoked if no [com.google.ar.sceneform.Node] was tapped.
     * The [RenderableInstance] param is the added instance of the model.
     * @param onModelError An error occurred while loading the model from the source file location.
     * @see setOnTapArPlaneListener
     */
    fun setOnTapArPlaneGlbModel(
        glbFileLocation: String,
        onLoaded: ((renderable: Renderable) -> Unit)? = null,
        onAdded: ((node: ArNode, renderableInstance: RenderableInstance) -> Unit)? = null,
        onError: ((exception: Throwable) -> Unit)? = null,
    ) {
        lifecycleScope.launchWhenCreated {
            try {
                val model = ModelRenderable.builder()
                    .setSource(context, Uri.parse(glbFileLocation))
                    .setIsFilamentGltf(true)
                    .await()
                onLoaded?.invoke(model!!)
                onTouchAr = { hitResult, _ ->
                    addChild(ArModelNode(hitResult).apply {
                        // Create the transformable model and add it to the anchor
                        val modelNode = TransformableNode(nodeGestureRecognizer)
                        val renderableInstance = setRenderable(model)!!.apply {
                            animate(true).start()
                        }
                        addChild(modelNode)
                        onAdded?.invoke(this, renderableInstance)
                    })
                }
            } catch (exception: Exception) {
                onError?.invoke(exception)
            }
        }
    }
}
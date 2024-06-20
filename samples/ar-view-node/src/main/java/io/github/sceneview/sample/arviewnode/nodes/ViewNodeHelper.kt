package io.github.sceneview.sample.arviewnode.nodes

import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.ar.core.Anchor
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import com.google.ar.sceneform.rendering.ViewRenderable
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ViewNode
import io.github.sceneview.sample.arviewnode.R
import io.github.sceneview.sample.arviewnode.nodes.events.ViewNodeEvent
import java.util.Locale
import java.util.concurrent.CompletableFuture

/**
 * ## Overview
 * * The ViewNodeHelper class is designed to facilitate the addition of view nodes anchored
 * in an AR scene. It creates and manages view nodes with customizable layouts,
 * ensuring they are anchored at specific points within the scene. This class integrates
 * with ARCore and uses Android's view system to display custom views in the AR environment.
 *
 * ## Constructor
 * * context (Context): The Android context in which the AR scene operates.
 * * sceneView (ARSceneView): The AR scene view that contains the nodes and camera.
 * * onViewNodeEvent (Function): A callback function that handles events related to view nodes.
 *
 * ## Example Usage
 * ```
 * // Define a callback to handle view node events
 * val viewNodeEventCallback: (ViewNodeEvent) -> Unit = { event ->
 *     when (event) {
 *         is ViewNodeEvent.NewViewNode -> {
 *             // Handle the new view node event
 *         }
 *     }
 * }
 *
 * // Create an instance of ViewNodeHelper
 * val viewNodeHelper = ViewNodeHelper(context, sceneView, viewNodeEventCallback)
 *
 * // Add a view node to the AR scene at a specific anchor
 * viewNodeHelper.addViewNode(anchor)
 * ```
 *
 * ## Notes
 * * Ensure that the AR environment and dependencies (like ARCore) are properly set
 * up for this class to function as intended.
 * * The layoutRes property can be customized to use different layouts for the view nodes.
 * * The onViewNodeEvent callback allows for flexible handling of events related to view nodes,
 * such as adding additional behavior or logging.
 */
class ViewNodeHelper(
    private val context: Context,
    private val sceneView: ARSceneView,
    val onViewNodeEvent: (ViewNodeEvent) -> Unit
) {
    /**
     * If an anchored ViewNode should show something else, then change
     * here the Layout-Resource. Also take a look at the function
     * [createView] within this class.
     */
    private val layoutRes = R.layout.view_node_text_view_annotation

    /**
     * A counter to keep track of the number of view nodes added.
     * This is used to differentiate between nodes, such as by updating a
     * text view within the layout.
     */
    private var annotationCounter: Int = 0

    /**
     * ## Overview
     * * Adds a view node to the AR scene at the specified anchor.
     * The view node is created from the specified layout resource and is rendered in the AR scene.
     *
     * ## Implementation Details:
     * * Annotation Counter Increment:
     *    * Increments the annotationCounter to keep track of the number of view nodes added.
     * * View Creation:
     *    * Calls createView() to inflate the view from the specified layout resource.
     * * ViewRenderable Creation:
     *    * Uses ViewRenderable.builder() to create a ViewRenderable from the inflated view.
     *    * Builds the ViewRenderable with the AR scene's engine.
     * * Handling Renderable:
     *    * When the ViewRenderable is successfully created, initializes a ViewAttachmentManager and a ViewNode.
     *    * Configures the ViewNode properties (renderable, scale, visibility, etc.).
     *    * Creates an AnchorNode using the provided anchor and adds the ViewNode as a child node.
     * * Event Callback:
     *    * Triggers the onViewNodeEvent callback with a NewViewNode event, passing the created AnchorNode.
     *
     * @param anchor (Anchor): The ARCore anchor where the view node will be placed.
     */
    fun addViewNode(anchor: Anchor) {
        annotationCounter++

        val view: View = createView()

        val viewRenderable = ViewRenderable
            .builder()
            .setView(context, view)
            .build(sceneView.engine)

        viewRenderable.thenAccept {
            val viewAttachmentManager = ViewAttachmentManager(
                context,
                sceneView
            )
            viewAttachmentManager.onResume()

            val viewNode = ViewNode(
                sceneView.engine,
                sceneView.modelLoader,
                viewAttachmentManager
            ).apply {
                setRenderable(it)
                scale = Scale(-1f, 1f, 1f)
                isEditable = true
                isVisible = true
                isRotationEditable = true
            }

            val anchorNode = AnchorNode(
                sceneView.engine,
                anchor
            ).apply {
                addChildNode(viewNode)
            }

            onViewNodeEvent(ViewNodeEvent.NewViewNode(anchorNode))
        }
    }

    /**
     * ## Overview
     * * Inflates the view from the specified layout resource and updates its content.
     *
     * ## Implementation Details:
     * * View Inflation:
     *    * Inflates the view from layoutRes using View.inflate().
     * * View Configuration:
     *    * Finds a TextView within the inflated view and updates its text with the
     *    annotationCounter value, formatted according to the locale.
     *
     * @return View: The inflated and configured view.
     */
    private fun createView(): View {
        val view: View = View.inflate(
            context,
            layoutRes,
            null
        )
        view.findViewById<TextView>(R.id.textView).text = String.format(
            Locale.getDefault(),
            context.getString(R.string.ar_tv_annotation),
            annotationCounter.toString()
        )

        return view
    }
}
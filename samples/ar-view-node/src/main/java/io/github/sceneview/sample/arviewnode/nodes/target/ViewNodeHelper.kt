package io.github.sceneview.sample.arviewnode.nodes.target

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.ar.core.Anchor
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import com.google.ar.sceneform.rendering.ViewRenderable
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ViewNode
import io.github.sceneview.sample.arviewnode.R
import io.github.sceneview.sample.arviewnode.nodes.events.ViewNodeEvent
import java.util.Locale

class ViewNodeHelper(
    private val context: Context,
    private val sceneView: ARSceneView,
    val onEvent: (ViewNodeEvent) -> Unit
) {
    /**
     * If an anchored ViewNode should show something else, then change
     * here the Layout-Resource. Also take a look at the function
     * [createView] within this class.
     */
    private val layoutRes = R.layout.view_node_text_view_annotation

    private var annotationCounter: Int = 0

    fun addViewNode(anchor: Anchor) {
        annotationCounter++

        val view: View = createView()

        val viewRenderable = ViewRenderable
            .builder()
            .setView(context, view)
            .build(sceneView.engine)

        viewRenderable!!.thenAccept {
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
            }

            viewNode.isEditable = true
            viewNode.isVisible = true
            viewNode.isRotationEditable = true

            val anchorNode = AnchorNode(sceneView.engine, anchor)
            anchorNode.isEditable = true
            anchorNode.isRotationEditable = true
            anchorNode.addChildNode(viewNode)

            onEvent(ViewNodeEvent.NewViewNode(anchorNode))
        }
    }

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
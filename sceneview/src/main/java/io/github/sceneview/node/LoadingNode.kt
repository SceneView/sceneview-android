package io.github.sceneview.node

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.R
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale

/**
 * ### Construct a [LoadingNode] with it Position, Rotation and Scale
 *
 * @param position See [Node.position]
 * @param rotation See [Node.rotation]
 * @param scale See [Node.scale]
 */
open class LoadingNode(
    context: Context,
    lifecycle: Lifecycle,
    position: Position = DEFAULT_POSITION,
    rotation: Rotation = DEFAULT_ROTATION,
    scale: Scale = DEFAULT_SCALE
) : ViewNode(position, rotation, scale) {

    var progressIndicator: CircularProgressIndicator? = null
    var textView: TextView? = null

    init {
        isFocusable = false
        loadView(context, lifecycle, R.layout.sceneview_loading_node)
    }

    override fun onViewLoaded(renderableInstance: RenderableInstance, view: View) {
        super.onViewLoaded(renderableInstance, view)

        progressIndicator = view.findViewById(R.id.progressIndicator)
        textView = view.findViewById(R.id.textView)
        renderableInstance.apply {
            isShadowCaster = false
            isShadowReceiver = false
            renderPriority = 0
        }
    }

    var text
        get() = textView?.text
        set(value) {
            textView?.text = value
        }
}
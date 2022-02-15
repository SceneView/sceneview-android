package io.github.sceneview.node

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.R
import io.github.sceneview.utils.Position

open class LoadingNode(
    context: Context,
    coroutineScope: LifecycleCoroutineScope
) : ViewNode() {

    var progressIndicator: CircularProgressIndicator? = null
    var textView: TextView? = null

    init {
        isFocusable = false
        position = Position(x = 0.0f, y = -2.0f, z = 0.0f)
        loadView(context, R.layout.sceneview_loading_node, coroutineScope)
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
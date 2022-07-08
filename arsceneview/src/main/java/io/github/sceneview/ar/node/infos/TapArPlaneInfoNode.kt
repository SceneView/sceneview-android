package io.github.sceneview.ar.node.infos

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.R
import io.github.sceneview.math.Position
import io.github.sceneview.node.ViewNode

open class TapArPlaneInfoNode(context: Context, lifecycle: Lifecycle) :
    ViewNode() {

    var textView: TextView? = null

    init {
        isSelectable = false
        position = Position(x = 0.0f, y = -0.75f, z = -2.0f)
        loadView(context, lifecycle, R.layout.sceneview_view_info_tap_ar_plane)
    }

    override fun onViewLoaded(renderableInstance: RenderableInstance, view: View) {
        super.onViewLoaded(renderableInstance, view)

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
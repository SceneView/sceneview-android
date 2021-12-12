package io.github.sceneview.ar.node.infos

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.R
import io.github.sceneview.node.ViewNode

open class TapArPlaneInfoNode(context: Context, coroutineScope: LifecycleCoroutineScope) :
    ViewNode(
        context = context,
        coroutineScope = coroutineScope,
        viewLayoutResId = R.layout.sceneview_view_info_tap_ar_plane,
        position = Vector3(0.0f, -1.0f, -2.0f),
    ) {

    var textView: TextView? = null

    init {
        isSelectable = false
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
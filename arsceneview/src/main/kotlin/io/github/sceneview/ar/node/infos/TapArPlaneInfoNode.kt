package io.github.sceneview.ar.node.infos

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.R
import io.github.sceneview.node.ModelNode

open class TapArPlaneInfoNode(context: Context, coroutineScope: LifecycleCoroutineScope) :
    ModelNode(
        position = Vector3(0.0f, -1.0f, -2.0f),
    ) {

    var textView: TextView? = null

    init {
        isSelectable = false

        setView(
            context = context,
            coroutineScope = coroutineScope,
            layoutResId = R.layout.sceneview_view_info_tap_ar_plane,
            onLoaded = { renderableInstance: RenderableInstance, view: View ->
                textView = view.findViewById(R.id.textView)
                renderableInstance.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                    renderPriority = 0
                }
            })
    }

    var text
        get() = textView?.text
        set(value) {
            textView?.text = value
        }
}
package io.github.sceneview.ar.node.infos

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.R
import io.github.sceneview.node.ViewNode
import io.github.sceneview.utils.Position

open class AugmentedImageInfoNode(context: Context, coroutineScope: LifecycleCoroutineScope) :
    ViewNode() {

    init {
        isFocusable = false
        position = Position(x = 0.5f, y = 0.5f, z = 0f)
        loadView(context, R.layout.sceneview_view_info_augmented_image, coroutineScope)
    }

    override fun onViewLoaded(renderableInstance: RenderableInstance, view: View) {
        super.onViewLoaded(renderableInstance, view)

        renderableInstance.apply {
            isShadowCaster = false
            isShadowReceiver = false
            renderPriority = 0
        }
    }
}

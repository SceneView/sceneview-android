package io.github.sceneview.ar.node.infos

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.ar.sceneform.rendering.ViewRenderable
import io.github.sceneview.ar.R
import io.github.sceneview.node.ViewNode
import io.github.sceneview.math.Position

open class AugmentedImageInfoNode(context: Context, coroutineScope: LifecycleCoroutineScope) :
    ViewNode() {

    init {
        isFocusable = false
        position = Position(x = 0.0f, y = 0.0f, z = -2.0f)
        loadView(context, R.layout.sceneview_view_info_augmented_image, coroutineScope) { _, _->
            renderable?.horizontalAlignment = ViewRenderable.HorizontalAlignment.CENTER
            renderable?.verticalAlignment = ViewRenderable.VerticalAlignment.CENTER
        }
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

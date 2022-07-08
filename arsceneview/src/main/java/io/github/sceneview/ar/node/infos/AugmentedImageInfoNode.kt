package io.github.sceneview.ar.node.infos

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.ar.sceneform.rendering.ViewRenderable
import io.github.sceneview.ar.R
import io.github.sceneview.math.Position
import io.github.sceneview.node.ViewNode

open class AugmentedImageInfoNode(context: Context, lifecycle: Lifecycle) :
    ViewNode() {

    init {
        isSelectable = false
        position = Position(x = 0.0f, y = 0.0f, z = -2.0f)
        loadView(context, lifecycle, R.layout.sceneview_view_info_augmented_image) { _, _ ->
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

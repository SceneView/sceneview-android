package io.github.sceneview.ar.node.infos

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.R
import io.github.sceneview.node.ViewNode

open class AugmentedImageInfoNode(context: Context, coroutineScope: LifecycleCoroutineScope) :
    ViewNode(
        context = context,
        coroutineScope = coroutineScope,
        position = Vector3(0.5f, 0.5f, 0f),
        viewLayoutResId = R.layout.sceneview_view_info_augmented_image,
    ) {

    init {
        isFocusable = false
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

package io.github.sceneview.ar.node.infos

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.sceneform.math.Vector3
import io.github.sceneview.ar.R
import io.github.sceneview.node.ModelNode

open class AugmentedImageInfoNode(context: Context, coroutineScope: LifecycleCoroutineScope) :
    ModelNode(
        context = context,
        coroutineScope = coroutineScope,
        position = Vector3(0.5f, 0.5f, 0f),
        viewLayoutResId = R.layout.sceneview_view_info_augmented_image,
        onViewLoaded = { renderableInstance, _ ->
            renderableInstance.apply {
                isShadowCaster = false
                isShadowReceiver = false
                renderPriority = 0
            }
        }
    ) {

    init {
        isSelectable = false
    }
}

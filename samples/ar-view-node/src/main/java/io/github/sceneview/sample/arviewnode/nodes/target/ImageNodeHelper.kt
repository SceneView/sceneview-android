package io.github.sceneview.sample.arviewnode.nodes.target

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.ar.core.Anchor
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ImageNode
import io.github.sceneview.sample.arviewnode.nodes.events.ImageNodeEvent

class ImageNodeHelper(
    private val context: Context,
    private val sceneView: ARSceneView,
    val onEvent: (ImageNodeEvent) -> Unit
) {
    /**
     * If an Anchored ImageNode should show something else, then
     * change the imageUrl.
     */
    private val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/0/01/Cat-1044750.jpg"

    fun addImageNode(anchor: Anchor) {
        Glide
            .with(context)
            .asBitmap()
            .load(imageUrl)
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .apply(RequestOptions.timeoutOf(5 * 60 * 1000))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val imageNode = ImageNode(
                        materialLoader = sceneView.materialLoader,
                        bitmap = resource
                    )

                    val anchorNode = AnchorNode(sceneView.engine, anchor)
                    anchorNode.addChildNode(imageNode)

                    onEvent(ImageNodeEvent.NewImageNode(anchorNode))
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }
}
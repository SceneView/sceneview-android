package io.github.sceneview.sample.arviewnode.nodes

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

/**
 * ## Overview
 * * The ImageNodeHelper class is designed to facilitate the addition of image nodes anchored
 * in an AR scene. It utilizes Glide for image loading and integrates with ARCore to place
 * the images at specific anchors within the scene.
 *
 * ## Constructor
 * * context (Context): The Android context in which the AR scene operates.
 * * sceneView (ARSceneView): The AR scene view that contains the nodes and camera.
 * * onImageNodeEvent (Function): A callback function that handles events related to image nodes.
 *
 * ## Example Usage
 * ```
 * // Define a callback to handle image node events
 * val imageNodeEventCallback: (ImageNodeEvent) -> Unit = { event ->
 *     when (event) {
 *         is ImageNodeEvent.NewImageNode -> {
 *             // Handle the new image node event
 *         }
 *     }
 * }
 *
 * // Create an instance of ImageNodeHelper
 * val imageNodeHelper = ImageNodeHelper(context, sceneView, imageNodeEventCallback)
 *
 * // Add an image node to the AR scene at a specific anchor
 * imageNodeHelper.addImageNode(anchor)
 * ```
 *
 * ## Notes
 * * Ensure that the AR environment and dependencies (like ARCore and Glide) are properly set up for this class to function as intended.
 * * The imageUrl can be customized to load different images as needed.
 * * The onImageNodeEvent callback allows for flexible handling of events related to image nodes, such as adding additional behavior or logging.
 */
class ImageNodeHelper(
    private val context: Context,
    private val sceneView: ARSceneView,
    val onImageNodeEvent: (ImageNodeEvent) -> Unit
) {
    /**
     * If an Anchored ImageNode should show something else, then
     * change the imageUrl.
     */
    private val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/0/01/Cat-1044750.jpg"

    /**
     * Glide request options for loading the image. These options include:
     * * A timeout of 5 Seconds.
     * * Image dimensions override (351x233 pixels).
     * * JPEG encoding quality set to 70.
     */
    private val requestOptions = RequestOptions()
        .timeout(5 * 60)
        .override(351, 233)
        .encodeQuality(70)

    /**
     * ## Overview
     * * Adds an image node to the AR scene at the specified anchor.
     * The image is loaded from the imageUrl and displayed as a Bitmap on the node.
     *
     * ## Implementation Details:
     * * Image Loading with Glide:
     *    * Uses Glide to load the image from the specified imageUrl.
     *    * Applies requestOptions for customizing the loading behavior.
     * * Handling Loaded Image:
     *    * When the image is successfully loaded, a new ImageNode is created with the loaded Bitmap.
     *    * An AnchorNode is created using the provided anchor and the AR scene's engine.
     *    * The ImageNode is added as a child node to the AnchorNode.
     * * Event Callback:
     *    * Triggers the onImageNodeEvent callback with a NewImageNode event, passing the created AnchorNode.
     *
     * @param anchor (Anchor): The ARCore anchor where the image node will be placed.
     */
    fun addImageNode(anchor: Anchor) {
        Glide
            .with(context)
            .asBitmap()
            .load(imageUrl)
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .apply(requestOptions)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val imageNode = ImageNode(
                        materialLoader = sceneView.materialLoader,
                        bitmap = resource
                    )

                    val anchorNode = AnchorNode(sceneView.engine, anchor)
                    anchorNode.addChildNode(imageNode)

                    onImageNodeEvent(ImageNodeEvent.NewImageNode(anchorNode))
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }
}
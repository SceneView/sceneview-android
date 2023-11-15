package io.github.sceneview.sample.araugmentedimage

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.addAugmentedImage
import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
import io.github.sceneview.ar.node.AugmentedImageNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ARSceneView

    val augmentedImageNodes = mutableListOf<AugmentedImageNode>()

    // TODO: Restore when
//    var qrCodeNode: VideoNode? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById<ARSceneView>(R.id.sceneView).apply {
            configureSession { session, config ->
                config.addAugmentedImage(
                    session, "rabbit",
                    requireContext().assets.open("augmentedimages/rabbit.jpg")
                        .use(BitmapFactory::decodeStream)
                )
                config.addAugmentedImage(
                    session, "qrcode",
                    requireContext().assets.open("augmentedimages/qrcode.png")
                        .use(BitmapFactory::decodeStream)
                )
            }
            onSessionUpdated = { session, frame ->
                frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
                    if (augmentedImageNodes.none { it.imageName == augmentedImage.name }) {
                        val augmentedImageNode = AugmentedImageNode(engine, augmentedImage).apply {
                            when (augmentedImage.name) {
                                "rabbit" -> addChildNode(
                                    ModelNode(
                                        modelInstance = modelLoader.createModelInstance(
                                            assetFileLocation = "models/rabbit.glb"
                                        ),
                                        scaleToUnits = 0.1f,
                                        centerOrigin = Position(0.0f)
                                    )
                                )

                                "qrcode" -> {}
                                // TODO: Wait for VideoNode to come back
//                                    addChildNode(VideoNode(
//                                    materialLoader = materialLoader,
//                                    player = MediaPlayer().apply {
//                                        setDataSource(
//                                            requireContext(),
//                                            Uri.parse("https://sceneview.github.io/assets/videos/ads/ar_camera_app_ad.mp4")
//                                        )
//                                        isLooping = true
//                                        setOnPreparedListener {
//                                            if (augmentedImage.isTracking) {
//                                                start()
//                                            }
//                                        }
//                                        prepareAsync()
//                                    }
//                                ).also { qrCodeNode ->
//                                    onTrackingStateChanged = { trackingState ->
//                                        when (trackingState) {
//                                            TrackingState.TRACKING -> {
//                                                if (!qrCodeNode.player.isPlaying) {
//                                                    qrCodeNode.player.start()
//                                                }
//                                            }
//
//                                            else -> {
//                                                if (qrCodeNode.player.isPlaying) {
//                                                    qrCodeNode.player.pause()
//                                                }
//                                            }
//                                        }
//                                    }
//                                })
                            }
                        }
                        addChildNode(augmentedImageNode)
                        augmentedImageNodes += augmentedImageNode
                    }
                }
            }
        }
    }
}
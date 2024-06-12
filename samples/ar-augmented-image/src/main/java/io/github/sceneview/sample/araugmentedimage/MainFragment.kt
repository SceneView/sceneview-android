package io.github.sceneview.sample.araugmentedimage

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.addAugmentedImage
import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
import io.github.sceneview.ar.node.AugmentedImageNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.sample.araugmentedimage.video.ExoPlayerNode

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ARSceneView

    val augmentedImageNodes = mutableListOf<AugmentedImageNode>()

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

                                "qrcode" -> {
                                    addChildNode(
                                        ExoPlayerNode(
                                            engine = engine,
                                            materialLoader = materialLoader,
//                                            size = Size(x = augmentedImage.extentX, y = augmentedImage.extentZ), // When the width of the image is set
                                            exoPlayer = ExoPlayer.Builder(requireContext()).build()
                                                .apply {
                                                    setMediaItem(MediaItem.fromUri("https://sceneview.github.io/assets/videos/ads/ar_camera_app_ad.mp4"))
                                                    prepare()
                                                    playWhenReady = true
                                                    repeatMode = Player.REPEAT_MODE_ALL
                                                },
//                                            chromaKeyColor = if (chromaKey) 0x2fff19 else null, // 0x2fff19 is colorOf(0.1843f, 1.0f, 0.098f)
                                        )
                                    )
                                }
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
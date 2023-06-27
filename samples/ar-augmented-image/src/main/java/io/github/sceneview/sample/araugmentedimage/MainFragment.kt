package io.github.sceneview.sample.araugmentedimage

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.AugmentedImageNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.VideoNode

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ArSceneView
    lateinit var videoNode: VideoNode

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById(R.id.sceneView)

        sceneView.addChild(
            AugmentedImageNode(
                sceneView.engine,
                imageName = "rabbit",
                bitmap = requireContext().assets.open("augmentedimages/rabbit.png")
                    .use(BitmapFactory::decodeStream)
            ).apply {
                loadModelGlbAsync(
                    glbFileLocation = "https://sceneview.github.io/assets/models/Spoons.glb"
                )
            }
        )

        sceneView.addChild(
            AugmentedImageNode(
                sceneView.engine,
                imageName = "qrcode",
                bitmap = requireContext().assets.open("augmentedimages/qrcode.png")
                    .use(BitmapFactory::decodeStream)
            ).apply {
                loadModelGlbAsync(
                    glbFileLocation = "https://sceneview.github.io/assets/models/Hair.glb",
                    // Place the model origin at the bottom center
                    centerOrigin = Position(y = -1.0f)
                )
            }
        )

        sceneView.addChild(AugmentedImageNode(
            engine = sceneView.engine,
            imageName = "video",
            bitmap = requireContext().assets.open("augmentedimages/video.jpg")
                .use(BitmapFactory::decodeStream),
            onUpdate = { node, _ ->
                if (node.isTracking) {
                    if (!videoNode.player.isPlaying) {
                        videoNode.player.start()
                    }
                } else {
                    if (videoNode.player.isPlaying) {
                        videoNode.player.pause()
                    }
                }
            }
        ).apply {
            videoNode = VideoNode(sceneView.engine, MediaPlayer().apply {
                setDataSource(
                    requireContext(),
                    Uri.parse("https://sceneview.github.io/assets/videos/ads/ar_camera_app_ad.mp4")
                )
                isLooping = true
                setOnPreparedListener {
                    if ((videoNode.parent as? AugmentedImageNode)?.isTracking == true) {
                        start()
                    }
                }
                prepareAsync()
            })
            addChild(videoNode)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        videoNode.player.stop()
    }
}
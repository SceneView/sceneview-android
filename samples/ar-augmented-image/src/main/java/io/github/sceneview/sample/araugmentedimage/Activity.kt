package io.github.sceneview.sample.araugmentedimage

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.ar.core.AugmentedImage
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.addAugmentedImage
import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
import io.github.sceneview.math.Position
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.sample.SceneviewTheme
import io.github.sceneview.sample.araugmentedimage.video.ExoPlayerNode

class Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val materialLoader = rememberMaterialLoader(engine)

                    val rabbitModelInstance = rememberModelInstance(modelLoader, "models/rabbit.glb")

                    var augmentedImages by remember { mutableStateOf<Map<String, AugmentedImage>>(emptyMap()) }

                    ARScene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        materialLoader = materialLoader,
                        sessionConfiguration = { session, config ->
                            config.addAugmentedImage(
                                session, "rabbit",
                                assets.open("augmentedimages/rabbit.jpg").use(BitmapFactory::decodeStream)
                            )
                            config.addAugmentedImage(
                                session, "qrcode",
                                assets.open("augmentedimages/qrcode.png").use(BitmapFactory::decodeStream)
                            )
                        },
                        onSessionUpdated = { _, frame ->
                            frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
                                augmentedImages = augmentedImages.toMutableMap().apply {
                                    this[augmentedImage.name] = augmentedImage
                                }
                            }
                        }
                    ) {
                        for ((name, augmentedImage) in augmentedImages) {
                            AugmentedImageNode(augmentedImage = augmentedImage) {
                                when (name) {
                                    "rabbit" -> {
                                        rabbitModelInstance?.let { instance ->
                                            ModelNode(
                                                modelInstance = instance,
                                                scaleToUnits = 0.1f,
                                                centerOrigin = Position(0.0f)
                                            )
                                        }
                                    }

                                    "qrcode" -> {
                                        val exoPlayer = remember {
                                            ExoPlayer.Builder(this@Activity).build().apply {
                                                setMediaItem(
                                                    MediaItem.fromUri(
                                                        "https://sceneview.github.io/assets/videos/ads/ar_camera_app_ad.mp4"
                                                    )
                                                )
                                                prepare()
                                                playWhenReady = true
                                                repeatMode = Player.REPEAT_MODE_ALL
                                            }
                                        }
                                        ExoPlayerNode(exoPlayer = exoPlayer)
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

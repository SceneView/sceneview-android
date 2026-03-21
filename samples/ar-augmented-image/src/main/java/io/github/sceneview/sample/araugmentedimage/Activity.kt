package io.github.sceneview.sample.araugmentedimage

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

                    // ── Status overlay ──
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Augmented Image",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (augmentedImages.isEmpty()) "Scanning for images..."
                            else "${augmentedImages.size} image(s) tracked",
                            color = if (augmentedImages.isEmpty()) Color(0xFFFFA726) else Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (augmentedImages.isNotEmpty()) {
                            augmentedImages.keys.forEach { name ->
                                Text(
                                    text = "  $name",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // ── Bottom hint ──
                    Text(
                        text = "Point camera at the rabbit image or QR code",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

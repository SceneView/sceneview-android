package io.github.sceneview.demo.demos

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberMediaPlayer

/**
 * Demonstrates VideoNode — a flat quad that plays a video from an asset file inside the 3D scene.
 *
 * Uses the convenience `VideoNode(videoPath = ...)` overload that manages MediaPlayer lifecycle
 * automatically, plus the `VideoNode(player = ...)` overload to show manual control.
 */
@Composable
fun VideoDemo(onBack: () -> Unit) {
    var isPlaying by remember { mutableStateOf(true) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    // Manual MediaPlayer so we can toggle play/pause from controls.
    val context = LocalContext.current
    val player = rememberMediaPlayer(
        context = context,
        assetFileLocation = "videos/sample.mp4",
        isLooping = true,
        autoStart = true
    )

    DemoScaffold(
        title = "Video",
        onBack = onBack,
        controls = {
            Text("Playback", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(onClick = {
                isPlaying = !isPlaying
                player?.let { p ->
                    if (isPlaying) p.start() else p.pause()
                }
            }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        }
    ) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,
            cameraManipulator = rememberCameraManipulator()
        ) {
            // VideoNode with a manual MediaPlayer — the quad auto-sizes to the video aspect ratio.
            player?.let { p ->
                VideoNode(player = p)
            }
        }
    }
}

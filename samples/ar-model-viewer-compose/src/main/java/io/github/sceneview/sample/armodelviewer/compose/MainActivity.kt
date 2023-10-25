package io.github.sceneview.sample.armodelviewer.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ar.core.Config
import com.google.ar.core.Plane
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.sample.Colors
import io.github.sceneview.sample.DarkColorPalette
import io.github.sceneview.sample.LightColorPalette
import io.github.sceneview.sample.Shapes
import io.github.sceneview.sample.Typography

private const val kModelFile = "https://sceneview.github.io/assets/models/DamagedHelmet.glb"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SceneViewTheme {
                // A surface container using the 'background' color from the theme
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    var isLoading by remember { mutableStateOf(false) }
                    var planeRenderer by remember { mutableStateOf(true) }
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val childNodes = rememberNodes()
                    ARScene(
                        modifier = Modifier.fillMaxSize(),
                        childNodes = childNodes,
                        engine = engine,
                        modelLoader = modelLoader,
                        planeRenderer = planeRenderer,
                        onSessionConfiguration = { session, config ->
                            config.depthMode =
                                when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                    true -> Config.DepthMode.AUTOMATIC
                                    else -> Config.DepthMode.DISABLED
                                }
                            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                            config.lightEstimationMode =
                                Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        },
                        onSessionUpdate = { _, frame ->
                            if (childNodes.isEmpty()) {
                                frame.getUpdatedPlanes()
                                    .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                                    ?.let { plane ->
                                        isLoading = true
                                        childNodes += AnchorNode(
                                            engine = engine,
                                            anchor = plane.createAnchor(plane.centerPose)
                                        ).apply {
                                            isEditable = true
                                            modelLoader.loadModelInstanceAsync(kModelFile) { modelInstance ->
                                                if (modelInstance != null) {
                                                    addChildNode(
                                                        ModelNode(
                                                            modelInstance = modelInstance,
                                                            // Scale to fit in a 0.5 meters cube
                                                            scaleToUnits = 0.5f,
                                                            // Bottom origin instead of center so the model base is on floor
                                                            centerOrigin = Position(y = -1.0f)
                                                        ).apply {
                                                            isEditable = true
                                                        }
                                                    )
                                                }
                                                isLoading = false
                                                planeRenderer = false
                                            }
                                        }
                                    }
                            }
                        }
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.Center),
                            color = Colors.Purple700
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SceneViewTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
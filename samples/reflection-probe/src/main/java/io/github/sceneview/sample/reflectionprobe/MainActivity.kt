package io.github.sceneview.sample.reflectionprobe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberScene
import io.github.sceneview.sample.SceneviewTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    val engine = rememberEngine()
                    val modelLoader = io.github.sceneview.rememberModelLoader(engine)
                    val materialLoader = rememberMaterialLoader(engine)
                    val environmentLoader = rememberEnvironmentLoader(engine)

                    // The Filament scene — passed explicitly so ReflectionProbeNode can override IBL.
                    val scene = rememberScene(engine)

                    val cameraNode = rememberCameraNode(engine) {
                        position = Position(z = 4.0f)
                        lookAt(Position(0f, 0f, 0f))
                    }
                    val mainLightNode = rememberMainLightNode(engine)

                    // Default (dark) environment — no skybox, low IBL.
                    val defaultEnvironment = rememberEnvironment(environmentLoader, isOpaque = true)

                    // High-quality HDR environment applied by the ReflectionProbeNode.
                    val probeEnvironment = rememberEnvironment(environmentLoader) {
                        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
                    }

                    // Track camera world position so the probe can evaluate zone membership.
                    var cameraPosition by remember { mutableStateOf(Position()) }

                    // Metallic sphere material — picks up IBL reflections clearly.
                    val metallicMaterial = remember(materialLoader) {
                        materialLoader.createColorInstance(
                            color = Color(0.8f, 0.8f, 0.9f),
                            metallic = 1.0f,
                            roughness = 0.1f
                        )
                    }

                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        materialLoader = materialLoader,
                        environmentLoader = environmentLoader,
                        scene = scene,
                        environment = defaultEnvironment,
                        mainLightNode = mainLightNode,
                        cameraNode = cameraNode,
                        cameraManipulator = rememberCameraManipulator(
                            orbitHomePosition = cameraNode.worldPosition
                        ),
                        onFrame = {
                            cameraPosition = cameraNode.worldPosition
                        }
                    ) {
                        // Global ReflectionProbeNode — always active; applies the HDR sky IBL
                        // to the entire scene so the metallic sphere picks up reflections.
                        ReflectionProbeNode(
                            filamentScene = scene,
                            environment = probeEnvironment,
                            radius = 0f, // global — no distance check
                            cameraPosition = cameraPosition
                        )

                        // Metallic sphere at the centre of the scene.
                        SphereNode(
                            radius = 0.8f,
                            materialInstance = metallicMaterial,
                            apply = {
                                scale = Scale(1f)
                            }
                        )
                    }

                    // Label
                    Text(
                        text = "ReflectionProbeNode — sky_2k HDR",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

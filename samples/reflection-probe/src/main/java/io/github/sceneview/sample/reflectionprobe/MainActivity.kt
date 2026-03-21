package io.github.sceneview.sample.reflectionprobe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

                    val scene = rememberScene(engine)
                    val cameraNode = rememberCameraNode(engine) {
                        position = Position(z = 5.0f, y = 1.0f)
                        lookAt(Position(0f, 0f, 0f))
                    }
                    val mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000f
                    }

                    // Default (dark) environment
                    val defaultEnvironment = rememberEnvironment(environmentLoader, isOpaque = true)

                    // HDR environment for the probe
                    val probeEnvironment = rememberEnvironment(environmentLoader) {
                        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
                    }

                    var cameraPosition by remember { mutableStateOf(Position()) }

                    // Controls
                    var probeEnabled by remember { mutableStateOf(true) }
                    var roughness by remember { mutableFloatStateOf(0.1f) }

                    // Material type selection
                    val materialTypes = listOf("Chrome", "Gold", "Copper", "Rough")
                    var selectedMaterial by remember { mutableStateOf("Chrome") }

                    val materialColor = when (selectedMaterial) {
                        "Chrome" -> Color(0.8f, 0.8f, 0.9f)
                        "Gold" -> Color(1.0f, 0.84f, 0.0f)
                        "Copper" -> Color(0.72f, 0.45f, 0.20f)
                        "Rough" -> Color(0.6f, 0.6f, 0.6f)
                        else -> Color(0.8f, 0.8f, 0.9f)
                    }
                    val materialRoughness = if (selectedMaterial == "Rough") 0.8f else roughness
                    val materialMetallic = if (selectedMaterial == "Rough") 0.3f else 1.0f

                    val sphereMaterial = remember(materialLoader, materialColor, materialRoughness, materialMetallic) {
                        materialLoader.createColorInstance(
                            color = materialColor,
                            metallic = materialMetallic,
                            roughness = materialRoughness
                        )
                    }

                    // Floor material
                    val floorMaterial = remember(materialLoader) {
                        materialLoader.createColorInstance(
                            color = Color(0.3f, 0.3f, 0.35f),
                            metallic = 0.0f,
                            roughness = 0.9f
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
                        // Reflection probe — toggle on/off to see the difference
                        if (probeEnabled) {
                            ReflectionProbeNode(
                                filamentScene = scene,
                                environment = probeEnvironment,
                                radius = 0f,
                                cameraPosition = cameraPosition
                            )
                        }

                        // Main sphere
                        SphereNode(
                            radius = 1.0f,
                            materialInstance = sphereMaterial,
                            position = Position(y = 1.0f)
                        )

                        // Small companion spheres showing different angles
                        SphereNode(
                            radius = 0.4f,
                            materialInstance = sphereMaterial,
                            position = Position(x = -2.0f, y = 0.4f, z = 0.5f)
                        )
                        SphereNode(
                            radius = 0.4f,
                            materialInstance = sphereMaterial,
                            position = Position(x = 2.0f, y = 0.4f, z = 0.5f)
                        )

                        // Floor
                        PlaneNode(
                            size = Scale(10f, 10f),
                            materialInstance = floorMaterial
                        )
                    }

                    // ── Top controls ──
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Reflection Probe",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = if (probeEnabled) "HDR Probe ON" else "Probe OFF",
                                color = if (probeEnabled) Color(0xFF4CAF50) else Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = probeEnabled,
                                onCheckedChange = { probeEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF4CAF50)
                                )
                            )
                        }

                        AnimatedVisibility(visible = selectedMaterial != "Rough") {
                            Column {
                                Text(
                                    text = "Roughness: ${"%.2f".format(roughness)}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Slider(
                                    value = roughness,
                                    onValueChange = { roughness = it },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // ── Bottom material chips ──
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        materialTypes.forEach { type ->
                            FilterChip(
                                selected = selectedMaterial == type,
                                onClick = { selectedMaterial = type },
                                leadingIcon = if (selectedMaterial == type) {
                                    { Icon(Icons.Default.Check, null, tint = Color.White) }
                                } else null
                            ) {
                                Text(type, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

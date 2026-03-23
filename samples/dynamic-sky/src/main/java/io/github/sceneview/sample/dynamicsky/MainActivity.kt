package io.github.sceneview.sample.dynamicsky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
<<<<<<< HEAD
import androidx.activity.enableEdgeToEdge
=======
>>>>>>> origin/feat/dynamic-sky-fog-node
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import io.github.sceneview.Scene
import io.github.sceneview.createEnvironment
import io.github.sceneview.createView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.DynamicSkyNode
import io.github.sceneview.node.FogNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import io.github.sceneview.sample.SceneviewTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
<<<<<<< HEAD
        enableEdgeToEdge()
=======
>>>>>>> origin/feat/dynamic-sky-fog-node
        setContent {
            SceneviewTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DynamicSkyScreen()
                }
            }
        }
    }
}

@Composable
fun DynamicSkyScreen() {
    // ── Controls state ────────────────────────────────────────────────────────────────────────────
    var timeOfDay by remember { mutableFloatStateOf(10f) }
    var turbidity by remember { mutableFloatStateOf(2f) }
    var sunIntensity by remember { mutableFloatStateOf(110_000f) }
    var fogEnabled by remember { mutableStateOf(false) }
    var fogDensity by remember { mutableFloatStateOf(0.04f) }
    var fogHeight by remember { mutableFloatStateOf(1.0f) }

    // ── Filament / SceneView resources ───────────────────────────────────────────────────────────
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 0.5f, z = 3f)
        lookAt(Position(0f, 0f, 0f))
    }

    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
            ?: createEnvironment(environmentLoader)
    }

    val foxInstance = rememberModelInstance(modelLoader, "models/Fox.glb")

    // Custom view — shadows enabled so the ground plane receives cast shadows
    val view = rememberView(engine) {
        createView(engine).apply {
            setShadowingEnabled(true)
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {

        // 3D viewport (top half)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                view = view,
                cameraNode = cameraNode,
                cameraManipulator = rememberCameraManipulator(
                    orbitHomePosition = cameraNode.worldPosition,
                    targetPosition = Position(0f, 0f, 0f)
                ),
                environment = environment
            ) {
                // Dynamic sun — position, colour and intensity all driven by timeOfDay
                DynamicSkyNode(
                    timeOfDay = timeOfDay,
                    turbidity = turbidity,
                    sunIntensity = sunIntensity
                )

                // Fog — writes View.fogOptions reactively; no-op when disabled
                FogNode(
                    view = view,
                    density = fogDensity,
                    height = fogHeight,
                    color = Color(red = 0.80f, green = 0.88f, blue = 1.00f, alpha = 1f),
                    enabled = fogEnabled
                )

                // Simple ground plane
                PlaneNode(
                    size = Size(10f, 10f),
                    position = Position(y = -0.5f)
                )

                // Fox model — null while loading; node appears on next recomposition
                foxInstance?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 0.012f,
                        position = Position(y = -0.5f),
                        autoAnimate = true
                    )
                }
            }

            Text(
                text = "Drag to orbit  •  Pinch to zoom",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 10.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Controls panel (bottom half)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Dynamic Sky & Fog Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ── DynamicSkyNode controls ────────────────────────────────────────────────────────
            SectionHeader("Sun  (DynamicSkyNode)")
            val hour = timeOfDay.toInt()
            val minute = ((timeOfDay - hour) * 60).toInt()
            val period = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            SliderRow(
                label = "Time of day",
                display = "%d:%02d %s".format(displayHour, minute, period),
                value = timeOfDay,
                min = 0f,
                max = 24f
            ) { timeOfDay = it }
            SliderRow(
                label = "Turbidity",
                display = "%.1f".format(turbidity),
                value = turbidity,
                min = 1f,
                max = 10f
            ) { turbidity = it }
            SliderRow(
                label = "Sun intensity",
                display = "%.0f lux".format(sunIntensity),
                value = sunIntensity,
                min = 0f,
                max = 150_000f
            ) { sunIntensity = it }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── FogNode controls ───────────────────────────────────────────────────────────────
            SectionHeader("Atmospheric Fog  (FogNode)")
            ToggleRow("Enabled", fogEnabled) { fogEnabled = it }
            if (fogEnabled) {
                SliderRow(
                    label = "Density",
                    display = "%.3f".format(fogDensity),
                    value = fogDensity,
                    min = 0.001f,
                    max = 0.3f
                ) { fogDensity = it }
                SliderRow(
                    label = "Height falloff",
                    display = "%.1f m".format(fogHeight),
                    value = fogHeight,
                    min = 0f,
                    max = 5f
                ) { fogHeight = it }
            }
        }
    }
}

// ── Shared UI helpers ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    display: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(display, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

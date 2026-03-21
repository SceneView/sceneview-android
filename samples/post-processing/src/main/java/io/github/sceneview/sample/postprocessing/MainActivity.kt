package io.github.sceneview.sample.postprocessing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.google.android.filament.View
import io.github.sceneview.Scene
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.createView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberView
import io.github.sceneview.sample.SceneviewTheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.MILLISECONDS

/**
 * Post-Processing showcase — demonstrates Bloom, Depth-of-Field, Screen-Space Ambient Occlusion
 * and Fog, all of which are available in Filament 1.56.0 via [View] options.
 *
 * ### Technique
 * Pass a custom [View] to [Scene] via `rememberView(engine) { createView(engine).apply { … } }`,
 * then mutate the view options reactively from Compose state. Because [Scene] re-renders every
 * frame the new option values are picked up immediately.
 *
 * None of these effects require new SceneView API — they are fully surfaced through the existing
 * `view` parameter on [Scene].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PostProcessingScreen()
                }
            }
        }
    }
}

@Composable
fun PostProcessingScreen() {
    // ── Post-processing controls (Compose state) ─────────────────────────────────────────────────

    // Bloom
    var bloomEnabled by remember { mutableStateOf(true) }
    var bloomStrength by remember { mutableFloatStateOf(0.35f) }
    var bloomLensFlare by remember { mutableStateOf(true) }

    // Depth of Field
    var dofEnabled by remember { mutableStateOf(false) }
    var dofCocScale by remember { mutableFloatStateOf(1.0f) }

    // Ambient Occlusion (SSAO)
    var ssaoEnabled by remember { mutableStateOf(true) }
    var ssaoIntensity by remember { mutableFloatStateOf(1.0f) }

    // Fog
    var fogEnabled by remember { mutableStateOf(false) }
    var fogDensity by remember { mutableFloatStateOf(0.05f) }

    // ── Filament / SceneView resources ───────────────────────────────────────────────────────────
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    // Pivot node — the camera orbits around it
    val centerNode = rememberNode(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(y = -0.3f, z = 2.2f)
        lookAt(centerNode)
        centerNode.addChildNode(this)
    }

    val cameraTransition = rememberInfiniteTransition(label = "CameraOrbit")
    val cameraRotation by cameraTransition.animateRotation(
        initialValue = Rotation(y = 0.0f),
        targetValue = Rotation(y = 360.0f),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12.seconds.toInt(MILLISECONDS))
        )
    )

    val modelInstance = rememberModelInstance(modelLoader, "models/damaged_helmet.glb")
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
    }

    // Custom View — enables shadowing so SSAO contact shadows work.
    // `createView` is the SceneView default factory; we enable shadows on top.
    val view = rememberView(engine) {
        createView(engine).apply {
            setShadowingEnabled(true)
        }
    }

    // Apply post-processing options reactively from Compose state.
    // These are plain field-writes on data-class options; cheap to call every recomposition.
    // Filament reads the current option values on each rendered frame.
    view.bloomOptions = view.bloomOptions.also {
        it.enabled = bloomEnabled
        it.strength = bloomStrength
        it.lensFlare = bloomLensFlare
        it.starburst = bloomLensFlare
        it.chromaticAberration = if (bloomLensFlare) 0.005f else 0.0f
    }

    view.depthOfFieldOptions = view.depthOfFieldOptions.also {
        it.enabled = dofEnabled
        it.cocScale = dofCocScale
        it.maxApertureDiameter = 0.01f
    }

    @Suppress("DEPRECATION")
    view.ambientOcclusion =
        if (ssaoEnabled) View.AmbientOcclusion.SSAO else View.AmbientOcclusion.NONE
    view.ambientOcclusionOptions = view.ambientOcclusionOptions.also {
        it.enabled = ssaoEnabled
        it.intensity = ssaoIntensity
        it.radius = 0.3f
        it.power = 1.0f
    }

    view.fogOptions = view.fogOptions.also {
        it.enabled = fogEnabled
        it.density = fogDensity
        it.distance = 0.5f
        it.cutOffDistance = 10f
        it.fogColorFromIbl = true
    }

    // ── Layout: 3D viewport (top half) + controls panel (bottom half) ───────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {

        // 3D viewport
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
                    targetPosition = centerNode.worldPosition
                ),
                environment = environment,
                mainLightNode = rememberMainLightNode(engine) {
                    intensity = 150_000f
                },
                onFrame = {
                    centerNode.rotation = cameraRotation
                    cameraNode.lookAt(centerNode)
                }
            ) {
                // Attach the pivot node to the scene's root
                Node(apply = { centerNode.addChildNode(this) })
                // Model appears when loaded; null-safe handles the async loading state
                modelInstance?.let { instance ->
                    ModelNode(modelInstance = instance, scaleToUnits = 0.5f)
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

        // Controls panel
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
                "Post-Processing Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ── Bloom ──────────────────────────────────────────────────────────────────────────
            SectionHeader("Bloom  (View.setBloomOptions)")
            ToggleRow("Enabled", bloomEnabled) { bloomEnabled = it }
            if (bloomEnabled) {
                SliderRow("Strength", bloomStrength, 0f, 1f) { bloomStrength = it }
                ToggleRow("Lens Flare + Starburst + Chromatic Aberration", bloomLensFlare) {
                    bloomLensFlare = it
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Depth of Field ─────────────────────────────────────────────────────────────────
            SectionHeader("Depth of Field  (View.setDepthOfFieldOptions)")
            ToggleRow("Enabled", dofEnabled) { dofEnabled = it }
            if (dofEnabled) {
                SliderRow("Circle of Confusion scale", dofCocScale, 0.1f, 5f) {
                    dofCocScale = it
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── SSAO ───────────────────────────────────────────────────────────────────────────
            SectionHeader("Ambient Occlusion  (View.setAmbientOcclusionOptions)")
            ToggleRow("Enabled", ssaoEnabled) { ssaoEnabled = it }
            if (ssaoEnabled) {
                SliderRow("Intensity", ssaoIntensity, 0f, 4f) { ssaoIntensity = it }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Fog ────────────────────────────────────────────────────────────────────────────
            SectionHeader("Atmospheric Fog  (View.setFogOptions)")
            ToggleRow("Enabled", fogEnabled) { fogEnabled = it }
            if (fogEnabled) {
                SliderRow("Density", fogDensity, 0.001f, 0.3f) { fogDensity = it }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "All effects use Filament 1.56.0 View APIs. " +
                        "No new SceneView wrapper needed — pass a custom view via " +
                        "rememberView(engine) { createView(engine).apply { … } }.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
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
            Text("%.3f".format(value), style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

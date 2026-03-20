package io.github.sceneview.sample.linepath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.colorOf
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.sample.SceneviewTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LinePath3DScene()
                }
            }
        }
    }
}

/**
 * Generates a 3D spiral path (helix) with [turns] full rotations, [radius] in the XZ plane,
 * rising [height] units along the Y axis.
 */
private fun spiralPoints(turns: Int = 4, steps: Int = 120, radius: Float = 0.8f, height: Float = 1.2f): List<Position> =
    (0..steps).map { i ->
        val t = i.toFloat() / steps.toFloat()
        val angle = t * turns * 2f * PI.toFloat()
        Position(
            x = cos(angle) * radius,
            y = -height / 2f + t * height,
            z = sin(angle) * radius
        )
    }

/**
 * Generates one period of a sine wave in the XZ plane.
 *
 * The [phase] offset shifts the wave, enabling animation.
 */
private fun sineWavePoints(phase: Float, steps: Int = 60, width: Float = 2.0f, amplitude: Float = 0.3f): List<Position> =
    (0..steps).map { i ->
        val t = i.toFloat() / steps.toFloat()
        val x = -width / 2f + t * width
        val y = 0f
        val z = sin(t * 2f * PI.toFloat() + phase) * amplitude
        Position(x = x, y = y, z = z)
    }

@Composable
fun LinePath3DScene() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    // Materials
    val redMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(Color.Red))
    }
    val greenMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(Color.Green))
    }
    val blueMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(Color.Blue))
    }
    val yellowMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(Color.Yellow))
    }
    val cyanMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(Color.Cyan))
    }

    // Animated phase for the sine wave
    val infiniteTransition = rememberInfiniteTransition(label = "SineWaveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SinePhase"
    )

    val spiralPts = remember { spiralPoints() }
    val sineWavePts = sineWavePoints(phase = phase)

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = Position(x = 0f, y = 1f, z = 3.5f),
            targetPosition = Position(x = 0f, y = 0f, z = 0f)
        )
    ) {
        // ── Axis gizmo: X (red), Y (green), Z (blue) ──────────────────────────────────────────────
        LineNode(
            start = Position(0f, 0f, 0f),
            end = Position(1f, 0f, 0f),
            materialInstance = redMaterial
        )
        LineNode(
            start = Position(0f, 0f, 0f),
            end = Position(0f, 1f, 0f),
            materialInstance = greenMaterial
        )
        LineNode(
            start = Position(0f, 0f, 0f),
            end = Position(0f, 0f, 1f),
            materialInstance = blueMaterial
        )

        // ── Spiral PathNode ────────────────────────────────────────────────────────────────────────
        PathNode(
            points = spiralPts,
            closed = false,
            materialInstance = yellowMaterial
        )

        // ── Animated sine-wave PathNode (positioned below the spiral) ──────────────────────────────
        PathNode(
            points = sineWavePts,
            closed = false,
            materialInstance = cyanMaterial,
            position = Position(x = 0f, y = -0.8f, z = 0f)
        )
    }
}

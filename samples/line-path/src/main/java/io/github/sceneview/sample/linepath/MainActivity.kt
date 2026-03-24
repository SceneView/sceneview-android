package io.github.sceneview.sample.linepath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LinePath3DScene()
                }
            }
        }
    }
}

private fun spiralPoints(
    turns: Int = 4,
    steps: Int = 120,
    radius: Float = 0.8f,
    height: Float = 1.2f
): List<Position> =
    (0..steps).map { i ->
        val t = i.toFloat() / steps.toFloat()
        val angle = t * turns * 2f * PI.toFloat()
        Position(
            x = cos(angle) * radius,
            y = -height / 2f + t * height,
            z = sin(angle) * radius
        )
    }

private fun sineWavePoints(
    phase: Float,
    steps: Int = 60,
    width: Float = 2.0f,
    amplitude: Float = 0.3f,
    frequency: Float = 1f
): List<Position> =
    (0..steps).map { i ->
        val t = i.toFloat() / steps.toFloat()
        val x = -width / 2f + t * width
        val z = sin(t * frequency * 2f * PI.toFloat() + phase) * amplitude
        Position(x = x, y = 0f, z = z)
    }

private fun lissajousPoints(
    phase: Float,
    steps: Int = 200,
    a: Int = 3,
    b: Int = 2,
    radius: Float = 0.8f
): List<Position> =
    (0..steps).map { i ->
        val t = i.toFloat() / steps.toFloat() * 2f * PI.toFloat()
        Position(
            x = sin(a * t + phase) * radius,
            y = sin(b * t) * radius,
            z = cos(a * t + phase) * radius * 0.3f
        )
    }

@Composable
fun LinePath3DScene() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

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
    val magentaMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(Color.Magenta))
    }

    // Animated phase
    val infiniteTransition = rememberInfiniteTransition(label = "PathTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    // Controls
    var amplitude by remember { mutableFloatStateOf(0.3f) }
    var frequency by remember { mutableFloatStateOf(1f) }
    var spiralTurns by remember { mutableIntStateOf(4) }
    var showAxis by remember { mutableStateOf(true) }

    // Pattern selection
    val patterns = listOf("Spiral + Wave", "Lissajous", "All")
    var selectedPattern by remember { mutableStateOf("Spiral + Wave") }

    val spiralPts = remember(spiralTurns) { spiralPoints(turns = spiralTurns) }
    val sineWavePts = sineWavePoints(phase = phase, amplitude = amplitude, frequency = frequency)
    val lissajousPts = lissajousPoints(phase = phase)

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = Position(x = 0f, y = 1f, z = 3.5f),
            targetPosition = Position(x = 0f, y = 0f, z = 0f)
        )
    ) {
        // Axis gizmo
        if (showAxis) {
            LineNode(start = Position(0f, 0f, 0f), end = Position(1f, 0f, 0f), materialInstance = redMaterial)
            LineNode(start = Position(0f, 0f, 0f), end = Position(0f, 1f, 0f), materialInstance = greenMaterial)
            LineNode(start = Position(0f, 0f, 0f), end = Position(0f, 0f, 1f), materialInstance = blueMaterial)
        }

        if (selectedPattern == "Spiral + Wave" || selectedPattern == "All") {
            PathNode(points = spiralPts, closed = false, materialInstance = yellowMaterial)
            PathNode(
                points = sineWavePts,
                closed = false,
                materialInstance = cyanMaterial,
                position = Position(y = -0.8f)
            )
        }

        if (selectedPattern == "Lissajous" || selectedPattern == "All") {
            PathNode(
                points = lissajousPts,
                closed = true,
                materialInstance = magentaMaterial,
                position = if (selectedPattern == "All") Position(y = 1.2f) else Position()
            )
        }
    }

    // ── Controls overlay ──
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text("Line & Path", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Text(
                "Amplitude: ${"%.1f".format(amplitude)}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Slider(
                value = amplitude,
                onValueChange = { amplitude = it },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(thumbColor = Color.Cyan)
            )

            Text(
                "Frequency: ${"%.0f".format(frequency)}x",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Slider(
                value = frequency,
                onValueChange = { frequency = it },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(thumbColor = Color.Cyan)
            )
        }
    }

    // ── Bottom pattern chips ──
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            patterns.forEach { pattern ->
                FilterChip(
                    selected = selectedPattern == pattern,
                    onClick = { selectedPattern = pattern },
                    label = { Text(pattern, color = Color.White, fontSize = 12.sp) }
                )
            }
        }
    }
}

package io.github.sceneview.demo.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.math.Position
import io.github.sceneview.math.Position2
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates [ShapeNode] — 2D polygon paths triangulated into 3D geometry.
 * A chip selector switches between triangle, star, and hexagon shapes.
 */
@Composable
fun ShapeDemo(onBack: () -> Unit) {
    var selectedShape by remember { mutableStateOf("Triangle") }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    val trianglePath = remember {
        listOf(
            Position2(0f, 0.5f),
            Position2(-0.5f, -0.3f),
            Position2(0.5f, -0.3f)
        )
    }

    val starPath = remember {
        buildList {
            val outerR = 0.5f
            val innerR = 0.2f
            for (i in 0 until 10) {
                val angle = (i * 36f - 90f) * (PI.toFloat() / 180f)
                val r = if (i % 2 == 0) outerR else innerR
                add(Position2(cos(angle) * r, sin(angle) * r))
            }
        }
    }

    val hexagonPath = remember {
        buildList {
            val r = 0.4f
            for (i in 0 until 6) {
                val angle = (i * 60f) * (PI.toFloat() / 180f)
                add(Position2(cos(angle) * r, sin(angle) * r))
            }
        }
    }

    val currentPath = when (selectedShape) {
        "Star" -> starPath
        "Hexagon" -> hexagonPath
        else -> trianglePath
    }
    val currentColor = when (selectedShape) {
        "Star" -> Color.Yellow
        "Hexagon" -> Color.Magenta
        else -> Color.Cyan
    }

    DemoScaffold(
        title = "Shape Node",
        onBack = onBack,
        controls = {
            Text("Shape", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Triangle", "Star", "Hexagon").forEach { shape ->
                    FilterChip(
                        selected = selectedShape == shape,
                        onClick = { selectedShape = shape },
                        label = { Text(shape) }
                    )
                }
            }
        }
    ) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            cameraManipulator = rememberCameraManipulator()
        ) {
            ShapeNode(
                polygonPath = currentPath,
                materialInstance = materialLoader.createColorInstance(currentColor),
                position = Position(y = 0f, z = -1f)
            )
        }
    }
}

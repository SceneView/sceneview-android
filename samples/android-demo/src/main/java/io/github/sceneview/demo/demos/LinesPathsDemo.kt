package io.github.sceneview.demo.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates LineNode (single segment) and PathNode (polyline through points).
 * Controls toggle visibility and adjust the number of points in the path.
 */
@Composable
fun LinesPathsDemo(onBack: () -> Unit) {
    var showLine by remember { mutableStateOf(true) }
    var showPath by remember { mutableStateOf(true) }
    var pointCount by remember { mutableFloatStateOf(12f) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    DemoScaffold(
        title = "Lines & Paths",
        onBack = onBack,
        controls = {
            Text("Visibility", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(showLine, onClick = { showLine = !showLine }, label = { Text("Line") })
                FilterChip(showPath, onClick = { showPath = !showPath }, label = { Text("Path") })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Path Points: ${pointCount.toInt()}",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = pointCount,
                onValueChange = { pointCount = it },
                valueRange = 3f..30f,
                steps = 27
            )
        }
    ) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            cameraManipulator = rememberCameraManipulator()
        ) {
            val redMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(Color.Red)
            }
            val greenMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(Color.Green)
            }

            // Single line segment
            if (showLine) {
                LineNode(
                    start = Position(x = -1.0f, y = -0.5f, z = 0f),
                    end = Position(x = 1.0f, y = 0.5f, z = 0f),
                    materialInstance = redMaterial,
                    position = Position(x = 0f, y = 0.4f)
                )
            }

            // Polyline path forming a spiral / circle pattern
            if (showPath) {
                val count = pointCount.toInt()
                val pathPoints = remember(count) {
                    (0 until count).map { i ->
                        val angle = (i.toFloat() / count) * 2f * Math.PI.toFloat()
                        val radius = 0.5f
                        Position(
                            x = cos(angle) * radius,
                            y = sin(angle) * radius,
                            z = 0f
                        )
                    }
                }
                PathNode(
                    points = pathPoints,
                    closed = true,
                    materialInstance = greenMaterial,
                    position = Position(x = 0f, y = -0.3f)
                )
            }
        }
    }
}

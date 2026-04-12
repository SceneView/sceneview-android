package io.github.sceneview.demo.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.math.Position
import io.github.sceneview.node.Node as NodeImpl
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader

/**
 * Demonstrates [PhysicsNode] — drop spheres that fall under gravity and bounce off the floor.
 *
 * Each "Drop" press adds a new sphere. "Reset" clears all spheres by incrementing a generation
 * key that forces full recomposition.
 */
@Composable
fun PhysicsDemo(onBack: () -> Unit) {
    var sphereCount by remember { mutableIntStateOf(1) }
    var generation by remember { mutableIntStateOf(0) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    DemoScaffold(
        title = "Physics",
        onBack = onBack,
        controls = {
            Text("Spheres: $sphereCount", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { sphereCount++ }) {
                    Text("Drop")
                }
                Button(onClick = {
                    sphereCount = 1
                    generation++
                }) {
                    Text("Reset")
                }
            }
        }
    ) {
        // key(generation) forces full recomposition on reset
        key(generation) {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                materialLoader = materialLoader,
                cameraManipulator = rememberCameraManipulator()
            ) {
                // Ground plane for visual reference
                PlaneNode(
                    materialInstance = materialLoader.createColorInstance(Color.DarkGray),
                    position = Position(y = 0f)
                )

                for (i in 0 until sphereCount) {
                    val xOffset = (i % 5 - 2) * 0.4f
                    val startY = 3f + i * 0.5f
                    val color = when (i % 4) {
                        0 -> Color.Red
                        1 -> Color.Blue
                        2 -> Color.Green
                        else -> Color.Yellow
                    }

                    // Capture the Node reference via apply so PhysicsNode can drive it.
                    var nodeRef by remember(i) { mutableStateOf<NodeImpl?>(null) }

                    Node(
                        position = Position(x = xOffset, y = startY, z = -2f),
                        apply = { nodeRef = this }
                    ) {
                        SphereNode(
                            radius = 0.15f,
                            materialInstance = materialLoader.createColorInstance(color)
                        )
                    }

                    // PhysicsNode attaches an onFrame callback that applies gravity + bounce.
                    nodeRef?.let { node ->
                        PhysicsNode(
                            node = node,
                            restitution = 0.7f,
                            radius = 0.15f
                        )
                    }
                }
            }
        }
    }
}

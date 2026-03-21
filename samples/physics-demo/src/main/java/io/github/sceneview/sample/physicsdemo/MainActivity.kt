package io.github.sceneview.sample.physicsdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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
import io.github.sceneview.math.Size
import io.github.sceneview.math.colorOf
import io.github.sceneview.node.PhysicsNode
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.sample.SceneviewTheme

/**
 * Physics Demo — tap anywhere to throw colored balls that fall and bounce off the floor.
 *
 * Demonstrates PhysicsNode with configurable restitution, color variety, and ball counter.
 */
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val materialLoader = rememberMaterialLoader(engine)
                    val environmentLoader = rememberEnvironmentLoader(engine)
                    val environment = rememberEnvironment(environmentLoader)

                    val cameraNode = rememberCameraNode(engine) {
                        position = Position(x = 0f, y = 1.5f, z = 4f)
                        lookAt(Position(0f, 0.5f, 0f))
                    }

                    val mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000f
                    }

                    // Ball colors
                    val ballColors = listOf(
                        Color(0xFFE53935), // red
                        Color(0xFF1E88E5), // blue
                        Color(0xFF43A047), // green
                        Color(0xFFFDD835), // yellow
                        Color(0xFFAB47BC), // purple
                        Color(0xFFFF7043), // orange
                    )

                    val balls = remember { mutableStateListOf<SphereNode>() }
                    var ballCount by remember { mutableStateOf(0) }
                    var restitution by remember { mutableFloatStateOf(0.65f) }

                    // Floor material — grid-like dark surface
                    val floorMaterial = remember(materialLoader) {
                        materialLoader.createColorInstance(
                            color = colorOf(0.25f, 0.25f, 0.28f),
                            metallic = 0.1f,
                            roughness = 0.8f
                        )
                    }

                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        cameraNode = cameraNode,
                        environment = environment,
                        mainLightNode = mainLightNode,
                        onGestureListener = rememberOnGestureListener(
                            onSingleTapConfirmed = { _, _ ->
                                val index = ballCount++
                                val sign = if (index % 2 == 0) 1f else -1f
                                val lateralSpeed = 0.3f + (index % 5) * 0.15f
                                val color = ballColors[index % ballColors.size]

                                val ballMaterial = materialLoader.createColorInstance(
                                    color = colorOf(color),
                                    metallic = 0.3f,
                                    roughness = 0.4f
                                )

                                val ball = SphereNode(
                                    engine = engine,
                                    radius = BALL_RADIUS,
                                    materialInstance = ballMaterial
                                ).apply {
                                    position = Position(
                                        x = sign * lateralSpeed * 0.5f,
                                        y = SPAWN_HEIGHT,
                                        z = 0f
                                    )
                                }
                                balls.add(ball)

                                if (balls.size > MAX_BALLS) {
                                    balls.removeAt(0)
                                }
                                true
                            }
                        )
                    ) {
                        // Floor
                        CubeNode(
                            size = Size(FLOOR_WIDTH, FLOOR_THICKNESS, FLOOR_DEPTH),
                            position = Position(y = -FLOOR_HALF_THICKNESS),
                            materialInstance = floorMaterial
                        )

                        // Balls
                        for ((idx, ball) in balls.withIndex()) {
                            val sign = if (idx % 2 == 0) 1f else -1f
                            val lateralSpeed = 0.3f + (idx % 5) * 0.15f

                            Node(apply = { addChildNode(ball) })

                            PhysicsNode(
                                node = ball,
                                mass = 1f,
                                restitution = restitution,
                                linearVelocity = Position(
                                    x = sign * lateralSpeed,
                                    y = 0f,
                                    z = 0f
                                ),
                                floorY = 0f,
                                radius = BALL_RADIUS
                            )
                        }
                    }

                    // ── Top-left: title + ball counter ──
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Physics Demo",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${balls.size} / $MAX_BALLS balls",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Bounciness: ${"%.0f".format(restitution * 100)}%",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Slider(
                            value = restitution,
                            onValueChange = { restitution = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFFE53935)
                            )
                        )
                    }

                    // ── Bottom hint ──
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Tap anywhere to throw a ball",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val MAX_BALLS = 12
        const val SPAWN_HEIGHT = 2.5f
        const val BALL_RADIUS = 0.15f
        const val FLOOR_WIDTH = 6f
        const val FLOOR_DEPTH = 6f
        const val FLOOR_THICKNESS = 0.1f
        const val FLOOR_HALF_THICKNESS = FLOOR_THICKNESS / 2f
    }
}

package io.github.sceneview.sample.textlabels

import android.os.Bundle
import android.view.MotionEvent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.sample.SceneviewTheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.MILLISECONDS

private data class LabelledObject(
    val position: Position,
    val color: Int,
    val name: String,
    val info: String,
    val radius: Float = 0.15f
)

private val objects = listOf(
    LabelledObject(Position(x = -1.0f, y = 0f, z = 0.3f), 0xFF4C8BF5.toInt(), "Earth", "12,742 km", 0.18f),
    LabelledObject(Position(x = -0.3f, y = 0.3f, z = -0.2f), 0xFFEA4335.toInt(), "Mars", "6,779 km", 0.12f),
    LabelledObject(Position(x = 0.5f, y = -0.1f, z = 0.1f), 0xFFFBBC04.toInt(), "Venus", "12,104 km", 0.17f),
    LabelledObject(Position(x = 1.0f, y = 0.2f, z = -0.3f), 0xFF34A853.toInt(), "Jupiter", "139,820 km", 0.22f),
    LabelledObject(Position(x = 0f, y = -0.3f, z = 0.5f), 0xFFAB47BC.toInt(), "Neptune", "49,528 km", 0.14f),
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    val engine = rememberEngine()
                    val materialLoader = rememberMaterialLoader(engine)
                    val environmentLoader = rememberEnvironmentLoader(engine)

                    // Label mode: 0 = name, 1 = info, 2 = both
                    val labelModes = remember {
                        mutableStateListOf(*Array(objects.size) { 0 })
                    }
                    var tapCount by remember { mutableIntStateOf(0) }

                    var cameraPos by remember { mutableStateOf(Position(x = 0f, y = 1f, z = 3f)) }

                    val centerNode = rememberNode(engine)
                    val cameraNode = rememberCameraNode(engine) {
                        position = Position(x = 0f, y = 0.8f, z = 2.5f)
                        lookAt(centerNode)
                        centerNode.addChildNode(this)
                    }

                    val cameraTransition = rememberInfiniteTransition(label = "CameraOrbit")
                    val cameraRotation by cameraTransition.animateRotation(
                        initialValue = Rotation(y = 0f),
                        targetValue = Rotation(y = 360f),
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 15.seconds.toInt(MILLISECONDS))
                        )
                    )

                    val environment = rememberEnvironment(environmentLoader) {
                        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
                    }

                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = io.github.sceneview.rememberModelLoader(engine),
                        materialLoader = materialLoader,
                        cameraNode = cameraNode,
                        cameraManipulator = rememberCameraManipulator(
                            orbitHomePosition = cameraNode.worldPosition,
                            targetPosition = centerNode.worldPosition
                        ),
                        environment = environment,
                        onFrame = {
                            centerNode.rotation = cameraRotation
                            cameraNode.lookAt(centerNode)
                            cameraPos = cameraNode.worldPosition
                        },
                        onGestureListener = rememberOnGestureListener()
                    ) {
                        Node(apply = { centerNode.addChildNode(this) })

                        objects.forEachIndexed { index, obj ->
                            val sphereMaterial = remember(materialLoader, obj.color) {
                                materialLoader.createColorInstance(color = obj.color)
                            }

                            SphereNode(
                                radius = obj.radius,
                                materialInstance = sphereMaterial,
                                apply = {
                                    position = obj.position
                                    isTouchable = true
                                    onSingleTapConfirmed = { _: MotionEvent ->
                                        labelModes[index] = (labelModes[index] + 1) % 3
                                        tapCount++
                                        true
                                    }
                                }
                            )

                            // Label text based on current mode
                            val labelText = when (labelModes[index]) {
                                0 -> obj.name
                                1 -> obj.info
                                2 -> "${obj.name}\n${obj.info}"
                                else -> obj.name
                            }

                            val labelHeight = if (labelModes[index] == 2) 0.24f else 0.16f

                            TextNode(
                                text = labelText,
                                fontSize = 48f,
                                textColor = android.graphics.Color.WHITE,
                                backgroundColor = 0xCC1A1A2E.toInt(),
                                widthMeters = 0.55f,
                                heightMeters = labelHeight,
                                position = Position(
                                    x = obj.position.x,
                                    y = obj.position.y + obj.radius + 0.22f,
                                    z = obj.position.z
                                ),
                                cameraPositionProvider = { cameraPos }
                            )
                        }
                    }

                    // ── Top-left: title ──
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "3D Text Labels",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${objects.size} planets  |  $tapCount taps",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
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
                            text = "Tap a planet to cycle: Name \u2192 Size \u2192 Both",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

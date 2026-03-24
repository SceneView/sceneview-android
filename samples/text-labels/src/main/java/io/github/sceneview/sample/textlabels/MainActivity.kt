package io.github.sceneview.sample.textlabels

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.createEnvironment
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.sample.SceneviewTheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.MILLISECONDS

/** Describes one labelled 3D object in the scene. */
private data class LabelledObject(
    val position: Position,
    val color: Int,           // ARGB packed colour for the sphere material
    val defaultLabel: String,
    val radius: Float = 0.15f
)

private val objects = listOf(
    LabelledObject(Position(x = -0.8f, y = 0f, z = 0f), 0xFF4C8BF5.toInt(), "Planet A"),
    LabelledObject(Position(x = 0f,    y = 0f, z = 0f), 0xFF34A853.toInt(), "Planet B"),
    LabelledObject(Position(x = 0.8f,  y = 0f, z = 0f), 0xFFEA4335.toInt(), "Planet C"),
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    val engine            = rememberEngine()
                    val materialLoader    = rememberMaterialLoader(engine)
                    val environmentLoader = rememberEnvironmentLoader(engine)

                    // One mutable label string per object — drives recomposition
                    val labels = remember {
                        mutableStateListOf(*objects.map { it.defaultLabel }.toTypedArray())
                    }

                    // Camera world position updated every frame so TextNodes can face the camera
                    var cameraPos by remember { mutableStateOf(Position(x = 0f, y = 1f, z = 3f)) }

                    val centerNode = rememberNode(engine)
                    val cameraNode = rememberCameraNode(engine) {
                        position = Position(x = 0f, y = 1f, z = 3f)
                        lookAt(centerNode)
                        centerNode.addChildNode(this)
                    }

                    // Slowly orbit the camera around the scene centre
                    val cameraTransition = rememberInfiniteTransition(label = "CameraOrbit")
                    val cameraRotation by cameraTransition.animateRotation(
                        initialValue = Rotation(y = 0f),
                        targetValue = Rotation(y = 360f),
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 12.seconds.toInt(MILLISECONDS))
                        )
                    )

                    val environment = rememberEnvironment(environmentLoader) {
                        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                            ?: createEnvironment(environmentLoader)
                    }

                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = rememberModelLoader(engine),
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
                        // Invisible pivot — the camera's orbit parent
                        Node(apply = { centerNode.addChildNode(this) })

                        objects.forEachIndexed { index, obj ->

                            // Pre-built PBR material for this sphere's colour
                            val sphereMaterial = remember(materialLoader, obj.color) {
                                materialLoader.createColorInstance(color = obj.color)
                            }

                            // The 3D sphere — tap it to cycle the label
                            SphereNode(
                                radius = obj.radius,
                                materialInstance = sphereMaterial,
                                apply = {
                                    position = obj.position
                                    isTouchable = true
                                    onSingleTapConfirmed = { _: MotionEvent ->
                                        labels[index] = nextLabel(labels[index], obj.defaultLabel)
                                        true
                                    }
                                }
                            )

                            // Floating text label above the sphere — always faces the camera
                            TextNode(
                                text = labels[index],
                                fontSize = 52f,
                                textColor = android.graphics.Color.WHITE,
                                backgroundColor = 0xCC1A1A2E.toInt(),
                                widthMeters = 0.55f,
                                heightMeters = 0.18f,
                                position = Position(
                                    x = obj.position.x,
                                    y = obj.position.y + obj.radius + 0.22f,
                                    z = obj.position.z
                                ),
                                cameraPositionProvider = { cameraPos }
                            )
                        }
                    }

                    // Bottom hint
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Tap a planet to change its label",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

/** Cycles the current label through a small set of values. */
private fun nextLabel(current: String, default: String): String {
    val options = listOf(default, "Tap again!", "Relabelled", default)
    val idx = options.indexOf(current)
    return options[(idx + 1) % options.size]
}

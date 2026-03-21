package io.github.sceneview.sample.modelviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.sample.SceneviewTheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.MILLISECONDS

private data class ModelEntry(val label: String, val assetPath: String, val scale: Float)

private val models = listOf(
    ModelEntry("Helmet", "models/damaged_helmet.glb", 1.0f),
    ModelEntry("Fox", "models/Fox.glb", 0.012f),
)

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
                    val environmentLoader = rememberEnvironmentLoader(engine)

                    var selectedIndex by remember { mutableIntStateOf(0) }
                    val selectedModel = models[selectedIndex]

                    // Animation playback state — reset when model changes.
                    var isPlaying by remember(selectedIndex) { mutableStateOf(true) }
                    var animationIndex by remember(selectedIndex) { mutableIntStateOf(0) }

                    val centerNode = rememberNode(engine)

                    val cameraNode = rememberCameraNode(engine) {
                        position = Position(y = -0.5f, z = 2.0f)
                        lookAt(centerNode)
                        centerNode.addChildNode(this)
                    }

                    val cameraTransition = rememberInfiniteTransition(label = "CameraTransition")
                    val cameraRotation by cameraTransition.animateRotation(
                        initialValue = Rotation(y = 0.0f),
                        targetValue = Rotation(y = 360.0f),
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 7.seconds.toInt(MILLISECONDS))
                        )
                    )

                    val modelInstance = rememberModelInstance(
                        modelLoader,
                        selectedModel.assetPath
                    )
                    val environment = rememberEnvironment(environmentLoader) {
                        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
                    }

                    // Derive animation info from the loaded instance (null while loading).
                    val animationCount by remember(modelInstance) {
                        derivedStateOf { modelInstance?.animator?.animationCount ?: 0 }
                    }
                    val currentAnimationName by remember(modelInstance, animationIndex) {
                        derivedStateOf {
                            modelInstance?.animator
                                ?.takeIf { animationIndex < it.animationCount }
                                ?.getAnimationName(animationIndex)
                        }
                    }

                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        cameraNode = cameraNode,
                        cameraManipulator = rememberCameraManipulator(
                            orbitHomePosition = cameraNode.worldPosition,
                            targetPosition = centerNode.worldPosition
                        ),
                        environment = environment,
                        onFrame = {
                            centerNode.rotation = cameraRotation
                            cameraNode.lookAt(centerNode)
                        },
                        onGestureListener = rememberOnGestureListener(
                            onDoubleTap = { _, node ->
                                node?.apply {
                                    scale *= 2.0f
                                }
                            }
                        )
                    ) {
                        Node(apply = { centerNode.addChildNode(this) })
                        modelInstance?.let { instance ->
                            ModelNode(
                                modelInstance = instance,
                                scaleToUnits = selectedModel.scale,
                                // autoAnimate = false so we drive playback manually.
                                autoAnimate = false,
                                // Passing null stops all animations; passing the name plays it.
                                animationName = if (isPlaying) currentAnimationName else null,
                                animationLoop = true,
                                animationSpeed = 1f
                            )
                        }
                    }

                    // Animation controls — only shown when the loaded model has animations.
                    if (animationCount > 0) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 80.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                    shape = MaterialTheme.shapes.extraLarge
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Play / Pause button
                            FilledTonalIconButton(onClick = { isPlaying = !isPlaying }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause
                                    else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play"
                                )
                            }

                            // Current animation name
                            Text(
                                text = currentAnimationName ?: "Animation ${animationIndex + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Next animation button — only shown when there are multiple animations.
                            if (animationCount > 1) {
                                FilledTonalIconButton(
                                    onClick = {
                                        animationIndex = (animationIndex + 1) % animationCount
                                        isPlaying = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next animation"
                                    )
                                }
                            }
                        }
                    }

                    // Model picker chips — bottom centre
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        models.forEachIndexed { index, model ->
                            FilterChip(
                                selected = index == selectedIndex,
                                onClick = { selectedIndex = index },
                                label = { Text(model.label) }
                            )
                        }
                    }

                    Image(
                        modifier = Modifier
                            .width(192.dp)
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.5f
                                ),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(8.dp),
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo"
                    )
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(id = R.string.app_name)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

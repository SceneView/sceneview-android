package io.github.sceneview.sample.modelviewer.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.sample.SceneviewTheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.MILLISECONDS

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val environmentLoader = rememberEnvironmentLoader(engine)

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

                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        cameraNode = cameraNode,
                        cameraManipulator = rememberCameraManipulator(
                            orbitHomePosition = cameraNode.worldPosition,
                            targetPosition = centerNode.worldPosition
                        ),
                        childNodes = listOf(centerNode,
                            rememberNode {
                                ModelNode(
                                    modelInstance = modelLoader.createModelInstance(
                                        assetFileLocation = "models/damaged_helmet.glb"
                                    ),
                                    scaleToUnits = 0.25f
                                )
                            }),
                        environment = environmentLoader.createHDREnvironment(
                            assetFileLocation = "environments/sky_2k.hdr"
                        )!!,
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
                    )
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
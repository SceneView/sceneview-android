package io.github.sceneview.sample.modelviewer.compose.cameramanipulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.math.Position
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import io.github.sceneview.sample.SceneviewTheme
import kotlin.math.sign

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
                    val view = rememberView(engine)
                    val collisionSystem = rememberCollisionSystem(view)

                    val centerNode = rememberNode(engine)

                    val cameraNode = rememberCameraNode(engine) {
                        position = Position(y = -0.5f, z = 2.0f)
                        lookAt(centerNode)
                        centerNode.addChildNode(this)
                    }

                    val cameraManipulator = rememberCameraManipulator(
                        creator = {
                            AdvancedCameraManipulator(
                                cameraNode = cameraNode,
                                collisionSystem = collisionSystem,
                                orbitHomePosition = cameraNode.worldPosition,
                                targetPosition = centerNode.worldPosition
                            )
                        }
                    )

                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        view = view,
                        cameraNode = cameraNode,
                        cameraManipulator = cameraManipulator,
                        childNodes = listOf(centerNode,
                            rememberNode {
                                ModelNode(
                                    modelInstance = modelLoader.createModelInstance(
                                        assetFileLocation = "models/damaged_helmet.glb"
                                    ),
                                    scaleToUnits = 0.25f
                                )
                            }),
                        collisionSystem = collisionSystem,
                        environment = environmentLoader.createHDREnvironment(
                            assetFileLocation = "environments/sky_2k.hdr"
                        )!!,
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

/**
 * Override default camera manipulator to achieve relative zooming based on distance from camera to
 * target and separation between two fingers.
 *
 * Target is calculated as closest node to center of finger's position in direction of camera.
 */
class AdvancedCameraManipulator(
    private val cameraNode: CameraNode,
    private val collisionSystem: CollisionSystem,
    orbitHomePosition: Position? = null,
    targetPosition: Position? = null
): CameraGestureDetector.DefaultCameraManipulator(
    orbitHomePosition = orbitHomePosition,
    targetPosition = targetPosition
) {
    private var scrollBeginCameraPosition = Position()
    private var scrollBeginDistance: Float? = 0f
    private var scrollBeginSeparation = 0f

    override fun scrollBegin(x: Int, y: Int, separation: Float) {
        val hitResults = collisionSystem.hitTest(x.toFloat(), y.toFloat())
        scrollBeginDistance = hitResults.firstOrNull()?.node?.position?.let {
            (cameraNode.position - it).toVector3().length()
        }
        scrollBeginCameraPosition = cameraNode.position
        scrollBeginSeparation = separation
    }

    override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {
        val beginDistance = scrollBeginDistance
        if (beginDistance == null) {
            super.scrollUpdate(x, y, prevSeparation, currSeparation)
            return
        }

        val movedVector = (cameraNode.position - scrollBeginCameraPosition).toVector3()
        val movedDirection = listOf(
            movedVector.x.sign,
            movedVector.y.sign,
            movedVector.z.sign,
        ).firstOrNull { it != 0f }?.sign ?: 1f

        val ratio = scrollBeginSeparation / currSeparation
        val moved = movedVector.length() * movedDirection
        val target = (beginDistance * ratio)
        val adjust = target - (beginDistance - moved)

        manipulator.scroll(x, y, adjust)
    }
}

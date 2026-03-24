package io.github.sceneview.sample.physicsdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
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
 * Physics Demo — tap anywhere to throw a ball that falls and bounces off the floor.
 *
 * - The floor is a flat CubeNode (thick slab at y = 0).
 * - Each tap spawns a SphereNode at y = 2.5 m above the floor with a small random
 *   horizontal impulse so balls spread out.
 * - Each sphere is driven by a pure-Kotlin PhysicsBody (Euler integration, 9.8 m/s² gravity,
 *   configurable restitution).
 * - Old balls (beyond MAX_BALLS) are removed from the list so the scene stays light.
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

                    // Camera sits slightly above and behind the scene, looking at the origin.
                    val cameraNode = rememberCameraNode(engine) {
                        position = Position(x = 0f, y = 1.5f, z = 4f)
                        lookAt(Position(0f, 0.5f, 0f))
                    }

                    val mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000f
                    }

                    // ── Physics state ─────────────────────────────────────────────────────────

                    /**
                     * Each entry is a SphereNode whose position will be driven by a PhysicsBody.
                     * We store the node directly in a SnapshotStateList so that recomposition
                     * is triggered when balls are added/removed.
                     */
                    val balls = remember { mutableStateListOf<SphereNode>() }

                    // Counter for giving each ball a slightly different horizontal velocity.
                    var ballCount by remember { mutableStateOf(0) }

                    // ── Scene ─────────────────────────────────────────────────────────────────

                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        cameraNode = cameraNode,
                        environment = environment,
                        mainLightNode = mainLightNode,
                        onGestureListener = rememberOnGestureListener(
                            onSingleTapConfirmed = { event, _ ->
                                // Spawn a new ball at every tap.
                                val index = ballCount++

                                // Alternate horizontal direction so balls spread left/right.
                                val sign = if (index % 2 == 0) 1f else -1f
                                val lateralSpeed = 0.3f + (index % 5) * 0.15f

                                val ball = SphereNode(
                                    engine = engine,
                                    radius = BALL_RADIUS,
                                    materialInstance = null
                                ).apply {
                                    position = Position(
                                        x = sign * lateralSpeed * 0.5f,
                                        y = SPAWN_HEIGHT,
                                        z = 0f
                                    )
                                }
                                balls.add(ball)

                                // Keep the scene lean: drop the oldest ball once we hit the cap.
                                if (balls.size > MAX_BALLS) {
                                    balls.removeAt(0)
                                }
                                true
                            }
                        )
                    ) {
                        // ── Floor slab ────────────────────────────────────────────────────────
                        // A thin box centred at y = -FLOOR_HALF_THICKNESS.
                        // The top surface is at y = 0 — matching PhysicsBody.floorY default.
                        CubeNode(
                            size = Size(FLOOR_WIDTH, FLOOR_THICKNESS, FLOOR_DEPTH),
                            position = Position(y = -FLOOR_HALF_THICKNESS),
                            materialInstance = null
                        )

                        // ── Balls ─────────────────────────────────────────────────────────────
                        for ((idx, ball) in balls.withIndex()) {
                            // Alternate horizontal launch velocity per ball.
                            val sign = if (idx % 2 == 0) 1f else -1f
                            val lateralSpeed = 0.3f + (idx % 5) * 0.15f

                            // Attach the ball node into the scene.
                            Node(apply = { addChildNode(ball) })

                            // Drive it with physics.
                            PhysicsNode(
                                node = ball,
                                mass = 1f,
                                restitution = RESTITUTION,
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

                    // ── UI overlay ────────────────────────────────────────────────────────────

                    TopAppBar(
                        title = { Text(text = stringResource(id = R.string.app_name)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    Text(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp),
                        text = "Tap anywhere to throw a ball",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    companion object {
        /** Maximum simultaneous balls in the scene. */
        const val MAX_BALLS = 10

        /** World-space Y at which new balls spawn. */
        const val SPAWN_HEIGHT = 2.5f

        /** Radius of each physics ball, metres. */
        const val BALL_RADIUS = 0.15f

        /** Bounciness: 0 = dead stop, 1 = perfect bounce. */
        const val RESTITUTION = 0.65f

        // Floor geometry
        const val FLOOR_WIDTH = 6f
        const val FLOOR_DEPTH = 6f
        const val FLOOR_THICKNESS = 0.1f
        const val FLOOR_HALF_THICKNESS = FLOOR_THICKNESS / 2f
    }
}

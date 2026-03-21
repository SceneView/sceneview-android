package io.github.sceneview.sample.modelviewer

import android.os.Bundle
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import io.github.sceneview.sample.SceneviewTheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.MILLISECONDS

/**
 * ShowcaseActivity — demonstrates 3D subtly integrated into a normal product page UI.
 *
 * The "product image" slot is a Scene composable. Everything else is standard Material3.
 * This is the core message of SceneView 3.0: 3D is just Compose UI.
 */
class ShowcaseActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // ── Top App Bar ──────────────────────────────────────────────
                        TopAppBar(
                            title = { Text("Product Detail") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        // ── Scrollable content ───────────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                        ) {

                            // ── 3D Viewer ────────────────────────────────────────────
                            // This looks like a product photo area, but it's a live 3D scene.
                            // The user can drag to orbit, pinch to zoom — exactly like any
                            // composable that responds to gesture input.

                            val engine = rememberEngine()
                            val modelLoader = rememberModelLoader(engine)
                            val environmentLoader = rememberEnvironmentLoader(engine)

                            val centerNode = rememberNode(engine)
                            val cameraNode = rememberCameraNode(engine) {
                                position = Position(y = -0.3f, z = 1.8f)
                                lookAt(centerNode)
                                centerNode.addChildNode(this)
                            }

                            val cameraTransition = rememberInfiniteTransition(label = "CameraOrbit")
                            val cameraRotation by cameraTransition.animateRotation(
                                initialValue = Rotation(y = 0.0f),
                                targetValue = Rotation(y = 360.0f),
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 9.seconds.toInt(MILLISECONDS))
                                )
                            )

                            val modelInstance = rememberModelInstance(
                                modelLoader,
                                "models/damaged_helmet.glb"
                            )
                            val environment = rememberEnvironment(environmentLoader) {
                                environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
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
                                    }
                                ) {
                                    Node(apply = { centerNode.addChildNode(this) })
                                    modelInstance?.let { instance ->
                                        ModelNode(
                                            modelInstance = instance,
                                            scaleToUnits = 0.25f
                                        )
                                    }
                                }

                                // "Explore" label overlay
                                Text(
                                    text = "360°  |  Drag to explore",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 12.dp, bottom = 10.dp)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.35f),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // ── Product info ─────────────────────────────────────────
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Spacer(Modifier.height(20.dp))

                                // Name + rating
                                Text(
                                    text = "Damaged Helmet MK-IV",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "★★★★★  (2,418 reviews)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(12.dp))

                                // Price
                                Text(
                                    text = "$129.99",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.height(16.dp))

                                // Description
                                Text(
                                    text = "Battle-tested composite shell with advanced impact " +
                                            "absorption. Features an adjustable visor, " +
                                            "ventilation channels, and a reinforced " +
                                            "attachment system. Shown here as a 3D model — " +
                                            "drag to inspect from any angle.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                                )

                                Spacer(Modifier.height(20.dp))

                                // Color swatches
                                Text(
                                    text = "Color",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        Color(0xFF3B82F6),
                                        Color(0xFFE5E7EB),
                                        Color(0xFF1F2937),
                                        Color(0xFFDC2626)
                                    ).forEachIndexed { idx, color ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .then(
                                                    if (idx == 0) Modifier.padding(0.dp) else Modifier
                                                )
                                        )
                                    }
                                }

                                Spacer(Modifier.height(28.dp))

                                // Action buttons
                                Button(
                                    onClick = { /* Add to cart */ },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Add to Cart",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { /* Launch AR viewer */ },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Try in AR",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                Spacer(Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

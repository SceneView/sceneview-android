@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.explore

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.demo.R
import io.github.sceneview.animation.Transition.animateRotation
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.StrokeCap

private data class ExploreModel(
    val label: String,
    val assetPath: String,
    val scale: Float,
    val cameraDistance: Float = 2.0f
)

private data class ExploreEnvironment(val label: String, val assetPath: String)

private val exploreEnvironments = listOf(
    ExploreEnvironment("Night", "environments/rooftop_night_2k.hdr"),
    ExploreEnvironment("Studio", "environments/studio_2k.hdr"),
    ExploreEnvironment("Warm", "environments/studio_warm_2k.hdr"),
    ExploreEnvironment("Sunset", "environments/sunset_2k.hdr"),
    ExploreEnvironment("Outdoor", "environments/outdoor_cloudy_2k.hdr"),
    ExploreEnvironment("Autumn", "environments/autumn_field_2k.hdr"),
)

private val exploreModels = listOf(
    ExploreModel("Toy Car", "models/toy_car.glb", 0.8f, 3.0f),
    ExploreModel("Red Car", "models/red_car.glb", 1.0f, 3.5f),
    ExploreModel("Game Boy", "models/game_boy_classic.glb", 0.7f, 2.5f),
    ExploreModel("Chair", "models/sheen_chair.glb", 0.6f, 3.5f),
    ExploreModel("Lamp", "models/iridescence_lamp.glb", 0.5f, 3.0f),
    ExploreModel("Geisha Mask", "models/geisha_mask.glb", 1.0f, 2.0f),
    ExploreModel("Space Helmet", "models/space_helmet.glb", 0.8f, 3.0f),
    ExploreModel("Robot Mantis", "models/animated_robot_mantis.glb", 0.7f, 3.5f),
    ExploreModel("Kawaii Meka", "models/animated_kawaii_meka.glb", 0.8f, 3.0f),
    ExploreModel("Carnotaurus", "models/animated_carnotaurus.glb", 0.7f, 3.5f),
    ExploreModel("Dragon", "models/animated_dragon.glb", 0.6f, 4.0f),
    ExploreModel("Butterfly", "models/animated_butterfly.glb", 0.8f, 2.5f),
    ExploreModel("Piano", "models/retro_piano.glb", 0.7f, 3.0f),
)

@Composable
fun ExploreScreen() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var selectedEnvIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    val selectedModel = exploreModels[selectedIndex]
    val selectedEnv = exploreEnvironments[selectedEnvIndex]

    val sceneDescription = stringResource(R.string.cd_3d_scene)
    Box(modifier = Modifier
        .fillMaxSize()
        .semantics { contentDescription = sceneDescription }
    ) {
        // ── Full-screen 3D Scene ───────────────────────────────────────────
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val environmentLoader = rememberEnvironmentLoader(engine)

        val centerNode = rememberNode(engine)

        val cameraNode = rememberCameraNode(engine) {
            position = Float3(y = -0.5f, z = selectedModel.cameraDistance)
            lookAt(centerNode)
            centerNode.addChildNode(this)
        }

        val cameraTransition = rememberInfiniteTransition(label = "CameraRotation")
        val cameraRotation by cameraTransition.animateRotation(
            initialValue = Float3(y = 0.0f),
            targetValue = Float3(y = 360.0f),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12_000)
            )
        )

        val modelInstance = rememberModelInstance(modelLoader, selectedModel.assetPath)
        val environment = key(selectedEnv.assetPath) {
            rememberEnvironment(environmentLoader) {
                environmentLoader.createHDREnvironment(selectedEnv.assetPath)
                ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
            }
        }

        val animationCount by remember(modelInstance) {
            derivedStateOf { modelInstance?.animator?.animationCount ?: 0 }
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
                    node?.apply { scale *= 1.5f }
                }
            )
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = selectedModel.scale,
                    autoAnimate = isPlaying && animationCount > 0,
                    animationLoop = true
                )
            }
        }

        // ── Loading indicator — visible while model loads ─────────────────
        if (modelInstance == null) {
            val loadingDescription = stringResource(R.string.cd_loading_model)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = loadingDescription },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // ── Top gradient overlay with title ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.explore_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.explore_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ── Bottom overlay: model picker + hint ────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animation controls — only shown for animated models
            if (animationCount > 0) {
                val animName = remember(modelInstance, animationCount) {
                    modelInstance?.animator?.getAnimationName(0) ?: ""
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f)
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.explore_pause) else stringResource(R.string.explore_play),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (animName.isNotBlank()) {
                        Text(
                            text = animName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    if (animationCount > 1) {
                        Text(
                            text = "\u00B7 " + stringResource(R.string.explore_animation_count, animationCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Gesture hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .alpha(0.6f)
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Text(
                    text = stringResource(R.string.explore_gesture_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            // Environment picker chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .alpha(0.7f)
                    .padding(bottom = 4.dp)
            ) {
                Icon(
                    Icons.Default.LightMode,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exploreEnvironments.forEachIndexed { index, env ->
                        FilterChip(
                            selected = index == selectedEnvIndex,
                            onClick = { selectedEnvIndex = index },
                            label = {
                                Text(
                                    env.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (index == selectedEnvIndex) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                containerColor = Color.White.copy(alpha = 0.10f),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Model picker chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                exploreModels.forEachIndexed { index, model ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        label = {
                            Text(
                                model.label,
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = Color.White.copy(alpha = 0.15f),
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

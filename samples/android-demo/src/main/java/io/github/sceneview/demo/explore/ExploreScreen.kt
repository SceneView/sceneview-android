@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.demo.R
import io.github.sceneview.demo.sketchfab.SketchfabModel
import io.github.sceneview.demo.sketchfab.SketchfabSearcher
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import kotlinx.coroutines.launch

// ── Data ──────────────────────────────────────────────────────────────────────

private data class ExploreEnvironment(val label: String, val assetPath: String)

private val environments = listOf(
    ExploreEnvironment("Night", "environments/rooftop_night_2k.hdr"),
    ExploreEnvironment("Studio", "environments/studio_2k.hdr"),
    ExploreEnvironment("Warm", "environments/studio_warm_2k.hdr"),
    ExploreEnvironment("Sunset", "environments/sunset_2k.hdr"),
    ExploreEnvironment("Outdoor", "environments/outdoor_cloudy_2k.hdr"),
    ExploreEnvironment("Autumn", "environments/autumn_field_2k.hdr"),
)

private const val CDN = "https://github.com/sceneview/sceneview/releases/download/assets-v1"

/** A few featured models loaded from CDN on demand -- no bundled assets needed. */
private val featuredModels = listOf(
    "Toy Car" to "$CDN/khronos_toy_car.glb",
    "Damaged Helmet" to "$CDN/khronos_damaged_helmet.glb",
    "Space Helmet" to "$CDN/space_helmet.glb",
    "Dragon" to "$CDN/animated_dragon.glb",
    "Porsche 911" to "$CDN/porsche_911.glb",
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ExploreScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SketchfabModel>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Current model: either a featured model (asset path) or Sketchfab download URL
    var currentModelUrl by remember { mutableStateOf(featuredModels[0].second) }
    var currentModelLabel by remember { mutableStateOf(featuredModels[0].first) }

    var selectedEnvIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var showSearch by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val selectedEnv = environments[selectedEnvIndex]

    fun performSearch(query: String) {
        if (query.isBlank()) return
        isSearching = true
        searchError = null
        scope.launch {
            try {
                searchResults = SketchfabSearcher.search(query)
                isSearching = false
            } catch (e: Exception) {
                searchError = e.message ?: "Search failed"
                isSearching = false
            }
        }
    }

    val sceneDescription = stringResource(R.string.cd_3d_scene)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = sceneDescription }
    ) {
        // ── 3D Scene ──────────────────────────────────────────────────────
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val environmentLoader = rememberEnvironmentLoader(engine)

        val centerNode = rememberNode(engine)

        val cameraNode = rememberCameraNode(engine) {
            position = Float3(y = -0.5f, z = 3.0f)
            lookAt(centerNode)
            centerNode.addChildNode(this)
        }

        val cameraTransition = rememberInfiniteTransition(label = "CameraRotation")
        val cameraRotation by cameraTransition.animateRotation(
            initialValue = Float3(y = 0.0f),
            targetValue = Float3(y = 360.0f),
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 12_000))
        )

        val modelInstance = rememberModelInstance(modelLoader, currentModelUrl)
        val environment = key(selectedEnv.assetPath) {
            rememberEnvironment(environmentLoader) {
                environmentLoader.createHDREnvironment(selectedEnv.assetPath)
                    ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
            }
        }

        val animationCount by remember(modelInstance) {
            derivedStateOf { modelInstance?.animator?.animationCount ?: 0 }
        }

        SceneView(
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
                onDoubleTap = { _, node -> node?.apply { scale *= 1.5f } }
            )
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    autoAnimate = isPlaying && animationCount > 0,
                    animationLoop = true
                )
            }
        }

        // ── Loading indicator ─────────────────────────────────────────────
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

        // ── Top gradient with title + search ─────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                            text = currentModelLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                IconButton(
                    onClick = { showSearch = !showSearch },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.18f)
                    )
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.explore_search),
                        tint = Color.White
                    )
                }
            }

            // Search bar
            AnimatedVisibility(visible = showSearch) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.explore_search_hint)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                        },
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                performSearch(searchQuery)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Search results
                    if (searchResults.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults, key = { it.uid }) { model ->
                                SketchfabResultCard(
                                    model = model,
                                    onClick = {
                                        currentModelUrl = model.downloadUrl
                                        currentModelLabel = model.name
                                        showSearch = false
                                    }
                                )
                            }
                        }
                    }

                    if (searchError != null) {
                        Text(
                            text = searchError!!,
                            color = Color(0xFFFF6B6B),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // ── Bottom overlay: controls ──────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animation play/pause
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
                            contentDescription = if (isPlaying) stringResource(R.string.explore_pause)
                            else stringResource(R.string.explore_play),
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
                            text = "\u00B7 ${stringResource(R.string.explore_animation_count, animationCount)}",
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

            // Environment chips
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
                    environments.forEachIndexed { index, env ->
                        FilterChip(
                            selected = index == selectedEnvIndex,
                            onClick = { selectedEnvIndex = index },
                            label = {
                                Text(
                                    env.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (index == selectedEnvIndex) FontWeight.Bold
                                    else FontWeight.Normal
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

            // Featured model chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                featuredModels.forEach { (label, path) ->
                    FilterChip(
                        selected = currentModelUrl == path,
                        onClick = {
                            currentModelUrl = path
                            currentModelLabel = label
                        },
                        label = {
                            Text(
                                label,
                                fontWeight = if (currentModelUrl == path) FontWeight.Bold
                                else FontWeight.Normal
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

@Composable
private fun SketchfabResultCard(
    model: SketchfabModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = model.authorName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (model.isAnimated) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Animated",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF34A853)
                )
            }
        }
    }
}

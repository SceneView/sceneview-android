@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.demo.R
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
import kotlinx.coroutines.delay

private const val CDN = "https://github.com/sceneview/sceneview/releases/download/assets-v1"

enum class ModelCategory(val label: String) {
    Vehicles("Vehicles"),
    Creatures("Creatures"),
    Objects("Objects"),
    Scenes("Scenes")
}

private data class ExploreModel(
    val label: String,
    val assetPath: String,
    val scale: Float,
    val cameraDistance: Float = 2.0f,
    val category: ModelCategory
)

private data class ExploreEnvironment(
    val label: String,
    val assetPath: String,
    val color: Color
)

private val exploreEnvironments = listOf(
    ExploreEnvironment("Night",   "environments/rooftop_night_2k.hdr",  Color(0xFF1A1A2E)),
    ExploreEnvironment("Studio",  "environments/studio_2k.hdr",          Color(0xFF4A4A4A)),
    ExploreEnvironment("Warm",    "environments/studio_warm_2k.hdr",     Color(0xFFB5651D)),
    ExploreEnvironment("Sunset",  "environments/sunset_2k.hdr",          Color(0xFFFF6B35)),
    ExploreEnvironment("Outdoor", "environments/outdoor_cloudy_2k.hdr",  Color(0xFF87CEEB)),
    ExploreEnvironment("Autumn",  "environments/autumn_field_2k.hdr",    Color(0xFFD2691E)),
)

private val exploreModels = listOf(
    // Featured (bundled — always available offline)
    ExploreModel("Duck",         "models/khronos_duck.glb",       1.0f, 2.0f, ModelCategory.Objects),
    ExploreModel("Fox",          "models/khronos_fox.glb",        0.8f, 2.5f, ModelCategory.Creatures),
    ExploreModel("Toon Cat",     "models/toon_cat.glb",           0.8f, 2.5f, ModelCategory.Creatures),
    ExploreModel("Shiba",        "models/shiba.glb",              0.8f, 2.5f, ModelCategory.Creatures),
    // Vehicles
    ExploreModel("Toy Car",      "$CDN/toy_car.glb",              0.8f, 3.0f, ModelCategory.Vehicles),
    ExploreModel("Red Car",      "$CDN/red_car.glb",              1.0f, 3.5f, ModelCategory.Vehicles),
    ExploreModel("Porsche 911",  "$CDN/porsche_911.glb",          0.6f, 4.0f, ModelCategory.Vehicles),
    ExploreModel("Porsche Turbo","$CDN/porsche_911_turbo.glb",    0.5f, 4.0f, ModelCategory.Vehicles),
    ExploreModel("Ferrari F40",  "$CDN/ferrari_f40.glb",          0.6f, 4.0f, ModelCategory.Vehicles),
    ExploreModel("Lamborghini",  "$CDN/lamborghini_countach.glb", 0.5f, 4.0f, ModelCategory.Vehicles),
    ExploreModel("Shelby Cobra", "$CDN/shelby_cobra.glb",         0.6f, 4.0f, ModelCategory.Vehicles),
    ExploreModel("Audi TT",      "$CDN/audi_tt.glb",              0.7f, 3.5f, ModelCategory.Vehicles),
    ExploreModel("BMW M3 E30",   "$CDN/bmw_m3_e30.glb",           0.6f, 4.0f, ModelCategory.Vehicles),
    ExploreModel("Mercedes AMG", "$CDN/mercedes_a45_amg.glb",     0.5f, 4.0f, ModelCategory.Vehicles),
    ExploreModel("Fiat Punto",   "$CDN/fiat_punto.glb",           0.7f, 3.5f, ModelCategory.Vehicles),
    ExploreModel("Cybertruck",   "$CDN/tesla_cybertruck.glb",     0.6f, 4.0f, ModelCategory.Vehicles),
    ExploreModel("Cyberpunk Car","$CDN/cyberpunk_car.glb",        0.8f, 3.5f, ModelCategory.Vehicles),
    ExploreModel("Hovercar",     "$CDN/cyberpunk_hovercar.glb",   0.6f, 4.0f, ModelCategory.Vehicles),
    // Creatures
    ExploreModel("Dragon",       "$CDN/animated_dragon.glb",          0.6f, 4.0f, ModelCategory.Creatures),
    ExploreModel("Black Dragon", "$CDN/black_dragon.glb",             0.5f, 4.5f, ModelCategory.Creatures),
    ExploreModel("Crystal Dragon","$CDN/khronos_dragon_attenuation.glb",0.6f,3.0f,ModelCategory.Creatures),
    ExploreModel("Carnotaurus",  "$CDN/animated_carnotaurus.glb",     0.7f, 3.5f, ModelCategory.Creatures),
    ExploreModel("Robot Mantis", "$CDN/animated_robot_mantis.glb",    0.7f, 3.5f, ModelCategory.Creatures),
    ExploreModel("Kawaii Meka",  "$CDN/animated_kawaii_meka.glb",     0.8f, 3.0f, ModelCategory.Creatures),
    ExploreModel("Butterfly",    "$CDN/animated_butterfly.glb",       0.8f, 2.5f, ModelCategory.Creatures),
    ExploreModel("Phoenix",      "$CDN/phoenix_bird.glb",             0.8f, 2.5f, ModelCategory.Creatures),
    ExploreModel("Cyber Guy",    "$CDN/cyberpunk_character.glb",      0.7f, 3.0f, ModelCategory.Creatures),
    ExploreModel("Mosquito",     "$CDN/mosquito_amber.glb",           1.0f, 2.0f, ModelCategory.Creatures),
    // Objects
    ExploreModel("Damaged Helmet","$CDN/khronos_damaged_helmet.glb",  0.8f, 2.5f, ModelCategory.Objects),
    ExploreModel("Space Helmet",  "$CDN/space_helmet.glb",            0.8f, 3.0f, ModelCategory.Objects),
    ExploreModel("Game Boy",      "$CDN/game_boy_classic.glb",        0.7f, 2.5f, ModelCategory.Objects),
    ExploreModel("PS5 Controller","$CDN/ps5_dualsense.glb",           0.8f, 2.5f, ModelCategory.Objects),
    ExploreModel("Switch",        "$CDN/nintendo_switch.glb",         0.8f, 2.5f, ModelCategory.Objects),
    ExploreModel("Nike Jordan",   "$CDN/nike_air_jordan.glb",         0.8f, 2.0f, ModelCategory.Objects),
    ExploreModel("Chair",         "$CDN/sheen_chair.glb",             0.6f, 3.5f, ModelCategory.Objects),
    ExploreModel("Lamp",          "$CDN/iridescence_lamp.glb",        0.5f, 3.0f, ModelCategory.Objects),
    ExploreModel("Piano",         "$CDN/retro_piano.glb",             0.7f, 3.0f, ModelCategory.Objects),
    ExploreModel("Geisha Mask",   "$CDN/geisha_mask.glb",             1.0f, 2.0f, ModelCategory.Objects),
    ExploreModel("Fantasy Book",  "$CDN/fantasy_book.glb",            0.7f, 2.5f, ModelCategory.Objects),
    ExploreModel("Water Bottle",  "$CDN/khronos_water_bottle.glb",    1.0f, 2.0f, ModelCategory.Objects),
    ExploreModel("Antique Camera","$CDN/khronos_antique_camera.glb",  0.7f, 2.5f, ModelCategory.Objects),
    ExploreModel("Corset",        "$CDN/khronos_corset.glb",          0.8f, 2.5f, ModelCategory.Objects),
    // Hero realistic (new — product showcase)
    ExploreModel("Moto Helmet",   "$CDN/moto_helmet.glb",             0.8f, 2.5f, ModelCategory.Objects),
    ExploreModel("DJI Mavic 3",   "$CDN/dji_mavic_3.glb",             0.7f, 3.0f, ModelCategory.Objects),
    ExploreModel("JBL Headphones","$CDN/jbl_tour_one_m3.glb",         0.8f, 2.5f, ModelCategory.Objects),
    ExploreModel("Canon EOS RP",  "$CDN/canon_eos_rp.glb",            0.8f, 2.5f, ModelCategory.Objects),
    ExploreModel("Rolex Watch",   "$CDN/rolex_watch.glb",             1.5f, 2.0f, ModelCategory.Objects),
    ExploreModel("Sneaker",       "$CDN/sneaker_vibe.glb",            0.6f, 2.5f, ModelCategory.Objects),
    ExploreModel("Guitar",        "$CDN/photorealistic_guitar.glb",   0.5f, 3.5f, ModelCategory.Objects),
    ExploreModel("Backpack",      "$CDN/school_backpack.glb",         0.6f, 3.0f, ModelCategory.Objects),
    // Scenes
    ExploreModel("Ship in Clouds","$CDN/ship_in_clouds.glb",          0.5f, 4.0f, ModelCategory.Scenes),
    ExploreModel("Earthquake",    "$CDN/earthquake_california.glb",   0.4f, 5.0f, ModelCategory.Scenes),
)

@Composable
fun ExploreScreen() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var selectedEnvIndex by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<ModelCategory?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isTimedOut by remember { mutableStateOf(false) }
    var retryKey by remember { mutableLongStateOf(0L) }

    val selectedModel = exploreModels[selectedIndex]
    val selectedEnv = exploreEnvironments[selectedEnvIndex]
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    val filteredModels = remember(selectedCategory) {
        if (selectedCategory == null) exploreModels
        else exploreModels.filter { it.category == selectedCategory }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 160.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        sheetContent = {
            ModelBrowserSheet(
                models = filteredModels,
                allModels = exploreModels,
                selectedIndex = selectedIndex,
                selectedCategory = selectedCategory,
                selectedModel = selectedModel,
                isExpanded = scaffoldState.bottomSheetState.currentValue == androidx.compose.material3.SheetValue.Expanded,
                onToggleExpand = {
                    scope.launch {
                        if (scaffoldState.bottomSheetState.currentValue == androidx.compose.material3.SheetValue.Expanded) {
                            scaffoldState.bottomSheetState.partialExpand()
                        } else {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                },
                onModelSelected = { model ->
                    selectedIndex = exploreModels.indexOf(model)
                    scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                },
                onCategorySelected = { cat -> selectedCategory = cat }
            )
        }
    ) { sheetPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = sheetPadding.calculateBottomPadding())
        ) {
            // ── Full-screen 3D Scene ──────────────────────────────────────
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
                animationSpec = infiniteRepeatable(animation = tween(12_000))
            )

            val modelInstance = key(selectedIndex, retryKey) {
                rememberModelInstance(modelLoader, selectedModel.assetPath)
            }

            val environment = key(selectedEnv.assetPath) {
                rememberEnvironment(environmentLoader) {
                    environmentLoader.createHDREnvironment(selectedEnv.assetPath)
                        ?: checkNotNull(
                            environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")
                        ) { "Bundled fallback HDR environments/rooftop_night_2k.hdr is missing or unreadable" }
                }
            }

            val animationCount by remember(modelInstance) {
                derivedStateOf { modelInstance?.animator?.animationCount ?: 0 }
            }

            LaunchedEffect(selectedIndex, retryKey) {
                isTimedOut = false
                delay(20_000)
                isTimedOut = true
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
                        scaleToUnits = selectedModel.scale,
                        autoAnimate = isPlaying && animationCount > 0,
                        animationLoop = true
                    )
                }
            }

            // ── Top overlay: title + env picker ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.explore_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = stringResource(R.string.explore_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Environment color picker
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        exploreEnvironments.forEachIndexed { idx, env ->
                            val selected = idx == selectedEnvIndex
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.2f else 1f,
                                label = "envScale"
                            )
                            Box(
                                modifier = Modifier
                                    .scale(scale)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(env.color)
                                    .then(
                                        if (selected) Modifier.border(
                                            2.dp, Color.White, CircleShape
                                        ) else Modifier
                                    )
                                    .clickable { selectedEnvIndex = idx }
                            )
                        }
                    }
                }
            }

            // ── Bottom overlay: animation controls + gesture hint ─────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animation controls
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
                                contentDescription = null,
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
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.alpha(0.55f)
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Text(
                        text = stringResource(R.string.explore_gesture_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // ── Loading / Error overlay ───────────────────────────────────
            AnimatedVisibility(
                visible = modelInstance == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTimedOut) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                "Could not load model",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Check your internet connection",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { retryKey = System.currentTimeMillis() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                color = Color.White.copy(alpha = 0.8f),
                                strokeWidth = 3.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Loading ${selectedModel.label}…",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelBrowserSheet(
    models: List<ExploreModel>,
    allModels: List<ExploreModel>,
    selectedIndex: Int,
    selectedCategory: ModelCategory?,
    selectedModel: ExploreModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onModelSelected: (ExploreModel) -> Unit,
    onCategorySelected: (ModelCategory?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Peek content: current model + category filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Current model info row — tap to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedModel.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedModel.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${allModels.size} models",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse" else "Browse models",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("All") },
                    shape = RoundedCornerShape(50)
                )
                ModelCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategorySelected(cat) },
                        label = { Text(cat.label) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Model grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            items(models) { model ->
                val isSelected = allModels.indexOf(model) == selectedIndex
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 0.93f else 1f,
                    label = "modelScale"
                )
                Surface(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onModelSelected(model) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = model.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

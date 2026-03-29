@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.samples

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.LightManager
import com.google.android.filament.View.AntiAliasing
import com.google.android.filament.View.Dithering
import com.google.android.filament.View.QualityLevel
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.demo.R
import io.github.sceneview.math.Position
import io.github.sceneview.node.DynamicSkyNode
import io.github.sceneview.node.FogNode
import io.github.sceneview.node.PhysicsNode
import io.github.sceneview.node.SphereNode as SphereNodeImpl
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView

/**
 * Represents a feature demo in the Samples grid.
 */
private data class SampleDemo(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val category: String,
    val accentColor: Color,
    val content: (@Composable () -> Unit)? = null
)

// ────────────────────────────────────────────────────────────────────────────
// Demo registry — assembled at composition time to use string resources
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberSampleDemos(): List<SampleDemo> = remember {
    listOf(
        // 3D Features
        SampleDemo(
            title = "Model Viewer",
            subtitle = "Load and render glTF/GLB 3D models with orbit camera",
            icon = Icons.Default.ViewInAr,
            category = "3D",
            accentColor = Color(0xFF1A73E8),
            content = { ModelViewerDemo() }
        ),
        SampleDemo(
            title = "Geometry Nodes",
            subtitle = "Procedural cube, sphere, cylinder, and plane geometry",
            icon = Icons.Default.CropSquare,
            category = "3D",
            accentColor = Color(0xFF34A853),
            content = { GeometryDemo() }
        ),
        SampleDemo(
            title = "Animations",
            subtitle = "Play, pause, and cycle through model animations",
            icon = Icons.Default.Animation,
            category = "3D",
            accentColor = Color(0xFFEA4335),
            content = { AnimationDemo() }
        ),
        SampleDemo(
            title = "Animation Control",
            subtitle = "Switch animations by name, control speed and looping",
            icon = Icons.Default.SlowMotionVideo,
            category = "3D",
            accentColor = Color(0xFFAB47BC),
            content = { AnimationControlDemo() }
        ),
        SampleDemo(
            title = "Dynamic Sky",
            subtitle = "Time-of-day sun position with atmospheric scattering",
            icon = Icons.Default.WbSunny,
            category = "Effects",
            accentColor = Color(0xFFFBBC04),
            content = { DynamicSkyDemo() }
        ),
        SampleDemo(
            title = "Lighting",
            subtitle = "Point, directional, and spot lights with shadows",
            icon = Icons.Default.Brightness7,
            category = "3D",
            accentColor = Color(0xFFFF6D00),
            content = { LightingDemo() }
        ),
        SampleDemo(
            title = "Dynamic Lighting",
            subtitle = "Adjust light color, intensity, and type in real time",
            icon = Icons.Default.Palette,
            category = "3D",
            accentColor = Color(0xFFD32F2F),
            content = { DynamicLightingDemo() }
        ),
        SampleDemo(
            title = "Camera Controls",
            subtitle = "Orbit, pan, zoom camera manipulator",
            icon = Icons.Default.PhotoCamera,
            category = "3D",
            accentColor = Color(0xFF9C27B0),
            content = { CameraControlsDemo() }
        ),
        SampleDemo(
            title = "Multi-Model Scene",
            subtitle = "Compose multiple models into a single styled scene",
            icon = Icons.Default.Layers,
            category = "3D",
            accentColor = Color(0xFF0097A7),
            content = { MultiModelDemo() }
        ),
        SampleDemo(
            title = "Post-Processing",
            subtitle = "Bloom, SSAO, FXAA, tone mapping, vignette",
            icon = Icons.Default.AutoAwesome,
            category = "Effects",
            accentColor = Color(0xFFE91E63),
            content = { PostProcessingDemo() }
        ),
        SampleDemo(
            title = "Fog",
            subtitle = "Height-based atmospheric volumetric fog",
            icon = Icons.Default.Cloud,
            category = "Effects",
            accentColor = Color(0xFF607D8B),
            content = { FogDemo() }
        ),
        SampleDemo(
            title = "Environment Gallery",
            subtitle = "Compare HDR environments on the same model side by side",
            icon = Icons.Default.Landscape,
            category = "Effects",
            accentColor = Color(0xFF558B2F),
            content = { EnvironmentGalleryDemo() }
        ),
        SampleDemo(
            title = "Text Labels",
            subtitle = "3D text rendering with billboard facing",
            icon = Icons.Default.TextFields,
            category = "Content",
            accentColor = Color(0xFF00BCD4),
            content = { TextLabelsDemo() }
        ),
        SampleDemo(
            title = "Line Paths",
            subtitle = "3D polylines and Lissajous curves",
            icon = Icons.Default.LinearScale,
            category = "Content",
            accentColor = Color(0xFF4CAF50),
            content = { LinePathsDemo() }
        ),
        SampleDemo(
            title = "Gesture Editing",
            subtitle = "Drag, rotate, and scale models with touch gestures",
            icon = Icons.Default.Gesture,
            category = "Interactive",
            accentColor = Color(0xFF5C6BC0),
            content = { GestureEditingDemo() }
        ),
        SampleDemo(
            title = "Physics",
            subtitle = "Rigid body simulation with bouncing balls",
            icon = Icons.Default.Science,
            category = "Advanced",
            accentColor = Color(0xFFFF5722),
            content = { PhysicsDemo() }
        ),
        SampleDemo(
            title = "Image Detection",
            subtitle = "Real-world image recognition with AR overlay",
            icon = Icons.Default.Image,
            category = "AR",
            accentColor = Color(0xFF3F51B5),
            content = { ImageDetectionDemo() }
        ),
        SampleDemo(
            title = "Reflection Probes",
            subtitle = "Local cubemap reflections for realistic materials",
            icon = Icons.Default.Tune,
            category = "Advanced",
            accentColor = Color(0xFF795548),
            content = { ReflectionProbesDemo() }
        ),
        SampleDemo(
            title = "glTF Cameras",
            subtitle = "Import and switch between cameras from glTF files",
            icon = Icons.Default.CameraAlt,
            category = "3D",
            accentColor = Color(0xFF009688),
            content = { GltfCamerasDemo() }
        ),
    )
}

@Composable
fun SamplesScreen() {
    val sampleDemos = rememberSampleDemos()
    var selectedDemo by remember { mutableStateOf<SampleDemo?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main grid
        SamplesGrid(
            demos = sampleDemos,
            onDemoClick = { demo ->
                if (demo.content != null) {
                    selectedDemo = demo
                }
            }
        )

        // Full-screen demo overlay
        AnimatedVisibility(
            visible = selectedDemo != null,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            selectedDemo?.let { demo ->
                DemoOverlay(
                    demo = demo,
                    onBack = { selectedDemo = null }
                )
            }
        }
    }
}

@Composable
private fun SamplesGrid(
    demos: List<SampleDemo>,
    onDemoClick: (SampleDemo) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.samples_title),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.samples_feature_count, demos.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category headers and items
            val categories = demos.groupBy { it.category }
            categories.forEach { (category, categoryDemos) ->
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(categoryDemos, key = { it.title }) { demo ->
                    SampleCard(
                        demo = demo,
                        onClick = { onDemoClick(demo) }
                    )
                }
            }

            // Bottom spacer
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SampleCard(
    demo: SampleDemo,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    val isLive = demo.content != null

    Card(
        onClick = onClick,
        enabled = isLive,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .semantics { contentDescription = "${demo.title}: ${demo.subtitle}" },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLive) 2.dp else 0.dp,
            pressedElevation = 6.dp
        )
    ) {
        Column {
            // Icon area with gradient accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                demo.accentColor.copy(alpha = 0.15f),
                                demo.accentColor.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = demo.accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = demo.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(28.dp),
                        tint = demo.accentColor
                    )
                }

                // Badge: LIVE for interactive demos, SOON for upcoming
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLive) demo.accentColor.copy(alpha = 0.9f)
                    else Color.Gray.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (isLive) stringResource(R.string.samples_badge_live) else stringResource(R.string.samples_badge_soon),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Text area
            Column(
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                )
            ) {
                Text(
                    text = demo.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = demo.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DemoOverlay(
    demo: SampleDemo,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Demo content fills the screen
        demo.content?.invoke()

        // Back button overlay
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back_button),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    demo.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Shared loading indicator composable
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier.size(36.dp),
        color = Color.White.copy(alpha = 0.7f),
        strokeWidth = 3.dp,
        strokeCap = StrokeCap.Round
    )
}

// ────────────────────────────────────────────────────────────────────────────
// Individual demo composables
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModelViewerDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 2.5f, y = 0.2f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_warm_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/toy_car.glb")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Model viewer 3D scene" }
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 2.5f, y = 0.2f),
                targetPosition = Position(0f, 0f, 0f)
            ),
            environment = environment
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    autoAnimate = true,
                    animationLoop = true
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun GeometryDemo() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 5.0f, y = 1.0f)
        lookAt(Float3(0f, 0.5f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = Position(z = 5.0f, y = 1.0f),
            targetPosition = Position(0f, 0.5f, 0f)
        ),
        environment = environment
    ) {
        val blue = remember(materialLoader) {
            materialLoader.createColorInstance(Color(0xFF1A73E8))
        }
        val red = remember(materialLoader) {
            materialLoader.createColorInstance(Color(0xFFEA4335))
        }
        val green = remember(materialLoader) {
            materialLoader.createColorInstance(Color(0xFF34A853))
        }
        val yellow = remember(materialLoader) {
            materialLoader.createColorInstance(Color(0xFFFBBC04))
        }

        CubeNode(
            size = Float3(0.8f),
            center = Float3(-2.0f, 0.4f, 0f),
            materialInstance = blue
        )
        SphereNode(
            radius = 0.5f,
            center = Float3(-0.6f, 0.5f, 0f),
            materialInstance = red
        )
        CylinderNode(
            radius = 0.3f,
            height = 1f,
            center = Float3(0.8f, 0.5f, 0f),
            materialInstance = green
        )
        PlaneNode(
            size = Float3(0.9f, 0.9f, 1f),
            center = Position(2.2f, 0.5f, 0f),
            materialInstance = yellow
        )
        // Ground plane
        val groundMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color(0xFF333333))
        }
        PlaneNode(
            size = Float3(8f, 8f, 1f),
            center = Position(0f, 0f, 0f),
            materialInstance = groundMat
        )
    }
}

@Composable
private fun AnimationDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 3.5f, y = 0.5f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/sunset_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/animated_robot_mantis.glb")

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 3.5f, y = 0.5f),
                targetPosition = Position(0f, 0f, 0f)
            ),
            environment = environment
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.7f,
                    autoAnimate = true,
                    animationLoop = true
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun AnimationControlDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 3.5f, y = 0.5f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_warm_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }

    // Fox has multiple animations typically
    val modelInstance = rememberModelInstance(modelLoader, "models/fox.glb")

    var animationSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(true) }
    var selectedAnimIndex by remember { mutableIntStateOf(0) }

    val animationCount by remember(modelInstance) {
        derivedStateOf { modelInstance?.animator?.animationCount ?: 0 }
    }
    val animationNames by remember(modelInstance, animationCount) {
        derivedStateOf {
            val animator = modelInstance?.animator ?: return@derivedStateOf emptyList()
            (0 until animationCount).map { i ->
                animator.getAnimationName(i).ifBlank { "Animation $i" }
            }
        }
    }

    val currentAnimName = animationNames.getOrNull(selectedAnimIndex)

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 3.5f, y = 0.5f),
                targetPosition = Position(0f, 0f, 0f)
            ),
            environment = environment
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.0f,
                    autoAnimate = false,
                    animationName = currentAnimName,
                    animationLoop = isLooping,
                    animationSpeed = animationSpeed
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Controls overlay
        if (animationCount > 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 48.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animation name chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    animationNames.forEachIndexed { index, name ->
                        FilterChip(
                            selected = index == selectedAnimIndex,
                            onClick = { selectedAnimIndex = index },
                            label = {
                                Text(
                                    name,
                                    fontWeight = if (index == selectedAnimIndex) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFAB47BC),
                                selectedLabelColor = Color.White,
                                containerColor = Color.Black.copy(alpha = 0.5f),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // Speed slider
                Text(
                    text = stringResource(R.string.control_animation_speed, animationSpeed),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = animationSpeed,
                    onValueChange = { animationSpeed = it },
                    valueRange = 0.1f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFAB47BC),
                        activeTrackColor = Color(0xFFAB47BC)
                    )
                )

                // Loop toggle
                FilterChip(
                    selected = isLooping,
                    onClick = { isLooping = !isLooping },
                    label = {
                        Text(
                            stringResource(R.string.control_animation_loop),
                            fontWeight = if (isLooping) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFAB47BC),
                        selectedLabelColor = Color.White,
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun DynamicSkyDemo() {
    val engine = rememberEngine()
    val view = rememberView(engine)
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 3.5f, y = 0.5f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/outdoor_cloudy_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/space_helmet.glb")

    var timeOfDay by remember { mutableFloatStateOf(16f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = null,
            environment = environment
        ) {
            DynamicSkyNode(
                timeOfDay = timeOfDay,
                turbidity = 3f
            )
            FogNode(
                view = view,
                density = 0.03f,
                height = 2f,
                color = Color(0xFFCCDDFF),
                enabled = true
            )
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    autoAnimate = false
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
        // Time of day slider
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Time of Day: ${timeOfDay.toInt()}:00",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = timeOfDay,
                onValueChange = { timeOfDay = it },
                valueRange = 5f..21f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFBBC04),
                    activeTrackColor = Color(0xFFFBBC04)
                )
            )
        }
    }
}

@Composable
private fun LightingDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 4.0f, y = 1.5f)
        lookAt(Float3(0f, 0.3f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/seal_statuette.glb")

    var sunIntensity by remember { mutableFloatStateOf(80000f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            environment = environment
        ) {
            // Sun light
            LightNode(
                type = LightManager.Type.SUN,
                apply = {
                    intensity(sunIntensity)
                    direction(0f, -1f, -0.5f)
                    castShadows(true)
                    color(1f, 0.95f, 0.85f)
                }
            )
            // Colored point light — orange accent
            LightNode(
                type = LightManager.Type.POINT,
                apply = {
                    intensity(50000f)
                    color(1f, 0.5f, 0.1f)
                    falloff(5f)
                },
                nodeApply = { position = Position(x = -1.5f, y = 1.5f, z = 1f) }
            )
            // Colored point light — blue accent
            LightNode(
                type = LightManager.Type.POINT,
                apply = {
                    intensity(50000f)
                    color(0.2f, 0.4f, 1f)
                    falloff(5f)
                },
                nodeApply = { position = Position(x = 1.5f, y = 1.5f, z = 1f) }
            )
            // Floor plane
            val floorMat = remember(materialLoader) {
                materialLoader.createColorInstance(Color(0xFF333333))
            }
            PlaneNode(
                size = Float3(6f, 6f, 1f),
                center = Position(0f, 0f, 0f),
                materialInstance = floorMat
            )
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.6f,
                    autoAnimate = true,
                    animationLoop = true
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
        // Sun intensity slider
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.control_sun_intensity, sunIntensity.toInt()),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = sunIntensity,
                onValueChange = { sunIntensity = it },
                valueRange = 0f..200000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF6D00),
                    activeTrackColor = Color(0xFFFF6D00)
                )
            )
        }
    }
}

@Composable
private fun DynamicLightingDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 4.0f, y = 1.5f)
        lookAt(Float3(0f, 0.3f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/damaged_helmet.glb")

    data class LightColor(val name: String, val r: Float, val g: Float, val b: Float, val chipColor: Color)
    val lightColors = remember {
        listOf(
            LightColor("Warm White", 1f, 0.95f, 0.85f, Color(0xFFFFF3E0)),
            LightColor("Cool White", 0.85f, 0.92f, 1f, Color(0xFFE3F2FD)),
            LightColor("Red", 1f, 0.2f, 0.1f, Color(0xFFE53935)),
            LightColor("Green", 0.2f, 1f, 0.3f, Color(0xFF43A047)),
            LightColor("Blue", 0.2f, 0.4f, 1f, Color(0xFF1E88E5)),
            LightColor("Purple", 0.7f, 0.2f, 1f, Color(0xFF8E24AA)),
            LightColor("Orange", 1f, 0.5f, 0.1f, Color(0xFFFF6D00)),
        )
    }

    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var intensity by remember { mutableFloatStateOf(100000f) }
    val selectedColor = lightColors[selectedColorIndex]

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 4.0f, y = 1.5f),
                targetPosition = Position(0f, 0.3f, 0f)
            ),
            environment = environment
        ) {
            LightNode(
                type = LightManager.Type.SUN,
                apply = {
                    intensity(intensity)
                    direction(0f, -1f, -0.5f)
                    castShadows(true)
                    color(selectedColor.r, selectedColor.g, selectedColor.b)
                }
            )
            LightNode(
                type = LightManager.Type.POINT,
                apply = {
                    intensity(intensity * 0.5f)
                    color(selectedColor.r, selectedColor.g, selectedColor.b)
                    falloff(6f)
                },
                nodeApply = { position = Position(x = -2f, y = 2f, z = 2f) }
            )
            val floorMat = remember(materialLoader) {
                materialLoader.createColorInstance(Color(0xFF222222))
            }
            PlaneNode(
                size = Float3(8f, 8f, 1f),
                center = Position(0f, -0.5f, 0f),
                materialInstance = floorMat
            )
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.0f,
                    autoAnimate = false
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 48.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.control_light_color),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lightColors.forEachIndexed { index, color ->
                    FilterChip(
                        selected = index == selectedColorIndex,
                        onClick = { selectedColorIndex = index },
                        label = {
                            Text(
                                color.name,
                                fontWeight = if (index == selectedColorIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.chipColor,
                            selectedLabelColor = if (color.chipColor.luminance() > 0.5f) Color.Black else Color.White,
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            labelColor = Color.White
                        )
                    )
                }
            }
            Text(
                text = stringResource(R.string.control_light_intensity, intensity.toInt()),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = intensity,
                onValueChange = { intensity = it },
                valueRange = 5000f..250000f,
                colors = SliderDefaults.colors(
                    thumbColor = selectedColor.chipColor,
                    activeTrackColor = selectedColor.chipColor
                )
            )
        }
    }
}

// Helper extension for color luminance
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

@Composable
private fun CameraControlsDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 4.0f, y = 1.0f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_warm_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = Position(z = 4.0f, y = 1.0f),
        targetPosition = Position(0f, 0f, 0f)
    )
    val modelInstance = rememberModelInstance(modelLoader, "models/iridescence_lamp.glb")

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = cameraManipulator,
            environment = environment
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.2f,
                    autoAnimate = true,
                    animationLoop = true
                )
            }
            // Floor reference grid of cubes
            val gridMat = remember(materialLoader) {
                materialLoader.createColorInstance(Color(0xFF444444))
            }
            for (x in -2..2) {
                for (z in -2..2) {
                    CubeNode(
                        size = Float3(0.08f),
                        center = Position(x.toFloat(), -0.5f, z.toFloat()),
                        materialInstance = gridMat
                    )
                }
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
        // Instruction overlay
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.control_orbit_instruction), color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.control_pan_instruction), color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.control_zoom_instruction), color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.control_double_tap_reset), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MultiModelDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 6.0f, y = 2.0f)
        lookAt(Float3(0f, 0.3f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/comfy_cafe_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }

    // Load multiple models simultaneously
    val sofa = rememberModelInstance(modelLoader, "models/leather_sofa.glb")
    val lamp = rememberModelInstance(modelLoader, "models/barn_lamp.glb")
    val plant = rememberModelInstance(modelLoader, "models/plant.glb")
    val vase = rememberModelInstance(modelLoader, "models/glass_vase_flowers.glb")

    val allLoaded = sofa != null && lamp != null && plant != null && vase != null

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 6.0f, y = 2.0f),
                targetPosition = Position(0f, 0.3f, 0f)
            ),
            environment = environment
        ) {
            LightNode(
                type = LightManager.Type.SUN,
                apply = {
                    intensity(80000f)
                    direction(-0.5f, -1f, -0.5f)
                    castShadows(true)
                    color(1f, 0.95f, 0.9f)
                }
            )
            // Floor
            val floorMat = remember(materialLoader) {
                materialLoader.createColorInstance(Color(0xFF8B7355))
            }
            PlaneNode(
                size = Float3(10f, 10f, 1f),
                center = Position(0f, 0f, 0f),
                materialInstance = floorMat
            )
            sofa?.let {
                ModelNode(
                    modelInstance = it,
                    scaleToUnits = 1.2f,
                    position = Position(x = 0f, y = 0f, z = -1f)
                )
            }
            lamp?.let {
                ModelNode(
                    modelInstance = it,
                    scaleToUnits = 0.8f,
                    position = Position(x = -1.8f, y = 0f, z = -0.5f)
                )
            }
            plant?.let {
                ModelNode(
                    modelInstance = it,
                    scaleToUnits = 0.6f,
                    position = Position(x = 1.8f, y = 0f, z = -0.5f)
                )
            }
            vase?.let {
                ModelNode(
                    modelInstance = it,
                    scaleToUnits = 0.3f,
                    position = Position(x = 0.5f, y = 0.45f, z = 0.5f)
                )
            }
        }
        if (!allLoaded) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LoadingIndicator()
                Text(
                    "Loading ${listOf(sofa, lamp, plant, vase).count { it != null }}/4 models",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        // Scene description
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Living Room Scene", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("4 models composed together", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FogDemo() {
    val engine = rememberEngine()
    val view = rememberView(engine)
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 4.0f, y = 0.8f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/outdoor_cloudy_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/geisha_mask.glb")

    var fogDensity by remember { mutableFloatStateOf(0.05f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = null,
            environment = environment
        ) {
            DynamicSkyNode(
                timeOfDay = 7f,
                turbidity = 5f
            )
            FogNode(
                view = view,
                density = fogDensity,
                height = 3f,
                color = Color(0xFFBBCCDD),
                enabled = true
            )
            // Ground plane
            val groundMat = remember(materialLoader) {
                materialLoader.createColorInstance(Color(0xFF556655))
            }
            PlaneNode(
                size = Float3(10f, 10f, 1f),
                center = Position(0f, -0.5f, 0f),
                materialInstance = groundMat
            )
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    autoAnimate = true,
                    animationLoop = true
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
        // Fog density slider
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.control_fog_density, "%.3f".format(fogDensity)),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = fogDensity,
                onValueChange = { fogDensity = it },
                valueRange = 0f..0.15f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF607D8B),
                    activeTrackColor = Color(0xFF607D8B)
                )
            )
        }
    }
}

@Composable
private fun EnvironmentGalleryDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val environments = remember {
        listOf(
            "Night" to "environments/rooftop_night_2k.hdr",
            "Studio" to "environments/studio_2k.hdr",
            "Warm Studio" to "environments/studio_warm_2k.hdr",
            "Sunset" to "environments/sunset_2k.hdr",
            "Outdoor" to "environments/outdoor_cloudy_2k.hdr",
            "Autumn" to "environments/autumn_field_2k.hdr",
            "Chinese Garden" to "environments/chinese_garden_2k.hdr",
            "Cafe" to "environments/comfy_cafe_2k.hdr",
            "Workshop" to "environments/artist_workshop_2k.hdr",
            "Pavilion" to "environments/pav_studio_2k.hdr",
        )
    }
    var selectedEnvIndex by remember { mutableIntStateOf(0) }
    val selectedEnv = environments[selectedEnvIndex]

    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 2.5f, y = 0.3f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = key(selectedEnv.second) {
        rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment(selectedEnv.second)
                ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
        }
    }

    val modelInstance = rememberModelInstance(modelLoader, "models/damaged_helmet.glb")

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 2.5f, y = 0.3f),
                targetPosition = Position(0f, 0f, 0f)
            ),
            environment = environment
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.9f,
                    autoAnimate = false
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 48.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                selectedEnv.first,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${environments.size} HDR environments available",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                environments.forEachIndexed { index, (label, _) ->
                    FilterChip(
                        selected = index == selectedEnvIndex,
                        onClick = { selectedEnvIndex = index },
                        label = {
                            Text(
                                label,
                                fontWeight = if (index == selectedEnvIndex) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF558B2F),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TextLabelsDemo() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 4.0f, y = 1.5f)
        lookAt(Float3(0f, 0.5f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        environment = environment
    ) {
        // Geometry shapes with labels
        val blue = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF1A73E8)) }
        val red = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFEA4335)) }
        val green = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF34A853)) }

        CubeNode(
            size = Float3(0.7f),
            center = Position(-1.5f, 0.35f, 0f),
            materialInstance = blue
        )
        TextNode(
            text = "Cube",
            position = Position(-1.5f, 1.1f, 0f),
            widthMeters = 0.5f,
            heightMeters = 0.15f,
            fontSize = 42f,
            cameraPositionProvider = { cameraNode.worldPosition }
        )

        SphereNode(
            radius = 0.45f,
            center = Position(0f, 0.45f, 0f),
            materialInstance = red
        )
        TextNode(
            text = "Sphere",
            position = Position(0f, 1.2f, 0f),
            widthMeters = 0.5f,
            heightMeters = 0.15f,
            fontSize = 42f,
            cameraPositionProvider = { cameraNode.worldPosition }
        )

        CylinderNode(
            radius = 0.3f,
            height = 0.8f,
            center = Position(1.5f, 0.4f, 0f),
            materialInstance = green
        )
        TextNode(
            text = "Cylinder",
            position = Position(1.5f, 1.1f, 0f),
            widthMeters = 0.6f,
            heightMeters = 0.15f,
            fontSize = 42f,
            cameraPositionProvider = { cameraNode.worldPosition }
        )
    }
}

@Composable
private fun LinePathsDemo() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 5.0f, y = 2.0f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = Position(z = 5.0f, y = 2.0f),
            targetPosition = Position(0f, 0f, 0f)
        ),
        environment = environment
    ) {
        val spiralMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFFF9800)) }
        val axisMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFE91E63)) }

        // Axis lines
        val xAxisMat = remember(materialLoader) { materialLoader.createColorInstance(Color.Red) }
        val yAxisMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF00FF00)) }
        val zAxisMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF4488FF)) }
        LineNode(start = Position(0f, 0f, 0f), end = Position(2f, 0f, 0f), materialInstance = xAxisMat)
        LineNode(start = Position(0f, 0f, 0f), end = Position(0f, 2f, 0f), materialInstance = yAxisMat)
        LineNode(start = Position(0f, 0f, 0f), end = Position(0f, 0f, 2f), materialInstance = zAxisMat)

        // Helix spiral as a PathNode
        val helixPoints = remember {
            (0..120).map { i ->
                val t = i / 120f * 4f * Math.PI.toFloat()
                Position(
                    x = kotlin.math.cos(t) * 1.2f,
                    y = i / 120f * 2f - 0.5f,
                    z = kotlin.math.sin(t) * 1.2f
                )
            }
        }
        PathNode(
            points = helixPoints,
            closed = false,
            materialInstance = spiralMat
        )

        // Lissajous curve
        val lissajousPoints = remember {
            (0..200).map { i ->
                val t = i / 200f * 2f * Math.PI.toFloat()
                Position(
                    x = kotlin.math.sin(3f * t) * 1.5f - 3f,
                    y = kotlin.math.sin(2f * t) * 1f + 0.5f,
                    z = 0f
                )
            }
        }
        PathNode(
            points = lissajousPoints,
            closed = true,
            materialInstance = axisMat
        )
    }
}

@Composable
private fun GestureEditingDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 4.0f, y = 1.5f)
        lookAt(Float3(0f, 0.3f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_warm_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/sneaker.glb")

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 4.0f, y = 1.5f),
                targetPosition = Position(0f, 0.3f, 0f)
            ),
            environment = environment,
            onGestureListener = rememberOnGestureListener(
                onDoubleTap = { _, node ->
                    // Reset scale on double tap
                    node?.apply { scale = Float3(1f) }
                }
            )
        ) {
            LightNode(
                type = LightManager.Type.SUN,
                apply = {
                    intensity(80000f)
                    direction(-0.5f, -1f, -0.5f)
                    castShadows(true)
                }
            )
            val floorMat = remember(materialLoader) {
                materialLoader.createColorInstance(Color(0xFF444444))
            }
            PlaneNode(
                size = Float3(6f, 6f, 1f),
                center = Position(0f, 0f, 0f),
                materialInstance = floorMat
            )
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    autoAnimate = true,
                    animationLoop = true,
                    isEditable = true,
                    apply = {
                        editableScaleRange = 0.3f..2.5f
                    }
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Gesture hint
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.control_gesture_drag_hint),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.control_double_tap_reset),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PhysicsDemo() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 5.0f, y = 2.5f)
        lookAt(Float3(0f, 0.5f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }

    // Restart counter — incrementing this triggers recomposition to reset balls
    var resetKey by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = null,
            environment = environment
        ) {
            // Floor
            val floorMat = remember(materialLoader) {
                materialLoader.createColorInstance(Color(0xFF555555))
            }
            PlaneNode(
                size = Float3(6f, 6f, 1f),
                center = Position(0f, 0f, 0f),
                materialInstance = floorMat
            )

            // Physics balls — each keyed by resetKey to force recreation on reset
            val ballColors = listOf(
                Color(0xFFEA4335),
                Color(0xFF1A73E8),
                Color(0xFF34A853),
                Color(0xFFFBBC04),
                Color(0xFFFF5722)
            )
            val ballConfigs = remember(resetKey) {
                listOf(
                    Triple(Position(-1.0f, 3.0f, 0f), Position(0.5f, 0f, 0f), 0.7f),
                    Triple(Position(0.5f, 4.0f, 0.3f), Position(-0.3f, 0f, 0.2f), 0.8f),
                    Triple(Position(-0.3f, 5.0f, -0.5f), Position(0.1f, 0f, -0.1f), 0.6f),
                    Triple(Position(0.8f, 3.5f, -0.2f), Position(-0.2f, 1f, 0.3f), 0.75f),
                    Triple(Position(0f, 6.0f, 0f), Position(0f, 0f, 0f), 0.85f)
                )
            }

            ballConfigs.forEachIndexed { index, (startPos, velocity, restitution) ->
                val ballMat = remember(materialLoader, index) {
                    materialLoader.createColorInstance(ballColors[index % ballColors.size])
                }
                val radius = 0.15f
                val sphereNode = remember(engine, resetKey, index) {
                    SphereNodeImpl(
                        engine = engine,
                        radius = radius,
                        materialInstance = ballMat
                    ).apply {
                        position = startPos
                    }
                }
                NodeLifecycle(sphereNode, null)
                PhysicsNode(
                    node = sphereNode,
                    restitution = restitution,
                    linearVelocity = velocity,
                    floorY = 0f,
                    radius = radius
                )
            }
        }

        // Reset button
        Button(
            onClick = { resetKey++ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5722)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.control_drop_again), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PostProcessingDemo() {
    val engine = rememberEngine()
    val view = rememberView(engine)
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 3.5f, y = 0.5f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_warm_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/sheen_chair.glb")

    var ssaoEnabled by remember { mutableStateOf(true) }
    var fxaaEnabled by remember { mutableStateOf(true) }
    var ditheringEnabled by remember { mutableStateOf(true) }

    // Apply post-processing settings reactively
    LaunchedEffect(ssaoEnabled) {
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
            enabled = ssaoEnabled
            quality = if (ssaoEnabled) QualityLevel.HIGH else QualityLevel.LOW
        }
    }
    LaunchedEffect(fxaaEnabled) {
        view.antiAliasing = if (fxaaEnabled) AntiAliasing.FXAA else AntiAliasing.NONE
    }
    LaunchedEffect(ditheringEnabled) {
        view.dithering = if (ditheringEnabled) Dithering.TEMPORAL else Dithering.NONE
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 3.5f, y = 0.5f),
                targetPosition = Position(0f, 0f, 0f)
            ),
            environment = environment
        ) {
            LightNode(
                type = LightManager.Type.SUN,
                apply = {
                    intensity(100000f)
                    direction(0f, -1f, -0.5f)
                    castShadows(true)
                }
            )
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    autoAnimate = true,
                    animationLoop = true
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
        // Toggle controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = ssaoEnabled,
                    onClick = { ssaoEnabled = !ssaoEnabled },
                    label = { Text("SSAO", fontWeight = if (ssaoEnabled) FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE91E63),
                        selectedLabelColor = Color.White,
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        labelColor = Color.White
                    )
                )
                FilterChip(
                    selected = fxaaEnabled,
                    onClick = { fxaaEnabled = !fxaaEnabled },
                    label = { Text("FXAA", fontWeight = if (fxaaEnabled) FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE91E63),
                        selectedLabelColor = Color.White,
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        labelColor = Color.White
                    )
                )
                FilterChip(
                    selected = ditheringEnabled,
                    onClick = { ditheringEnabled = !ditheringEnabled },
                    label = { Text("Dithering", fontWeight = if (ditheringEnabled) FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE91E63),
                        selectedLabelColor = Color.White,
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun ImageDetectionDemo() {
    val context = LocalContext.current
    val arAvailability = remember {
        try {
            com.google.ar.core.ArCoreApk.getInstance().checkAvailability(context)
        } catch (e: Exception) {
            com.google.ar.core.ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        }
    }

    if (!arAvailability.isSupported) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.image_detection_ar_unavailable),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)

    var detectedImages by remember { mutableStateOf<List<AugmentedImage>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            collisionSystem = collisionSystem,
            cameraNode = cameraNode,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.DISABLED
                // Create an image database with a programmatic reference image.
                val bitmap = android.graphics.Bitmap.createBitmap(
                    200, 200, android.graphics.Bitmap.Config.ARGB_8888
                ).apply {
                    val canvas = android.graphics.Canvas(this)
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#1A73E8")
                        style = android.graphics.Paint.Style.FILL
                    }
                    canvas.drawRect(0f, 0f, 200f, 200f, paint)
                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 48f
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    canvas.drawText("SV", 100f, 115f, paint)
                }
                config.augmentedImageDatabase = AugmentedImageDatabase(session).also { db ->
                    db.addImage("sceneview_logo", bitmap, 0.15f)
                    bitmap.recycle()
                }
            },
            onSessionUpdated = { _, frame ->
                detectedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
                    .filter { it.trackingState == TrackingState.TRACKING }
                    .toList()
            }
        ) {
            detectedImages.forEach { image ->
                AnchorNode(anchor = image.createAnchor(image.centerPose)) {
                    val mat = remember(materialLoader) {
                        materialLoader.createColorInstance(Color(0xFF3F51B5))
                    }
                    CubeNode(
                        size = Float3(
                            image.extentX,
                            0.02f,
                            image.extentZ
                        ),
                        materialInstance = mat
                    )
                }
            }
        }

        // Status overlay
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = if (detectedImages.isEmpty())
                    stringResource(R.string.image_detection_point_camera)
                else
                    stringResource(R.string.image_detection_count, detectedImages.size),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }

        // Info banner at bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.image_detection_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stringResource(R.string.image_detection_desc),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ReflectionProbesDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val environments = remember {
        listOf(
            "Night" to "environments/rooftop_night_2k.hdr",
            "Studio" to "environments/studio_2k.hdr",
            "Warm" to "environments/studio_warm_2k.hdr",
            "Sunset" to "environments/sunset_2k.hdr",
            "Outdoor" to "environments/outdoor_cloudy_2k.hdr",
            "Autumn" to "environments/autumn_field_2k.hdr",
        )
    }
    var selectedEnvIndex by remember { mutableIntStateOf(0) }
    val selectedEnv = environments[selectedEnvIndex]

    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 2.5f, y = 0.3f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = key(selectedEnv.second) {
        rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment(selectedEnv.second)
                ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
        }
    }

    // Use a highly reflective model
    val modelInstance = rememberModelInstance(modelLoader, "models/space_helmet.glb")

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 2.5f, y = 0.3f),
                targetPosition = Position(0f, 0f, 0f)
            ),
            environment = environment
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    autoAnimate = false
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
        // Environment picker
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.control_environment_reflections),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(R.string.control_environment_reflections_desc),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                environments.forEachIndexed { index, (label, _) ->
                    FilterChip(
                        selected = index == selectedEnvIndex,
                        onClick = { selectedEnvIndex = index },
                        label = {
                            Text(
                                label,
                                fontWeight = if (index == selectedEnvIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF795548),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GltfCamerasDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    data class CameraPreset(val name: String, val position: Float3, val target: Float3)

    val presets = remember {
        listOf(
            CameraPreset("Front", Float3(z = 3.0f, y = 0.3f), Float3(0f, 0f, 0f)),
            CameraPreset("Top", Float3(y = 3.5f, z = 0.5f), Float3(0f, 0f, 0f)),
            CameraPreset("Side", Float3(x = 3.0f, y = 0.5f), Float3(0f, 0f, 0f)),
            CameraPreset("Low Angle", Float3(z = 2.5f, y = -0.3f), Float3(0f, 0.5f, 0f)),
            CameraPreset("3/4 View", Float3(x = 2f, y = 1.5f, z = 2f), Float3(0f, 0f, 0f)),
        )
    }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selected = presets[selectedIndex]

    val cameraNode = rememberCameraNode(engine) {
        position = selected.position
        lookAt(selected.target)
    }

    // Update camera on preset change
    LaunchedEffect(selected) {
        cameraNode.position = selected.position
        cameraNode.lookAt(selected.target)
    }

    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/animated_bunny_detective.glb")

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = null,
            environment = environment
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.0f,
                    autoAnimate = true,
                    animationLoop = true
                )
            }
        }
        if (modelInstance == null) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
        // Camera preset picker
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.control_camera_label, selected.name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { index, preset ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        label = {
                            Text(
                                preset.name,
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF009688),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

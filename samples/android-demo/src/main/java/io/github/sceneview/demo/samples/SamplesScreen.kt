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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import io.github.sceneview.node.DynamicSkyNode
import io.github.sceneview.node.FogNode
import io.github.sceneview.node.PhysicsNode
import io.github.sceneview.node.SphereNode as SphereNodeImpl
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.StrokeCap
import com.google.android.filament.LightManager
import io.github.sceneview.math.Position

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

private val sampleDemos = listOf(
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
        title = "Camera Controls",
        subtitle = "Orbit, pan, zoom camera manipulator",
        icon = Icons.Default.PhotoCamera,
        category = "3D",
        accentColor = Color(0xFF9C27B0),
        content = { CameraControlsDemo() }
    ),
    SampleDemo(
        title = "Post-Processing",
        subtitle = "Bloom, SSAO, FXAA, tone mapping, vignette",
        icon = Icons.Default.AutoAwesome,
        category = "Effects",
        accentColor = Color(0xFFE91E63)
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
        accentColor = Color(0xFF3F51B5)
    ),
    SampleDemo(
        title = "Reflection Probes",
        subtitle = "Local cubemap reflections for realistic materials",
        icon = Icons.Default.Tune,
        category = "Advanced",
        accentColor = Color(0xFF795548)
    ),
    SampleDemo(
        title = "glTF Cameras",
        subtitle = "Import and switch between cameras from glTF files",
        icon = Icons.Default.CameraAlt,
        category = "3D",
        accentColor = Color(0xFF009688)
    ),
)

@Composable
fun SamplesScreen() {
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
                            "Samples",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${demos.size} feature demos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
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
            .scale(scale),
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
                        text = if (isLive) "LIVE" else "SOON",
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
                    contentDescription = "Back",
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
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
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round
            )
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
        cameraManipulator = null,
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

        CubeNode(
            size = Float3(0.8f),
            center = Float3(-1.5f, 0.4f, 0f),
            materialInstance = blue
        )
        SphereNode(
            radius = 0.5f,
            center = Float3(0f, 0.5f, 0f),
            materialInstance = red
        )
        CylinderNode(
            radius = 0.3f,
            height = 1f,
            center = Float3(1.5f, 0.5f, 0f),
            materialInstance = green
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
            cameraManipulator = null,
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
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round
            )
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
                timeOfDay = 16f,
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
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round
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
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round
            )
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
                text = "Sun Intensity: ${sunIntensity.toInt()} lux",
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
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round
            )
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
                Text("1 finger — Orbit", color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text("2 fingers — Pan", color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text("Pinch — Zoom", color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text("Double tap — Reset", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
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
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round
            )
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
                text = "Fog Density: ${"%.3f".format(fogDensity)}",
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
        cameraManipulator = null,
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
            Text("Drop Again", fontWeight = FontWeight.Bold)
        }
    }
}

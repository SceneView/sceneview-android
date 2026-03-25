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
import androidx.compose.ui.draw.clip
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
        accentColor = Color(0xFFFF6D00)
    ),
    SampleDemo(
        title = "Camera Controls",
        subtitle = "Orbit, pan, zoom camera manipulator",
        icon = Icons.Default.PhotoCamera,
        category = "3D",
        accentColor = Color(0xFF9C27B0)
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
        accentColor = Color(0xFF607D8B)
    ),
    SampleDemo(
        title = "Text Labels",
        subtitle = "3D text rendering with billboard facing",
        icon = Icons.Default.TextFields,
        category = "Content",
        accentColor = Color(0xFF00BCD4)
    ),
    SampleDemo(
        title = "Line Paths",
        subtitle = "3D polylines and Lissajous curves",
        icon = Icons.Default.LinearScale,
        category = "Content",
        accentColor = Color(0xFF4CAF50)
    ),
    SampleDemo(
        title = "Physics",
        subtitle = "Rigid body simulation with bouncing balls",
        icon = Icons.Default.Science,
        category = "Advanced",
        accentColor = Color(0xFFFF5722)
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
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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

                // "Interactive" badge for demos with content
                if (demo.content != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = demo.accentColor.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "LIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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
}

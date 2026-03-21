@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.gallery

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Gallery item representing a 3D model or procedural geometry to display in the list.
 */
private data class GalleryItem(
    val title: String,
    val subtitle: String,
    val type: GalleryItemType,
    val environment: String = "environments/rooftop_night_2k.hdr"
)

private sealed class GalleryItemType {
    data class Model(val path: String, val scale: Float = 1.0f) : GalleryItemType()
    data class Geometry(val shape: GeometryShape, val color: Color) : GalleryItemType()
}

private enum class GeometryShape { CUBE, SPHERE, CYLINDER }

private val galleryItems = listOf(
    // Realistic models — PBR showcase
    GalleryItem(
        "Toy Car",
        "Clearcoat automotive paint",
        GalleryItemType.Model("models/toy_car.glb", 0.8f),
        environment = "environments/studio_warm_2k.hdr"
    ),
    GalleryItem(
        "Sheen Chair",
        "Velvet fabric with sheen",
        GalleryItemType.Model("models/sheen_chair.glb", 0.6f),
        environment = "environments/studio_2k.hdr"
    ),
    GalleryItem(
        "Iridescence Lamp",
        "Glass sphere with iridescence",
        GalleryItemType.Model("models/iridescence_lamp.glb", 0.5f),
        environment = "environments/rooftop_night_2k.hdr"
    ),
    GalleryItem(
        "Geisha Mask",
        "Painted lacquer & metal",
        GalleryItemType.Model("models/geisha_mask.glb", 1.0f),
        environment = "environments/studio_warm_2k.hdr"
    ),
    GalleryItem(
        "Space Helmet",
        "Sci-fi PBR metals & glass",
        GalleryItemType.Model("models/space_helmet.glb", 0.8f),
        environment = "environments/rooftop_night_2k.hdr"
    ),
    GalleryItem(
        "Seal Statuette",
        "Photogrammetry bronze scan",
        GalleryItemType.Model("models/seal_statuette.glb", 0.8f),
        environment = "environments/outdoor_cloudy_2k.hdr"
    ),
    // Animated models
    GalleryItem(
        "Robot Mantis",
        "Mechanical creature walk",
        GalleryItemType.Model("models/animated_robot_mantis.glb", 0.7f),
        environment = "environments/sunset_2k.hdr"
    ),
    GalleryItem(
        "Kawaii Meka",
        "Hand-painted mech · 3 animations",
        GalleryItemType.Model("models/animated_kawaii_meka.glb", 0.8f),
        environment = "environments/studio_2k.hdr"
    ),
    GalleryItem(
        "Toon Horse",
        "Rigged & animated gallop",
        GalleryItemType.Model("models/animated_toon_horse.glb", 0.7f),
        environment = "environments/autumn_field_2k.hdr"
    ),
    GalleryItem(
        "Carnotaurus",
        "Low-poly dinosaur",
        GalleryItemType.Model("models/animated_carnotaurus.glb", 0.7f),
        environment = "environments/outdoor_cloudy_2k.hdr"
    ),
    GalleryItem(
        "Bunny Detective",
        "Charming character animation",
        GalleryItemType.Model("models/animated_bunny_detective.glb", 0.8f),
        environment = "environments/studio_warm_2k.hdr"
    ),
    // Procedural geometry
    GalleryItem(
        "Blue Cube",
        "Procedural box geometry",
        GalleryItemType.Geometry(GeometryShape.CUBE, Color(0xFF4285F4)),
        environment = "environments/studio_2k.hdr"
    ),
    GalleryItem(
        "Red Sphere",
        "Procedural sphere geometry",
        GalleryItemType.Geometry(GeometryShape.SPHERE, Color(0xFFEA4335)),
        environment = "environments/sunset_2k.hdr"
    ),
    GalleryItem(
        "Green Cylinder",
        "Procedural cylinder geometry",
        GalleryItemType.Geometry(GeometryShape.CYLINDER, Color(0xFF34A853)),
        environment = "environments/autumn_field_2k.hdr"
    ),
)

@Composable
fun GalleryScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Gallery")
                        Text(
                            "${galleryItems.size} live 3D scenes · models, animations & geometry",
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header hint
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.6f)
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Default.ViewInAr,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Each card renders a live 3D scene — scroll to stress-test!",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(galleryItems, key = { it.title }) { item ->
                GalleryCard(item = item)
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GalleryCard(item: GalleryItem) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp
    ) {
        Column {
            // 3D Scene
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF1A1A2E)
            ) {
                when (item.type) {
                    is GalleryItemType.Model -> ModelScene(
                        modelPath = item.type.path,
                        scaleToUnits = item.type.scale,
                        environmentPath = item.environment
                    )
                    is GalleryItemType.Geometry -> GeometryScene(
                        shape = item.type.shape,
                        color = item.type.color,
                        environmentPath = item.environment
                    )
                }
            }

            // Info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = "Interactive",
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(0.4f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModelScene(
    modelPath: String,
    scaleToUnits: Float,
    environmentPath: String = "environments/rooftop_night_2k.hdr"
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 2.5f, y = 0.2f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment(environmentPath)
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, modelPath)

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
                scaleToUnits = scaleToUnits,
                autoAnimate = true,
                animationLoop = true
            )
        }
    }
}

@Composable
private fun GeometryScene(
    shape: GeometryShape,
    color: Color,
    environmentPath: String = "environments/rooftop_night_2k.hdr"
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 3.0f, y = 0.5f)
        lookAt(Float3(0f, 0.3f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment(environmentPath)
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
        val mat = remember(materialLoader) {
            materialLoader.createColorInstance(color)
        }
        when (shape) {
            GeometryShape.CUBE -> CubeNode(
                size = Float3(1f),
                center = Float3(0f, 0.5f, 0f),
                materialInstance = mat
            )
            GeometryShape.SPHERE -> SphereNode(
                radius = 0.5f,
                center = Float3(0f, 0.5f, 0f),
                materialInstance = mat
            )
            GeometryShape.CYLINDER -> CylinderNode(
                radius = 0.3f,
                height = 1f,
                center = Float3(0f, 0.5f, 0f),
                materialInstance = mat
            )
        }
    }
}

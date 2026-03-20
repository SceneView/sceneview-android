package io.github.sceneview.demo.showcase

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ThreeDRotation
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbCloudy

/**
 * Complete catalog of all available SceneView nodes for the showcase.
 */
object NodeCatalog {

    val allNodes = listOf(
        // Geometry
        NodeDemo(
            name = "CubeNode",
            description = "Procedural box geometry with configurable size and material.",
            icon = Icons.Default.CropSquare,
            category = NodeCategory.GEOMETRY,
            codeSnippet = """
Scene {
    val mat = remember(materialLoader) {
        materialLoader.createColorInstance(Color.Blue)
    }
    CubeNode(
        size = Size(1f),
        center = Position(0f, 0.5f, 0f),
        materialInstance = mat
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "SphereNode",
            description = "Procedural sphere geometry with configurable radius.",
            icon = Icons.Default.Circle,
            category = NodeCategory.GEOMETRY,
            codeSnippet = """
Scene {
    val mat = remember(materialLoader) {
        materialLoader.createColorInstance(Color.Red)
    }
    SphereNode(
        radius = 0.5f,
        center = Position(0f, 0.5f, 0f),
        materialInstance = mat
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "CylinderNode",
            description = "Procedural cylinder geometry with radius and height.",
            icon = Icons.Default.Circle,
            category = NodeCategory.GEOMETRY,
            codeSnippet = """
Scene {
    val mat = remember(materialLoader) {
        materialLoader.createColorInstance(Color.Green)
    }
    CylinderNode(
        radius = 0.3f,
        height = 1f,
        center = Position(0f, 0.5f, 0f),
        materialInstance = mat
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "PlaneNode",
            description = "Procedural quad / plane geometry.",
            icon = Icons.Default.Crop169,
            category = NodeCategory.GEOMETRY,
            codeSnippet = """
Scene {
    val mat = remember(materialLoader) {
        materialLoader.createColorInstance(Color.Yellow)
    }
    PlaneNode(
        size = Size(1f, 1f),
        center = Position(0f, 0f, 0f),
        materialInstance = mat
    )
}""".trimIndent()
        ),

        // Model
        NodeDemo(
            name = "ModelNode",
            description = "Renders glTF/GLB 3D models with animations.",
            icon = Icons.Default.ViewInAr,
            category = NodeCategory.MODEL,
            codeSnippet = """
Scene {
    val modelInstance = rememberModelInstance(
        modelLoader, "models/damaged_helmet.glb"
    )
    modelInstance?.let {
        ModelNode(modelInstance = it)
    }
}""".trimIndent()
        ),

        // Light & Environment
        NodeDemo(
            name = "LightNode",
            description = "Directional, point, or spot light source.",
            icon = Icons.Default.Brightness7,
            category = NodeCategory.LIGHT,
            codeSnippet = """
Scene {
    LightNode(
        type = LightManager.Type.POINT,
        apply = {
            intensity(100_000f)
            color(1f, 0.9f, 0.8f)
            position(0f, 2f, 0f)
        }
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "DynamicSkyNode",
            description = "Procedural sky dome with sun position control.",
            icon = Icons.Default.WbCloudy,
            category = NodeCategory.LIGHT,
            codeSnippet = """
var timeOfDay by remember { mutableFloatStateOf(12f) }
Scene {
    DynamicSkyNode(
        timeOfDay = timeOfDay,  // 0-24: 6=sunrise, 12=noon, 18=sunset
        turbidity = 2f,
        sunIntensity = 110_000f
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "ReflectionProbeNode",
            description = "Zone-based IBL override for localized reflections.",
            icon = Icons.Default.Circle,
            category = NodeCategory.LIGHT,
            codeSnippet = """
val probeEnv = rememberEnvironment(environmentLoader) {
    environmentLoader.createHDREnvironment("envs/studio.hdr")!!
}
Scene(scene = scene) {
    ReflectionProbeNode(
        filamentScene = scene,
        environment = probeEnv,
        position = Position(0f, 1f, 0f),
        radius = 3f
    )
}""".trimIndent()
        ),

        // Camera
        NodeDemo(
            name = "CameraNode",
            description = "Perspective or orthographic camera for custom viewpoints.",
            icon = Icons.Default.CameraAlt,
            category = NodeCategory.CAMERA,
            codeSnippet = """
val cameraNode = rememberCameraNode(engine) {
    position = Position(0f, 1f, 3f)
    lookAt(Position(0f, 0f, 0f))
}
Scene(cameraNode = cameraNode) {
    // scene content
}""".trimIndent()
        ),

        // Content
        NodeDemo(
            name = "ImageNode",
            description = "Displays a 2D image as a textured quad in 3D space.",
            icon = Icons.Default.Image,
            category = NodeCategory.CONTENT,
            codeSnippet = """
Scene {
    ImageNode(
        imageFileLocation = "images/logo.png",
        size = Size(1f, 1f)
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "TextNode",
            description = "3D text rendering with customizable font and style.",
            icon = Icons.Default.TextFields,
            category = NodeCategory.CONTENT,
            codeSnippet = """
Scene {
    TextNode(
        text = "Hello SceneView!",
        fontSize = 48f,
        textColor = Color.WHITE,
        widthMeters = 0.6f,
        heightMeters = 0.2f,
        position = Position(0f, 1f, 0f)
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "BillboardNode",
            description = "Always-facing-camera sprite/billboard node.",
            icon = Icons.Default.ThreeDRotation,
            category = NodeCategory.CONTENT,
            codeSnippet = """
Scene {
    BillboardNode(
        bitmap = myBitmap,
        widthMeters = 0.5f,
        heightMeters = 0.5f,
        position = Position(0f, 1f, 0f)
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "LineNode",
            description = "Renders a line between two 3D points.",
            icon = Icons.Default.LinearScale,
            category = NodeCategory.CONTENT,
            codeSnippet = """
Scene {
    val mat = remember(materialLoader) {
        materialLoader.createColorInstance(Color.Cyan)
    }
    LineNode(
        start = Position(0f, 0f, 0f),
        end = Position(1f, 1f, 0f),
        materialInstance = mat
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "PathNode",
            description = "Renders a path through a series of 3D points.",
            icon = Icons.Default.LinearScale,
            category = NodeCategory.CONTENT,
            codeSnippet = """
Scene {
    PathNode(
        points = listOf(
            Position(0f, 0f, 0f),
            Position(1f, 0.5f, 0f),
            Position(2f, 0f, 0f)
        )
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "VideoNode",
            description = "Video playback on a 3D quad surface.",
            icon = Icons.Default.Videocam,
            category = NodeCategory.CONTENT,
            codeSnippet = """
val player = remember {
    MediaPlayer().apply {
        setDataSource(context, videoUri)
        isLooping = true; prepare(); start()
    }
}
DisposableEffect(Unit) { onDispose { player.release() } }

Scene {
    VideoNode(player = player)
}""".trimIndent()
        ),

        // AR
        NodeDemo(
            name = "AnchorNode",
            description = "World-space anchor that tracks a real-world position.",
            icon = Icons.Default.LocationOn,
            category = NodeCategory.AR,
            requiresAR = true,
            codeSnippet = """
var anchor by remember { mutableStateOf<Anchor?>(null) }
ARScene(
    onTouchEvent = { event, hitResult ->
        if (event.action == MotionEvent.ACTION_UP && hitResult != null)
            anchor = hitResult.createAnchor()
        true
    }
) {
    anchor?.let { a ->
        AnchorNode(anchor = a) {
            ModelNode(modelInstance = model, scaleToUnits = 0.5f)
        }
    }
}""".trimIndent()
        ),
        NodeDemo(
            name = "AugmentedImageNode",
            description = "Detects and tracks real-world images.",
            icon = Icons.Default.Image,
            category = NodeCategory.AR,
            requiresAR = true,
            codeSnippet = """
var trackedImages by remember { mutableStateOf(listOf<AugmentedImage>()) }
ARScene(
    sessionConfiguration = { session, config ->
        config.augmentedImageDatabase =
            AugmentedImageDatabase(session)
                .apply { addImage("marker", bitmap, 0.15f) }
    },
    onSessionUpdated = { _, frame ->
        trackedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }
    }
) {
    trackedImages.forEach { image ->
        AugmentedImageNode(augmentedImage = image) {
            ModelNode(modelInstance = model, scaleToUnits = image.extentX)
        }
    }
}""".trimIndent()
        ),
    )

    val categories: Map<NodeCategory, List<NodeDemo>> =
        allNodes.groupBy { it.category }
}

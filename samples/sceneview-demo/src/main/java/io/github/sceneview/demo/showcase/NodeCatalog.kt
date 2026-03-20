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
    CubeNode(
        size = Size(1f),
        center = Position(0f, 0.5f, 0f),
        apply = {
            materialInstance = materialLoader
                .createColorInstance(Color.Blue)
        }
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
    SphereNode(
        radius = 0.5f,
        center = Position(0f, 0.5f, 0f),
        apply = {
            materialInstance = materialLoader
                .createColorInstance(Color.Red)
        }
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
    CylinderNode(
        radius = 0.3f,
        height = 1f,
        center = Position(0f, 0.5f, 0f),
        apply = {
            materialInstance = materialLoader
                .createColorInstance(Color.Green)
        }
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
    PlaneNode(
        size = Size(1f, 1f),
        center = Position(0f, 0f, 0f),
        apply = {
            materialInstance = materialLoader
                .createColorInstance(Color.Yellow)
        }
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
Scene {
    DynamicSkyNode(
        sunPosition = Position(0f, 1f, -1f)
    )
}""".trimIndent()
        ),
        NodeDemo(
            name = "ReflectionProbeNode",
            description = "Zone-based IBL override for localized reflections.",
            icon = Icons.Default.Circle,
            category = NodeCategory.LIGHT,
            codeSnippet = """
Scene {
    ReflectionProbeNode(
        environment = environmentLoader
            .createHDREnvironment("envs/studio.hdr"),
        halfExtent = Float3(2f, 2f, 2f)
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
Scene {
    CameraNode(
        position = Position(0f, 1f, 3f),
        lookAt = Position(0f, 0f, 0f)
    )
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
        imageUrl = "https://example.com/image.png",
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
    BillboardNode {
        TextNode(text = "I face the camera!")
    }
}""".trimIndent()
        ),
        NodeDemo(
            name = "LineNode",
            description = "Renders a line between two 3D points.",
            icon = Icons.Default.LinearScale,
            category = NodeCategory.CONTENT,
            codeSnippet = """
Scene {
    LineNode(
        start = Position(0f, 0f, 0f),
        end = Position(1f, 1f, 0f),
        apply = {
            materialInstance = materialLoader
                .createColorInstance(Color.Cyan)
        }
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
Scene {
    VideoNode(
        videoUri = "asset:///video.mp4",
        size = Size(1.6f, 0.9f)
    )
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
ARScene {
    val anchor = rememberAnchor(hitResult)
    anchor?.let {
        AnchorNode(anchor = it) {
            ModelNode(modelInstance = model)
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
ARScene(
    sessionConfiguration = { session, config ->
        config.augmentedImageDatabase =
            AugmentedImageDatabase(session)
                .apply { addImage("marker", bitmap) }
    }
) {
    AugmentedImageNode(imageName = "marker") {
        ModelNode(modelInstance = model)
    }
}""".trimIndent()
        ),
    )

    val categories: Map<NodeCategory, List<NodeDemo>> =
        allNodes.groupBy { it.category }
}

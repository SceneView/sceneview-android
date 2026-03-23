# SceneView Samples — Recipe Index

This directory contains sample apps demonstrating SceneView capabilities.
Android samples are self-contained Gradle projects. iOS samples are in `SceneViewSwift/Examples/`.

## Quick reference — "I want to..."

| I want to... | Sample | Key code |
|---|---|---|
| Show a 3D model with orbit camera | `model-viewer` | `Scene { ModelNode(modelInstance) }` |
| Place a model in AR on a surface | `ar-model-viewer` | `ARScene { AnchorNode { ModelNode() } }` |
| Control camera orbit/pan/zoom | `camera-manipulator` | `rememberCameraManipulator()` |
| Use cameras from a glTF file | `gltf-camera` | `CameraNode(camera = gltfCamera)` |
| Draw lines and curves | `line-path` | `LineNode(start, end)`, `PathNode(points)` |
| Add text labels in 3D | `text-labels` | `TextNode(text = "Label")` |
| Procedural sky + fog atmosphere | `dynamic-sky` | `DynamicSkyNode`, `FogNode` |
| Add physics (gravity, bounce) | `physics-demo` | Tap-to-spawn with Euler integration |
| Bloom, DoF, SSAO effects | `post-processing` | View options (bloomOptions, etc.) |
| Local reflections / IBL zones | `reflection-probe` | `ReflectionProbeNode` |
| Detect real-world images in AR | `ar-augmented-image` | `AugmentedImageNode(image)` |
| Share AR anchors across devices | `ar-cloud-anchor` | `CloudAnchorNode(anchor)` |
| Visualize AR feature points | `ar-point-cloud` | ARCore point cloud rendering |
| Build a full demo app | `sceneview-demo` | 4-tab showcase app |

## Samples by category

### 3D Scenes

| Sample | Description | Complexity |
|---|---|---|
| `model-viewer` | Load a glTF/GLB model, HDR environment, orbit camera, animations | Beginner |
| `camera-manipulator` | Orbit, pan, zoom camera with gesture and collision hit-testing | Beginner |
| `gltf-camera` | Import and use camera nodes from glTF files | Intermediate |
| `line-path` | LineNode, PathNode, procedural curves, animated sine waves | Intermediate |
| `text-labels` | World-space text labels with face-to-camera constraints | Intermediate |
| `dynamic-sky` | Procedural sky + fog atmosphere, real-time parameter sliders | Advanced |
| `physics-demo` | Tap-to-spawn spheres with gravity, bounce, Euler integration | Advanced |
| `post-processing` | Bloom, Depth of Field, SSAO, fog — all post-processing effects | Advanced |
| `reflection-probe` | ReflectionProbeNode, zone-based IBL switching | Advanced |
| `autopilot-demo` | Procedural geometry scene + HUD overlay — no model files needed | Showcase |

### Augmented Reality

| Sample | Description | Complexity |
|---|---|---|
| `ar-model-viewer` | Tap-to-place on planes, model picker, pinch/rotate | Beginner |
| `ar-augmented-image` | Image detection, overlay 3D content on real images | Intermediate |
| `ar-cloud-anchor` | Persistent cross-device anchors via Google Cloud | Advanced |
| `ar-point-cloud` | ARCore feature point visualization | Intermediate |

### Showcase

| Sample | Description |
|---|---|
| `sceneview-demo` | Play Store demo app — Explore, Showcase, Gallery, QA tabs |

## Common recipes (copy-paste ready)

### Minimal 3D model viewer

```kotlin
@Composable
fun ModelViewer() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, "models/helmet.glb")

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        model?.let { ModelNode(modelInstance = it, scaleToUnits = 1f, autoAnimate = true) }
    }
}
```

### Minimal AR tap-to-place

```kotlin
@Composable
fun ARTapToPlace() {
    var anchor by remember { mutableStateOf<Anchor?>(null) }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, "models/chair.glb")

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        onSessionUpdated = { _, frame ->
            if (anchor == null) {
                anchor = frame.getUpdatedPlanes()
                    .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                    ?.let { frame.createAnchorOrNull(it.centerPose) }
            }
        }
    ) {
        anchor?.let { a ->
            AnchorNode(anchor = a) {
                model?.let { ModelNode(modelInstance = it, scaleToUnits = 0.5f) }
            }
        }
    }
}
```

### Procedural geometry (no model files)

```kotlin
@Composable
fun ProceduralScene() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val material = rememberMaterialInstance(materialLoader)

    Scene(modifier = Modifier.fillMaxSize(), engine = engine) {
        CubeNode(size = Size(0.5f), materialInstance = material)
        SphereNode(radius = 0.3f, materialInstance = material,
            position = Position(x = 1f))
        CylinderNode(radius = 0.2f, height = 0.8f, materialInstance = material,
            position = Position(x = -1f))
    }
}
```

### Embed Compose UI in 3D

```kotlin
@Composable
fun ComposeIn3D() {
    val engine = rememberEngine()
    val windowManager = rememberViewNodeManager()

    Scene(modifier = Modifier.fillMaxSize(), engine = engine) {
        ViewNode(windowManager = windowManager) {
            Card { Text("Hello from 3D!") }
        }
    }
}
```

### Animated model with controls

```kotlin
@Composable
fun AnimatedModel() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, "models/character.glb")
    var isPlaying by remember { mutableStateOf(true) }

    Column {
        Scene(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            engine = engine, modelLoader = modelLoader
        ) {
            model?.let { ModelNode(modelInstance = it, autoAnimate = isPlaying) }
        }
        Button(onClick = { isPlaying = !isPlaying }) {
            Text(if (isPlaying) "Pause" else "Play")
        }
    }
}
```

### Multiple models in a scene

```kotlin
@Composable
fun MultiModelScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val helmet = rememberModelInstance(modelLoader, "models/helmet.glb")
    val car = rememberModelInstance(modelLoader, "models/car.glb")

    Scene(modifier = Modifier.fillMaxSize(), engine = engine, modelLoader = modelLoader) {
        helmet?.let { ModelNode(modelInstance = it, scaleToUnits = 0.5f,
            position = Position(x = -0.5f)) }
        car?.let { ModelNode(modelInstance = it, scaleToUnits = 0.5f,
            position = Position(x = 0.5f)) }
    }
}
```

### iOS (SceneViewSwift)

| Sample | Description |
|---|---|
| `SceneViewSwift/Examples/SceneViewDemo/` | 3-tab iOS demo: Explore (3D viewer), Shapes (all primitives), AR (tap-to-place) |

### Cross-Platform Recipes

Side-by-side Android + iOS implementations in `samples/recipes/`:

| Recipe | Description |
|---|---|
| `model-viewer` | Load and display a 3D model with orbit camera |
| `ar-tap-to-place` | AR plane detection + tap to place a model |
| `procedural-geometry` | Procedural shapes with PBR materials |
| `text-labels` | 3D text and billboard labels |
| `physics` | Tap-to-spawn with gravity and bounce |

## Shared module

`samples/common/` contains shared helpers (theme, icons, navigation) used across all Android samples.

## Running a sample

### Android

```bash
./gradlew :samples:model-viewer:installDebug
```

### iOS

Open `SceneViewSwift/Examples/SceneViewDemo/` in Xcode 16+ and run on a simulator or device.

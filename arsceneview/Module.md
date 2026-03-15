# Module arsceneview

AR scene rendering for Jetpack Compose, powered by ARCore and Google Filament.

## Quick start

```kotlin
dependencies {
    implementation("io.github.sceneview:arsceneview:3.0.0")
}
```

```kotlin
@Composable
fun ARScreen() {
    var anchor by remember { mutableStateOf<Anchor?>(null) }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val instance = rememberModelInstance(modelLoader, "models/helmet.glb")

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        onSessionUpdated = { session, frame ->
            if (anchor == null) {
                frame.hitTest(frame.width / 2f, frame.height / 2f)
                    .firstOrNull { it.isValid(depthPoint = false, point = false) }
                    ?.let { anchor = it.createAnchor() }
            }
        },
    ) {
        anchor?.let { AnchorNode(engine = engine, anchor = it) {
            instance?.let { ModelNode(modelInstance = it, scaleToUnits = 0.5f) }
        }}
    }
}
```

## API overview

### ARScene composable

| Composable | Description |
|---|---|
| `ARScene { }` | Root AR scene. Manages an ARCore Session. Accepts an `ARSceneScope` content block. |

### AR remember helpers

| Function | Returns | Description |
|---|---|---|
| `rememberARCameraNode(engine)` | `ARCameraNode` | Camera driven by ARCore pose each frame. |
| `rememberARCameraStream(engine)` | `ARCameraStream` | OpenGL external texture for camera background + depth occlusion. |
| `rememberAREnvironment(engine)` | `HDREnvironment` | No skybox, neutral IBL; updated by LightEstimator from the camera feed. |

All `rememberXxx` helpers from the base `sceneview` module are also available.

### AR node composables (inside `ARScene { }`)

| Node | Description |
|---|---|
| `AnchorNode` | Tracks a world-space `Anchor`. Children follow the anchor pose. |
| `PoseNode` | Tracks a raw ARCore `Pose` without an anchor. |
| `HitResultNode` | Creates and tracks an anchor from a `HitResult`. |
| `AugmentedImageNode` | Renders content anchored to a detected `AugmentedImage`. |
| `AugmentedFaceNode` | Renders content anchored to a detected `AugmentedFace`. |
| `CloudAnchorNode` | Creates or resolves a Cloud Anchor; exposes resolution state. |
| `TrackableNode` | Generic node for any `Trackable` (plane, point, etc.). |
| `StreetscapeGeometryNode` | Renders Geospatial API building/terrain geometry. |

## Session configuration examples

### Default (world-facing, plane detection)

```kotlin
ARScene(
    planeRenderer = true,
    sessionConfiguration = { session, config ->
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
    },
)
```

### Front-facing camera (face tracking)

```kotlin
ARScene(
    sessionFeatures = setOf(Session.Feature.FRONT_CAMERA),
    sessionConfiguration = { _, config ->
        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
    },
)
```

### Geospatial / Street View

```kotlin
ARScene(
    sessionConfiguration = { session, config ->
        if (session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
            config.geospatialMode = Config.GeospatialMode.ENABLED
        }
    },
)
```

## Package structure

| Package | Contents |
|---|---|
| `io.github.sceneview.ar` | `ARScene`, `ARSceneView`, `ARSceneScope`, `ARCore` |
| `io.github.sceneview.ar.node` | `AnchorNode`, `PoseNode`, `HitResultNode`, `TrackableNode`, augmented nodes |
| `io.github.sceneview.ar.arcore` | `ArSession`, `ArFrame`, session lifecycle helpers |
| `io.github.sceneview.ar.camera` | `ARCameraNode`, `ARCameraStream` |
| `io.github.sceneview.ar.light` | `LightEstimator` — real-world lighting from camera feed |

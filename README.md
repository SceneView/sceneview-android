# SceneView for Android

![SceneView Logo](https://github.com/SceneView/sceneview-android/assets/6597529/ad382001-a771-4484-9746-3ad200d00f05)

> ## 3D is just Compose UI.

SceneView 3.0 brings the full power of Google Filament and ARCore into Jetpack Compose.
Write a `Scene { }` the same way you write a `Column { }`. Nodes are composables.
Lifecycle is automatic. State drives everything.

[![Sceneview](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Sceneview&color=6c35aa)](https://search.maven.org/artifact/io.github.sceneview/sceneview)
[![ARSceneview](https://img.shields.io/maven-central/v/io.github.sceneview/arsceneview.svg?label=ARSceneview&color=6c35aa)](https://search.maven.org/artifact/io.github.sceneview/arsceneview)
[![Filament](https://img.shields.io/badge/Filament-v1.56.0-yellow)](https://github.com/google/filament)
[![ARCore](https://img.shields.io/badge/ARCore-v1.53.0-c961cb)](https://github.com/google-ar/arcore-android-sdk)

[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)
[![Open Collective](https://opencollective.com/sceneview/tiers/badge.svg?label=Donators%20)](https://opencollective.com/sceneview)

## The idea

You already know how to build a screen:

```kotlin
Column {
    Text("Title")
    Image(painter = painterResource(R.drawable.cover), contentDescription = null)
    Button(onClick = { /* ... */ }) { Text("Open") }
}
```

This is a 3D scene — a photorealistic helmet, HDR lighting, orbit-camera gestures:

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
    }
    LightNode(type = LightManager.Type.SUN) {
        intensity(100_000f)
        castShadows(true)
    }
}
```

Same pattern. Same Kotlin. Same mental model — now with depth.

No engine lifecycle callbacks. No `addChildNode` / `removeChildNode`. No `onResume`/`onPause`
overrides. No manual cleanup. The Compose runtime handles all of it.

---

## AR in 15 lines

```kotlin
var anchor by remember { mutableStateOf<Anchor?>(null) }

ARScene(
    modifier = Modifier.fillMaxSize(),
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
            ModelNode(modelInstance = helmet, scaleToUnits = 0.5f)
        }
    }
}
```

When the plane is detected, `anchor` becomes non-null. Compose recomposes. `AnchorNode` enters
the composition. The model appears — anchored to the physical world. When `anchor` is cleared, the
node is removed and destroyed automatically. **Pure Compose semantics, in AR.**

---

## What's new in 3.0

SceneView 3.0 is a ground-up rewrite around a single idea: **3D is just more Compose UI.**

| What changed | What it means for you |
|---|---|
| `Scene { }` / `ARScene { }` content block | Declare nodes as composables — no list, no `add()` |
| `SceneScope` / `ARSceneScope` DSL | Every node type (`ModelNode`, `AnchorNode`, `LightNode`, ...) is `@Composable` |
| `NodeScope` trailing lambda | Nest child nodes exactly like `Column { }` nests children |
| `rememberModelInstance` | Async loading — returns `null` while loading, recomposes when ready |
| `SceneNodeManager` | Internal bridge — Compose snapshot state drives the Filament scene graph |
| `ViewNode` | Embed any Compose UI as a 3D billboard inside the scene |
| `SurfaceType` enum | Choose `SurfaceView` (best performance) or `TextureView` (transparency) |
| All resources are `remember` | Engine, loaders, environment, camera — Compose owns the lifecycle |

See [MIGRATION.md](MIGRATION.md) for a step-by-step upgrade guide from 2.x.

## Table of Contents

- [What's new in 3.0](#whats-new-in-30)
- [3D with Compose](#3d-with-compose)
    - [Installation](#3d-installation)
    - [Quick start](#3d-quick-start)
    - [SceneScope DSL reference](#scenescope-dsl)
    - [Samples](#3d-samples)
- [AR with Compose](#ar-with-compose)
    - [Installation](#ar-installation)
    - [Quick start](#ar-quick-start)
    - [ARSceneScope DSL reference](#arscenescope-dsl)
    - [Samples](#ar-samples)
- [Resources](#resources)
- [Support the project](#support-the-project)

---

## <a name="3d-with-compose"></a>3D with Compose

### <a name="3d-installation"></a>Installation

```gradle
dependencies {
    implementation("io.github.sceneview:sceneview:3.0.0")
}
```

### <a name="3d-quick-start"></a>Quick start

`Scene` is a `@Composable` that renders a Filament 3D viewport. Think of it as a `Box` that adds
a third dimension — everything inside its trailing block is declared with the **SceneScope** DSL.

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    // Loaded asynchronously — null until ready, then recomposition places it in the scene
    val modelInstance = rememberModelInstance(modelLoader, "models/damaged_helmet.glb")
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = environment,
        cameraManipulator = rememberCameraManipulator(),
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000.0f },
        onGestureListener = rememberOnGestureListener(
            onDoubleTap = { _, node -> node?.apply { scale *= 2.0f } }
        )
    ) {
        // ── Everything below is 3D Compose ─────────────────────────────────

        modelInstance?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
        }

        // Nodes nest exactly like Compose UI
        Node(position = Position(y = 1.5f)) {
            CubeNode(size = Size(0.2f), materialInstance = redMaterial)
            SphereNode(radius = 0.1f)
        }
    }
}
```

That's it. No engine lifecycle callbacks, no `onResume`/`onPause` overrides, no manual scene graph
bookkeeping. The Compose runtime handles all of it.

### <a name="scenescope-dsl"></a>SceneScope DSL reference

All composables available inside `Scene { }`:

| Composable | Description |
|---|---|
| `ModelNode(modelInstance, scaleToUnits?)` | Renders a glTF/GLB model. Set `isEditable = true` to enable pinch-to-scale and drag-to-rotate. |
| `LightNode(type)` | Directional, point, spot, or sun light |
| `CameraNode()` | Named camera (e.g. imported from a glTF) |
| `CubeNode(size, materialInstance?)` | Box geometry |
| `SphereNode(radius, materialInstance?)` | Sphere geometry |
| `CylinderNode(radius, height, materialInstance?)` | Cylinder geometry |
| `PlaneNode(size, normal, materialInstance?)` | Flat quad geometry |
| `ImageNode(bitmap / fileLocation / resId)` | Image rendered on a plane |
| `ViewNode(windowManager) { ComposeUI }` | **Compose UI rendered as a 3D surface** |
| `MeshNode(primitiveType, vertexBuffer, indexBuffer)` | Custom GPU mesh |
| `Node()` | Pivot / group node |

**Gesture sensitivity** — `Node` exposes `scaleGestureSensitivity: Float` (default `0.5`). Lower
values make pinch-to-scale feel more progressive. Tune it per-node in the `apply` block:

```kotlin
ModelNode(modelInstance = instance, isEditable = true, apply = {
    scaleGestureSensitivity = 0.3f   // 1.0 = raw, lower = more damped
    editableScaleRange = 0.2f..1.0f
})
```

Every node accepts an optional `content` trailing lambda — a **NodeScope** where child composables
are automatically parented to the enclosing node:

```kotlin
Scene {
    Node(position = Position(y = 0.5f)) {    // NodeScope
        ModelNode(modelInstance = helmet)     // child of Node
        CubeNode(size = Size(0.05f))          // sibling, still a child of Node
    }
}
```

**Async model loading** — `rememberModelInstance` returns `null` while the file loads on
`Dispatchers.IO`, then triggers recomposition. The node appears automatically when ready:

```kotlin
Scene {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 0.5f)
    }
}
```

**Compose UI inside 3D space** — `ViewNode` renders any composable onto a plane in the scene:

```kotlin
val windowManager = rememberViewNodeManager()

Scene {
    ViewNode(windowManager = windowManager) {
        Card {
            Text("Hello from 3D!")
            Button(onClick = { /* ... */ }) { Text("Click me") }
        }
    }
}
```

**Reactive state** — pass any `State` directly into node parameters. The scene updates on every
state change with no manual synchronisation:

```kotlin
var rotationY by remember { mutableFloatStateOf(0f) }
LaunchedEffect(Unit) { while (true) { withFrameNanos { rotationY += 0.5f } } }

Scene {
    ModelNode(
        modelInstance = helmet,
        rotation = Rotation(y = rotationY)   // recomposes on every frame change
    )
}
```

**Tap interaction** — `isEditable = true` enables pinch-to-scale, drag-to-move, and
two-finger-rotate gestures on any node with zero extra code:

```kotlin
Scene(
    onGestureListener = rememberOnGestureListener(
        onSingleTapConfirmed = { event, node -> println("Tapped: ${node?.name}") }
    )
) {
    ModelNode(modelInstance = helmet, isEditable = true)
}
```

**Surface type** — choose the backing Android surface:

```kotlin
// SurfaceView — renders behind Compose layers, best GPU performance (default)
Scene(surfaceType = SurfaceType.Surface)

// TextureView — renders inline with Compose, supports transparency / alpha blending
Scene(surfaceType = SurfaceType.TextureSurface, isOpaque = false)
```

### <a name="3d-samples"></a>Samples

| Sample | What it shows |
|---|---|
| [Model Viewer](/samples/model-viewer) | Animated camera orbit around a glTF model, HDR environment, double-tap to scale |
| [glTF Camera](/samples/gltf-camera) | Use a camera node imported directly from a glTF file |
| [Camera Manipulator](/samples/camera-manipulator) | Orbit / pan / zoom camera interaction |
| [Autopilot Demo](/samples/autopilot-demo) | Full animated scene built entirely with geometry nodes — no model files needed |

---

## <a name="ar-with-compose"></a>AR with Compose

### <a name="ar-installation"></a>Installation

```gradle
dependencies {
    // Includes sceneview — no need to add both
    implementation("io.github.sceneview:arsceneview:3.0.0")
}
```

Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.ar" android:required="true" />

<application>
    <meta-data android:name="com.google.ar.core" android:value="required" />
</application>
```

### <a name="ar-quick-start"></a>Quick start

`ARScene` is `Scene` with ARCore wired in. The camera is driven by ARCore tracking. Everything
else — anchors, models, lights, UI — is declared in the **ARSceneScope** content block. Normal
Compose state decides what is in the scene.

```kotlin
var anchor by remember { mutableStateOf<Anchor?>(null) }

val engine = rememberEngine()
val modelLoader = rememberModelLoader(engine)
val modelInstance = rememberModelInstance(modelLoader, "models/helmet.glb")

ARScene(
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    modelLoader = modelLoader,
    cameraNode = rememberARCameraNode(engine),
    planeRenderer = true,
    sessionConfiguration = { session, config ->
        config.depthMode =
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                Config.DepthMode.AUTOMATIC
            else Config.DepthMode.DISABLED
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
    },
    onSessionUpdated = { _, frame ->
        if (anchor == null) {
            anchor = frame.getUpdatedPlanes()
                .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                ?.let { frame.createAnchorOrNull(it.centerPose) }
        }
    }
) {
    // ── AR Compose content ───────────────────────────────────────────────────

    anchor?.let {
        AnchorNode(anchor = it) {
            // All SceneScope nodes are available inside AR nodes too
            modelInstance?.let { instance ->
                ModelNode(modelInstance = instance, scaleToUnits = 0.5f)
            }
        }
    }
}
```

The anchor drives state. When `anchor` changes, Compose recomposes and `AnchorNode` appears. When
the anchor is cleared, the node is removed and destroyed automatically. **AR state is just
Kotlin state.**

### <a name="arscenescope-dsl"></a>ARSceneScope DSL reference

`ARScene { }` provides everything from [SceneScope](#scenescope-dsl) plus:

| Composable | Description |
|---|---|
| `AnchorNode(anchor)` | Follows a real-world ARCore anchor |
| `PoseNode(pose)` | Follows a world-space pose (non-persistent) |
| `HitResultNode(xPx, yPx)` | Auto hit-tests at a screen coordinate each frame |
| `HitResultNode { frame -> hitResult }` | Custom hit-test lambda |
| `AugmentedImageNode(augmentedImage)` | Tracks a detected real-world image |
| `AugmentedFaceNode(augmentedFace)` | Renders a mesh aligned to a detected face |
| `CloudAnchorNode(anchor)` | Persistent cross-device anchor via Google Cloud |
| `TrackableNode(trackable)` | Follows any ARCore trackable |
| `StreetscapeGeometryNode(streetscapeGeometry)` | Renders a Geospatial streetscape mesh |

**Augmented Images**

```kotlin
ARScene(
    sessionConfiguration = { session, config ->
        config.augmentedImageDatabase = AugmentedImageDatabase(session).also { db ->
            db.addImage("cover", coverBitmap)
        }
    },
    onSessionUpdated = { _, frame ->
        frame.getUpdatedTrackables(AugmentedImage::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }
            .forEach { detectedImages += it }
    }
) {
    detectedImages.forEach { image ->
        AugmentedImageNode(augmentedImage = image) {
            ModelNode(modelInstance = rememberModelInstance(modelLoader, "drone.glb"))
        }
    }
}
```

**Augmented Faces**

```kotlin
ARScene(
    sessionFeatures = setOf(Session.Feature.FRONT_CAMERA),
    sessionConfiguration = { _, config ->
        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
    },
    onSessionUpdated = { session, _ ->
        detectedFaces = session.getAllTrackables(AugmentedFace::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }
    }
) {
    detectedFaces.forEach { face ->
        AugmentedFaceNode(augmentedFace = face, meshMaterialInstance = faceMaterial)
    }
}
```

**Geospatial Streetscape**

```kotlin
ARScene(
    sessionConfiguration = { _, config ->
        config.geospatialMode = Config.GeospatialMode.ENABLED
        config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.ENABLED
    },
    onSessionUpdated = { _, frame ->
        geometries = frame.getUpdatedTrackables(StreetscapeGeometry::class.java).toList()
    }
) {
    geometries.forEach { geo ->
        StreetscapeGeometryNode(streetscapeGeometry = geo, meshMaterialInstance = buildingMat)
    }
}
```

### <a name="ar-samples"></a>Samples

| Sample | What it shows |
|---|---|
| [AR Model Viewer](/samples/ar-model-viewer) | Tap-to-place on detected planes, model picker, animated reticle, pinch-to-scale, drag-to-rotate |
| [AR Augmented Image](/samples/ar-augmented-image) | Overlay content on detected real-world images |
| [AR Cloud Anchors](/samples/ar-cloud-anchor) | Host and resolve persistent cross-device anchors |
| [AR Point Cloud](/samples/ar-point-cloud) | Visualise ARCore feature points |
| [Autopilot Demo](/samples/autopilot-demo) | Autonomous AR scene driven entirely by Compose state |

---

## Resources

### Documentation
- [3D API Reference](https://sceneview.github.io/api/sceneview-android/sceneview/)
- [AR API Reference](https://sceneview.github.io/api/sceneview-android/arsceneview/)
- [Migration Guide — v2.x → v3.0](MIGRATION.md)
- [Changelog](CHANGELOG.md)

### Community
- [Website](https://sceneview.github.io/)
- [Discord](https://discord.gg/UbNDDBTNqb)
- [YouTube](https://www.youtube.com/results?search_query=Sceneview+android)

### Related Projects
- [Google Filament](https://github.com/google/filament)
- [Google ARCore](https://github.com/google-ar/arcore-android-sdk)

## Support the project

SceneView is open-source and community-funded.

- [Open Collective](https://opencollective.com/sceneview/contribute/say-thank-you-ask-a-question-ask-for-features-and-fixes-33651) — one-time or recurring support
- [GitHub Sponsors](https://github.com/sponsors/ThomasGorisse) — sponsor the maintainer directly
- [SceneView Merchandise](https://sceneview.threadless.com/designs/sceneview)
- Open a Pull Request — contributions welcome

<p align="center">
  <img src="https://github.com/SceneView/sceneview-android/assets/6597529/ad382001-a771-4484-9746-3ad200d00f05" alt="SceneView" width="600" />
</p>

<h1 align="center">SceneView for Android</h1>

<p align="center">
  <strong>3D and AR as Jetpack Compose composables.</strong><br />
  The #1 3D/AR SDK for Android. Built on Google Filament and ARCore.
</p>

<p align="center">
  <a href="https://search.maven.org/artifact/io.github.sceneview/sceneview"><img src="https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Maven%20Central&color=6c35aa" alt="Maven Central" /></a>
  <a href="https://github.com/SceneView/sceneview-android/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
  <a href="https://developer.android.com/about/versions/nougat"><img src="https://img.shields.io/badge/API-24%2B-brightgreen.svg" alt="API Level" /></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-2.3.10-7F52FF.svg?logo=kotlin" alt="Kotlin" /></a>
  <a href="https://github.com/google/filament"><img src="https://img.shields.io/badge/Filament-v1.70.0-yellow" alt="Filament" /></a>
  <a href="https://github.com/google-ar/arcore-android-sdk"><img src="https://img.shields.io/badge/ARCore-v1.53.0-c961cb" alt="ARCore" /></a>
</p>

<p align="center">
  <a href="https://discord.gg/UbNDDBTNqb"><img src="https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square" alt="Discord" /></a>
  <a href="https://opencollective.com/sceneview"><img src="https://opencollective.com/sceneview/tiers/badge.svg?label=Donators%20" alt="Open Collective" /></a>
  <a href="https://github.com/SceneView/sceneview-android/stargazers"><img src="https://img.shields.io/github/stars/SceneView/sceneview-android?style=flat-square" alt="Stars" /></a>
</p>

---

> ## 3D is just Compose UI.

Write a `Scene { }` the same way you write a `Column { }`. Nodes are composables.
Lifecycle is automatic. State drives everything.

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
    }
}
```

Same pattern. Same Kotlin. Same mental model -- now with depth.

No engine lifecycle callbacks. No `addChildNode` / `removeChildNode`. No `onResume`/`onPause`
overrides. No manual cleanup. The Compose runtime handles all of it.

---

## Why SceneView?

| | **SceneView** | Sceneform | Raw Filament | Raw ARCore | Unity | Rajawali |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Compose-native** | Yes | No | No | No | No | No |
| **Actively maintained** | Yes | Deprecated | Yes | Yes | Yes | Dormant |
| **Declarative scene graph** | Yes | No | No | No | No | No |
| **glTF/GLB loading** | 1 line | 10+ lines | 50+ lines | N/A | Built-in | Plugin |
| **AR support** | Built-in | Built-in | No | Low-level | Plugin | No |
| **APK size overhead** | ~5 MB | ~8 MB | ~3 MB | ~1 MB | 50+ MB | ~2 MB |
| **Learning curve** | Low | Medium | Very High | Very High | Medium | Medium |
| **Compose lifecycle** | Automatic | Manual | Manual | Manual | N/A | Manual |
| **Physics** | Built-in | No | No | No | Built-in | No |
| **Android-native** | Yes | Yes | Yes | Yes | No | Yes |

**SceneView is the only Compose-native 3D/AR SDK for Android.** It replaced Google Sceneform (deprecated 2021) and wraps Filament + ARCore into a high-level API that feels like writing regular Compose UI.

---

## Table of Contents

- [Quick Start](#quick-start)
- [3D with Compose](#3d-with-compose)
- [AR with Compose](#ar-with-compose)
- [SceneScope DSL Reference](#scenescope-dsl)
- [ARSceneScope DSL Reference](#arscenescope-dsl)
- [Samples](#samples)
- [Platform Roadmap](#platform-roadmap)
- [Made with SceneView](#made-with-sceneview)
- [Community](#community)
- [Support the Project](#support-the-project)

---

## Quick Start

### 1. Add the dependency

```gradle
// build.gradle.kts (app module)
dependencies {
    // 3D only
    implementation("io.github.sceneview:sceneview:3.2.0")

    // AR + 3D (includes sceneview -- no need to add both)
    implementation("io.github.sceneview:arsceneview:3.2.0")
}
```

### 2. Drop a model into your Compose screen

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

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
        modelInstance?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
        }
    }
}
```

That's it. Orbit camera, HDR lighting, PBR rendering, gesture interaction, automatic lifecycle management. Zero boilerplate.

### 3. For AR, add manifest entries

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.ar" android:required="true" />

<application>
    <meta-data android:name="com.google.ar.core" android:value="required" />
</application>
```

---

## 3D with Compose

`Scene` is a `@Composable` that renders a Filament 3D viewport. Think of it as a `Box` that adds
a third dimension -- everything inside its trailing block is declared with the **SceneScope** DSL.

### Async model loading

`rememberModelInstance` returns `null` while the file loads on `Dispatchers.IO`, then triggers
recomposition. The node appears automatically when ready:

```kotlin
Scene {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 0.5f)
    }
}
```

### Node hierarchy

Nodes nest exactly like Compose UI -- every node accepts a `content` trailing lambda:

```kotlin
Scene {
    Node(position = Position(y = 0.5f)) {
        ModelNode(modelInstance = helmet)
        CubeNode(size = Size(0.05f))
    }
}
```

### Reactive state

Pass any `State` directly into node parameters. The scene updates on every state change:

```kotlin
var rotationY by remember { mutableFloatStateOf(0f) }
LaunchedEffect(Unit) { while (true) { withFrameNanos { rotationY += 0.5f } } }

Scene {
    ModelNode(
        modelInstance = helmet,
        rotation = Rotation(y = rotationY)
    )
}
```

### Compose UI inside 3D space

`ViewNode` renders any composable onto a plane in the scene:

```kotlin
val windowManager = rememberViewNodeManager()

Scene(viewNodeWindowManager = windowManager) {
    ViewNode(windowManager = windowManager) {
        Card {
            Text("Hello from 3D!")
            Button(onClick = { /* ... */ }) { Text("Click me") }
        }
    }
}
```

### Physics

Add gravity, bounce, and collision:

```kotlin
Scene {
    val ball = ModelNode(modelInstance = ballInstance, position = Position(y = 3f))
    PhysicsNode(node = ball, restitution = 0.8f, floorY = 0f)
}
```

### Dynamic sky + fog

Time-of-day lighting and atmospheric effects, fully reactive:

```kotlin
var timeOfDay by remember { mutableFloatStateOf(8f) }

Scene {
    DynamicSkyNode(timeOfDay = timeOfDay, turbidity = 4f)
    FogNode(view = view, density = 0.03f, height = 2f)
    ModelNode(modelInstance = scene)
}

Slider(value = timeOfDay, onValueChange = { timeOfDay = it }, valueRange = 0f..24f)
```

### Gesture interaction

`isEditable = true` enables pinch-to-scale, drag-to-move, and two-finger-rotate:

```kotlin
Scene(
    onGestureListener = rememberOnGestureListener(
        onSingleTapConfirmed = { event, node -> println("Tapped: ${node?.name}") }
    )
) {
    ModelNode(modelInstance = helmet, isEditable = true)
}
```

### Surface type

```kotlin
Scene(surfaceType = SurfaceType.Surface)          // SurfaceView, best perf (default)
Scene(surfaceType = SurfaceType.TextureSurface, isOpaque = false)  // TextureView, alpha
```

---

## AR with Compose

`ARScene` is `Scene` with ARCore wired in. The camera is driven by ARCore tracking. Everything
else is declared in the **ARSceneScope** content block. Normal Compose state decides what is in the scene.

### AR in 15 lines

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
the composition. The model appears -- anchored to the physical world. When `anchor` is cleared, the
node is removed and destroyed automatically. **Pure Compose semantics, in AR.**

### Full AR setup

```kotlin
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
    onSessionUpdated = { _, frame -> /* per-frame AR logic */ }
) {
    // ARSceneScope DSL -- all SceneScope nodes + AR-specific nodes
}
```

### Augmented Images

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

### Augmented Faces

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

### Geospatial Streetscape

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

---

## <a name="scenescope-dsl"></a>SceneScope DSL Reference

All composables available inside `Scene { }`:

| Composable | Description |
|---|---|
| `ModelNode(modelInstance, scaleToUnits?)` | Renders a glTF/GLB model. `isEditable = true` for pinch-to-scale and drag-to-rotate. |
| `LightNode(type)` | Directional, point, spot, or sun light |
| `CameraNode()` | Named camera (e.g. imported from a glTF) |
| `CubeNode(size, materialInstance?)` | Box geometry |
| `SphereNode(radius, materialInstance?)` | Sphere geometry |
| `CylinderNode(radius, height, materialInstance?)` | Cylinder geometry |
| `PlaneNode(size, normal, materialInstance?)` | Flat quad geometry |
| `ImageNode(bitmap / fileLocation / resId)` | Image rendered on a plane |
| `VideoNode(player, size?)` | Video on a 3D plane with optional chroma key |
| `ViewNode(windowManager) { ComposeUI }` | **Compose UI rendered as a 3D surface** |
| `MeshNode(primitiveType, vertexBuffer, indexBuffer)` | Custom GPU mesh |
| `PhysicsNode(node, mass, restitution)` | Rigid body simulation -- gravity, floor collision, sleep detection |
| `DynamicSkyNode(timeOfDay, turbidity)` | Time-of-day sun light -- sunrise/sunset with warm color transitions |
| `FogNode(view, density, height, color)` | Atmospheric fog -- distance and height-based volumetric effect |
| `ReflectionProbeNode(filamentScene, environment)` | Local or global IBL (cubemap) override zones |
| `LineNode(start, end)` | Single 3D line segment |
| `PathNode(points, closed)` | 3D polyline through ordered points |
| `BillboardNode(bitmap)` | Camera-facing image quad |
| `TextNode(text, fontSize, textColor)` | Camera-facing text label -- Canvas-rendered on a quad |
| `Node()` | Pivot / group node |

**Gesture sensitivity** -- `Node` exposes `scaleGestureSensitivity: Float` (default `0.5`):

```kotlin
ModelNode(modelInstance = instance, isEditable = true, apply = {
    scaleGestureSensitivity = 0.3f
    editableScaleRange = 0.2f..1.0f
})
```

---

## <a name="arscenescope-dsl"></a>ARSceneScope DSL Reference

`ARScene { }` provides everything from SceneScope plus:

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

---

## Samples

> **Try them all:** [Browse samples on the docs site](https://sceneview.github.io/samples/) | Download APKs from [GitHub Releases](https://github.com/SceneView/sceneview-android/releases)

### 3D Samples

| Sample | What it shows | Links |
|---|---|---|
| [Model Viewer](/samples/model-viewer) | Orbit camera around a glTF model, HDR environment, double-tap to scale | [Source](/samples/model-viewer) |
| [glTF Camera](/samples/gltf-camera) | Use a camera node imported directly from a glTF file | [Source](/samples/gltf-camera) |
| [Camera Manipulator](/samples/camera-manipulator) | Orbit / pan / zoom camera interaction | [Source](/samples/camera-manipulator) |
| [Autopilot Demo](/samples/autopilot-demo) | Full animated scene built entirely with geometry nodes -- no model files needed | [Source](/samples/autopilot-demo) |
| [Physics Demo](/samples/physics-demo) | Tap to throw colored balls -- gravity, floor collision, sleep detection, bounciness control | [Source](/samples/physics-demo) |
| [Dynamic Sky](/samples/dynamic-sky) | Time-of-day sun cycle + turbidity + atmospheric fog slider controls | [Source](/samples/dynamic-sky) |
| [Post-Processing](/samples/post-processing) | Bloom, depth of field, SSAO, vignette, fog, tone mapping, FXAA toggles | [Source](/samples/post-processing) |
| [Line & Path](/samples/line-path) | 3-axis gizmo, spiral, animated sine-wave, Lissajous curves | [Source](/samples/line-path) |
| [Text Labels](/samples/text-labels) | Camera-facing 3D planet labels -- tap to cycle display modes | [Source](/samples/text-labels) |
| [Reflection Probe](/samples/reflection-probe) | Metallic sphere with IBL override, material picker, roughness control | [Source](/samples/reflection-probe) |

### AR Samples

| Sample | What it shows | Links |
|---|---|---|
| [AR Model Viewer](/samples/ar-model-viewer) | Tap-to-place on detected planes, model picker, pinch-to-scale, drag-to-rotate | [Source](/samples/ar-model-viewer) |
| [AR Augmented Image](/samples/ar-augmented-image) | Overlay 3D content on detected real-world images | [Source](/samples/ar-augmented-image) |
| [AR Cloud Anchors](/samples/ar-cloud-anchor) | Host and resolve persistent cross-device anchors via Google Cloud | [Source](/samples/ar-cloud-anchor) |
| [AR Point Cloud](/samples/ar-point-cloud) | Visualize ARCore feature points in real time | [Source](/samples/ar-point-cloud) |

---

## Platform Roadmap

| Version | Focus | Status |
|---|---|---|
| **3.2.0** | Physics, dynamic sky, fog, reflections, lines, text labels | Released |
| **3.3.0** | Raycast, gesture improvements, collision callbacks, AR HDR upgrade | In progress |
| **3.4.0** | LOD, instanced rendering, preloading API | Planned |
| **4.0.0** | Multi-scene, `PortalNode`, Android XR / spatial computing, KMP iOS proof-of-concept | Planned |

See [ROADMAP.md](ROADMAP.md) for the full roadmap with details.

---

## Made with SceneView

Using SceneView in your app? [Open a PR](https://github.com/SceneView/sceneview-android/pulls) to add it here.

<!-- Add your app: | [App Name](link) | Brief description | -->

---

## Community

| Channel | Link |
|---|---|
| Discord | [Join the server](https://discord.gg/UbNDDBTNqb) -- ask questions, share projects, get help |
| GitHub Discussions | [Discussions](https://github.com/SceneView/sceneview-android/discussions) -- feature requests, Q&A |
| YouTube | [SceneView tutorials](https://www.youtube.com/results?search_query=Sceneview+android) |
| Website | [sceneview.github.io](https://sceneview.github.io/) |

### Documentation

- [3D API Reference](https://sceneview.github.io/api/sceneview-android/sceneview/)
- [AR API Reference](https://sceneview.github.io/api/sceneview-android/arsceneview/)
- [Migration Guide (v2.x to v3.0)](MIGRATION.md)
- [Changelog](CHANGELOG.md)

### Related Projects

- [Google Filament](https://github.com/google/filament) -- real-time physically based rendering engine
- [Google ARCore](https://github.com/google-ar/arcore-android-sdk) -- AR tracking and scene understanding

---

## Support the Project

SceneView is open-source and community-funded. If it saves you time, consider giving back:

- [Open Collective](https://opencollective.com/sceneview/contribute/say-thank-you-ask-a-question-ask-for-features-and-fixes-33651) -- one-time or recurring support
- [GitHub Sponsors](https://github.com/sponsors/ThomasGorisse) -- sponsor the maintainer directly
- [SceneView Merchandise](https://sceneview.threadless.com/designs/sceneview)
- Star the repo -- it helps discoverability
- Open a Pull Request -- contributions welcome

---

<p align="center">
  <sub>Built with Google Filament and ARCore. Apache 2.0 License.</sub>
</p>

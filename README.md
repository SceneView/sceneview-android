# SceneView

> **3D & AR for every platform.**

Build 3D and AR experiences with the UI frameworks you already know.
Same concepts, same simplicity — Android, iOS, Web, Desktop, TV, Flutter, React Native.

[![Android](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Android&color=1a73e8)](https://search.maven.org/artifact/io.github.sceneview/sceneview)
[![AR](https://img.shields.io/maven-central/v/io.github.sceneview/arsceneview.svg?label=AR&color=1a73e8)](https://search.maven.org/artifact/io.github.sceneview/arsceneview)
[![MCP](https://img.shields.io/npm/v/sceneview-mcp?label=MCP&color=1a73e8)](https://www.npmjs.com/package/sceneview-mcp)
[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)
[![Sponsors](https://img.shields.io/github/sponsors/ThomasGorisse?label=Sponsors&color=ea4aaa)](https://github.com/sponsors/ThomasGorisse)

---

## Quick look

```kotlin
// Android — Jetpack Compose
Scene(modifier = Modifier.fillMaxSize()) {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true)
    }
}
```

```swift
// iOS — SwiftUI
SceneView(environment: .studio) {
    ModelNode(named: "helmet.usdz")
        .scaleToUnits(1.0)
}
```

No engine boilerplate. No lifecycle callbacks. The runtime handles everything.

---

## Platforms

| Platform | Renderer | Framework | Status |
|---|---|---|---|
| **Android** | Filament | Jetpack Compose | Stable |
| **Android TV** | Filament | Compose TV | Alpha |
| **iOS / macOS / visionOS** | RealityKit | SwiftUI | Alpha |
| **Web** | Filament.js (WASM) | Kotlin/JS + WebXR | Alpha |
| **Desktop** | Software renderer | Compose Desktop | Alpha |
| **Flutter** | Native per platform | PlatformView | Alpha |
| **React Native** | Native per platform | Fabric | Alpha |

---

## Install

**Android** (3D + AR):
```kotlin
dependencies {
    implementation("io.github.sceneview:sceneview:3.3.0")     // 3D
    implementation("io.github.sceneview:arsceneview:3.3.0")   // AR (includes 3D)
}
```

**iOS / macOS / visionOS** (Swift Package Manager):
```
https://github.com/SceneView/SceneViewSwift.git  (from: 3.3.0)
```

**Web** (Kotlin/JS + Filament.js):
```kotlin
dependencies {
    implementation("io.github.sceneview:sceneview-web:3.3.0")
}
```

**Desktop** / **Flutter** / **React Native**: see [samples/](samples/)

---

## 3D scene

`Scene` is a Composable that renders a Filament 3D viewport. Nodes are composables inside it.

```kotlin
Scene(
    modifier = Modifier.fillMaxSize(),
    engine = rememberEngine(),
    modelLoader = rememberModelLoader(engine),
    environment = rememberEnvironment(engine, "envs/studio.hdr"),
    cameraManipulator = rememberCameraManipulator()
) {
    // Model — async loaded, appears when ready
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true)
    }

    // Geometry — procedural shapes
    CubeNode(size = Size(0.2f))
    SphereNode(radius = 0.1f, position = Position(x = 0.5f))

    // Nesting — same as Column { Row { } }
    Node(position = Position(y = 1.0f)) {
        LightNode(apply = { type(LightManager.Type.POINT); intensity(50_000f) })
        CubeNode(size = Size(0.05f))
    }
}
```

### Node types

| Node | What it does |
|---|---|
| `ModelNode` | glTF/GLB model with animations. `isEditable = true` for gestures. |
| `LightNode` | Sun, directional, point, or spot light. `apply` is a **named parameter**. |
| `CubeNode` / `SphereNode` / `CylinderNode` / `PlaneNode` | Procedural geometry |
| `ImageNode` | Image on a plane |
| `ViewNode` | **Compose UI rendered as a 3D surface** |
| `MeshNode` | Custom GPU mesh |
| `Node` | Group / pivot |

---

## AR scene

`ARScene` is `Scene` with ARCore. The camera follows real-world tracking.

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
    anchor?.let {
        AnchorNode(anchor = it) {
            ModelNode(modelInstance = helmet, scaleToUnits = 0.5f)
        }
    }
}
```

Plane detected → `anchor` set → Compose recomposes → model appears. Clear anchor → node removed. **AR state is just Kotlin state.**

### AR node types

| Node | What it does |
|---|---|
| `AnchorNode` | Follows a real-world anchor |
| `AugmentedImageNode` | Tracks a detected image |
| `AugmentedFaceNode` | Face mesh overlay |
| `CloudAnchorNode` | Persistent cross-device anchor |
| `StreetscapeGeometryNode` | Geospatial streetscape mesh |

---

## Apple (iOS / macOS / visionOS)

Native Swift Package built on RealityKit. 17 node types.

```swift
SceneView(environment: .studio) {
    ModelNode(named: "helmet.usdz").scaleToUnits(1.0)
    GeometryNode.cube(size: 0.1, color: .blue).position(x: 0.5)
    LightNode.directional(intensity: 1000)
}
.cameraControls(.orbit)
```

AR on iOS:

```swift
ARSceneView(planeDetection: .horizontal) { position, arView in
    GeometryNode.cube(size: 0.1, color: .blue)
        .position(position)
}
```

**Install:** `https://github.com/SceneView/SceneViewSwift.git` (SPM, from 3.3.0)

---

## Architecture

Each platform uses its **native renderer**. Shared logic lives in KMP.

```
sceneview-core (Kotlin Multiplatform)
├── math, collision, geometry, physics, animation
│
├── sceneview (Android)      → Filament + Jetpack Compose
├── arsceneview (Android)    → ARCore
├── SceneViewSwift (Apple)   → RealityKit + SwiftUI
├── sceneview-web (Web)      → Filament.js + WebXR
└── sceneview-desktop (JVM)  → Compose Desktop
```

---

## Samples

| Sample | Platform | Run |
|---|---|---|
| `samples/android-demo` | Android | `./gradlew :samples:android-demo:assembleDebug` |
| `samples/android-tv-demo` | Android TV | `./gradlew :samples:android-tv-demo:assembleDebug` |
| `samples/ios-demo` | iOS | Open in Xcode |
| `samples/web-demo` | Web | `./gradlew :samples:web-demo:jsBrowserRun` |
| `samples/desktop-demo` | Desktop | `./gradlew :samples:desktop-demo:run` |
| `samples/flutter-demo` | Flutter | `cd samples/flutter-demo && flutter run` |
| `samples/react-native-demo` | React Native | See README |

---

## AI integration

SceneView is on the [MCP Registry](https://registry.modelcontextprotocol.io) — any AI assistant can use it to generate correct 3D/AR code.

```bash
npx sceneview-mcp
```

The MCP server provides API reference, code samples, setup guides, validation, and migration tools for all platforms.

---

## Links

- [Website](https://sceneview.github.io/)
- [Documentation](https://sceneview.github.io/quickstart/)
- [Discord](https://discord.gg/UbNDDBTNqb)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [Migration v2 → v3](MIGRATION.md)

## Support

- [GitHub Sponsors](https://github.com/sponsors/ThomasGorisse) — [Sponsors](SPONSORS.md)
- [Open Collective](https://opencollective.com/sceneview)

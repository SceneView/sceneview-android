# SceneView 3.3.0 Release Notes

**Copy-paste template for GitHub Release.**

---

## Title
SceneView 3.3.0 — Cross-Platform: Android + iOS + macOS + visionOS

## Body

### Highlights

SceneView 3.3.0 is the **first cross-platform release** — shipping 16 node types on iOS/macOS/visionOS via SwiftUI + RealityKit, alongside 8 new Android node types and 6 new sample apps. This is the biggest feature release since the Compose rewrite.

### Cross-platform: SceneViewSwift (NEW)

SceneView now supports **iOS, macOS, and visionOS** via the SceneViewSwift package — a native Swift library built on RealityKit and ARKit.

**16 iOS node types shipping in v3.3.0:**

| Node | What it does |
|---|---|
| `ModelNode` | USDZ/glTF models with animations |
| `GeometryNode` | Box, sphere, cylinder, plane, custom meshes |
| `LightNode` | Directional, point, and spot lights |
| `CameraNode` | Camera configuration and control |
| `MeshNode` | Custom mesh rendering |
| `DynamicSkyNode` | Time-of-day sun with sky model |
| `FogNode` | Atmospheric fog |
| `ReflectionProbeNode` | Local IBL override for reflective surfaces |
| `PhysicsNode` | RealityKit physics simulation — gravity, collision, bounce |
| `LineNode` | Single 3D line segment |
| `PathNode` | 3D polyline through a list of points |
| `TextNode` | Camera-facing text label |
| `BillboardNode` | Camera-facing image quad |
| `ImageNode` | 2D image on a 3D surface |
| `VideoNode` | Video playback on a 3D surface |
| `AugmentedImageNode` | Real-world image detection and overlay (AR) |

**Installation (Swift Package Manager):**

```swift
dependencies: [
    .package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")
]
```

**iOS 3D scene (SwiftUI):**

```swift
import SceneViewSwift

struct ContentView: View {
    var body: some View {
        SceneView {
            ModelNode(named: "robot.usdz")
            LightNode(.directional)
        }
    }
}
```

**iOS AR scene (SwiftUI):**

```swift
ARSceneView { anchor in
    ModelNode(named: "chair.usdz")
        .scale(0.5)
}
```

### New Android node types

| Node | What it does |
|---|---|
| `DynamicSkyNode` | Time-of-day sun with colour model (sunrise → noon → sunset) |
| `FogNode` | Atmospheric fog driven by Compose state |
| `ReflectionProbeNode` | Local IBL override for reflective surfaces |
| `PhysicsNode` | Rigid body simulation — gravity, collision, bounce |
| `LineNode` | Single 3D line segment |
| `PathNode` | 3D polyline through a list of points |
| `TextNode` | Camera-facing text label |
| `BillboardNode` | Camera-facing image quad |

### New samples

- **dynamic-sky** — interactive time-of-day, turbidity, and fog controls
- **reflection-probe** — metallic spheres with local cubemap reflections
- **physics-demo** — tap-to-throw balls with gravity and bounce
- **post-processing** — toggle bloom, depth-of-field, SSAO, and fog
- **line-path** — 3D lines, spirals, axis gizmos, animated sine wave
- **text-labels** — camera-facing text labels on 3D spheres

### MCP server

The `@sceneview/mcp` npm package now includes iOS documentation, enabling AI assistants to generate correct SwiftUI + RealityKit code alongside Android Compose code. Install with `npx @anthropic-ai/create-mcp`.

### Documentation

- **Full docs site launched** at [sceneview.github.io](https://sceneview.github.io/)
- iOS quickstart and samples documentation added
- 20+ pages including quickstart, recipes cookbook, FAQ, architecture guide, and more
- `llms.txt` updated with all new APIs (Android and iOS)

### Getting started

**Android:**
```kotlin
// 3D only
implementation("io.github.sceneview:sceneview:3.3.0")

// 3D + AR
implementation("io.github.sceneview:arsceneview:3.3.0")
```

**iOS / macOS / visionOS:**
```swift
// Package.swift
.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")
```

### Example: Dynamic sky with fog (Android)

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    DynamicSkyNode(timeOfDay = 14f, turbidity = 2f)
    FogNode(view = view, density = 0.05f, color = Color(0xFFCCDDFF))
    rememberModelInstance(modelLoader, "models/scene.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 2.0f)
    }
}
```

### Example: 3D model viewer (iOS)

```swift
SceneView {
    ModelNode(named: "helmet.usdz")
    LightNode(.directional)
    DynamicSkyNode(timeOfDay: 14)
}
```

### Example: Physics (Android)

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    val ball = rememberModelInstance(modelLoader, "models/ball.glb")
    ball?.let {
        val node = ModelNode(modelInstance = it, scaleToUnits = 0.1f)
        PhysicsNode(node = node, mass = 1f, restitution = 0.6f,
            linearVelocity = Position(0f, 5f, -3f), floorY = 0f)
    }
}
```

### Migration

No breaking changes from 3.1.x. Just update the version number.

### Full changelog

See [CHANGELOG](https://sceneview.github.io/changelog/) for the complete list of changes.

---

**Thank you to all contributors!** Join the discussion on [Discord](https://discord.gg/UbNDDBTNqb).

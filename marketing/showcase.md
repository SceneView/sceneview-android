# SceneView — The #1 Cross-Platform 3D & AR SDK

*Why thousands of developers chose SceneView — and why you should too.*

---

## The pitch in 10 seconds

SceneView makes 3D and AR work **exactly like your UI framework**. Jetpack Compose on Android. SwiftUI on iOS, macOS, and visionOS. Nodes are declarative UI. State drives the scene. Lifecycle is automatic. No boilerplate, no manual cleanup, no learning a separate rendering paradigm.

**Android:**
```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    ModelNode(modelInstance = helmet, scaleToUnits = 1.0f, autoAnimate = true)
    LightNode(type = LightManager.Type.SUN, apply = { intensity(100_000.0f) })
}
```

**iOS:**
```swift
SceneView {
    ModelNode(named: "helmet.usdz")
        .scaleToUnits(1.0)
    LightNode(.directional, intensity: 100_000)
}
```

Production-quality 3D viewers. Five lines on each platform. Same patterns you write every day.

---

## What you get

### 26+ Android node types, 16 iOS node types — all declarative

| Category | Android Nodes | iOS Nodes |
|---|---|---|
| **Models** | `ModelNode` — glTF/GLB with animations, gestures, scaling | `ModelNode` — USDZ/glTF with animations |
| **Geometry** | `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode` | `GeometryNode` — box, sphere, cylinder, plane, custom |
| **Lighting** | `LightNode` (sun, point, spot, directional), `DynamicSkyNode`, `ReflectionProbeNode` | `LightNode`, `DynamicSkyNode`, `ReflectionProbeNode` |
| **Atmosphere** | `FogNode` — distance/height fog driven by state | `FogNode` |
| **Media** | `ImageNode`, `VideoNode` (with chromakey), `ViewNode` (any Composable in 3D) | `ImageNode`, `VideoNode` |
| **Text** | `TextNode`, `BillboardNode` — camera-facing labels and UI callouts | `TextNode`, `BillboardNode` |
| **Drawing** | `LineNode`, `PathNode` — 3D polylines, measurements, animated paths | `LineNode`, `PathNode` |
| **Physics** | `PhysicsNode` — rigid body simulation, collision, gravity | `PhysicsNode` — RealityKit physics |
| **AR** | `AnchorNode`, `HitResultNode`, `AugmentedImageNode`, `AugmentedFaceNode`, `CloudAnchorNode`, `StreetscapeGeometryNode` | `AugmentedImageNode` (via ARSceneView) |
| **Structure** | `Node` (grouping/pivots), `CameraNode`, `MeshNode` | `CameraNode`, `MeshNode` |

Every one of these is a declarative UI element. They enter the scene on composition, update when state changes, and destroy themselves when they leave. Zero imperative code.

---

### Cross-platform architecture

```
┌─────────────────────────────────────────────────┐
│                 Application Layer                │
│     Compose UI / SwiftUI / Flutter / React Native│
├─────────────────────────────────────────────────┤
│               SceneView Platform SDKs            │
│                                                   │
│   Android: Scene {} · ARScene {} (Filament)       │
│   Apple: SceneView · ARSceneView (RealityKit)     │
├─────────────────────────────────────────────────┤
│               SceneView Core (KMP)               │
│   Math · Collision · Geometry · Animation ·       │
│   Physics · Scene Graph                           │
├────────────────────┬────────────────────────────┤
│  Google Filament   │       RealityKit            │
│    (Android)       │ (iOS/macOS/visionOS)        │
└────────────────────┴────────────────────────────┘
```

**Key decision:** KMP shares logic, not rendering. Native renderers on each platform for best performance.

---

### Production rendering

**Android — Google Filament:**
The same physically-based rendering engine used inside Google's own apps. PBR, HDR lighting, dynamic shadows, post-processing. 60fps on mid-range devices.

**iOS — RealityKit:**
Apple's native 3D engine. Metal-accelerated PBR, environment-based lighting, shadows. The only path to visionOS spatial computing.

---

### Full AR integration

**Android — ARCore:**
```kotlin
ARScene(
    planeRenderer = true,
    onSessionUpdated = { _, frame ->
        anchor = frame.getUpdatedPlanes()
            .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
            ?.let { frame.createAnchorOrNull(it.centerPose) }
    }
) {
    anchor?.let { a ->
        AnchorNode(anchor = a) {
            ModelNode(modelInstance = sofa, scaleToUnits = 0.5f)
        }
    }
}
```

**iOS — ARKit:**
```swift
ARSceneView { anchor in
    ModelNode(named: "sofa.usdz")
        .scale(0.5)
}
```

**Android AR features:** Plane detection, image tracking, face mesh, cloud anchors, environmental HDR, streetscape geometry, geospatial API.

**iOS AR features:** Plane detection, image tracking, world tracking, face tracking (with ARKit).

---

## What makes SceneView different

### 1. It's your UI framework — not a wrapper

60% of top Play Store apps use Jetpack Compose. SwiftUI is the standard on Apple platforms. SceneView's scene graph IS the UI tree. The runtime owns it.

### 2. Cross-platform with native renderers

The only open-source 3D SDK that's declarative-UI-native on BOTH Android and Apple. Not a lowest-common-denominator wrapper — native performance on each platform.

### 3. Zero boilerplate lifecycle

**Android:** `remember*` helpers manage everything. No `destroy()` calls.
**iOS:** SwiftUI and ARC manage everything. No manual cleanup.

### 4. Thread safety by default

**Android:** Filament requires all JNI calls on the main thread. `rememberModelInstance` handles it.
**iOS:** RealityKit handles threading internally.

### 5. AI-assisted development

SceneView ships with an MCP server and `llms.txt` covering both platforms. AI tools generate correct code for Android and iOS.

---

## Real-world use cases

### E-commerce: product viewer in 10 lines

Replace a static image with an interactive 3D viewer on either platform. The customer orbits the product with one finger.

### Furniture & interior design: AR placement

Let customers see furniture in their living room. Tap to place, pinch to resize. Works on Android (ARCore) and iOS (ARKit).

### Education & training

Interactive 3D models controlled by standard UI components. Works the same on student tablets (Android) and iPads (iOS).

### Gaming & entertainment

`PhysicsNode` provides rigid body simulation on both platforms. Tap-to-throw, collision, gravity.

---

## The numbers

| Metric | Android | iOS |
|---|---|---|
| **Node types** | 26+ composable | 16 SwiftUI |
| **Rendering** | Filament 1.70 — PBR, 60fps | RealityKit — Metal, 60fps |
| **AR backend** | ARCore 1.53 | ARKit |
| **Min version** | API 24 (Android 7.0) | iOS 17+ |
| **Setup** | 1 Gradle line | 1 SPM line |
| **Model viewer** | ~5 lines Kotlin | ~4 lines Swift |
| **AR placement** | ~15 lines Kotlin | ~5 lines Swift |
| **License** | Apache 2.0 | Apache 2.0 |

---

## Get started in 60 seconds

**Android:**
```gradle
// 3D only
implementation("io.github.sceneview:sceneview:3.3.0")

// 3D + AR
implementation("io.github.sceneview:arsceneview:3.3.0")
```

**iOS / macOS / visionOS:**
```swift
// Package.swift or Xcode: File > Add Package Dependencies
.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")
```

**Step 2:** Drop a scene into any screen — Compose or SwiftUI.

**Step 3:** Ship it.

---

## Links

- **GitHub**: [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)
- **Maven Central**: `io.github.sceneview:sceneview:3.3.0`
- **Swift Package Manager**: `from: "3.3.0"`
- **Docs**: [sceneview.github.io](https://sceneview.github.io/)
- **Discord**: [discord.gg/UbNDDBTNqb](https://discord.gg/UbNDDBTNqb)
- **MCP server**: `npx @sceneview/mcp` for AI-assisted development
- **Samples**: 15 Android apps + iOS examples in the repository

---

*SceneView is open source (Apache 2.0). Built on Google Filament + ARCore (Android) and RealityKit + ARKit (Apple). Used in production by apps on Google Play and the App Store.*

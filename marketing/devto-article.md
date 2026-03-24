---
title: "SceneView 3.3.0 — Cross-platform 3D & AR for Compose and SwiftUI"
published: true
description: "26+ Android node types, 16 iOS node types, physics, dynamic sky, fog, reflections — native on both platforms, declarative on both."
tags: android, ios, kotlin, swift
canonical_url: https://sceneview.github.io/showcase/
cover_image: # Add a 1000x420 image of a SceneView render here
---

You already know how to build a Compose screen. A `Column` with some children. A `Box` with overlapping layers. You've done it a hundred times.

What if a 3D scene worked exactly the same way — on both Android and iOS?

```kotlin
// Android — Jetpack Compose
Scene(modifier = Modifier.fillMaxSize()) {
    ModelNode(modelInstance = helmet, scaleToUnits = 1.0f, autoAnimate = true)
    LightNode(type = LightManager.Type.SUN, apply = { intensity(100_000.0f) })
}
```

```swift
// iOS — SwiftUI
SceneView {
    ModelNode(named: "helmet.usdz")
        .scaleToUnits(1.0)
    LightNode(.directional, intensity: 100_000)
}
```

Same pattern. Same mental model. Native on both platforms — now with depth.

---

## What's new in 3.3.0: Cross-platform

SceneView 3.3.0 is the **first cross-platform release**. The library now supports:

| Platform | UI Framework | Renderer | Status |
|---|---|---|---|
| Android | Jetpack Compose | Google Filament | Stable |
| iOS | SwiftUI | RealityKit | Alpha |
| macOS | SwiftUI | RealityKit | Alpha |
| visionOS | SwiftUI | RealityKit | Alpha |

**Key architecture decision:** KMP shares logic (math, collision, geometry, animations), not rendering. Each platform uses its native renderer for best performance and tooling.

### 16 iOS node types

SceneViewSwift ships with 16 node types built on RealityKit:

`ModelNode`, `GeometryNode`, `LightNode`, `CameraNode`, `MeshNode`, `DynamicSkyNode`, `FogNode`, `ReflectionProbeNode`, `PhysicsNode`, `LineNode`, `PathNode`, `TextNode`, `BillboardNode`, `ImageNode`, `VideoNode`, `AugmentedImageNode`

### Physics

`PhysicsNode` brings rigid body simulation on both platforms. Gravity, collision detection, tap-to-throw.

**Android:**
```kotlin
Scene {
    PhysicsNode(
        node = ball,
        mass = 1.0f,
        restitution = 0.8f,
        floorY = 0f
    )
}
```

**iOS (RealityKit physics):**
```swift
SceneView {
    PhysicsNode(mass: 1.0, restitution: 0.8) {
        GeometryNode(.sphere(radius: 0.15))
            .position(y: 2.5)
    }
}
```

### Dynamic sky

`DynamicSkyNode` drives sun position from a single `timeOfDay` value on both platforms.

### Fog, reflections, lines, text

- `FogNode` — atmospheric fog with density and height falloff
- `ReflectionProbeNode` — local cubemap reflections for metallic surfaces
- `LineNode` / `PathNode` — 3D polylines (measurements, drawing, animated paths)
- `TextNode` / `BillboardNode` — camera-facing text labels in 3D space

All available on both Android and iOS.

### Post-processing (Android)

Bloom, depth-of-field, SSAO, fog — all toggleable from Compose state.

---

## The use case nobody talks about

Most 3D demos show a rotating helmet on a black background. Cool — but who needs that?

The real opportunity: **subtle 3D**. Replace a flat `Image()` on your product page with a `Scene {}`:

**Android:**
```kotlin
val model = rememberModelInstance(modelLoader, "models/shoe.glb")
Scene(
    modifier = Modifier.fillMaxWidth().height(300.dp),
    cameraManipulator = rememberCameraManipulator()
) {
    model?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
}
```

**iOS:**
```swift
SceneView {
    ModelNode(named: "shoe.usdz")
        .scaleToUnits(1.0)
}
.frame(height: 300)
```

The customer orbits the product with one finger. No separate "3D viewer" screen. No Unity integration project. Just a composable or a SwiftUI view.

---

## AR works the same way

**Android:** `ARScene` is `Scene` with ARCore wired in:

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

**iOS:** `ARSceneView` is `SceneView` with ARKit wired in:

```swift
ARSceneView { anchor in
    ModelNode(named: "sofa.usdz")
        .scale(0.5)
}
```

Plane detection, image tracking, and more — as composables on Android, as SwiftUI views on iOS.

---

## ViewNode — the feature nobody else has (Android)

Render **any Composable** directly inside 3D space:

```kotlin
AnchorNode(anchor = sofaAnchor) {
    ModelNode(modelInstance = sofa)
    ViewNode {
        Card {
            Text("Sofa Pro", style = MaterialTheme.typography.titleMedium)
            Text("$599", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = {}) { Text("Buy in AR") }
        }
    }
}
```

A real Compose `Card` with buttons, text fields, images — floating in 3D space next to your AR content. No other Android 3D library does this.

---

## vs. the alternatives

| | SceneView | Sceneform | Unity | Raw ARCore | RealityKit |
|---|---|---|---|---|---|
| **Declarative UI** | Compose + SwiftUI | No | No | No | SwiftUI only |
| **Cross-platform** | Android + Apple | Android only | All | Android only | Apple only |
| **Setup** | 1 line | Archived | Separate pipeline | 500+ lines | N/A |
| **APK/IPA size** | ~5 MB | ~3 MB | 40–350 MB | ~1 MB | N/A |
| **Physics** | Built-in | No | Built-in | No | Built-in |
| **Open source** | Apache 2.0 | Apache 2.0 | Proprietary | Proprietary | Proprietary |
| **Status** | Active | Dead (2021) | Active | No UI layer | Active |

---

## What's next: v4.0

- Multiple `Scene {}` composables on one screen
- `PortalNode` — scene inside a scene (AR portals)
- `SceneView-XR` — Android XR spatial computing
- Deeper KMP integration — more shared logic across platforms
- Flutter and React Native bridges via SceneViewSwift

---

## Get started

**Android:**
```gradle
// 3D only
implementation("io.github.sceneview:sceneview:3.3.0")

// 3D + AR
implementation("io.github.sceneview:arsceneview:3.3.0")
```

**iOS / macOS / visionOS:**
```swift
.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")
```

15 Android sample apps. iOS examples. Full API docs. MCP server for AI-assisted development.

- **GitHub**: [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)
- **Docs**: [sceneview.github.io](https://sceneview.github.io)
- **Discord**: [discord.gg/UbNDDBTNqb](https://discord.gg/UbNDDBTNqb)

---

*SceneView is open source (Apache 2.0). Built on Google Filament and ARCore (Android) + RealityKit and ARKit (Apple).*

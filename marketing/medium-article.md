# 3D is just declarative UI — how SceneView went cross-platform

*Published on [Medium](https://medium.com/) — copy/paste ready*

---

You already know how to build a Compose screen. A `Column` with some children. A `Box` with overlapping layers. You've done it a hundred times.

And if you're an iOS developer, you know SwiftUI — a `VStack` with some children, a `ZStack` with overlapping layers.

What if a 3D scene worked exactly the same way — on both platforms?

```kotlin
// Android — Jetpack Compose
Scene(modifier = Modifier.fillMaxSize()) {
    ModelNode(modelInstance = helmet, scaleToUnits = 1.0f, autoAnimate = true)
    LightNode(type = LightManager.Type.SUN)
}
```

```swift
// iOS — SwiftUI
SceneView {
    ModelNode(named: "helmet.usdz")
        .scaleToUnits(1.0)
    LightNode(.directional)
}
```

Same pattern. Same mental model. Native on both platforms — now with depth.

That's the idea behind SceneView, and I want to show you what it means in practice.

---

## The problem with 3D on mobile

Before SceneView, adding a 3D model to a mobile app looked roughly like this:

**Android:**
1. Create a `SceneView` in XML layout
2. Wire up lifecycle callbacks manually
3. Load your model in `onResume`, check if the engine is ready, handle the async response
4. Add child nodes imperatively
5. Remember to remove them in `onPause`. Or was it `onStop`? Don't forget `destroy()`.

**iOS:**
1. Create a `SCNView` or `ARSCNView` in UIKit
2. Manage the scene graph imperatively
3. Handle ARSession lifecycle manually
4. Or use RealityKit directly — powerful, but no cross-platform story

Even experienced engineers would spend a full day on a basic AR model placement.

---

## SceneView: nodes are declarative UI

The SceneView approach starts from a different premise: **the scene graph should work like the UI tree**.

On Android, nodes are composable functions. On iOS, they follow SwiftUI patterns. They enter the scene on first composition. They're destroyed when they leave. State drives everything.

```kotlin
// Android
var showHelmet by remember { mutableStateOf(true) }

Scene(modifier = Modifier.fillMaxSize()) {
    if (showHelmet) {
        ModelNode(modelInstance = helmet, scaleToUnits = 1.0f)
    }
}
```

```swift
// iOS
@State private var showHelmet = true

SceneView {
    if showHelmet {
        ModelNode(named: "helmet.usdz")
            .scaleToUnits(1.0)
    }
}
```

Toggle the state and the node disappears or appears — properly managed — without a single line of imperative cleanup.

---

## Now cross-platform: v3.3.0

SceneView 3.3.0 is the first cross-platform release. The library uses **native renderers** on each platform:

| Platform | UI Framework | Renderer | Node Types |
|---|---|---|---|
| Android | Jetpack Compose | Google Filament | 26+ |
| iOS | SwiftUI | RealityKit | 16 |
| macOS | SwiftUI | RealityKit | 16 |
| visionOS | SwiftUI | RealityKit | 16 |

**Architecture:** Kotlin Multiplatform shares logic (math, collision, geometry, animation). Each platform uses its native renderer. Best performance, native tooling, native debugging.

### iOS node types (16 — all new in v3.3.0)

`ModelNode`, `GeometryNode`, `LightNode`, `CameraNode`, `MeshNode`, `DynamicSkyNode`, `FogNode`, `ReflectionProbeNode`, `PhysicsNode`, `LineNode`, `PathNode`, `TextNode`, `BillboardNode`, `ImageNode`, `VideoNode`, `AugmentedImageNode`

Every major feature category is covered: models, geometry, lighting, atmosphere, physics, text, drawing, media, and AR.

---

## The entire resource lifecycle

**Android:** Every Filament resource is a remembered value with automatic cleanup:

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/helmet.glb")
    val environment = rememberEnvironment(rememberEnvironmentLoader(engine)) {
        createHDREnvironment("environments/sky_2k.hdr")
            ?: createEnvironment(environmentLoader)
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = environment,
        cameraManipulator = rememberCameraManipulator(),
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000.0f }
    ) {
        modelInstance?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true) }
    }
}
// All resources are destroyed automatically when the composable leaves the tree
```

**iOS:** SwiftUI manages the lifecycle. ARC handles memory.

```swift
struct ModelViewerView: View {
    var body: some View {
        SceneView {
            ModelNode(named: "helmet.usdz")
                .scaleToUnits(1.0)
            LightNode(.directional, intensity: 100_000)
            DynamicSkyNode(timeOfDay: 14)
        }
    }
}
// Resources released when the view disappears
```

---

## AR works the same way

**Android:** `ARScene` is `Scene` with ARCore wired in:

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

**iOS:** `ARSceneView` is `SceneView` with ARKit wired in:

```swift
ARSceneView { anchor in
    ModelNode(named: "helmet.usdz")
        .scale(0.5)
}
```

---

## The use case nobody talks about: subtle 3D

The more interesting question is: **what happens when 3D is easy enough to add to a screen where it's not the main feature?**

SceneView is small enough, and the API is familiar enough, that 3D becomes a finishing touch rather than a major feature:

### Product image → product viewer

Replace a static image in your product detail screen with a 3D scene. The product rotates slowly. The user can orbit it with one finger. About 10 extra lines of code on either platform.

### Education & training

Interactive 3D anatomy models, molecular structures — all controlled by standard UI components. Students manipulate, not just watch.

### Data visualization

A 3D bar chart or rotating globe inside a dashboard widget. The data is still UI state — you're just rendering it in 3D.

---

## Getting started

**Android:**
```gradle
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

The minimal 3D scene:

**Android (3 lines):**
```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    rememberModelInstance(rememberModelLoader(rememberEngine()), "models/helmet.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 1.0f)
    }
}
```

**iOS (4 lines):**
```swift
SceneView {
    ModelNode(named: "helmet.usdz")
        .scaleToUnits(1.0)
    LightNode(.directional)
}
```

---

## Links

- **GitHub**: [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)
- **API Reference (Android 3D)**: [sceneview.github.io/api/sceneview/sceneview](https://sceneview.github.io/api/sceneview/sceneview/)
- **API Reference (Android AR)**: [sceneview.github.io/api/sceneview/arsceneview](https://sceneview.github.io/api/sceneview/arsceneview/)
- **iOS (SceneViewSwift)**: Swift Package Manager — `from: "3.3.0"`
- **Discord**: [discord.gg/UbNDDBTNqb](https://discord.gg/UbNDDBTNqb)
- **MCP server**: `npx @sceneview/mcp` for AI-assisted development

---

*SceneView is open source (Apache 2.0). Built with Google Filament + ARCore (Android) and RealityKit + ARKit (Apple).*

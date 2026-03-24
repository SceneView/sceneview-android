# Introducing SceneView 3.3.0 — Cross-Platform 3D & AR for Compose and SwiftUI

*Published on: Medium / dev.to*
*Tags: Android, iOS, JetpackCompose, SwiftUI, AR, 3D, Kotlin, Swift, CrossPlatform*

---

If you've ever tried to add 3D content or augmented reality to a mobile app, you know the pain. Three hundred lines of boilerplate before you see a single triangle on screen. SurfaceView lifecycle management. Manual gesture handling. Rendering loops. Session management.

SceneView 3.3.0 changes all of that — on **both** Android and iOS.

## What is SceneView?

SceneView is an open-source cross-platform library that brings 3D and AR to **Jetpack Compose** on Android and **SwiftUI** on iOS, macOS, and visionOS. It uses native renderers on each platform — [Filament](https://google.github.io/filament/) + [ARCore](https://developers.google.com/ar) on Android, [RealityKit](https://developer.apple.com/documentation/realitykit/) + [ARKit](https://developer.apple.com/arkit/) on Apple — exposed through simple, declarative APIs.

### Android (Jetpack Compose)

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    Scene(engine = engine, modelLoader = modelLoader) {
        ModelNode(
            modelInstance = rememberModelInstance(modelLoader, "models/car.glb"),
            scaleToUnits = 1.0f
        )
    }
}
```

### iOS (SwiftUI)

```swift
import SceneViewSwift

struct ModelViewerView: View {
    var body: some View {
        SceneView {
            ModelNode(named: "car.usdz")
                .scaleToUnits(1.0)
            LightNode(.directional)
        }
    }
}
```

No lifecycle callbacks. No `onResume` / `onPause` / `onDestroy` chaining. No manual cleanup. The UI framework manages everything.

And AR tap-to-place — the most common AR use case — is just as simple on both platforms:

**Android:**
```kotlin
ARScene(
    planeRenderer = true,
    onSessionUpdated = { session, frame ->
        // ARCore updates automatically
    }
) {
    // Nodes placed here are anchored in the real world
}
```

**iOS:**
```swift
ARSceneView { anchor in
    ModelNode(named: "chair.usdz")
        .scale(0.5)
}
```

## Why Compose and SwiftUI for 3D?

Traditional 3D APIs are imperative: you create objects, store references, update them in callbacks, and tear them down when you're done. This creates a mismatch with modern mobile development, where state drives UI. SceneView resolves that mismatch by treating **3D nodes as composables** (Android) and **views** (iOS).

The mental model maps perfectly on both platforms:

| Compose UI | SceneView 3D (Android) | SwiftUI | SceneView 3D (iOS) |
|---|---|---|---|
| `Column { }` | `Scene { }` | `VStack { }` | `SceneView { }` |
| `Text("Hello")` | `TextNode("Hello", ...)` | `Text("Hello")` | `TextNode(text: "Hello")` |
| `Image(res)` | `ModelNode(instance)` | `Image(name)` | `ModelNode(named: name)` |
| `remember { }` | `rememberModelInstance()` | `@State` | Async loading |
| State → recomposition | State → 3D graph update | State → view update | State → 3D graph update |

Once you know Compose or SwiftUI, you already know SceneView.

## What's new in 3.3.0

### Cross-platform: iOS, macOS, visionOS support

SceneView 3.3.0 introduces **SceneViewSwift** — a native Swift package built on RealityKit and ARKit. It ships with **16 node types** matching the Android API patterns:

`ModelNode`, `GeometryNode`, `LightNode`, `CameraNode`, `MeshNode`, `DynamicSkyNode`, `FogNode`, `ReflectionProbeNode`, `PhysicsNode`, `LineNode`, `PathNode`, `TextNode`, `BillboardNode`, `ImageNode`, `VideoNode`, `AugmentedImageNode`

Install via Swift Package Manager:
```swift
.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")
```

### Compose-native DSL (Android)

Every node type is a composable function that lives inside the `Scene { }` block. State changes trigger automatic 3D graph updates — no manual `setPosition()` calls needed.

```kotlin
var modelPath by remember { mutableStateOf("models/car.glb") }

Scene(engine = engine, modelLoader = modelLoader) {
    ModelNode(
        modelInstance = rememberModelInstance(modelLoader, modelPath),
        scaleToUnits = 1.0f
    )
    // Swap the model: just change the state
    // modelPath = "models/chair.glb" triggers recomposition → new model loads
}
```

### 26+ Android node types, 16 iOS node types

From simple geometry to physics and post-processing:

- **ModelNode** — glTF/GLB models (Android) / USDZ models (iOS) with animations
- **TextNode** — Camera-facing 3D text labels
- **BillboardNode** — Always-facing-camera sprites
- **PhysicsNode** — Rigid body simulation with gravity
- **DynamicSkyNode** — Time-of-day sun lighting
- **FogNode** — Volumetric atmospheric fog
- **VideoNode** — Video on a 3D surface
- **LineNode / PathNode** — Polyline rendering
- **GeometryNode** (iOS) / CubeNode, SphereNode, CylinderNode, PlaneNode (Android) — Geometry primitives
- And more: LightNode, CameraNode, ReflectionProbeNode, ImageNode, MeshNode...
- Android extras: ViewNode (embed any Compose UI in 3D), AugmentedFaceNode, CloudAnchorNode, StreetscapeGeometryNode

### Resource loading without threading headaches

**Android:** Filament requires all rendering calls on the main thread — a common source of crashes in low-level code. SceneView handles this automatically:

```kotlin
// This is safe. rememberModelInstance loads async, returns null while loading.
val model = rememberModelInstance(modelLoader, "models/car.glb")
model?.let { ModelNode(modelInstance = it) }
```

**iOS:** RealityKit handles async loading natively. SceneView wraps it cleanly.

### Gesture support built in

**Android:** Pinch-to-scale, two-finger-rotate, tap selection, drag-to-move — all built in.
**iOS:** RealityKit gesture system integrated with SwiftUI gestures.

## Getting started in 5 minutes

### Android

**1. Add the dependency:**
```kotlin
// build.gradle.kts — 3D only
implementation("io.github.sceneview:sceneview:3.3.0")

// 3D + AR
implementation("io.github.sceneview:arsceneview:3.3.0")
```

**2. Write your first scene:**
```kotlin
@Composable
fun MyScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio.hdr")!!
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = environment
    ) {
        val model = rememberModelInstance(modelLoader, "models/car.glb")
        model?.let {
            ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true)
        }
    }
}
```

### iOS

**1. Add the Swift package:**
```swift
// Package.swift or Xcode: File > Add Package Dependencies
.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")
```

**2. Write your first scene:**
```swift
import SceneViewSwift

struct MyScene: View {
    var body: some View {
        SceneView {
            ModelNode(named: "car.usdz")
                .scaleToUnits(1.0)
            LightNode(.directional)
        }
    }
}
```

## The AI-first library

SceneView was designed with AI-assisted development in mind. The library ships with `llms.txt` — a machine-readable API reference that AI coding assistants (Claude, Copilot, Gemini) can use to generate correct 3D and AR code on the first try — for **both** Android and iOS.

An MCP server (`@sceneview/mcp`) provides real-time API access to AI assistants, including iOS documentation.

This is part of a larger philosophy: **if the API is simple enough for an AI to get right, it's simple enough for a developer to get right too.** Complex, low-level APIs produce bugs whether the author is human or AI. Simple, declarative APIs produce correct code.

## What's next

The roadmap continues with:

- **3.4+** — More iOS node parity, deeper KMP core integration
- **4.0.0** — Multi-scene engine sharing, Android XR, PortalNode, deeper KMP iOS integration
- **Flutter / React Native** — PlatformView wrapping SceneViewSwift for cross-framework iOS support
- **visionOS spatial** — Immersive spaces, hand tracking, spatial anchors

## Resources

- **GitHub:** [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)
- **API Reference:** `llms.txt` in the repo root
- **Samples:** 15 Android demo apps + iOS examples included
- **Demo app:** SceneView Demo on Google Play
- **MCP server:** `npx @sceneview/mcp` for AI-assisted development

---

*SceneView is open-source (Apache 2.0 License). Contributions welcome.*

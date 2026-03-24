# SceneView 4.0 — What's coming

*A look at the next major release and why it matters for cross-platform 3D & AR.*

---

## The journey so far

| Version | Theme | Key moment |
|---|---|---|
| **2.x** | View-based Sceneform successor | First Filament + ARCore integration |
| **3.0** | Compose rewrite | Nodes become composables — "3D is just Compose UI" |
| **3.1** | Stability + DX | `rememberModelInstance`, camera manipulator, gesture polish |
| **3.2** | Physics & Spatial UI | `PhysicsNode`, `DynamicSkyNode`, `FogNode`, `LineNode`, `TextNode`, `ReflectionProbeNode`, post-processing |
| **3.3.0** | **Cross-platform** | iOS/macOS/visionOS via SceneViewSwift (16 node types), MCP server, AI-first SDK |
| **4.0** | Multi-scene, portals, XR | The release that makes SceneView a cross-platform standard |

Each release has expanded what's possible without changing the core principle:
**nodes are declarative UI, state drives the scene, lifecycle is automatic.**

v3.3.0 proved the cross-platform architecture works — native renderers (Filament + RealityKit), shared logic (KMP), consistent developer experience. v4.0 builds on that foundation.

---

## What v3.3.0 already delivers (the cross-platform foundation)

### Android (Stable)
- 26+ composable node types
- Google Filament 1.70 — PBR, HDR, 60fps
- Full ARCore integration
- Post-processing: bloom, DOF, SSAO, fog
- MCP server for AI-assisted development

### iOS / macOS / visionOS (Alpha)
- **16 node types** built on RealityKit: ModelNode, GeometryNode, LightNode, CameraNode, MeshNode, DynamicSkyNode, FogNode, ReflectionProbeNode, PhysicsNode, LineNode, PathNode, TextNode, BillboardNode, ImageNode, VideoNode, AugmentedImageNode
- SwiftUI-native API matching Android patterns
- ARKit integration via ARSceneView
- Swift Package Manager installation (`from: "3.3.0"`)
- iOS 17+ / macOS 14+ / visionOS 1+

### Shared (KMP Core)
- Math, collision, geometry, animation algorithms
- Physics simulation
- Scene graph logic
- commonMain → Android + XCFramework (Apple)

---

## 4.0 feature preview

### Multiple `Scene {}` composables on one screen

Today, you get one `Scene` per screen. In 4.0, multiple independent scenes share resources, each with its own camera, environment, and node tree.

**Android:**
```kotlin
@Composable
fun DashboardScreen() {
    Column {
        Scene(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            engine = engine,
            environment = studioEnvironment
        ) {
            ModelNode(modelInstance = product, scaleToUnits = 1.0f)
        }

        Scene(
            modifier = Modifier.size(200.dp),
            engine = engine,
            environment = darkEnvironment
        ) {
            SphereNode(radius = 0.5f, materialInstance = globeMaterial)
        }

        LazyColumn { /* regular content */ }
    }
}
```

**iOS:**
```swift
struct DashboardView: View {
    var body: some View {
        VStack {
            SceneView {
                ModelNode(named: "product.usdz")
            }
            .frame(height: 300)

            SceneView {
                GeometryNode(.sphere(radius: 0.5))
            }
            .frame(width: 200, height: 200)

            List { /* regular content */ }
        }
    }
}
```

**Why it matters:** Dashboards, e-commerce feeds, social timelines — anywhere you want multiple 3D elements without a single Scene owning the full viewport. Works on both platforms.

---

### `PortalNode` — a scene inside a scene

Render a secondary scene inside a 3D frame. Think of it as a window into another world.

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    ModelNode(modelInstance = room, scaleToUnits = 2.0f)

    PortalNode(
        position = Position(0f, 1.5f, -2f),
        size = Size(1.2f, 1.8f),
        scene = portalScene
    ) {
        ModelNode(modelInstance = fantasyLandscape, scaleToUnits = 5.0f)
        DynamicSkyNode(sunPosition = Position(0.2f, 0.8f, 0.3f))
        FogNode(density = 0.05f, color = Color(0.6f, 0.7f, 1.0f))
    }
}
```

**Use cases:** AR portals, product showcases with custom lighting, game level transitions, real estate walkthroughs.

---

### Spatial computing: Android XR + visionOS

**Android XR:**
```kotlin
// New module
implementation("io.github.sceneview:sceneview-xr:4.0.0")

XRScene(modifier = Modifier.fillMaxSize()) {
    ModelNode(
        modelInstance = furniture,
        scaleToUnits = 1.0f,
        position = Position(0f, 0f, -2f)
    )
    ViewNode(position = Position(0.5f, 1.5f, -1.5f)) {
        Card { Text("Tap to customize") }
    }
}
```

**visionOS:** SceneViewSwift already targets visionOS. v4.0 adds immersive space support and spatial anchor integration.

**Why it matters:** Your 3D skills transfer to headsets on both ecosystems. Same `ModelNode`, `LightNode`, `ViewNode` — just placed in spatial space.

---

### Deeper iOS parity

v4.0 closes more gaps between Android and iOS:
- More node types on iOS (approaching Android's 26+)
- Cross-framework bridges: Flutter plugin, React Native module wrapping SceneViewSwift
- Tighter KMP core integration — more shared algorithms

---

### Filament 2.x migration

When Filament 2.x stabilizes, SceneView 4.0 will adopt it for improved rendering, better materials, and reduced memory. Transparent to users — the composable API stays the same.

---

## The v4.0 vision

SceneView started as "make 3D easy on Android." v3.0 proved that 3D could work like Compose. v3.2 added physics, atmosphere, and spatial UI. v3.3.0 went cross-platform.

v4.0 removes the last limitations:

| Limitation today | v4.0 solution |
|---|---|
| One Scene per screen | Multiple independent Scenes (both platforms) |
| Flat scene graph | `PortalNode` — scenes within scenes |
| Phone/tablet only | Android XR + visionOS spatial computing |
| iOS at 16 nodes | Approaching Android parity |
| Separate framework ecosystems | Flutter/React Native bridges via SceneViewSwift |

The goal: **SceneView becomes the standard way to do 3D on mobile and spatial platforms** — from a product thumbnail to a headset experience, all with native APIs.

---

## Timeline

v4.0 is on the [roadmap](https://github.com/SceneView/sceneview/blob/main/ROADMAP.md) as the next major release. Follow the repo for updates.

**You don't need to wait for 4.0.** Everything in v3.3.0 is production-ready on Android and usable (alpha) on iOS today. v4.0 adds capabilities on top — it doesn't replace anything.

**Android:**
```gradle
implementation("io.github.sceneview:sceneview:3.3.0")
implementation("io.github.sceneview:arsceneview:3.3.0")
```

**iOS / macOS / visionOS:**
```swift
.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")
```

---

*[github.com/SceneView/sceneview](https://github.com/SceneView/sceneview) — Apache 2.0 — the #1 cross-platform 3D & AR SDK*

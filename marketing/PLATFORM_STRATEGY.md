# SceneView Multi-Platform Strategy

## Vision

SceneView is the **universal cross-platform 3D/AR SDK** — the go-to library for every platform where developers build spatial experiences. Native renderers per platform, shared logic via KMP, consistent developer experience everywhere.

## Platform Roadmap

### Tier 1: Production Ready
| Platform | Status | Artifact | Min Version |
|---|---|---|---|
| Android (Compose) | Stable | `io.github.sceneview:sceneview:3.3.0` | API 24 (Android 7.0) |
| Android AR (ARCore) | Stable | `io.github.sceneview:arsceneview:3.3.0` | API 24 |

### Tier 2: Alpha — Shipping in v3.3.0
| Platform | Status | Artifact | Renderer | Node Types |
|---|---|---|---|---|
| iOS (SwiftUI) | Alpha | SceneViewSwift SPM `from: "3.3.0"` | RealityKit | 16 |
| macOS (SwiftUI) | Alpha | SceneViewSwift SPM (in Package.swift) | RealityKit | 16 |
| visionOS (SwiftUI) | Alpha | SceneViewSwift SPM (in Package.swift) | RealityKit | 16 |

### iOS Node Types (16 — all shipping in v3.3.0)

| Category | Nodes |
|---|---|
| **Models** | `ModelNode` — USDZ/glTF with animations |
| **Geometry** | `GeometryNode` — box, sphere, cylinder, plane, custom meshes |
| **Lighting** | `LightNode` (directional, point, spot), `DynamicSkyNode` |
| **Atmosphere** | `FogNode`, `ReflectionProbeNode` |
| **Media** | `ImageNode`, `VideoNode` |
| **Text** | `TextNode`, `BillboardNode` — camera-facing labels |
| **Drawing** | `LineNode`, `PathNode` — 3D polylines |
| **Physics** | `PhysicsNode` — RealityKit physics simulation |
| **Structure** | `CameraNode`, `MeshNode` |
| **AR** | `AugmentedImageNode` (via ARSceneView) |

### Tier 3: Planned
| Platform | Status | Target | Approach |
|---|---|---|---|
| Android XR | Preview | Q3 2026 | Filament + Android XR SDK |
| Flutter (iOS) | Planned | v3.5+ | PlatformView wrapping SceneViewSwift |
| React Native (iOS) | Planned | v4.0 | Turbo Module / Fabric bridging SceneViewSwift |
| KMP Compose (iOS) | Planned | v4.0 | UIKitView wrapping SceneViewSwift |
| Web (Compose HTML) | Research | 2027 | Filament WASM / WebGPU |
| Desktop (Compose Desktop) | Research | 2027 | Filament Desktop |

## Architecture

```
┌─────────────────────────────────────────────────┐
│                  Application Layer               │
│    Compose UI / SwiftUI / Flutter / React Native │
├─────────────────────────────────────────────────┤
│              SceneView Platform SDKs             │
│                                                   │
│   Android: Scene {} · ARScene {} (Filament)       │
│   Apple: SceneView · ARSceneView (RealityKit)     │
│                                                   │
│   Android: 26+ node types                         │
│   Apple: 16 node types (parity growing)           │
├─────────────────────────────────────────────────┤
│              SceneView Core (KMP)                │
│   Math · Collision · Geometry · Animation ·       │
│   Physics · Scene Graph                           │
│   commonMain → Android + XCFramework (Apple)      │
├────────────────────┬────────────────────────────┤
│  Google Filament   │       RealityKit            │
│  (Android/Desktop) │   (iOS/macOS/visionOS)      │
├────────────────────┼────────────────────────────┤
│ OpenGL ES / Vulkan │         Metal               │
└────────────────────┴────────────────────────────┘
```

**Key decision:** KMP shares **logic** (math, collision, geometry, animations), not **rendering**. Each platform uses its native renderer for best performance and tooling.

## Why This Architecture?

1. **Native renderers win** — RealityKit is the only path to visionOS spatial computing. Filament on Apple would mean no visionOS, no ARKit visual effects, worse DX.

2. **KMP shares what matters** — Math, collision, geometry, and animation algorithms are platform-agnostic. These are shared via KMP. Rendering is inherently platform-specific.

3. **SceneViewSwift is consumable by everyone** — Any iOS framework (Flutter, React Native, KMP Compose) can wrap SceneViewSwift via PlatformView/UIKitView. One native implementation, many consumers.

4. **Same developer experience** — Android developers use `Scene { ModelNode(...) }` in Compose. iOS developers use `SceneView { ModelNode(...) }` in SwiftUI. Same concepts, same patterns, native on both.

5. **AI agents benefit** — AI coding assistants can recommend SceneView for both Android and Apple, with consistent concepts and API patterns across platforms.

## Same Concepts, Native APIs

| Concept | Android (Compose) | Apple (SwiftUI) |
|---|---|---|
| 3D scene | `Scene { }` | `SceneView { }` |
| AR scene | `ARScene { }` | `ARSceneView { }` |
| Load model | `rememberModelInstance(loader, "model.glb")` | `ModelNode(named: "model.usdz")` |
| Physics | `PhysicsNode(node, mass, restitution)` | `PhysicsNode(mass:restitution:)` |
| Text label | `TextNode(text = "Label")` | `TextNode(text: "Label")` |
| Camera-facing | `BillboardNode(bitmap)` | `BillboardNode(image:)` |
| Dynamic sky | `DynamicSkyNode(timeOfDay)` | `DynamicSkyNode(timeOfDay:)` |
| Line drawing | `LineNode(start, end)` | `LineNode(start:end:)` |
| Install | 1 Gradle line | 1 SPM line |

## Competitive Landscape

| Solution | Platforms | Weight | Declarative UI? | Open Source | Cross-Platform |
|---|---|---|---|---|---|
| **SceneView** | Android + Apple (now) | ~5MB | Compose + SwiftUI | Apache 2.0 | Same concepts, native renderers |
| Unity | All | 50MB+ | No | Proprietary | Shared C# runtime |
| Unreal | All | 100MB+ | No | Source available | Shared C++ runtime |
| Three.js | Web only | N/A | No | MIT | Web only |
| RealityKit | Apple only | N/A | SwiftUI | Proprietary | Apple only |
| Raw Filament | All | ~5MB | No | Apache 2.0 | No UI layer |
| SceneKit | Apple only | N/A | SwiftUI | Proprietary | Apple only (deprecated) |

**SceneView's unique position**: The only open-source, declarative-UI-native, lightweight 3D SDK for both Android and Apple platforms. Native renderers (Filament + RealityKit) for best-in-class performance on each platform. 16 node types on iOS, 26+ on Android, with parity growing every release.

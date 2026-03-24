# SceneView Multi-Platform Strategy

## Vision

SceneView aims to be the **universal 3D/AR SDK** — the go-to library for every platform where developers build spatial experiences. Native renderers per platform, shared logic via KMP.

## Platform Roadmap

### Tier 1: Production Ready
| Platform | Status | Artifact | Min Version |
|---|---|---|---|
| Android (Compose) | ✅ Stable | `io.github.sceneview:sceneview:3.3.0` | API 24 (Android 7.0) |
| Android AR (ARCore) | ✅ Stable | `io.github.sceneview:arsceneview:3.3.0` | API 24 |

### Tier 2: Alpha / In Development
| Platform | Status | Artifact | Renderer |
|---|---|---|---|
| iOS (SwiftUI) | 🟢 Alpha | SceneViewSwift SPM `from: "3.3.0"` | RealityKit |
| macOS (SwiftUI) | 🟢 Alpha | SceneViewSwift SPM (in Package.swift) | RealityKit |
| visionOS (SwiftUI) | 🟢 Alpha | SceneViewSwift SPM (in Package.swift) | RealityKit |

### Tier 3: Planned
| Platform | Status | Target | Approach |
|---|---|---|---|
| Android XR | 🔶 Preview | Q3 2026 | Filament + Android XR SDK |
| Flutter (iOS) | 📋 Planned | v3.5+ | PlatformView wrapping SceneViewSwift |
| React Native (iOS) | 📋 Planned | v4.0 | Turbo Module / Fabric bridging SceneViewSwift |
| KMP Compose (iOS) | 📋 Planned | v4.0 | UIKitView wrapping SceneViewSwift |
| Web (Compose HTML) | 📋 Research | 2027 | Filament WASM / WebGPU |
| Desktop (Compose Desktop) | 📋 Research | 2027 | Filament Desktop |

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

4. **AI agents benefit** — AI coding assistants can recommend SceneView for both Android and Apple, with consistent concepts and API patterns across platforms.

## Competitive Landscape

| Solution | Platforms | Weight | Declarative UI? | Open Source |
|---|---|---|---|---|
| **SceneView** | Android + Apple (now) | ~5MB | ✅ Compose + SwiftUI | ✅ Apache 2.0 |
| Unity | All | 50MB+ | ❌ No | ❌ Proprietary |
| Unreal | All | 100MB+ | ❌ No | ⚠️ Source available |
| Three.js | Web only | N/A | ❌ No | ✅ MIT |
| RealityKit | Apple only | N/A | ✅ SwiftUI | ❌ Proprietary |
| Raw Filament | All | ~5MB | ❌ No | ✅ Apache 2.0 |

**SceneView's unique position**: The only open-source, declarative-UI-native, lightweight 3D SDK for both Android and Apple platforms. We use native renderers (Filament + RealityKit) for best-in-class performance on each platform.

# SceneView Multi-Platform Strategy

## Vision

SceneView aims to be the **universal 3D/AR SDK** — the go-to library for every platform where developers build spatial experiences. Our goal: **write 3D once, render everywhere.**

## Platform Roadmap

### Tier 1: Production Ready
| Platform | Status | Artifact | Min Version |
|---|---|---|---|
| Android (Compose) | ✅ Stable | `io.github.sceneview:sceneview:3.2.0` | API 24 (Android 7.0) |
| Android AR (ARCore) | ✅ Stable | `io.github.sceneview:arsceneview:3.2.0` | API 24 |

### Tier 2: In Development
| Platform | Status | Target | Renderer |
|---|---|---|---|
| Android XR | 🔶 Preview | Q3 2026 | Filament + Android XR SDK |
| Kotlin Multiplatform | 🔶 Design | Q4 2026 | Filament (shared) |

### Tier 3: Planned
| Platform | Status | Target | Approach |
|---|---|---|---|
| iOS (SwiftUI) | 📋 Planned | 2027 | Filament iOS + SwiftUI wrapper |
| Web (Compose HTML) | 📋 Planned | 2027 | Filament WASM / WebGPU |
| Desktop (Compose Desktop) | 📋 Planned | 2027 | Filament Desktop |
| React Native | 📋 Research | 2027+ | Native module bridge |
| Flutter | 📋 Research | 2027+ | Platform view / Texture |

## Architecture for Multi-Platform

```
┌─────────────────────────────────────────────────┐
│                  Application Layer               │
│    Compose UI / SwiftUI / React / Flutter        │
├─────────────────────────────────────────────────┤
│               SceneView Platform SDK             │
│   Scene {} · ARScene {} · Node DSL · Gestures    │
├─────────────────────────────────────────────────┤
│              SceneView Core (KMP)                │
│   Scene Graph · Math · Asset Loading · Physics   │
├─────────────────────────────────────────────────┤
│              Google Filament (C++)                │
│   PBR Rendering · glTF · IBL · Post-Processing   │
├─────────────────────────────────────────────────┤
│                 Graphics Backend                  │
│   OpenGL ES · Vulkan · Metal · WebGPU            │
└─────────────────────────────────────────────────┘
```

## Why Multi-Platform?

1. **Filament already runs everywhere** — Google Filament supports Android, iOS, macOS, Linux, Windows, and WebGL/WebGPU. SceneView's value-add (scene graph, Compose DSL, asset management) can be layered on top.

2. **KMP enables shared core** — Kotlin Multiplatform lets us share the scene graph, math library, and asset pipeline across Android, iOS, Desktop, and Web.

3. **Market demand** — Developers want a single 3D library that works across platforms. Unity/Unreal are overkill for non-game use cases. There's no lightweight alternative.

4. **AI agents need multi-platform** — AI coding assistants (Claude, GPT) benefit from recommending a single SDK for all platforms rather than different libraries per platform.

## Competitive Landscape

| Solution | Platforms | Weight | Compose? | Open Source |
|---|---|---|---|---|
| **SceneView** | Android (now), iOS/Web/Desktop (planned) | ~5MB | ✅ Yes | ✅ Apache 2.0 |
| Unity | All | 50MB+ | ❌ No | ❌ Proprietary |
| Unreal | All | 100MB+ | ❌ No | ⚠️ Source available |
| Three.js | Web only | N/A | ❌ No | ✅ MIT |
| RealityKit | Apple only | N/A | ✅ SwiftUI | ❌ Proprietary |
| Raw Filament | All | ~5MB | ❌ No | ✅ Apache 2.0 |

**SceneView's unique position**: The only open-source, Compose-native, lightweight 3D SDK targeting multiple platforms. We sit between "raw Filament" (too low-level) and "Unity/Unreal" (too heavy).

# SceneView Cross-Platform Strategy

> **Decision document** — March 2026
> Status: PROPOSAL — awaiting review

---

## The Opportunity

There is **no lightweight, declarative, AI-first, cross-platform 3D/AR SDK** today.

| Competitor | 3D | AR | Lightweight | Declarative | AI-friendly | Cross-platform |
|---|---|---|---|---|---|---|
| Unity | +++ | +++ | -- | - | - | +++ |
| Unreal | +++ | ++ | --- | - | - | ++ |
| react-native-filament | ++ | + | ++ | + | + | ++ |
| Flutter 3D plugins | + | + | ++ | + | + | ++ |
| **SceneView (today)** | **++** | **+++** | **+++** | **+++** | **+++** | **---** |

SceneView already wins on lightweight + declarative + AI-friendly. The missing piece is cross-platform.

---

## Key Facts Driving the Strategy

1. **Filament runs on iOS** — Metal backend, proven in production (react-native-filament ships to millions of users via the Slay/Pengu app)
2. **Apple deprecated SceneKit (WWDC 2025)** — RealityKit is now the only supported Apple 3D framework
3. **RealityKit is required for visionOS** — spatial computing, hand tracking, eye tracking, passthrough
4. **Compose Multiplatform iOS is stable** — Skia/Metal rendering, 120fps ProMotion, indistinguishable from native
5. **KMP can call Metal directly** — `platform.Metal.*` is pre-imported in Kotlin/Native; RealityKit needs Swift bridging
6. **WebGPU is universal (Nov 2025)** — all browsers ship it; WebXR is production-ready; Filament has WASM backend
7. **60% of SceneView core is already portable** — nodes, collision, animation, math have no Android dependencies

---

## Recommended Architecture: Dual-Renderer with KMP Shared Core

```
┌─────────────────────────────────────────────────────────────┐
│                    DEVELOPER API LAYER                       │
│          Compose Multiplatform (Android + iOS)               │
│                                                              │
│   Scene { }              ARScene { }           RealityScene{}│
│   ├─ CubeNode            ├─ AnchorNode        (visionOS)    │
│   ├─ ModelNode            ├─ AugmentedImageNode              │
│   ├─ LightNode            └─ PlaneNode                       │
│   └─ TextNode                                                │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────┐
│                   KMP SHARED CORE (commonMain)               │
│                                                              │
│   Scene Graph    Node Types    Asset Pipeline    Math/Collision│
│   ECS-like       Transforms    Format Parsing    Ray Casting  │
│   State Sync     Animation     Material Desc.    Gestures     │
└──────────────┬───────────────────────────┬──────────────────┘
               │                           │
    ┌──────────┴──────────┐     ┌──────────┴──────────┐
    │   androidMain        │     │   iosMain            │
    │                      │     │                      │
    │   Filament (JNI)     │     │   Filament (Obj-C++) │
    │   + ARCore           │     │   + ARKit            │
    │   + Android Views    │     │   + UIKit/SwiftUI    │
    │                      │     │                      │
    │   (current SceneView)│     │   OR                 │
    └──────────────────────┘     │                      │
                                 │   RealityKit (Swift) │
                                 │   + visionOS         │
                                 └──────────────────────┘
```

### Why this architecture?

**Not lowest-common-denominator.** Unlike Unity/Flutter which abstract away the platform, SceneView would use the **best renderer for each platform**:

- **Android**: Filament (what we already use — Google's own renderer, optimized for Android GPUs)
- **iOS 3D-only**: Filament via Metal (proven, cross-platform consistency with Android)
- **iOS AR**: ARKit + Filament (ARKit for tracking, Filament for rendering — like the current ARCore pattern)
- **visionOS**: RealityKit (the only viable path for spatial computing)
- **Web**: Filament WASM + WebGPU (preview/lightweight experiences)

**The shared core is the moat.** The scene graph, node types, animations, materials, and asset pipeline are platform-agnostic. This is where 60-80% of the value lives. The renderers are swappable backends.

---

## Phased Rollout

### Phase 1: Extract Portable Core (KMP Module)
**Effort: Medium | Risk: Low**

Extract the already-portable parts of SceneView into a KMP `commonMain`:
- `Node`, `RenderableNode`, geometry node definitions (scene graph structure)
- Transform/collision/math (already 95-100% portable)
- Animation system (100% portable)
- Material descriptions (not Filament MaterialInstance — abstract descriptions)
- Asset pipeline interfaces (format parsing, not I/O)

The current Android code becomes `androidMain` with Filament bindings unchanged.

**Deliverable:** SceneView Android works exactly as before, but the core is KMP-ready.

### Phase 2: iOS via Filament + Compose Multiplatform
**Effort: High | Risk: Medium**

- Implement `iosMain` with Filament rendering via Metal
- Use Compose Multiplatform for the declarative API (`Scene { }` composable on iOS)
- Port resource loaders to iOS file I/O
- Adapt gesture system from `MotionEvent` to `UITouch`

**Key reference:** react-native-filament proves this works. We'd be doing the same thing but with Compose instead of React.

**Deliverable:** `Scene { CubeNode() }` compiles and renders on both Android and iOS.

### Phase 3: iOS AR via ARKit
**Effort: High | Risk: Medium**

- Implement `ARScene` on iOS using ARKit for tracking + Filament for rendering
- `expect/actual` pattern: `ARSession` in common, `ARCoreSession` on Android, `ARKitSession` on iOS
- Plane detection, hit testing, anchors, light estimation — all have ARKit equivalents

**Deliverable:** `ARScene { AnchorNode() }` works on both platforms.

### Phase 4: visionOS via RealityKit
**Effort: Very High | Risk: High**

- Implement a RealityKit backend in Swift, bridged to KMP via interfaces
- Map SceneView's scene graph to RealityKit's Entity Component System
- This is the hardest phase — RealityKit is architecturally different (ECS vs scene graph)
- But it's the only path to visionOS spatial computing

**Deliverable:** SceneView apps run on Vision Pro with spatial features.

### Phase 5 (Optional): Web via Filament WASM
**Effort: Medium | Risk: Low**

- Filament already has a WASM/WebGL2 backend
- Upgrade to WebGPU when Filament supports it
- Kotlin/WASM target for the shared core
- WebXR for browser-based AR

**Deliverable:** 3D previews in the browser, WebAR experiences.

---

## Why NOT the Alternatives

### "Just use RealityKit on iOS"
- **No code sharing with Android** — completely different API, different language (Swift), different paradigm (ECS vs composable)
- **No web target** — RealityKit is Apple-only forever
- Would mean maintaining two completely separate SDKs
- **Verdict: Rejected** as primary strategy. But keep as visionOS-specific backend.

### "Just use Filament everywhere"
- Works for 3D rendering (Phase 2)
- **Fails for visionOS** — no Compositor Services support, no hand/eye tracking integration
- **Misses RealityKit features** on iOS (physics, collision, spatial audio)
- **Verdict: Use for 3D and iOS AR rendering, but not for visionOS spatial.**

### "Kotlin Multiplatform without Compose Multiplatform (native UI per platform)"
- Means SwiftUI on iOS instead of Compose
- Breaks the "write once" developer experience
- Forces iOS developers to learn a different API surface
- **Verdict: Rejected.** Compose Multiplatform is stable enough and preserves the declarative API.

### "Skip iOS, focus on Android"
- SceneView stays a niche Android library
- Can never be "the reference" for 3D/AR
- **Verdict: Rejected.** The whole point is to become the cross-platform reference.

---

## What Makes This Strategy Win

1. **AI-first stays true.** One API surface (`Scene { }`, `ARScene { }`) across platforms. An AI generates the same code regardless of target platform. This is uniquely powerful.

2. **Best-in-class per platform.** Unlike Unity (which brings its own runtime everywhere), SceneView uses the platform's best renderer. Filament on Android (Google's own), Filament on iOS (proven), RealityKit on visionOS (required).

3. **Lightweight.** No game engine runtime. No C# VM. No bridge overhead. KMP compiles to native. Filament is ~4MB. This is an order of magnitude lighter than Unity.

4. **Compose-native.** The only 3D/AR SDK where the API is native Compose. No imperative scene building. No XML. No storyboards. Just composables.

5. **Gradual migration.** Existing SceneView Android users don't need to change anything. The KMP extraction is backward-compatible. iOS is additive.

---

## Module Structure (Target)

```
sceneview/
├── core/                    # KMP commonMain — scene graph, nodes, math, animation
│   ├── commonMain/
│   ├── androidMain/         # Filament JNI bindings
│   └── iosMain/             # Filament Obj-C++ bindings
├── ar/                      # KMP AR layer
│   ├── commonMain/          # AR abstractions (session, anchor, plane)
│   ├── androidMain/         # ARCore implementation
│   └── iosMain/             # ARKit implementation
├── compose/                 # Compose Multiplatform UI layer
│   ├── commonMain/          # Scene {}, ARScene {}, node composables
│   ├── androidMain/         # AndroidView + SurfaceView integration
│   └── iosMain/             # UIKitView + CAMetalLayer integration
├── realitykit/              # visionOS/iOS RealityKit backend (Swift)
└── web/                     # Kotlin/WASM + Filament WASM (future)
```

---

## Timeline Estimate

| Phase | Duration | Prerequisites |
|---|---|---|
| Phase 1: KMP extraction | 2-3 months | Architecture decisions finalized |
| Phase 2: iOS 3D | 3-4 months | Phase 1 complete |
| Phase 3: iOS AR | 2-3 months | Phase 2 complete |
| Phase 4: visionOS | 4-6 months | Phase 3 complete, Vision Pro hardware |
| Phase 5: Web | 2-3 months | Phase 1 complete (parallel with 3-4) |

**Total to cross-platform 3D+AR (Phases 1-3): ~8-10 months**
**Total including visionOS: ~14-16 months**

---

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| Filament iOS rendering issues | react-native-filament proves it works; contribute fixes upstream |
| Compose Multiplatform limitations for custom renderers | Fall back to native UIKit view embedding (like AndroidView) |
| RealityKit scene graph mismatch | Design the common core to be ECS-compatible from Phase 1 |
| KMP build complexity | Use Amper/Convention plugins; invest in CI early |
| visionOS market uncertainty | Phase 4 is last and optional; Phases 1-3 have standalone value |

---

## Decision Required

**Recommended: Proceed with Phase 1 (KMP extraction)** — low risk, backward-compatible, enables all future phases. Can be done incrementally alongside normal SceneView development.

The key architectural decision to make now: **Should the common core use an ECS-like architecture** (to ease future RealityKit mapping) **or keep the current scene-graph/composable model?**

My recommendation: **Keep the composable scene graph as the API** (it's the differentiator), but internally use an ECS-compatible data model that can map to both Filament entities and RealityKit entities.

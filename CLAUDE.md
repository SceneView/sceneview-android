# SceneView — Claude Code guide

## Project purpose

SceneView is an **AI-first SDK**: its primary goal is to enable Claude (and other AI
assistants) to help developers build 3D and AR apps in Jetpack Compose. Every design
decision — API surface, documentation, samples, `llms.txt` — should be optimized so
that when a developer asks an AI "build me an AR app", the AI can produce correct,
complete, working code on the first try.

**Implication for contributors:** when adding or changing APIs, always ask "can an AI
read the docs and generate correct code for this?" If not, simplify the API or improve
the documentation until it can.

## About

SceneView provides 3D and AR as declarative UI for Android (Jetpack Compose, Filament,
ARCore) and iOS (SwiftUI, RealityKit, ARKit), with shared logic in Kotlin Multiplatform.

## Full API reference

See [`llms.txt`](./llms.txt) at the repo root for the complete, machine-readable API reference:
composable signatures, node types, resource loading, threading rules, and common patterns.

## When writing any SceneView code

- Use `Scene { }` for 3D-only scenes (`io.github.sceneview:sceneview:3.2.0`)
- Use `ARScene { }` for augmented reality (`io.github.sceneview:arsceneview:3.2.0`)
- Declare nodes as composables inside the trailing content block — not imperatively
- Load models with `rememberModelInstance(modelLoader, "models/file.glb")` — returns `null`
  while loading, always handle the null case
- `LightNode`'s `apply` is a **named parameter** (`apply = { intensity(…) }`), not a trailing lambda

## Critical threading rule

Filament JNI calls must run on the **main thread**. Never call `modelLoader.createModel*`
or `materialLoader.*` from a background coroutine directly.
`rememberModelInstance` handles this correctly — use it in composables.
For imperative code, use `modelLoader.loadModelInstanceAsync`.

## Samples

| Directory | Demonstrates |
|---|---|
| `samples/model-viewer` | 3D model, HDR environment, orbit camera |
| `samples/ar-model-viewer` | Tap-to-place, plane detection, pinch/rotate |
| `samples/gltf-camera` | Cameras imported from a glTF file |
| `samples/camera-manipulator` | Orbit / pan / zoom camera |
| `samples/ar-augmented-image` | Real-world image detection + overlay |
| `samples/ar-cloud-anchor` | Persistent cross-device anchors |
| `samples/ar-point-cloud` | ARCore feature point visualisation |
| `samples/autopilot-demo` | Autonomous AR demo |

## Module structure

| Module | Purpose |
|---|---|
| `sceneview-core/` | KMP module — portable collision, math, triangulation (commonMain/androidMain/iosMain) |
| `sceneview/` | Android 3D library — `Scene`, `SceneScope`, all node types (depends on sceneview-core) |
| `arsceneview/` | AR layer — `ARScene`, `ARSceneScope`, ARCore integration |
| `samples/common/` | Shared helpers across sample apps |
| `mcp/` | `@sceneview/mcp` — MCP server for AI assistant integration |

## Session continuity

Every Claude Code session MUST read this section first to stay in sync.

### Current state (last updated: 2026-03-23)

- **Active branch**: `main`
- **Latest release**: v3.2.0 (Maven Central)
- **Project philosophy**: SceneView is an AI-first SDK — everything optimized
  so AI assistants can generate correct 3D/AR Compose code on the first try
- **Cross-platform**: Android (Jetpack Compose + Filament) and iOS (SwiftUI + RealityKit)
- **KMP core** (`sceneview-core/`): collision, math, triangulation, animation, geometry,
  physics shared across Android and iOS via Kotlin Multiplatform
- **SceneViewSwift** (`SceneViewSwift/`): iOS library — Swift Package, iOS 17+ / visionOS 1+,
  11 Swift files, demo app, 35+ unit tests
- **Demo app** (`samples/sceneview-demo/`): Play Store ready, 4-tab architecture (Explore,
  Showcase, Gallery, QA), Material 3 Expressive
- **MCP server** (`mcp/`): published npm package for AI assistant integration
- **Website**: `docs/` (MkDocs) + `sceneview.github.io`
- **Pending**: GitHub secrets for Play Store deployment (keystore + service account)

### How to update this section

After completing significant work, update the "Current state" block above with:
1. The active branch name
2. A brief summary of what changed
3. Any new decisions or design choices made
4. Update the date

---

## KMP Roadmap (Kotlin Multiplatform → iOS)

### Current KMP state

`sceneview-core/` already targets `android`, `iosArm64`, `iosSimulatorArm64`, `iosX64` with:
- Collision system, triangulation (Earcut/Delaunay), logging abstraction, math utilities in `commonMain`

`sceneview/` and `arsceneview/` are Android-only (99 + 29 Kotlin files respectively).

### Code shareability analysis

| Category | % of code | Action |
|---|---|---|
| Pure logic (math, geometry, animations, data classes) | ~25% | Move to `sceneview-core/commonMain` |
| Filament JNI wrappers (managers, loaders, nodes) | ~40% | `androidMain` + `expect/actual` bridge for iOS |
| Android framework (SurfaceView, MotionEvent, Context, ARCore) | ~35% | Stay in `androidMain`; needs iOS equivalents |

### Key blockers

1. **Filament iOS bindings** — No official Kotlin/Native artifacts. Requires Swift interop via `cinterop`.
2. **Compose Multiplatform iOS** — Still in beta; iOS UI layer may need native SwiftUI fallback.
3. **ARKit vs ARCore parity** — Cloud anchors and some ARCore features have no ARKit equivalent.

### Phased plan

| Phase | Scope | Complexity |
|---|---|---|
| 1 — Core restructure | Move math/geometry/animation to `sceneview-core/commonMain`; add `expect/actual FilamentBridge` | Medium |
| 2 — iOS surface | Filament Metal backend via Swift → Kotlin/Native `cinterop`; gesture bridges | High |
| 3 — iOS AR (ARKit) | ARKit→ARCore API mapping for planes, image tracking, face tracking | Very High |
| 4 — Gesture system | `expect/actual GestureBridge`; iOS: `UIGestureRecognizer` | Low |
| 5 — Resource abstraction | `expect/actual ResourceLoader`; iOS: bundle API; migrate to Fuel KMP | Low |
| 6 — Test & stabilize | iOS sample, CI/CD, device perf profiling | Medium |

**Estimated total effort:** 12–18 weeks · ~2,500–3,500 new LoC
**Estimated code reuse after KMP:** ~40% of `sceneview` shared between Android and iOS

### MVP target

- Phase 1–2 complete → basic iOS 3D rendering (no AR), ship as `sceneview:4.0.0-ios-alpha`
- Phase 3–4 complete → ARKit integration, ship as `arsceneview:4.0.0-ios-beta`

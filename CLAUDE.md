# SceneView for Android — Claude Code guide

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

SceneView provides 3D and AR as Jetpack Compose composables for Android, built on
Google Filament and ARCore.

## Full API reference

See [`llms.txt`](./llms.txt) at the repo root for the complete, machine-readable API reference:
composable signatures, node types, resource loading, threading rules, and common patterns.

## When writing any SceneView code

- Use `Scene { }` for 3D-only scenes (`io.github.sceneview:sceneview:3.1.1`)
- Use `ARScene { }` for augmented reality (`io.github.sceneview:arsceneview:3.1.1`)
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

### Current state (last updated: 2026-03-21)

- **Active branch**: `claude/identify-project-focus-FU1rl`
- **Project philosophy established**: SceneView is an AI-first SDK — everything optimized
  so AI assistants can generate correct 3D/AR Compose code on the first try
- **KMP migration (Phase 2)**: Extracted portable math module to sceneview-core
  - `sceneview-core/commonMain/math/` — type aliases, transforms, conversions, comparisons, slerp, lookAt, color helpers
  - `sceneview-core/commonMain/collision/` — RayHit now includes getWorldPosition() with kotlin-math
  - `sceneview-core/commonMain/triangulation/` — Earcut now includes Float2 convenience method
  - Added `dev.romainguy:kotlin-math:1.6.0` as `api` dependency to sceneview-core
  - expect/actual `ulp()` for JVM and iOS
  - Removed 14 duplicate collision files + 2 triangulation files from sceneview module
  - sceneview module now depends on sceneview-core via `api project(':sceneview-core')`
  - sceneview Math.kt reduced to Android/Filament-only extensions (Box, Color)
- **llms.txt**: Major update — added 6 missing node types (TextNode, BillboardNode, LineNode,
  PathNode, MeshNode, material creation), complete remember* helpers reference table,
  ARScene session lifecycle callbacks
- **Demo app** (`samples/sceneview-demo/`): **Play Store ready**
  - **4-tab architecture**: Explore, Showcase, Gallery, QA
  - **Explore tab**: Full-screen 3D viewer with orbit camera, environment + model picker chips
    - 8 models: ToyCar, SheenChair, IridescenceLamp (Khronos), GeishaMask, SpaceHelmet,
      SealStatuette, RobotMantis, KawaiiMeka (Sketchfab)
    - 6-environment HDR switcher: Night / Studio / Warm / Sunset / Outdoor / Autumn
    - Auto-rotating camera, gradient overlays
  - **Showcase tab**: Live 3D previews per node type, category filter chips, code snippets
  - **Gallery tab**: 14 cards — 6 realistic models + 5 animated + 3 procedural geometry,
    each card uses a curated HDR environment matching its mood
  - **QA tab**: Stress-test scenes, spring animations
  - **Material 3 Expressive**: `MaterialExpressiveTheme` + dynamic color
  - **Models** (11 GLB): 3 Khronos PBR demos + 3 realistic Sketchfab + 5 animated Sketchfab
  - **Environments** (6 HDR): rooftop_night, studio, studio_warm, outdoor_cloudy, sunset, autumn_field
  - **Build**: `assembleDebug` ✓ · `lint` ✓ · `bundleRelease` ✓
  - **Play Store**: Metadata EN+FR, feature graphic, icon 512px, 4 screenshot mockups, release notes
  - **App icon**: Adaptive icon — isometric 3D cube on M3 purple background
- **Pending**: GitHub secrets for Play Store deployment (keystore + service account)

### Design direction

- **Material 3 Expressive** everywhere — fully rounded shapes, spring/physics animations, dynamic color
- Clean, professional, modern look
- Use expressive components when available: FloatingToolbar, LoadingIndicator, ButtonGroup

### Decisions already made

- M3 1.5.0-alpha15 (latest expressive alpha) over stable 1.3.x
- Dynamic color (Android 12+) with custom light/dark fallback palette
- `MotionScheme.expressive()` as default motion scheme
- LargeTopAppBar with exit-until-collapsed scroll behavior on both screens
- Spring-based animations (DampingRatioLowBouncy) for card expand/collapse

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

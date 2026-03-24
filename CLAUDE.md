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

SceneView provides 3D and AR as declarative UI for Android (Jetpack Compose + Filament + ARCore)
and iOS (SwiftUI + RealityKit + ARKit).

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
| `SceneViewSwift/` | iOS library — Swift Package, SwiftUI + RealityKit (iOS 17+ / visionOS 1+) |
| `mcp/` | `@sceneview/mcp` — MCP server for AI assistant integration |

## Session continuity

Every Claude Code session MUST read this section first to stay in sync.

### Current state (last updated: 2026-03-24, Phase 11 — Sync & Process)

- **Active branch**: `fix/cross-platform-coherence`
- **Project philosophy established**: SceneView is an AI-first SDK — everything optimized
  so AI assistants can generate correct 3D/AR Compose code on the first try
- **KMP migration (Phase 9 complete)**:
  - **sceneview-core: 54 commonMain + 19 commonTest + 3 androidMain + 3 iosMain = 79 files**
  - **sceneview-core commonMain** now contains:
    - `collision/` (15 files) — Vector3, Quaternion, Matrix, Ray, Box, Sphere, Intersections
    - `math/` (5 files) — type aliases, transforms, comparisons, Color (sRGB↔linear, HSV, luminance), CameraProjection (viewToWorld/worldToView/viewToRay, exposure), TransformConversions (local↔world coordinate space)
    - `triangulation/` (2 files) — Earcut, Delaunator
    - `logging/` (1 file) — expect/actual logWarning
    - `rendering/` (7 files) — TransformManagerBridge, NodeTransform, Vertex, SceneNode, ResourceLoader, MaterialDefaults (PBR constants + render priorities), AssetPath (value class for asset paths)
    - `components/` (3 files) — Component, CameraComponent, LightComponent interfaces
    - `gesture/` (3 files) — CameraManipulator, GestureListener with TouchEvent/TouchAction, GestureTransforms (scale damping, rotation damping)
    - `environment/` (1 file) — Environment<L, S> generic data class
    - `animation/` (8 files) — Easing, lerp/slerp, PropertyAnimation, SpringAnimator, AnimationTimeUtils (frame↔time↔fraction), SmoothTransform (interpolation state machine), AnimationPlayback (playback state + time computation + scaleToFitUnits)
    - `scene/` (1 file) — SceneGraph manager with hit testing, node find, frame dispatch
    - `geometries/` (8 files) — GeometryData, Cube, Sphere, Cylinder, Plane, Line, Path, Shape vertex generators
    - `physics/` (1 file) — PhysicsSimulation (Euler integration, floor bounce, restitution, sleep)
    - `utils/` (2 files) — Duration interval/fps calculations, TimeSource (expect/actual nanoTime)
  - **expect/actual abstractions** (3 total): `logWarning`, `ulp`, `nanoTime`
  - kotlin-math 1.6.0 as `api` dependency
  - Removed 14 duplicate collision files + 2 triangulation files from sceneview
  - sceneview depends on sceneview-core via `api project(':sceneview-core')`
  - Build: plugins DSL, default hierarchy template, native target warning suppressed
  - **iOS strategy**: dual approach — KMP for cross-platform apps, native SwiftUI for pure iOS
- **SceneViewSwift** (`SceneViewSwift/`): iOS library (Swift Package, iOS 17+ / visionOS 1+, **11 files** — fully implemented)
  - `SceneView` — full RealityView wrapper with orbit camera, default lighting (sun + fill), drag/pinch/tap gestures, environment support, auto-rotate
  - `ARSceneView` — UIViewRepresentable wrapping ARView, ARWorldTrackingConfiguration, plane detection, tap-to-place raycast, coaching overlay, scene reconstruction
  - `ModelNode` — USDZ loading (by name or URL), collision generation, scaleToUnits, animations (playAll/playAt/stop/pause), grounding shadow
  - `GeometryNode` — cube (with cornerRadius), sphere, cylinder, cone, plane; PBR material support (.simple/.pbr/.unlit), auto collision
  - `TextNode` — 3D extruded text via MeshResource.generateText, centering, custom font
  - `BillboardNode` — always-faces-camera via BillboardComponent, convenience `.text()` factory
  - `LineNode` — line segment (thin cylinder) + axisGizmo factory (X=red, Y=green, Z=blue)
  - `LightNode` — DirectionalLight/PointLight/SpotLight with real RealityKit components, shadow config, color presets (.white/.warm/.cool/.custom)
  - `CameraControls` — orbit camera with inertia, auto-rotation, elevation clamping, configurable sensitivity
  - `SceneEnvironment` — 6 HDR presets with EnvironmentResource loading + thread-safe cache
  - `AnchorNode` — world position and plane anchors for AR content placement
  - **iOS Demo app** (`SceneViewSwift/Examples/SceneViewDemo/`): 3-tab app (Explore, Shapes, AR) with Xcode project
  - **Build**: `swift build` ✓ (macOS CLT), needs Xcode 16+ for full iOS build
- **llms.txt**: Cross-platform — added iOS recipes (model-viewer, procedural, text, AR),
  platform mapping table, KMP shared module description
- **Cross-platform recipes** (`samples/recipes/`): 5 side-by-side Android + iOS recipes
  - model-viewer, ar-tap-to-place, procedural-geometry, text-labels, physics
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
- **Website redesign** (`docs/` MkDocs + `sceneview.github.io` hosted site):
  - MkDocs: Google-inspired palette (blue primary, clean surfaces), rounded cards, hero section
  - Homepage: features grid with screenshots, code examples, samples gallery
  - Parallel session working on `sceneview.github.io` repo: updating repo cards, adding Compose tag,
    quick-start code snippets, demo app section, version badge
  - Direction: modern, rounded, Google-like but not a copy — no colored icons, clean backgrounds
- **Phase 10 — iOS Native** (2026-03-23):
  - All 11 Swift files fully implemented (no more stubs/TODOs)
  - SceneView: real IBL loading via ImageBasedLightComponent, auto-rotate with Task loop, fixed pinch zoom (cumulative→delta)
  - ARSceneView: proper UIViewRepresentable with dynamic content via `(position, arView)` callback
  - New: PathNode (polyline, circle, grid factories)
  - iOS Demo app: 4 Explore scenes (Shapes, Metallic, 3D Text, Gizmo) + 9 shape types + AR tap-to-place
  - Tests: 35+ unit tests (CameraControls, CameraControlsEdgeCases, SceneEnvironment)
  - CI: `.github/workflows/ios.yml` — macOS 15, Xcode 16, build + test + demo
  - App Store: `APP_STORE_SETUP.md` with metadata EN/FR, ExportOptions.plist, icon generator
  - Build: `swift build` passes clean on macOS CLT
  - **Needs Xcode 16+** for full iOS simulator build and App Store upload
- **Pending**: GitHub secrets for Play Store deployment (keystore + service account)
- **Phase 11 — Sync & Process** (2026-03-24):
  - All versions aligned to 3.2.2 (gradle.properties × 4, mcp/package.json, llms.txt, docs, README)
  - `.gitignore` fixed — removed ~120 tracked Swift build artifacts, added SwiftPM/Xcode/MkDocs/Claude patterns
  - MCP server rebuilt from source, repository URLs fixed (sceneview-android → sceneview)
  - PathNode added to iOS section of llms.txt
  - CHANGELOG.md updated with 3.2.2 entry
  - Stale stashes cleaned (3 dropped, 1 kept for TextNode 3.4.0)
  - New Claude Code commands: `/sync-check` (pre-PR verification), `/release` (guided release workflow)
  - Process: run `/sync-check` before every PR to prevent version drift

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

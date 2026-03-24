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
ARCore) and Apple platforms (SwiftUI, RealityKit, ARKit) — iOS, macOS, and visionOS —
with shared logic in Kotlin Multiplatform.

## Full API reference

See [`llms.txt`](./llms.txt) at the repo root for the complete, machine-readable API reference:
composable signatures, node types, resource loading, threading rules, and common patterns.

## When writing any SceneView code

- Use `Scene { }` for 3D-only scenes (`io.github.sceneview:sceneview:3.3.0`)
- Use `ARScene { }` for augmented reality (`io.github.sceneview:arsceneview:3.3.0`)
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
| `samples/dynamic-sky` | Time-of-day sun position, DynamicSkyNode |
| `samples/reflection-probe` | Local cubemap reflections, material picker |
| `samples/physics-demo` | Rigid body physics, colored balls, bounciness |
| `samples/post-processing` | Bloom, SSAO, FXAA, tone mapping, vignette |
| `samples/line-path` | 3D polylines, Lissajous curves, amplitude/frequency |
| `samples/text-labels` | 3D text, planet labels, tap interaction |
| `samples/sceneview-demo` | Play Store demo app, 4-tab Material 3 Expressive |

## Module structure

| Module | Purpose |
|---|---|
| `sceneview-core/` | KMP module — portable collision, math, geometry, animation, physics (commonMain/androidMain/iosMain) |
| `sceneview/` | Android 3D library — `Scene`, `SceneScope`, all node types (Filament renderer) |
| `arsceneview/` | Android AR layer — `ARScene`, `ARSceneScope`, ARCore integration |
| `SceneViewSwift/` | Apple 3D+AR library — `SceneView`, `ARSceneView` (RealityKit renderer, iOS/macOS/visionOS) |
| `samples/common/` | Shared helpers across sample apps |
| `mcp/` | `@sceneview/mcp` — MCP server for AI assistant integration |
| `flutter/` | Flutter plugin scaffold — PlatformView bridge to SceneView (Android + iOS) |
| `react-native/` | React Native module scaffold — Fabric/Turbo bridge to SceneView |

## Session continuity

Every Claude Code session MUST read this section first to stay in sync.

**NOTE FOR OTHER SESSIONS:** Always check `git log --oneline -20` on the current branch
to see recent work before starting. The branch `claude/resume-sceneview-ios-PjPVi` may
need to be merged to main first.

### Current state (last updated: 2026-03-24)

- **Active branch**: `claude/resume-sceneview-ios-PjPVi` (needs merge to main after stabilization)
- **Latest release**: v3.3.0
- **What was done this session (2026-03-24)**:

  Phase 1 — SceneViewSwift stabilization (COMPLETE):
  - New nodes: DynamicSkyNode, FogNode, ReflectionProbeNode, MeshNode
  - Enhanced: ModelNode (named animations, materials, collision), LightNode (shadows, attenuation), CameraNode (FOV, DOF, exposure)
  - 16 node types total in SceneViewSwift

  Phase 2 — Tests (COMPLETE):
  - 65+ new edge case, error condition, and platform tests
  - EdgeCaseTests.swift, PlatformTests.swift
  - All node types have dedicated test files

  Phase 3 — Docs (COMPLETE):
  - iOS Quickstart (quickstart-ios.md)
  - iOS API Cheatsheet (cheatsheet-ios.md)
  - 2 SwiftUI codelabs (3D + AR)
  - iOS Samples page (samples-ios.md)
  - mkdocs.yml updated with all new pages
  - llms.txt updated with complete iOS API reference

  Phase 4 — MCP (COMPLETE):
  - 8 iOS Swift samples in samples.ts
  - get_ios_setup tool in index.ts
  - Swift code validation in validator.ts
  - iOS ARKit guides, best practices, troubleshooting
  - dist rebuilt

  Phase 5 — Full project stabilization (COMPLETE):
  - Android sceneview: KDoc audit, all 21 composables documented
  - Android arsceneview: KDoc for 13 files, **critical bug fix** in Frame.hitTest (direction was using origin)
  - KMP sceneview-core: **4 critical math bugs fixed** (Ray-box intersection, SAT overlap, box-box axes, Delaunator bounds)
  - Sample apps: merge conflict resolved, Material 2→3 migration, hardcoded deps removed, exposed password removed
  - Website: SEO meta tags, structured data, cross-linking, SwiftUI tab on homepage, 4 iOS FAQ
  - Marketing: all 20 files updated for cross-platform messaging
  - MCP: iOS samples verified, validator corrected, 368 tests pass
  - Swift audit: endif guards fixed, duplicate extension extracted, Sendable correctness, missing rotation/discardableResult
  - Test suite: 1081 lines of tests added across 15 files, imports fixed
  - Repo cleanup: .gitignore enriched, SceneViewSwift README updated

  Phase 6 — Cross-framework scaffolds (COMPLETE):
  - Flutter plugin scaffold: `flutter/sceneview_flutter/` — Dart API, Android ComposeView bridge, iOS SceneViewSwift bridge
  - React Native module scaffold: `react-native/react-native-sceneview/` — TypeScript, Android ViewManager, iOS RCTViewManager

- **What's next (for future sessions)**:
  - **PRIORITY 1 — CREATE PR & MERGE TO MAIN**:
    1. Run `gh pr create --base main --head claude/resume-sceneview-ios-PjPVi` with a summary of all 6 phases (28 commits)
    2. If `gh` auth fails, print the URL for manual creation: `https://github.com/SceneView/sceneview/compare/main...claude/resume-sceneview-ios-PjPVi`
    3. PR title: `feat: iOS SceneViewSwift stabilization, tests, docs, MCP, cross-framework scaffolds`
    4. PR body should list all 6 phases with key highlights (see "What was done" above)
    5. Once PR is created, merge it (squash or merge commit, maintainer's choice)
  - Sync llms.txt across docs/docs/llms.txt and mcp/llms.txt
  - Consider v3.4.0-alpha release tag
  - Complete Flutter/React Native bridges (currently scaffolds with TODOs)
  - KMP core XCFramework: build and integrate into SceneViewSwift
  - visionOS spatial features (immersive spaces, hand tracking)
  - GitHub Pages deployment for updated docs site
  - Play Store deployment (needs keystore + service account secrets)

- **Known constraints**:
  - Cannot push directly to `main` (proxy restriction, only claude/* branches)
  - No GitHub API token available — PRs must be created manually on GitHub, or use `gh auth login` first
  - Container is ephemeral — tokens/env don't persist between sessions

- **Project philosophy**: SceneView is an AI-first SDK — everything optimized
  so AI assistants can generate correct 3D/AR Compose code on the first try
- **Cross-platform strategy**: native renderer per platform — Filament (Android),
  RealityKit (Apple: iOS, macOS, visionOS). KMP shares logic, not rendering.
- **KMP core** (`sceneview-core/`): collision, math, triangulation, animation, geometry,
  physics shared across Android and Apple via Kotlin Multiplatform
- **SceneViewSwift** (`SceneViewSwift/`): now 16 node types — Apple library — Swift Package,
  iOS 17+ / macOS 14+ / visionOS 1+, RealityKit + ARKit, 3D + AR, consumable by Swift
  native, Flutter (PlatformView), React Native (Fabric), KMP Compose (UIKitView)
- **Demo app** (`samples/sceneview-demo/`): Play Store ready, 4-tab architecture (Explore,
  Showcase, Gallery, QA), Material 3 Expressive
- **MCP server** (`mcp/`): published npm package for AI assistant integration; now with
  iOS support (8 Swift samples, `get_ios_setup` tool, Swift code validation)
- **Website**: `docs/` (MkDocs) + `sceneview.github.io`
- **Pending**: GitHub secrets for Play Store deployment (keystore + service account)

### How to update this section

After completing significant work, update the "Current state" block above with:
1. The active branch name
2. A brief summary of what changed
3. Any new decisions or design choices made
4. Update the date

---

## Cross-platform strategy

### Architecture: native renderer per platform

```
┌─────────────────────────────────────────────┐
│              sceneview-core (KMP)            │
│     math, collision, geometry, animations    │
│         commonMain → XCFramework             │
└──────────┬──────────────────┬───────────────┘
           │                  │
    ┌──────▼──────┐   ┌──────▼──────┐
    │  sceneview  │   │SceneViewSwift│
    │  (Android)  │   │   (Apple)    │
    │  Filament   │   │  RealityKit  │
    └──────┬──────┘   └──────┬──────┘
           │                  │
     Compose UI        SwiftUI (native)
                       Flutter (PlatformView)
                       React Native (Fabric)
                       KMP Compose (UIKitView)
```

**Key decision:** KMP shares **logic** (math, collision, geometry, animations), not **rendering**.
Each platform uses its native renderer: Filament on Android, RealityKit on Apple.

Rationale:
- RealityKit is the only path to visionOS spatial computing
- Swift Package integration (1 line SPM) vs KMP XCFramework (opaque binary, poor DX)
- SceneViewSwift is consumable by any iOS framework (Flutter, React Native, KMP Compose)
- No Filament dependency on Apple = smaller binary, native debugging, native tooling

### Supported platforms

| Platform | Renderer | Framework | Status |
|---|---|---|---|
| Android | Filament | Jetpack Compose | Stable (v3.3.0) |
| iOS | RealityKit | SwiftUI | Alpha (v3.3.0) |
| macOS | RealityKit | SwiftUI | Alpha (v3.3.0, in Package.swift) |
| visionOS | RealityKit | SwiftUI | Alpha (v3.3.0, in Package.swift) |

### KMP core role

`sceneview-core/` targets `android`, `iosArm64`, `iosSimulatorArm64`, `iosX64` with shared:
- Collision system (Ray, Box, Sphere, Intersections)
- Triangulation (Earcut, Delaunator)
- Geometry generation (Cube, Sphere, Cylinder, Plane, Path, Line, Shape)
- Animation (Spring, Property, Interpolation, SmoothTransform)
- Physics simulation
- Scene graph, math utilities, logging

SceneViewSwift can consume this as an XCFramework for shared algorithms,
while keeping RealityKit as the rendering backend.

### Cross-framework iOS consumption

| Framework | Integration method |
|---|---|
| Swift native | `import SceneViewSwift` via SPM |
| Flutter | Plugin with `PlatformView` wrapping `SceneView`/`ARSceneView` |
| React Native | Turbo Module / Fabric component bridging to SceneViewSwift |
| KMP Compose | `UIKitView` in Compose iOS wrapping the underlying UIView |

### Phased plan (revised)

| Phase | Scope | Complexity |
|---|---|---|
| 1 — SceneViewSwift stabilization | Complete 3D+AR API, add macOS target, tests, docs | Medium |
| 2 — KMP core consumption | Build XCFramework from sceneview-core, integrate into SceneViewSwift | Medium |
| 3 — Cross-framework bridges | Flutter plugin, React Native module | Medium |
| 4 — visionOS spatial | Immersive spaces, hand tracking, spatial anchors | High |
| 5 — Docs & website | Update all docs/README/site for multi-platform (iOS, macOS, visionOS) | Low |

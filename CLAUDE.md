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

One unified showcase app per platform — all features integrated into tabs.

| Directory | Platform | Demonstrates |
|---|---|---|
| `samples/android-demo` | Android | Play Store app — 4-tab Material 3 (3D, AR, Samples, About), 14 demos |
| `samples/android-tv-demo` | Android TV | D-pad controls, model cycling, auto-rotation |
| `samples/web-demo` | Web | Browser 3D viewer, Filament.js (WASM), WebXR AR/VR |
| `samples/ios-demo` | iOS | App Store app — 3-tab SwiftUI (3D, AR, Samples) |
| `samples/desktop-demo` | Desktop | Software 3D renderer, Compose Desktop (Windows/macOS/Linux) |
| `samples/flutter-demo` | Flutter | PlatformView bridge demo (Android + iOS) |
| `samples/react-native-demo` | React Native | Fabric bridge demo (Android + iOS) |
| `samples/common` | Shared | Helpers and utilities for all Android samples |
| `samples/recipes` | Docs | Markdown code recipes (model-viewer, AR, physics, geometry, text) |

## Module structure

| Module | Purpose |
|---|---|
| `sceneview-core/` | KMP module — portable collision, math, geometry, animation, physics (commonMain/androidMain/iosMain/jsMain) |
| `sceneview/` | Android 3D library — `Scene`, `SceneScope`, all node types (Filament renderer) |
| `arsceneview/` | Android AR layer — `ARScene`, `ARSceneScope`, ARCore integration |
| `sceneview-web/` | Web 3D library — Kotlin/JS + Filament.js (same engine as Android, WebGL2/WASM) |
| `SceneViewSwift/` | Apple 3D+AR library — `SceneView`, `ARSceneView` (RealityKit renderer, iOS/macOS/visionOS) |
| `samples/common/` | Shared helpers across sample apps |
| `mcp/` | `@sceneview/mcp` — MCP server for AI assistant integration |
| `flutter/` | Flutter plugin — PlatformView bridge to SceneView (Android + iOS), with native rendering |
| `react-native/` | React Native module — Fabric/Turbo bridge to SceneView (Android + iOS), with native rendering |

## Session continuity

Every Claude Code session MUST read this section first to stay in sync.

**NOTE FOR OTHER SESSIONS:** Always run `/sync-check` at the start and end of every session.
Never say "everything is good" without verifying published packages.

### Current state (last updated: 2026-03-24)

- **Active branch**: `feat/multi-platform-expansion` (branched from main)
- **Latest release**: v3.3.0 (published: Maven Central ✅, npm ✅, SPM ✅, GitHub Release ✅)
- **What was done this session (2026-03-24 evening)**:

  Multi-platform expansion session:
  - **sceneview-web** module: Kotlin/JS + Filament.js (WASM) — same engine as Android, for browser
    - External bindings for filament-js npm package (Engine, Scene, View, Camera, Renderer, AssetLoader)
    - SceneView DSL builder (camera, light, model, environment)
    - sceneview-core JS target added (math, collision, geometry shared with web)
  - **Android TV sample**: `samples/android-tv-demo` — D-pad controls, model cycling, auto-rotation
  - **iOS demo app**: `samples/ios-demo` — SwiftUI 3-tab app (3D, AR, Samples) for App Store
  - **App Store workflow**: `.github/workflows/app-store.yml` — TestFlight CI/CD with certificate management
  - **Release workflow updated**: sceneview-web npm publish added to release.yml
  - **Flutter bridge completed**: Android (ComposeView + Scene composable), iOS (UIHostingController + SceneViewSwift)
  - **React Native bridge completed**: Android (ComposeView + Scene), iOS (UIHostingController + SceneViewSwift)

- **What was done previously (2026-03-24)**:

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

- **What was done (2026-03-25 night, autonomous session)**:
  - WASM target enabled in sceneview-core (wasmJs(), 14 tests)
  - WebXR AR/VR in sceneview-web (6 declaration files, Filament integration, tests)
  - CI fix: material-icons pinned to 1.7.8 (1.10.5 doesn't exist)
  - Desktop software 3D renderer (Compose Canvas, wireframe cube/octahedron/diamond)
  - Android demo app Material 3 (4 tabs, 14 samples, blue branding)
  - Website Kobweb deployed to GitHub Pages (live)
  - SceneView Pro revenue structure (3 passive layers)
  - Platform roadmap: Android XR added, Wear OS / Android Auto excluded
  - Permissions fixed for autonomous night work

- **What's next (for future sessions)**:
  - **PRIORITY 1 — CREATE PR & MERGE `feat/multi-platform-expansion`**:
    1. Run `gh pr create --base main --head feat/multi-platform-expansion`
    2. PR title: `feat: multi-platform expansion — web (Filament.js), Android TV, iOS demo, Flutter/RN bridges`
    3. Merge after review
  - **PRIORITY 2 — Kobweb website** (branch: `feat/multi-platform-expansion`, sources in `website/`):
    - **Architecture**: Kobweb (Compose HTML / Kotlin/JS) + model-viewer web component for live 3D
    - **Now that sceneview-web exists**: use sceneview-web (Filament.js native) instead of model-viewer for the 3D demos — same engine as Android
    - **Sources on disk** (13 Kotlin files):
      - `website/src/jsMain/kotlin/.../pages/`: Index, Quickstart, Samples, Playground, Changelog
      - `website/src/jsMain/kotlin/.../components/`: NavBar, Footer, Layout, CodeBlock, FeatureCard, PlatformTabs, Seo, ModelViewer
      - `website/src/jsMain/kotlin/.../Theme.kt` + `AppEntry.kt`
    - **Design**: Material Design 3 Expressive (28px radius, spring easing, tonal surfaces, Inter font)
    - **SEO/LLM**: llms.txt, llms-full.txt, sitemap.xml, robots.txt, structured-data.json, per-page meta/OG/Twitter
    - **Build config**: `website/build.gradle.kts`, `website/settings.gradle.kts`, `website/gradle/libs.versions.toml` (Kotlin 2.0.20, Kobweb 0.19.2)
    - **Model**: DamagedHelmet.glb (Khronos CC0) in `website/src/jsMain/resources/public/models/`
    - **Status**: scaffold built, M3 styled, model-viewer integrated, build passes (`kobwebExport`), needs assembly + deploy
    - **Process commands**: `/sync-check` (pre-PR verification), `/release` (guided release workflow) — already on main
  - Sync llms.txt across docs/docs/llms.txt and mcp/llms.txt (add web API)
  - Consider v3.4.0-alpha release tag
  - KMP core XCFramework: build and integrate into SceneViewSwift
  - visionOS spatial features (immersive spaces, hand tracking)
  - Compose Desktop module (Filament JNI on Windows/Linux/macOS)
  - **GitHub Pages migration**:
    1. Repo Settings → Pages → Source → "GitHub Actions"
    2. Trigger `workflow_dispatch` on `docs.yml`
  - **Store deployments** (need secrets):
    - Play Store: `UPLOAD_KEYSTORE_BASE64`, `PLAY_STORE_SERVICE_ACCOUNT_JSON`
    - App Store: `IOS_BUILD_CERTIFICATE_BASE64`, `APP_STORE_CONNECT_API_KEY`
  - Publish sceneview-web to npm: `@sceneview/sceneview-web`
  - Publish Flutter plugin to pub.dev
  - Publish React Native module to npm

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
- **Demo app** (`samples/android-demo/`): Play Store ready, 4-tab architecture (Explore,
  Showcase, Gallery, QA), Material 3 Expressive
- **MCP server** (`mcp/`): published npm package for AI assistant integration; now with
  iOS support (8 Swift samples, `get_ios_setup` tool, Swift code validation)
- **Website**: `docs/` (MkDocs), deployed via native GitHub Pages from this repo
  (no longer uses separate `sceneview.github.io` repo — that repo can be archived)
- **Pending**: GitHub secrets for Play Store deployment (keystore + service account)
- **GitHub Pages setup required**: In repo Settings → Pages → Source, select "GitHub Actions"
  (no `PERSONAL_TOKEN` secret needed anymore)

### How to update this section

After completing significant work, update the "Current state" block above with:
1. The active branch name
2. A brief summary of what changed
3. Any new decisions or design choices made
4. Update the date

---

## Long-running session rules

Based on [Anthropic harness design for long-running apps](https://www.anthropic.com/engineering/harness-design-long-running-apps).

### Context management
- **Read `.claude/handoff.md` at session start** — structured handoff artifact
- **Update `.claude/handoff.md` at session end** — what was done, decisions, next steps
- **Context resets > compaction** — when context gets long, start a fresh session with handoff
- **Don't prematurely wrap up** — if approaching context limits, hand off cleanly instead

### Separate generator from evaluator
- **Never self-evaluate** — run `/evaluate` or `/review` as a separate step
- Evaluators should be skeptical; generators should be creative
- If any evaluation criterion scores 1-2/5, it's BLOCKING — fix before pushing

### Sprint contracts
- Before starting a feature chunk, define **what "done" looks like**
- Use the sprint contract template in `.claude/handoff.md`
- Prevents scope creep and ensures alignment

### Decomposition
- **One feature at a time** — break complex work into discrete chunks
- Each chunk should compile, test, and be commitable independently
- Don't attempt end-to-end execution of large features in one go

### Criteria-driven quality
- Use measurable criteria (compile? tests pass? review checklist?)
- Weight criteria: Safety (3x) > Correctness (3x) > API consistency (2x) > Completeness (2x) > Minimality (1x)
- Explicit > vague — "tests pass" beats "looks good"

### Complexity hygiene
- Every harness component encodes an assumption about model limitations
- Regularly stress-test: does this hook/check still add value?
- Remove scaffolding that newer model capabilities make unnecessary

### Available evaluator commands
| Command | Role |
|---|---|
| `/review` | Code review checklist (threading, Compose API, style) |
| `/evaluate` | Independent quality assessment (5 criteria, weighted scores) |
| `/test` | Test coverage audit |
| `/sync-check` | Repo synchronization verification |
| `/contribute` | Full contribution workflow |

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
| Android TV | Filament | Compose TV | Alpha (sample app) |
| Android XR | Jetpack XR SceneCore | Compose XR | Planned |
| iOS | RealityKit | SwiftUI | Alpha (v3.3.0) |
| macOS | RealityKit | SwiftUI | Alpha (v3.3.0, in Package.swift) |
| visionOS | RealityKit | SwiftUI | Alpha (v3.3.0, in Package.swift) |
| Web | Filament.js (WASM) | Kotlin/JS | Alpha (sceneview-web + WebXR) |
| Desktop | Software / Filament JNI | Compose Desktop | Alpha (software renderer) |
| Flutter | Filament / RealityKit | PlatformView | Alpha (bridge implemented) |
| React Native | Filament / RealityKit | Fabric | Alpha (bridge implemented) |

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

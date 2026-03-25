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

### Current state (last updated: 2026-03-25)

- **Active branch**: `main`
- **Latest release**: v3.4.3 (GitHub Release), v3.3.0 (Maven Central — NOT yet published as 3.4.x)
- **MCP server**: `io.github.sceneview/mcp` v3.4.7 (npm + registry)
- **GitHub org**: renamed `SceneView` → `sceneview` (lowercase)
- **Website**: sceneview.github.io (static HTML + playground + embed widget, deployed)

- **What was done (2026-03-25 marathon — 90+ commits)**:

  Massive marathon session covering releases, infrastructure, bugs, branding, docs, and monetization:
  - **Releases**: v3.4.0, v3.4.1, v3.4.2, v3.4.3 on GitHub; MCP v3.4.7 on npm + official registry
  - **Bug fixes**: AR crash #713 (materials for Filament 1.70.0), MeshNode boundingBox #711
  - **15 audit issues fixed**, 6 Dependabot vulns resolved, 28 stale refs cleaned
  - **Website**: static HTML (replaced Kobweb), playground page, 3D embed widget, smart links (/go/)
  - **iOS**: Xcode project, signing config, App Store workflow (macos-14 runner) — needs real Apple cert
  - **Branding**: SVG logos, adaptive icons, favicon, social preview
  - **Docs**: platforms page, Android XR, visionOS, community, issue templates, SPONSORS.md, CONTRIBUTING.md
  - **Pro/Revenue**: strategy doc, MCP Pro API scaffold, GPT Store prep
  - **Revenue active**: GitHub Sponsors (Stripe verified), Polar.sh (test mode), W-8BEN filed
  - **DevOps**: Discord webhook, render_3d_preview MCP tool
  - **GitHub org renamed** to lowercase `sceneview`
  - **LinkedIn**: 3 posts drafted (DO NOT POST without Thomas approval)

- **What was done previously (2026-03-25 night, autonomous session)**:
  - WASM target enabled in sceneview-core (wasmJs(), 14 tests)
  - WebXR AR/VR in sceneview-web (6 declaration files, Filament integration, tests)
  - CI fix: material-icons pinned to 1.7.8 (1.10.5 doesn't exist)
  - Desktop software 3D renderer (Compose Canvas, wireframe cube/octahedron/diamond)
  - Android demo app Material 3 (4 tabs, 14 samples, blue branding)
  - Website Kobweb deployed to GitHub Pages (live)
  - SceneView Pro revenue structure (3 passive layers)
  - Platform roadmap: Android XR added, Wear OS / Android Auto excluded

- **What was done previously (2026-03-24)**:

  Multi-platform expansion (merged to main):
  - sceneview-web module (Kotlin/JS + Filament.js WASM)
  - Android TV sample (`samples/android-tv-demo`)
  - iOS demo app (`samples/ios-demo`)
  - Flutter bridge (`flutter/`), React Native bridge (`react-native/`)
  - SceneViewSwift: 16 node types, full test suite, docs, MCP iOS support
  - KMP sceneview-core: 4 critical math bugs fixed
  - Android: KDoc audit, Frame.hitTest bug fix, Material 3 migration

- **Pending for Thomas (manual actions)**:
  - Login to Apple Developer to create real iOS distribution certificate
  - Publish LinkedIn post (3 drafts ready, DO NOT POST without approval)
  - Polar.sh Go Live (switch from test mode to production)
  - Delete old Play Store apps (AR Wall Paint, AR for TikTok, Info Trafic Nantes)
  - Re-enable Mac sleep (Battery settings)

- **What's next (for future sessions)**:
  - **PRIORITY 1 — Maven Central v3.4.0 publish**: gradle.properties still says 3.3.0, needs version bump + Sonatype publish
  - **PRIORITY 2 — App Store first TestFlight build**: needs real Apple cert (Thomas action first)
  - **PRIORITY 3 — Play Store deploy**: key reset should be done by ~27 March
  - **PRIORITY 4 — GitHub Sponsors tier update**: configure tiers ($5/$15/$50/$200) and benefits
  - **PRIORITY 5 — MCP Pro backend**: connect Stripe + Redis via Cloudflare Workers
  - **PRIORITY 6 — LinkedIn post**: publish when approved (3 drafts ready)
  - Filament JNI Desktop (18-29 day effort, high complexity)
  - KMP core XCFramework: build and integrate into SceneViewSwift
  - visionOS spatial features (immersive spaces, hand tracking)
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
- **MCP server** (`mcp/`): `io.github.sceneview/mcp` v3.4.7 on npm + registry; iOS support
  (8 Swift samples, `get_ios_setup` tool, Swift code validation), `render_3d_preview` tool
- **Website**: sceneview.github.io (static HTML + playground + embed widget, deployed)
- **Pending**: Maven Central v3.4.0 publish, App Store TestFlight (needs Apple cert), Play Store deploy (~27 March)

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

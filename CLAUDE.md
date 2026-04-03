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

## QUALITY RULES (MANDATORY — every session, every commit)

**ZERO TOLERANCE for bugs reaching the user.** Every change must be verified before push.

### Before EVERY push to main:
1. **Compile check**: `./gradlew :sceneview:compileReleaseKotlin :arsceneview:compileReleaseKotlin`
2. **Unit tests**: `./gradlew :sceneview:test :arsceneview:testDebugUnitTest`
3. **Bundle build** (if store-affecting): `./gradlew :samples:android-demo:bundleRelease`
4. **Website JS** (if website changed): `node -c website-static/js/sceneview.js`
5. **Full gate**: `bash .claude/scripts/pre-push-check.sh`

### Rules:
- NEVER push code that doesn't compile
- NEVER push without running tests
- NEVER modify website JS without validating syntax
- NEVER deploy to stores without verifying the bundle builds locally first
- When an agent modifies code, ALWAYS verify compilation before committing
- If a review finds blockers, fix them ALL before pushing — no exceptions

### Quality plan: `.claude/plans/v4.0-quality-plan.md`

## About

SceneView provides 3D and AR as declarative UI for Android (Jetpack Compose, Filament,
ARCore) and Apple platforms (SwiftUI, RealityKit, ARKit) — iOS, macOS, and visionOS —
with shared logic in Kotlin Multiplatform.

## Full API reference

See [`llms.txt`](./llms.txt) at the repo root for the complete, machine-readable API reference:
composable signatures, node types, resource loading, threading rules, and common patterns.

## Design System (Google Stitch)

See [`DESIGN.md`](./DESIGN.md) for the complete design system: colors, typography, spacing,
radius, shadows, motion, breakpoints, and component patterns.

**Rules:**
- Always read `DESIGN.md` before generating any UI code (website, app, docs)
- Use CSS custom properties — never hardcode color/spacing/radius values
- Support both light and dark modes
- Follow Material 3 Expressive patterns

**Google Stitch MCP:** when configured, enables direct UI generation from Stitch projects.
To set up: `npm install @google/stitch-sdk`, then add the Stitch MCP server in Claude Code settings.

## When writing any SceneView code

- Use `SceneView { }` for 3D-only scenes (`io.github.sceneview:sceneview:3.6.1`)
- Use `ARSceneView { }` for augmented reality (`io.github.sceneview:arsceneview:3.6.1`)
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
| `samples/desktop-demo` | Desktop | Wireframe placeholder (NOT SceneView) — Compose Canvas, no Filament |
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
| `samples/` | All demo apps — one per platform (`android-demo`, `ios-demo`, `web-demo`, etc.) |
| `mcp/` | `sceneview-mcp` — MCP server + `packages/` (automotive, gaming, healthcare, interior) + `docs/` |
| `flutter/` | Flutter plugin — PlatformView bridge to SceneView (Android + iOS), with native rendering |
| `react-native/` | React Native module — Fabric/Turbo bridge to SceneView (Android + iOS), with native rendering |
| `assets/` | Shared 3D models (GLB + USDZ) and environments for demos and website |
| `tools/` | Build utilities — Filament material generation, asset download, try-demo script |
| `website-static/` | Static HTML/CSS/JS website (sceneview.github.io) |
| `docs/` | MkDocs documentation source (built by CI) |
| `branding/` | Logo SVGs, brand guide, store asset specs |
| `buildSrc/` | Gradle build logic + detekt config |
| `.github/` | CI workflows + community docs (CoC, Security, Support, Governance, Sponsors, Privacy) |

## Version Location Map

**Source of truth:** `gradle.properties` -> `VERSION_NAME=X.Y.Z`

Every file below MUST be updated when bumping the version. Use `/version-bump` or `bash .claude/scripts/sync-versions.sh --fix`.

| Category | File | Pattern |
|---|---|---|
| **Android** | `gradle.properties` (root) | `VERSION_NAME=X.Y.Z` |
| | `sceneview/gradle.properties` | `VERSION_NAME=X.Y.Z` |
| | `arsceneview/gradle.properties` | `VERSION_NAME=X.Y.Z` |
| | `sceneview-core/gradle.properties` | `VERSION_NAME=X.Y.Z` |
| **npm** | `mcp/package.json` | `"version": "X.Y.Z"` |
| | `mcp/src/index.ts` | version in server info |
| | `sceneview-web/package.json` | `"version": "X.Y.Z"` |
| | `react-native/react-native-sceneview/package.json` | `"version": "X.Y.Z"` |
| **Flutter** | `flutter/sceneview_flutter/pubspec.yaml` | `version: X.Y.Z` |
| | `flutter/.../android/build.gradle` | `version 'X.Y.Z'` |
| | `flutter/.../ios/sceneview_flutter.podspec` | `s.version = 'X.Y.Z'` |
| **Docs** | `llms.txt` | `io.github.sceneview:sceneview:X.Y.Z` |
| | `README.md` | install snippets |
| | `CLAUDE.md` | code examples section |
| | `docs/docs/index.md` | install snippets |
| | `docs/docs/quickstart.md` | dependency snippets |
| | `docs/docs/llms-full.txt` | artifact versions |
| | `docs/docs/cheatsheet.md` | install snippets |
| | `docs/docs/platforms.md` | install line |
| | `docs/docs/android-xr.md` | install snippets |
| | `docs/docs/migration.md` | "upgrade to" version |
| **Website** | `website-static/index.html` | softwareVersion, badge, code |
| | `sceneview.github.io/index.html` | deployed version (separate repo) |
| **Samples** | `samples/android-demo/build.gradle` | versionName default |
| | `sceneview/Module.md` | version ref |
| **Swift** | `SceneViewSwift/` uses git tag `vX.Y.Z` | not a file version |

**Automation:**
- `bash .claude/scripts/sync-versions.sh` — checks all 30+ locations
- `bash .claude/scripts/sync-versions.sh --fix` — auto-fixes mismatches
- `bash .claude/scripts/quality-gate.sh` — full pre-push quality gate
- `bash .claude/scripts/cross-platform-check.sh` — API parity across platforms
- `bash .claude/scripts/release-checklist.sh` — pre-release validation

---

## Session continuity

Every Claude Code session MUST read this section first to stay in sync.

**NOTE FOR OTHER SESSIONS:** Always run `/sync-check` at the start and end of every session.
Never say "everything is good" without verifying published packages.

### Current state (last updated: 2026-04-02, session 25)

- **Active branch**: `main`
- **Latest release**: v3.6.1 (GitHub Release + Maven Central + npm — ALL PUBLISHED)
- **Android rewrite**: SceneRenderer, NodeGestureDelegate/AnimationDelegate/State, ARPermissionHandler
- **Demo app**: Material 3 Expressive, 4 tabs, 40 models, 19 sample demos
- **MCP servers**: sceneview-mcp 3.5.4 on npm (26 tools, 2360 tests), 9 MCPs total
- **sceneview-web**: v3.6.1 on npm (Kotlin/JS + Filament.js)
- **GitHub orgs**: `sceneview`, `sceneview-tools`, `mcp-tools-lab`
- **Website**: redesigned — 8 sections on index, showcase rewritten, playground enhanced (7 platforms, camera manipulator, Open in Claude), docs 404 fixed
- **Playground**: 13 examples, 7 platforms, 23 models, camera manipulator, Open in Claude + AI dropdown
- **Branding**: 22 PNG exports generated, organized in branding/exports/
- **Open Collective**: logo + cover + tiers (Backer $10, Sponsor $50, Gold $200) + 10 tags
- **Claude Artifacts**: documented in llms.txt with CDN templates + 26 model URLs
- **Filament**: 1.70.1 (bumped from 1.70.0, #762 closed)

For full session history, see memory file `project_session_history.md`.
For current priorities and next steps, see `.claude/handoff.md`.

### How to update

After significant work, update this block and `.claude/handoff.md`.

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
| `/version-bump` | Coordinated version update across all platforms |
| `/publish-check` | Verify all published artifacts are up to date |
| `/release` | Full release lifecycle (bump, changelog, tag, publish) |
| `/maintain` | Daily maintenance sweep (CI, issues, deps, quality) |

---

## Automation ecosystem

### Hooks (settings.json)

Hooks trigger automatically on specific Claude Code actions:

| Trigger | When | Action |
|---|---|---|
| Pre-commit version check | `git commit` | Blocks if VERSION_NAME mismatches across modules |
| Post-edit gradle.properties | Any gradle.properties edit | Reminds to update ALL version locations |
| Post-edit Android API | Edit in `sceneview/src/` | Reminds to check SceneViewSwift + llms.txt |
| Post-edit Swift API | Edit in `SceneViewSwift/Sources/` | Reminds to check Android + llms.txt |
| Post-push reminder | `git push` | Reminds to update CLAUDE.md and website |

### Scripts (.claude/scripts/)

| Script | Purpose |
|---|---|
| `sync-versions.sh` | Scan ALL version declarations, report/fix mismatches |
| `cross-platform-check.sh` | Compare Android vs iOS vs Web API surface, report gaps |
| `release-checklist.sh` | Pre-release validation (versions, changelog, tests, etc.) |

### Version location map

Source of truth: `gradle.properties` → `VERSION_NAME=X.Y.Z`

| File | Field |
|---|---|
| `gradle.properties` (root) | `VERSION_NAME=` |
| `sceneview/gradle.properties` | `VERSION_NAME=` |
| `arsceneview/gradle.properties` | `VERSION_NAME=` |
| `sceneview-core/gradle.properties` | `VERSION_NAME=` |
| `mcp/package.json` | `"version":` |
| `llms.txt` | Artifact version references |
| `README.md` | Install snippets |
| `CLAUDE.md` | "Latest release" in session state |

### Published artifact registry

| Artifact | Platform | How to check |
|---|---|---|
| sceneview | Maven Central | Maven search API |
| arsceneview | Maven Central | Maven search API |
| sceneview-mcp | npm | `npm view sceneview-mcp version` |
| sceneview-web | npm | `npm view sceneview-web version` |
| SceneViewSwift | SPM (git tags) | `git tag -l 'v*'` |
| GitHub Release | GitHub | `gh release list` |
| Website | GitHub Pages | sceneview.github.io |

### Quality gates (must pass before any push to main)

1. All versions aligned (run `sync-versions.sh`)
2. No lint errors in library modules
3. Unit tests pass (`./gradlew :sceneview-core:allTests`)
4. MCP tests pass (`cd mcp && npm test`)
5. llms.txt matches current public API
6. CLAUDE.md session state is current
7. No model-viewer or Three.js in website code
8. No external CDN dependencies in website

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
| Desktop | Wireframe placeholder (not SceneView) | Compose Desktop | Placeholder (Filament JNI not available) |
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

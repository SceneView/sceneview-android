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
| `sceneview/` | Core 3D library — `Scene`, `SceneScope`, all node types |
| `arsceneview/` | AR layer — `ARScene`, `ARSceneScope`, ARCore integration |
| `samples/common/` | Shared helpers across sample apps |
| `mcp/` | `@sceneview/mcp` — MCP server for AI assistant integration |

## Session continuity

Every Claude Code session MUST read this section first to stay in sync.

### Current state (last updated: 2026-03-21)

- **Active branch**: `claude/identify-project-focus-FU1rl`
- **Project philosophy established**: SceneView is an AI-first SDK — everything optimized
  so AI assistants can generate correct 3D/AR Compose code on the first try
- **llms.txt**: Major update — added 6 missing node types (TextNode, BillboardNode, LineNode,
  PathNode, MeshNode, material creation), complete remember* helpers reference table,
  ARScene session lifecycle callbacks
- **Demo app** (`samples/sceneview-demo/`): **Play Store ready**
  - **4-tab architecture**: Explore, Showcase, Gallery, QA
  - **Explore tab**: Full-screen 3D viewer with orbit camera, HDR environment, model picker
    - Models: ToyCar, SheenChair, IridescenceLamp + 5 Sketchfab models (sedan, cat, phoenix, etc.)
    - Auto-rotating camera with infinite transition
    - Gradient overlays, model picker chips
  - **Showcase tab**: Live 3D previews in cards (geometry + model nodes render in real-time)
    - Code snippets still visible below previews
    - Category filtering with M3 FilterChips
  - **Gallery tab**: LazyColumn stress-test with 13 live 3D scenes (models + geometry)
  - **QA tab**: Spring-animated progress bar, reset button, scale animation on cards
  - **Material 3 Expressive**: `MaterialExpressiveTheme` + `MotionScheme.expressive()` + dynamic color
  - **Assets**: 11 GLB models (Khronos + Sketchfab), studio_2k.hdr (Poly Haven)
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

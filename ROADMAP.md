# SceneView Roadmap

## Completed

### iOS — SceneViewSwift (Phase 10, shipped)

SceneView now supports iOS via **SwiftUI + RealityKit** (`SceneViewSwift` package).

- 11 Swift files: `SceneView`, `ARSceneView`, `ModelNode`, `GeometryNode`, `TextNode`,
  `BillboardNode`, `LineNode`, `PathNode`, `LightNode`, `CameraControls`, `SceneEnvironment`, `AnchorNode`
- iOS 17+ / visionOS 1+, Swift Package Manager
- iOS Demo app (3-tab: Explore, Shapes, AR)
- 35+ unit tests, CI via GitHub Actions (macOS 15, Xcode 16)

### Kotlin Multiplatform — sceneview-core (Phase 9, shipped)

- 79 files across `commonMain`, `commonTest`, `androidMain`, `iosMain`
- Collision, math, geometry, animation, triangulation, physics — all cross-platform
- `kotlin-math 1.6.0` as API dependency
- Targets: `android`, `iosArm64`, `iosSimulatorArm64`, `iosX64`

---

## 3.3.0 — Physics & Interactions

### SDK
- **`PhysicsNode`** — rigid body / collision via Bullet or JBullet wrapper
- **`RaycastNode`** — tap/drag hit-testing against scene geometry (not just AR planes)
- **Gesture improvements** — scale clamp, rotation axis lock, velocity flick on release
- `onCollision` callback in `SceneScope`

### Ecosystem
- MCP tool: `get_node_reference` — look up any node type's full API from an AI assistant
- Codelab: Physics & Interactions

---

## 3.4.0 — Environment & Lighting

### SDK
- **`DynamicSkyNode`** — time-of-day sun position driven by Compose state
- **`ReflectionProbeNode`** — local cubemap reflections per region
- **`FogNode`** — distance/height fog as composable state
- ARCore `EnvironmentalHDR` upgrade — capture real camera feed for AR environment estimation

### Ecosystem
- Codelab: Dynamic Environments

---

## 3.5.0 — Spatial UI

### SDK
- `ViewNode` depth-ordering fix for edge cases with transparent Compose layers
- Enhanced `BillboardNode` / `TextNode` / `LineNode` / `PathNode` on Android
  (iOS equivalents already shipped in SceneViewSwift)

### Ecosystem
- Codelab: Spatial UI

---

## 4.0.0 — Multi-scene & Platform Expansion

### SDK
- Multiple independent `Scene {}` composables on the same screen sharing one `Engine`
- **`PortalNode`** — render a secondary scene inside a 3D frame (AR portals)
- Filament 2.x migration (when stable)
- **`SceneView-XR`** module — Android XR / spatial computing support

### Ecosystem
- `llms.txt` auto-generated from KDoc at release time
- GitHub Discussions enabled + triage labels for community

---

## Backlog

| Priority | Task | Status |
|----------|------|--------|
| 1 | **Material 3 Expressive design** — Clean, professional UI using `MaterialExpressiveTheme`, `MotionScheme.expressive()`, fully rounded shapes, spring animations, dynamic color, and expressive components across the entire demo app | In progress |

---

## Ongoing

- Keep `llms.txt` and MCP server in sync with every public API change
- Keep SceneViewSwift API surface aligned with Android SDK changes
- Enable GitHub Discussions for community Q&A

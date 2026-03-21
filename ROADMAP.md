# SceneView Roadmap

## 3.2.0 — Physics, Environment, Spatial UI ✅

*Released — all items shipped.*

### SDK
- **`PhysicsNode`** — rigid body / collision via Bullet or JBullet wrapper
- **`DynamicSkyNode`** — time-of-day sun position driven by Compose state
- **`FogNode`** — reactive `View.fogOptions` wrapper (density, height falloff, colour)
- **`ReflectionProbeNode`** — local cubemap reflections per region
- **`LineNode`** / **`PathNode`** — Filament `LINES` primitive; live GPU buffer updates
- **`BillboardNode`** — camera-facing quad via `onFrame` + `lookAt`
- **`TextNode`** — extends `BillboardNode`; Canvas-rendered text bitmap

### Ecosystem
- MCP tool: `get_node_reference` — look up any node type's full API from an AI assistant
- 6 new sample apps: physics-demo, post-processing, dynamic-sky, line-path, text-labels, reflection-probe
- APK distribution workflow — sample APKs attached to every GitHub Release

---

## 3.3.0 — Interaction & Polish

### SDK
- **`RaycastNode`** — tap/drag hit-testing against scene geometry (not just AR planes)
- **Gesture improvements** — scale clamp, rotation axis lock, velocity flick on release
- `onCollision` callback in `SceneScope`
- `ViewNode` depth-ordering fix for edge cases with transparent Compose layers
- ARCore `EnvironmentalHDR` upgrade — capture real camera feed for AR environment estimation

### Quality
- Consolidate legacy collision math to Kotlin-Math (`Float3`, `Quaternion`)
- Migrate `CollisionSystem` from Java to Kotlin
- Reduce TODO/FIXME count to zero in SDK modules

### Ecosystem
- Codelab: Physics & Interactions
- Codelab: Dynamic Environments

---

## 3.4.0 — Performance & Developer Experience

### SDK
- Scene-graph level-of-detail (LOD) — automatic mesh simplification by distance
- Instanced rendering helper — efficient duplicate node rendering
- Preloading API — warm up models/materials before scene is visible

### Ecosystem
- Codelab: Spatial UI
- `llms.txt` auto-generated from KDoc at release time

---

## 4.0.0 — Multi-scene & Platform Expansion

### SDK
- Multiple independent `Scene {}` composables on the same screen sharing one `Engine`
- **`PortalNode`** — render a secondary scene inside a 3D frame (AR portals)
- Filament 2.x migration (when stable)
- **`SceneView-XR`** module — Android XR / spatial computing support

### Ecosystem
- Kotlin Multiplatform proof of concept (iOS via Filament Metal backend)
- GitHub Discussions enabled + triage labels for community

---

## Ongoing

- Keep `llms.txt` and MCP server in sync with every public API change
- Enable GitHub Discussions for community Q&A
- All sample APKs downloadable from every GitHub Release

# SceneView Roadmap

## 3.2.0 — Physics & Interactions

### SDK
- **`PhysicsNode`** — rigid body / collision via Bullet or JBullet wrapper
- **`RaycastNode`** — tap/drag hit-testing against scene geometry (not just AR planes)
- **Gesture improvements** — scale clamp, rotation axis lock, velocity flick on release
- `onCollision` callback in `SceneScope`

### Ecosystem
- MCP tool: `get_node_reference` — look up any node type's full API from an AI assistant
- Codelab: Physics & Interactions

---

## 3.3.0 — Environment & Lighting

### SDK
- **`DynamicSkyNode`** — time-of-day sun position driven by Compose state
- **`ReflectionProbeNode`** — local cubemap reflections per region
- **`FogNode`** — distance/height fog as composable state
- ARCore `EnvironmentalHDR` upgrade — capture real camera feed for AR environment estimation

### Ecosystem
- Codelab: Dynamic Environments

---

## 3.4.0 — Spatial UI

### SDK
- **`BillboardNode`** — node always faces camera (labels, tooltips, UI callouts)
- **`TextNode`** — 3D text geometry
- **`LineNode` / `PathNode`** — 3D polylines (measurements, AR guides, drawing)
- `ViewNode` depth-ordering fix for edge cases with transparent Compose layers

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
- Kotlin Multiplatform proof of concept (iOS via Filament Metal backend)
- GitHub Discussions enabled + triage labels for community

---

## Backlog

| Priority | Task | Status |
|----------|------|--------|
| 1 | **Material 3 Expressive design** — Clean, professional UI using `MaterialExpressiveTheme`, `MotionScheme.expressive()`, fully rounded shapes, spring animations, dynamic color, and expressive components (FloatingToolbar, LoadingIndicator, ButtonGroup) across the entire demo app | In progress |

---

## Ongoing

- Keep `llms.txt` and MCP server in sync with every public API change
- Enable GitHub Discussions for community Q&A

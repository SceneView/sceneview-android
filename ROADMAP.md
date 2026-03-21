# SceneView Roadmap

## 3.2.0 — Shipped

Everything below has been implemented and released:

- **`PhysicsNode`** — rigid body simulation with gravity, collision, bounce
- **`DynamicSkyNode`** — time-of-day sun position driven by Compose state
- **`FogNode`** — distance/height fog as composable state
- **`ReflectionProbeNode`** — local cubemap reflections per region
- **`BillboardNode`** — camera-facing image quad
- **`TextNode`** — camera-facing text labels rendered via Canvas
- **`LineNode` / `PathNode`** — 3D polylines, measurements, animated paths
- Post-processing pipeline (bloom, DoF, SSAO, fog) via Filament View
- 6 new sample apps: dynamic-sky, physics-demo, post-processing, reflection-probe, line-path, text-labels
- MCP server (`@sceneview/mcp`) for AI-assisted development
- Full documentation website with 13 pages

---

## 3.3.0 — Next minor

### New features
- [ ] `AnimationController` — composable-level control over blending, cross-fading, and layered animations
- [ ] `CollisionNode` — declarative collision detection between nodes (bounding box and sphere)
- [ ] `GizmoNode` — visual transform handles for editor-style interaction
- [ ] `ParticleNode` — GPU particle system composable (fire, smoke, sparkles)
- [ ] Material editor support — runtime material parameter modification via Compose state

### Improvements
- [ ] Improved `PhysicsNode` — inter-node collision callbacks, constraints, joints
- [ ] `TextNode` rich text — multiple fonts, sizes, and colors in one label
- [ ] Performance dashboard — composable overlay showing FPS, draw calls, triangle count
- [ ] `ModelNode` LOD — automatic level-of-detail switching based on camera distance
- [ ] `onCollision` callback in `SceneScope`
- [ ] Gesture improvements — scale clamp, rotation axis lock, velocity flick on release

---

## 4.0.0 — Next major

### Multi-scene
- [ ] Multiple independent `Scene { }` composables on one screen
- [ ] Shared `Engine` across scenes with independent cameras and environments
- [ ] 3D content in `LazyColumn`, `Pager`, `BottomSheet`

### Portal rendering
- [ ] `PortalNode` — render a secondary scene inside a 3D frame
- [ ] Independent lighting and environment per portal
- [ ] AR portals — real-world windows into virtual scenes

### SceneView-XR
- [ ] `XRScene { }` composable for Android XR spatial computing
- [ ] Passthrough AR on headsets
- [ ] Spatial UI via `ViewNode` in XR space
- [ ] Hand tracking input

### Platform expansion
- [ ] Kotlin Multiplatform proof of concept (iOS via Filament's Metal backend)
- [ ] Shared scene definition across Android and iOS
- [ ] Platform-specific renderers

### Infrastructure
- [ ] Filament 2.x migration (when stable)
- [ ] `llms.txt` auto-generated from KDoc at release time

---

## Community wishlist

Frequently requested features under evaluation:

- [ ] Web export (Filament has a WebGL backend)
- [ ] Networking — synchronized multi-user scenes
- [ ] Audio spatialization — 3D positional audio tied to nodes
- [ ] Shader graph — visual material editor with Compose preview

---

## How to influence the roadmap

1. **Vote** — thumbs-up on [feature request issues](https://github.com/SceneView/sceneview-android/issues?q=label%3Aenhancement)
2. **Discuss** — describe your use case in [GitHub Discussions](https://github.com/SceneView/sceneview-android/discussions)
3. **Contribute** — PRs for roadmap items are especially welcome
4. **Sponsor** — [GitHub Sponsors](https://github.com/sponsors/ThomasGorisse) and [Open Collective](https://opencollective.com/sceneview) fund dedicated development time

# SceneView Roadmap

> **Vision**: Become the #1 open-source 3D & AR SDK for mobile developers.
> **Strategy**: Ship fast, ship often. Every feature = a patch release. Majors for breaking changes only.

---

## Versioning strategy

- **Patch (3.3.x)**: Every feature, fix, or improvement ships immediately as a patch
- **Minor (3.x.0)**: Accumulated features grouped for marketing / communication
- **Major (x.0.0)**: Breaking API changes only (Filament upgrade, API redesign)
- **Rule**: Never accumulate unreleased work. Tag → Publish → Communicate.

---

## v3.3.0 — Current release (published)

### What shipped
- **Android SDK**: 21+ composable node types, Filament 1.70.0, ARCore 1.53.0
- **SceneViewSwift (Apple)**: 17 node types, iOS 17+ / macOS 14+ / visionOS 1+, RealityKit + ARKit
- **sceneview-core (KMP)**: Shared math, collision, geometry, animation, physics
- **MCP server**: 10 tools, iOS + Android samples, Swift validation, published on npm
- **Website**: MkDocs Material, codelabs (Android + iOS), cheatsheets, quickstarts
- **CI/CD**: Automated release pipeline (Maven Central + npm + GitHub Release + Dokka)
- **Cross-framework scaffolds**: Flutter plugin + React Native module (scaffolds)

### Published artifacts
| Artifact | Registry | Version |
|---|---|---|
| `io.github.sceneview:sceneview` | Maven Central | 3.3.0 |
| `io.github.sceneview:arsceneview` | Maven Central | 3.3.0 |
| `io.github.sceneview:sceneview-core` | Maven Central | 3.3.0 |
| `sceneview-mcp` | npm | 3.3.0 |
| `SceneViewSwift` | SPM (GitHub tag) | v3.3.0 |

---

## Next patches (3.3.x) — Ship as ready

Each item below ships as its own patch release the moment it's done.

### High priority
- [ ] **Enable GitHub Pages** — repo Settings → Pages → Source: "GitHub Actions"
- [ ] **Post-processing sample PR** — Bloom, DoF, SSAO, Fog (already built)
- [ ] **ar-model-viewer UX** — better pinch scale + two-finger rotate, plane mesh vis
- [ ] **model-viewer animations** — play/pause/scrub controls, morph targets demo
- [ ] Gesture improvements — scale clamp, rotation axis lock, velocity flick
- [ ] `onCollision` callback in `SceneScope`

### Medium priority
- [ ] `ViewNode` depth-ordering fix (transparent Compose layers)
- [ ] ARCore `EnvironmentalHDR` upgrade
- [ ] macOS target tested and stable for SceneViewSwift
- [ ] DocC documentation for SceneViewSwift

### Low priority
- [ ] KDoc/DocC auto-generation at release time
- [ ] GitHub Discussions enabled + triage labels

---

## v3.4.0 — KMP core consumption

- Build XCFramework from `sceneview-core`
- Integrate into SceneViewSwift for shared algorithms
- Shared physics simulation across Android and Apple
- New codelabs: Dynamic Environments, Spatial UI

---

## v4.0.0 — Multi-scene, XR, cross-framework

### Android
- Multiple independent `Scene {}` sharing one `Engine`
- **PortalNode** — render-to-texture portal between scenes
- Filament 2.x migration
- **SceneView-XR** — Android XR / spatial computing

### Apple
- visionOS spatial computing — immersive spaces, hand tracking

### Cross-framework
- Flutter plugin (PlatformView → SceneViewSwift)
- React Native module (Turbo Module / Fabric)
- KMP Compose (UIKitView → SceneViewSwift)

---

## Process rules

1. **Every PR runs `/sync-check`** before merge
2. **Every feature = immediate patch release** (tag + publish)
3. **Never say "done" without checking published packages** (Maven Central, npm, SPM)
4. **CLAUDE.md updated at end of every session**
5. **llms.txt and MCP updated with every public API change**
6. **Docs site updated with every user-facing change**

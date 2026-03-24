# SceneView Roadmap

## v3.3.0 — Unified version, cross-platform, website (current)

### SDK — Android
- **PhysicsNode**, **RaycastNode**, **DynamicSkyNode**, **ReflectionProbeNode**, **FogNode** — shipped
- **BillboardNode**, **TextNode**, **LineNode / PathNode** — shipped
- Gesture improvements — scale clamp, rotation axis lock, velocity flick
- `onCollision` callback in `SceneScope`

### SDK — Apple (SceneViewSwift)
- **SceneView** (3D) + **ARSceneView** (AR) in SwiftUI — alpha
- iOS 17+ / macOS 14+ / visionOS 1+ via RealityKit
- Node types: ModelNode, AnchorNode, GeometryNode, LightNode, CameraNode, ImageNode, VideoNode, PhysicsNode, AugmentedImageNode
- **New nodes**: DynamicSkyNode, FogNode, ReflectionProbeNode — done
- **Enhanced nodes**: ModelNode (named animations, materials, collision), LightNode (shadows, attenuation), CameraNode (FOV, DOF, exposure) — done
- **Full test suite**: 15+ node types with edge cases and platform tests (65+ tests) — done
- Swift Package Manager distribution (`from: "3.3.0"`)

### Ecosystem
- **Unified versioning** — all modules (sceneview, arsceneview, sceneview-core, MCP, SceneViewSwift) at 3.3.0
- **Website** (`docs/`) — MkDocs Material, platform logo ticker, codelabs, cheatsheet
- **MCP server** (`mcp/`) — 10 tools + 2 resources, published on npm; iOS support added (8 Swift samples, `get_ios_setup` tool, Swift code validation) — done
- **Docs**: iOS quickstart, cheatsheet, 2 SwiftUI codelabs added — done
- `llms.txt` covers full API (26+ node types, all platforms) — updated

---

## v3.4.0 — SceneViewSwift stabilization

### SDK — Apple
- ~~Complete API parity for core node types with Android~~ — largely done (15 node types, 3 new + 3 enhanced)
- macOS target tested and stable
- ~~Unit test coverage for all node types~~ — done (65+ tests covering 15+ node types)
- KDoc/DocC documentation

### SDK — Android
- `ViewNode` depth-ordering fix for edge cases with transparent Compose layers
- ARCore `EnvironmentalHDR` upgrade — real camera feed for AR environment estimation

### Ecosystem
- ~~Codelab: iOS 3D scene with SwiftUI~~ — done (2 SwiftUI codelabs added)
- ~~`llms.txt` updated with SceneViewSwift API details~~ — done

---

## v3.5.0 — KMP core consumption

### SDK
- Build XCFramework from `sceneview-core` (math, collision, geometry, animations)
- Integrate XCFramework into SceneViewSwift for shared algorithms
- Shared physics simulation across Android and Apple

### Ecosystem
- Codelab: Dynamic Environments
- Codelab: Spatial UI

---

## v4.0.0 — Multi-scene, XR, cross-framework bridges

### SDK — Android
- Multiple independent `Scene {}` composables sharing one `Engine`
- **`PortalNode`** — render a secondary scene inside a 3D frame (AR portals)
- Filament 2.x migration (when stable)
- **`SceneView-XR`** module — Android XR / spatial computing support

### SDK — Apple
- visionOS spatial computing — immersive spaces, hand tracking, spatial anchors

### Cross-framework bridges
- Flutter plugin — `PlatformView` wrapping SceneViewSwift
- React Native module — Turbo Module / Fabric component
- KMP Compose — `UIKitView` wrapping SceneViewSwift

### Ecosystem
- `llms.txt` auto-generated from KDoc at release time
- GitHub Discussions enabled + triage labels for community

---

## Backlog

| Priority | Task | Status |
|----------|------|--------|
| 1 | **Material 3 Expressive design** — demo app with `MaterialExpressiveTheme`, spring animations, dynamic color, expressive components | In progress |
| 2 | **Play Store deployment** — GitHub secrets for keystore + service account | Pending |

---

## Ongoing

- Keep `llms.txt` and MCP server in sync with every public API change
- Keep all module versions synchronized
- Website maintenance and content updates

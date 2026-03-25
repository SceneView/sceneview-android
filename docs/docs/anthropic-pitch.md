# SceneView + Anthropic: Interactive 3D for Claude

**Technical Partnership Proposal**
SceneView — Open-Source 3D/AR SDK | March 2026

---

## 1. The Gap: Claude Cannot Render Interactive 3D

Claude generates text, code, images, charts, and interactive artifacts — but has no
native capability for interactive 3D content. This leaves a significant category of
developer and end-user needs unaddressed:

| User segment | What they ask Claude | Current experience |
|---|---|---|
| Developers building 3D/AR apps | "Show me a 3D model viewer in Compose" | Code-only output, no visual preview |
| Architects and designers | "Render this floor plan in 3D" | Static image at best |
| E-commerce merchants | "Let me preview this product in 3D" | No capability |
| Real estate agents | "Create a virtual tour of this property" | No capability |
| Educators | "Show me the solar system in 3D" | Flat diagram |
| Game developers | "Prototype this scene" | Code with no live preview |

Every major AI platform is racing to add multimodal output. 3D is the next frontier
after images and charts. The first platform to offer interactive 3D in-conversation
gains a significant competitive advantage with developer and enterprise audiences.

---

## 2. The Solution: SceneView MCP + `create_3d_artifact`

SceneView has already built this capability. It works today via the MCP protocol:

```
User: "Show me a 3D model of a damaged car for an insurance claim"

Claude (via SceneView MCP):
  1. Calls create_3d_artifact tool
  2. Generates an interactive 3D scene
  3. User gets a live, rotatable 3D view in the conversation
```

**The `create_3d_artifact` tool** produces self-contained HTML artifacts with four
render modes:

| Mode | Use case | Output |
|---|---|---|
| `model_viewer` | Display .glb/.gltf models | Interactive viewer with orbit controls |
| `scene_builder` | Procedural 3D scenes | Programmatic geometry, lights, materials |
| `ar_preview` | AR placement preview | WebXR-ready scene |
| `data_viz` | 3D data visualization | 3D charts, scatter plots, terrain |

Each artifact is a single HTML file — no external dependencies, no server required.
It renders via Filament.js (Google's production renderer compiled to WASM).

---

## 3. Why SceneView

### Production readiness

| Metric | Value |
|---|---|
| GitHub stars | 1,500+ |
| Test suite | 590 tests (core: 86, Android: 350+, Swift: 154) |
| License | Apache 2.0 |
| Active maintenance | 100+ commits in March 2026 alone |
| MCP Registry | Listed as `io.github.sceneview/mcp` |

### Platform coverage — 9 platforms from one SDK

| Platform | Renderer | Status |
|---|---|---|
| Android | Filament | Stable (Maven Central) |
| iOS | RealityKit | Alpha (Swift Package) |
| macOS | RealityKit | Alpha |
| visionOS | RealityKit | Alpha |
| Web | Filament.js (WASM) | Alpha |
| Desktop | Compose Desktop | Alpha |
| Android TV | Filament | Alpha |
| Flutter | PlatformView bridge | Alpha |
| React Native | Fabric bridge | Alpha |

### Architecture: native renderer per platform

SceneView uses each platform's native 3D engine — Filament on Android/Web, RealityKit
on Apple platforms. Shared logic (math, collision, geometry, animation) lives in a
Kotlin Multiplatform core module. This means:

- No abstraction-layer performance penalty
- Access to platform-specific features (ARCore, ARKit, visionOS spatial computing)
- Smaller binaries than cross-platform renderers

---

## 4. Why Filament.js Over Three.js

For Claude artifacts (web-rendered HTML), SceneView uses Filament.js — Google's
physically-based renderer compiled to WebAssembly.

| Criterion | Filament.js | Three.js |
|---|---|---|
| **Binary size** | ~2 MB (WASM) | ~600 KB (JS) + typical projects reach 2-5 MB with loaders |
| **Runtime memory** | Lower — WASM linear memory | Higher — JS heap + GC pressure |
| **Render quality** | Physically-based by default (same engine as Android) | Requires manual PBR setup |
| **Performance** | Near-native (WASM) | JS-bound, no WASM acceleration |
| **Mobile perf** | Optimized for mobile GPUs (designed for Android) | General-purpose, not mobile-optimized |
| **Production proof** | Ships in every Android device (Filament is Android's 3D renderer) | Widely used but not a system-level component |
| **glTF support** | Native, spec-compliant | Via loader, occasional spec gaps |

For Claude's use case — rendering 3D artifacts on mobile (Claude iOS/Android app) and
web (claude.ai) — Filament's mobile optimization and WASM performance are directly
relevant advantages.

Three.js is an excellent general-purpose library, but Filament was purpose-built for
the constraints Claude artifacts face: mobile devices, limited memory, battery
sensitivity, and the need for correct PBR rendering out of the box.

---

## 5. Integration Options

Four tiers of integration, from lightweight to deep:

### Option A: Featured MCP (minimal effort)

Feature SceneView MCP prominently in the MCP Registry for 3D-related queries.
Claude already routes to relevant MCPs — this ensures SceneView surfaces when users
need 3D capabilities.

- **Effort**: Curation/editorial only
- **Impact**: Developers discover 3D capability organically

### Option B: Built-in 3D artifact type (medium effort)

Add `3d` as a native artifact type alongside `code`, `image`, and `chart`. When Claude
generates a 3D artifact, it renders as an interactive 3D viewer in the conversation —
no MCP installation required.

- **Effort**: Artifact renderer integration
- **Impact**: 3D becomes a first-class Claude capability for all users

### Option C: "3D for Claude" co-development (higher effort)

Joint initiative to develop optimized 3D artifact rendering. SceneView provides the
rendering engine and 3D expertise; Anthropic provides the artifact infrastructure and
distribution.

- **Effort**: Engineering partnership
- **Impact**: Best possible 3D experience, co-branded

### Option D: Recommended MCP for 3D use cases

SceneView listed as the recommended/default MCP when Claude detects 3D-related intent
in user queries, with an install prompt for users who don't have it connected.

- **Effort**: Intent detection + recommendation UI
- **Impact**: Seamless discovery without mandatory bundling

---

## 6. Existing Traction

SceneView's MCP ecosystem is already deployed and functional:

### MCP servers published (npm + MCP Registry)

| Server | Vertical | Package |
|---|---|---|
| `@anthropic/sceneview` | Core 3D/AR SDK | `io.github.sceneview/mcp` |
| Real Estate | Property visualization | Published |
| E-Commerce | Product 3D preview | Published |
| Architecture | Building visualization | Published |
| Legal Documents | Document 3D annotation | Published |
| Social Media | 3D content creation | Published |
| French Admin | Administrative 3D tools | Published |

### Live infrastructure

- **Embed widget**: `sceneview.github.io/embed` — drop-in 3D viewer for any webpage
- **Playground**: `sceneview.github.io/playground` — interactive 3D scene builder
- **`create_3d_artifact` tool**: generates self-contained 3D HTML artifacts
- **`render_3d_preview` tool**: server-side 3D rendering for thumbnails

### Community

- Active GitHub discussions and issue tracker
- Discord community
- Regular releases (5 releases in March 2026: v3.4.0 through v3.4.4)

---

## 7. The Ask

We propose starting with **Option A or D** — low-effort integration that validates
demand — with a path toward **Option B** if usage data supports it.

Specifically:

1. **Immediate**: Feature `io.github.sceneview/mcp` in the MCP Registry for
   3D/AR-related categories
2. **Short-term**: Explore adding `3d` as a supported artifact type using
   SceneView's Filament.js renderer
3. **Ongoing**: Collaborate on optimizing the 3D artifact experience for Claude's
   mobile and web clients

### What SceneView brings to the table

- Production 3D rendering engine (Filament.js, WASM)
- Working MCP integration with `create_3d_artifact`
- 9-platform coverage for native mobile follow-through
- Open source (Apache 2.0) — no licensing friction
- Active maintenance and rapid iteration

### What we need from Anthropic

- Visibility in the MCP Registry
- Technical guidance on artifact renderer integration
- Feedback on 3D artifact UX requirements
- Access to artifact SDK (if pursuing Option B)

---

## Contact

- **Project**: [github.com/sceneview](https://github.com/sceneview)
- **MCP Registry**: `io.github.sceneview/mcp`
- **Website**: [sceneview.github.io](https://sceneview.github.io)
- **Maintainer**: Thomas Gorisse

---

*SceneView is an independent open-source project (Apache 2.0). This proposal is for
technical partnership discussion — not a commercial sales pitch.*

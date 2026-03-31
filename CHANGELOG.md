# Changelog

## 3.6.0 — Comprehensive quality audit, SwiftUI fixes, website migration (2026-03-31)

### SceneViewSwift
- Fixed SceneSnapshot visionOS compilation (ARView unavailable)
- Fixed VideoNode memory leak (NotificationCenter observer never removed)
- Fixed CameraNode macOS support (removed unnecessary platform guards)
- Removed unreachable dead code in GeometryNode

### Website
- Migrated ALL pages from model-viewer/Three.js to sceneview.js
- Removed Three.js (53K LOC) and model-viewer.min.js
- Rewrote sceneview-demo.html to use SceneView.modelViewer() API
- Fixed 3 demo pages crashing from non-existent API calls
- Fixed model paths in claude-3d.html
- Deleted 5 dead demo pages + fixed sitemap.xml
- Added 404.html page for GitHub Pages
- Fixed og:image/twitter:image meta tags (SVG → PNG) across all 8 pages
- Fixed sceneview.js version mismatch (runtime 1.5.0 → 3.6.0)
- Fixed IBL path (relative → absolute) for embed/preview subdirectory pages
- Improved synthetic IBL fallback lighting for Claude Artifacts

### Branding
- Generated 22 PNG exports from SVG sources (logo, app icon, favicon, social, npm, store)
- Created favicon.ico (multi-resolution)
- Updated Open Collective: logo, cover, tiers (Backer $10, Sponsor $50, Gold $200), 10 tags

### AI Integration
- Added Claude Artifacts section to llms.txt (HTML template, CDN URLs, 26 models)
- Updated MCP tool count: 22 → 26 tools, 2360 tests across 98 suites

### Dependencies
- Bumped Filament 1.70.0 → 1.70.1

### CI/CD
- Fixed maintenance.yml (Filament version grep, graceful fallback)
- Fixed docs.yml (download-artifact version, deploy retry)
- All 10 workflows verified green

### Version alignment
- Updated 100+ files from 3.5.0/3.5.1 to 3.6.0
- All satellite MCPs (automotive, gaming, healthcare, interior) aligned

---

## 3.5.1 — macOS support, environment picker, MCP 3.5.3 (2026-03-29)

### Apple platforms
- Native macOS support in SceneViewSwift (all source files + demo app)
- macOS App Store submission (build 357, pending review)
- iOS App Store submission (build 355, pending review)
- Environment picker UI with 6 HDR presets (Studio, Outdoor, Sunset, Night, Warm, Autumn)
- Proper macOS app icon sizes (16px to 1024px)
- Swift 6 strict concurrency fix (`@MainActor` on HapticManager)

### MCP Server v3.5.3
- Updated all dependency references from 3.4.7 to 3.5.0
- Published to npm as sceneview-mcp@3.5.3
- 1204 tests passing

### CI/CD
- Extended app-store.yml with macOS deploy job (parallel iOS + macOS)
- Fixed TestFlight deploy failure (Swift 6 concurrency)

### Documentation
- Added ViewNode, SceneSnapshot, SceneEnvironment.allPresets to llms.txt
- Rebuilt docs site — zero stale version references
- Fixed CDN versions in README (1.2.0 → 3.5.1) and website (1.4.0 → 3.5.1)

### Assets
- URL-based model loading (Android + iOS)
- 6 iOS HDR environments
- Progressive texture loading (Filament async)
- 25 models migrated to GitHub Releases CDN (Play Store compliance)

## 3.5.0 — Full coherence audit, version alignment (2026-03-29)

### Version coherence
- Unified all version references across 60+ files to 3.5.0
- Fixed module gradle.properties (sceneview, arsceneview, sceneview-core)
- Updated MCP source + dist files, docs, website, samples, Flutter, React Native
- Fixed Flutter/React Native Android build files (were still on 2.3.0)

### Documentation
- Updated llms.txt, all docs, codelabs, cheatsheets, quickstarts
- Updated CLAUDE.md code samples and platform table
- Cross-platform version consistency across all READMEs

## 3.4.7 — MCP 18 tools, orbit fix, geometry demo (2026-03-26)

### MCP Server v3.4.13
- 4 new tools: `get_platform_setup`, `migrate_code`, `debug_issue`, `generate_scene`
- 834 tests across all tools

### Bug fixes
- Orbit controls: corrected inverted horizontal/vertical camera drag
- 3 core math/collision bugs fixed
- Removed stale CI job

### Website
- Geometry demo: mini-city with 4 presets (City, Park, Abstract, Minimal)
- Meta tags, sitemap, favicon, canonical URLs polished

---

## 3.4.6 — Procedural 3D geometry in Claude Artifacts (2026-03-26)

### Highlights
- `create_3d_artifact` MCP tool with geometry type: procedural shapes with PBR materials
- SceneView.js v1.1.0 published to npm: one-liner web 3D with auto Filament WASM loading
- Filament.js PBR rendering on website (replaced model-viewer)
- 9 MCP servers all at v2.0.0

---

## 3.4.5 — SceneView Web with Filament.js WASM (2026-03-26)

### Features
- Real 3D rendering in browser via Google Filament compiled to WebAssembly
- 25 KB bundle (+ Filament.js from CDN)
- Live demo at sceneview.github.io

### Other
- Website mobile polish, 50+ broken links fixed
- GitHub Sponsors: 3 new tiers; Polar.sh approved with Stripe
- MCP v3.4.9: `create_3d_artifact` tool (590 tests)

---

## 3.4.4 — Play Store readiness, MCP legal (2026-03-25)

### Features
- Android demo: Play Store readiness (crash prevention, dark mode, store listing)
- MCP Server: Terms of Service, Privacy Policy, disclaimers added
- GitHub Sponsors tier structure

---

## 3.4.3 — Embeddable 3D widget (2026-03-25)

### Features
- Embeddable 3D viewer via single `<iframe>` snippet
- MCP `render_3d_preview` accepts code snippets and direct model URLs
- Web demo: branded UI, model selector, loading indicator

---

## 3.4.2 — Critical AR fix, MeshNode improvement (2026-03-25)

### Breaking fix
- AR materials regenerated for Filament 1.70.0 — previous materials crashed all AR apps

### Features
- `MeshNode` now accepts optional `boundingBox` parameter

### Security
- 6 Dependabot vulnerabilities fixed, 15 audit issues resolved
- 28 stale repository references updated

---

## 3.4.1 — Website, smart links, 3D preview (2026-03-25)

### Features
- Website rebuilt: Kobweb replaced with static HTML/CSS/JS + model-viewer 3D
- Smart links: `/go` (platform redirect), `/preview` (3D preview), `/preview/embed` (iframe viewer)
- MCP `render_3d_preview` tool for AI-generated 3D previews

### Infrastructure
- 21 secrets configured (Apple + Android + Maven + npm)
- README rewritten (622 to 200 lines)

---

## 3.4.0 — Multi-platform expansion (2026-03-25)

### New platforms
- **Web** — `sceneview-web` module: Filament.js (WASM) rendering + WebXR AR/VR
- **Desktop** — `samples/desktop-demo`: Compose Desktop, software 3D renderer
- **Android TV** — `samples/android-tv-demo`: D-pad controls, model cycling
- **Flutter** — `samples/flutter-demo`: PlatformView bridge (Android + iOS)
- **React Native** — `samples/react-native-demo`: Fabric bridge (Android + iOS)

### Android showcase
- Unified `samples/android-demo` — Material 3 Expressive, 4 tabs, 14 demos
- Blue branding with isometric cube icon

### Infrastructure
- **MCP Registry** — SceneView MCP published at `io.github.ThomasGorisse/sceneview`
- **21 GitHub Secrets** — Android + iOS + Maven + npm fully configured
- **Apple Developer** — Distribution certificate, provisioning profile, API key
- **CI/CD** — Play Store + App Store workflows ready

### Samples cleanup
- 15 obsolete samples deleted, merged into unified platform demos
- `{platform}-demo` naming convention across all 7 platforms
- Code recipes preserved in `samples/recipes/`

### Fixes
- material-icons-extended pinned to 1.7.8 (1.10.5 not published on Google Maven)
- wasmJs target disabled (kotlin-math lacks WASM variant)
- AR emulator script updated for new sample structure

---

## 3.3.0 — Unified versioning, cross-platform, website

### Version unification
- **All modules aligned to 3.3.0** — sceneview, arsceneview, sceneview-core, MCP server, SceneViewSwift, docs, and all references across the repo are now at a single unified version

### SceneViewSwift (Apple)
- **iOS 17+ / macOS 14+ / visionOS 1+** via RealityKit — alpha
- Node types: ModelNode, AnchorNode, GeometryNode, LightNode, CameraNode, ImageNode, VideoNode, PhysicsNode, AugmentedImageNode
- PBR material system with textures
- Swift Package Manager distribution

### SceneViewSwift — new nodes and enhancements
- **DynamicSkyNode** — procedural time-of-day sky with sun position, atmospheric scattering
- **FogNode** — volumetric fog with density, color, and distance falloff
- **ReflectionProbeNode** — local cubemap reflections for realistic environment lighting
- **ModelNode enhancements** — named animation playback, runtime material swapping, collision shapes
- **LightNode enhancements** — shadow configuration, attenuation radius and falloff
- **CameraNode enhancements** — field of view, depth of field, exposure control

### MCP server — iOS support
- **8 Swift sample snippets** for iOS code generation
- **`get_ios_setup`** tool for Swift/iOS project bootstrapping
- **Swift code validation** in `validate_code` tool
- iOS-specific guides and documentation

### Tests
- **65+ new tests** covering edge cases and platform-specific behavior
- Test coverage for all 15+ SceneViewSwift node types
- Platform tests for iOS-specific RealityKit integration

### Website
- Platform logo ticker on homepage — infinite-scroll marquee showing all supported platforms and technologies (Android, iOS, macOS, visionOS, Compose, SwiftUI, Filament, RealityKit, ARCore, ARKit, Kotlin, Swift)
- CSS-only animation with fade edges, hover-to-pause, dark mode support

### Documentation
- Updated ROADMAP.md to reflect current state (SceneViewSwift exists, phased plan revised)
- Updated PLATFORM_STRATEGY.md — native renderer per platform architecture (Filament + RealityKit)
- All codelabs, cheatsheet, migration guide updated to 3.3.0
- **iOS quickstart guide** — step-by-step setup for SceneViewSwift
- **iOS cheatsheet** — quick reference for SwiftUI 3D/AR patterns
- **2 SwiftUI codelabs** — hands-on tutorials for iOS 3D scenes and AR

---

## 3.1.2 — Sample polish, CI fixes, maintenance tooling

### Fixes
- `autopilot-demo`: remove deprecated `engine` parameter from `PlaneNode`, `CubeNode`, `CylinderNode` constructors (API aligned with composable node design)
- CI: fix AR emulator stability — wait for launcher, dismiss ANR dialogs, kill Pixel Launcher before screenshots

### Sample improvements
- `model-viewer`: scale up Damaged Helmet 0.25 → 1.0; add Fox model (CC0, KhronosGroup glTF-Sample-Assets) with model picker chip row
- `camera-manipulator`: scale up model 0.25 → 1.0; add gesture hint bar (Drag·Orbit / Pinch·Zoom / Pan·Move)

### Developer tooling
- `/maintain` Claude Code skill + daily maintenance GitHub Action for automated SDK upkeep
- AR emulator CI job using x86\_64 Linux + ARCore emulator APK for screenshot verification
- `ROADMAP.md` added covering 3.2–4.0 milestones

## 3.1.1 — Build compatibility patch

- Downgrade AGP from 8.13.2 → 8.11.1 for Android Studio compatibility
- Update AGP classpath in root `build.gradle` to match
- Refresh `gltf-camera` sample: animated BrainStem character + futuristic rooftop night environment

## 3.1.0 — VideoNode, reactive animation API

### New features
- `VideoNode` — render a video stream (MediaPlayer / ExoPlayer) as a textured 3D surface
- Reactive animation API — drive node animations from Compose state
- `ViewNode` rename — `ViewNode2` unified into `ViewNode`

### Fixes
- `ToneMapper.Linear` in `ARScene` prevents overlit camera background
- `ImageNode` SIGABRT: destroy `MaterialInstance` before texture on dispose
- `cameraNode` registered with `SceneNodeManager` so HUD-parented nodes render correctly
- Entities removed from scene before destroy to prevent SIGABRT
- `UiHelper` API corrected for Filament 1.56.0

### AI tooling
- MCP server: `validate_code`, `list_samples`, `get_migration_guide` tools + live Issues resource
- 89 unit tests for MCP validator, samples, migration guide, and issues modules

## 3.0.0 — Compose-native rewrite

### Breaking changes

The entire public API has been redesigned around Jetpack Compose. There is no source-compatible
upgrade path from 2.x; see the [Migration guide](https://sceneview.github.io/migration/) for a step-by-step walkthrough.

#### `Scene` and `ARScene` — new DSL-first signature

Nodes are no longer passed as a list. They are declared as composable functions inside a
trailing content block:

```kotlin
// 2.x
Scene(
    childNodes = rememberNodes {
        add(ModelNode(modelInstance = loader.createModelInstance("helmet.glb")))
    }
)

// 3.0
Scene {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
    }
}
```

#### `SceneScope` — new composable DSL

All node types (`ModelNode`, `LightNode`, `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode`,
`ImageNode`, `ViewNode`, `MeshNode`, `Node`) are now `@Composable` functions inside `SceneScope`.
Child nodes are declared in a `NodeScope` trailing lambda, matching how Compose UI nesting works.

#### `ARSceneScope` — new AR composable DSL

All AR node types (`AnchorNode`, `PoseNode`, `HitResultNode`, `AugmentedImageNode`,
`AugmentedFaceNode`, `CloudAnchorNode`, `TrackableNode`, `StreetscapeGeometryNode`) are now
`@Composable` functions inside `ARSceneScope`.

#### `rememberModelInstance` — async, null-while-loading

```kotlin
// Returns null while loading; recomposes with the instance when ready
val instance = rememberModelInstance(modelLoader, "models/helmet.glb")
```

#### `SurfaceType` — new enum

Replaces the previous boolean flag. Controls whether the 3D surface renders behind Compose layers
(`SurfaceType.Surface`, SurfaceView) or inline (`SurfaceType.TextureSurface`, TextureView).

#### `PlaneVisualizer` — converted to Kotlin

`PlaneVisualizer.java` has been removed. `PlaneVisualizer.kt` replaces it.

#### Removed classes

The following legacy Java/Sceneform classes have been removed from the public API:

- All classes under `com.google.ar.sceneform.*` — replaced by Kotlin equivalents under the same
  package path (`.kt` files).
- All classes under `io.github.sceneview.collision.*` — replaced by Kotlin equivalents.
- All classes under `io.github.sceneview.animation.*` — replaced by Kotlin equivalents.

#### Samples restructured

All samples are now pure `ComponentActivity` + `setContent { }`. Fragment-based layouts have been
removed. The `model-viewer-compose`, `camera-manipulator-compose`, and `ar-model-viewer-compose`
modules have been merged into `model-viewer`, `camera-manipulator`, and `ar-model-viewer`
respectively.

### Bug fixes

- **`ModelNode.isEditable`** — `SideEffect` was resetting `isEditable` to the parameter default
  (`false`) on every recomposition, silently disabling gestures when `isEditable = true` was set
  only inside `apply { }`. Pass `isEditable = true` as a named parameter to maintain it correctly.
- **ARCore install dialog** — Removed `canBeInstalled()` pre-check that threw
  `UnavailableDeviceNotCompatibleException` before `requestInstall()` was called, preventing the
  ARCore install prompt from ever appearing on fresh devices.
- **Camera background black** — `ARCameraStream` used `RenderableManager.Builder(4)` with only
  1 geometry primitive defined (invalid in Filament). Fixed to `Builder(1)`.
- **Camera stream recreated on every recomposition** — `rememberARCameraStream` used a default
  lambda parameter as a `remember` key; lambdas produce a new instance on every call, making the
  key unstable. Fixed by keying on `materialLoader` only.
- **Render loop stale camera stream** — The render-loop coroutine captured `cameraStream` at
  launch; recomposition could recreate the stream while the loop kept updating the old (destroyed)
  one. Fixed with an `AtomicReference` updated via `SideEffect`.

### New features

- **`SceneScope` / `ARSceneScope`** — fully declarative, reactive 3D/AR content DSL
- **`NodeScope`** — nested child nodes using Compose's natural trailing lambda pattern
- **`SceneNodeManager`** — internal bridge that syncs Compose snapshot state with the Filament
  scene graph, enabling reactive updates without manual `addChildNode`/`removeChildNode` calls
- **`SurfaceType`** — explicit surface-type selection (`Surface` vs `TextureSurface`)
- **`ViewNode`** — Compose UI content rendered as a 3D plane surface in the scene
- **`Engine.drainFramePipeline()`** — consolidated fence-drain extension for surface resize/destroy
- **`rememberViewNodeManager()`** — lifecycle-safe window manager for `ViewNode` composables
- **Autopilot Demo** — new sample demonstrating autonomous animation and scene composition
- **Camera Manipulator** — new dedicated sample for orbit/pan/zoom camera control
- **`Node.scaleGestureSensitivity`** — new `Float` property (default `0.5`) that damps
  pinch-to-scale gestures. Applied as `1f + (rawFactor − 1f) × sensitivity` in `onScale`,
  making scaling feel progressive without reducing the reachable scale range. Set it per-node in
  the `apply` block alongside `editableScaleRange`.
- **AR Model Viewer sample** — redesigned with animated scanning reticle (corner brackets +
  pulsing ring), model picker (Helmet / Rabbit), auto-dismissing gesture hints,
  `enableEdgeToEdge()`, and a clean Material 3 UI.

---

## 2.3.0

- AGP 8.9.1
- Filament 1.56.0 / ARCore 1.48.0
- Documentation improvements
- Camera Manipulator sample renamed

# Changelog

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

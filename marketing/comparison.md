# SceneView vs. the alternatives

*An honest comparison for developers evaluating 3D and AR options on Android and iOS.*

---

## The landscape

If you want 3D or AR in a mobile app today, here are your options:

| Library | Platforms | Approach | Status |
|---|---|---|---|
| **SceneView** | Android + iOS + macOS + visionOS | Compose + SwiftUI, native renderers | Active, v3.3.0 |
| **Google Sceneform** | Android only | View-based, custom renderer, ARCore | Abandoned (archived 2021) |
| **Raw ARCore SDK** | Android only | Low-level session/frame API, bring your own renderer | Active but no UI layer |
| **RealityKit** | Apple only | SwiftUI, Metal renderer, ARKit | Active, Apple-only |
| **Unity** | All | Full game engine embedded via activity/view controller | Active, heavy |
| **Rajawali** | Android only | OpenGL ES wrapper, imperative scene graph | Maintenance mode |
| **three.js (WebView)** | Web (via WebView) | JavaScript 3D in a WebView | Active, but web-only perf |
| **Babylon Native** | Cross-platform | C++ cross-platform runtime | Early stage |

---

## Side-by-side: adding a 3D model viewer

### SceneView — Android (Compose)

```kotlin
// build.gradle
implementation("io.github.sceneview:sceneview:3.3.0")

// One composable, that's it
@Composable
fun ModelViewer() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, "models/helmet.glb")

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        model?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
    }
}
```

**Lines of code:** ~15
**Files touched:** 1 (your composable)
**XML layouts:** 0
**Lifecycle callbacks:** 0
**Manual cleanup:** 0

---

### SceneView — iOS (SwiftUI)

```swift
// Package.swift
.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")

// One SwiftUI view, that's it
import SceneViewSwift

struct ModelViewer: View {
    var body: some View {
        SceneView {
            ModelNode(named: "helmet.usdz")
                .scaleToUnits(1.0)
            LightNode(.directional)
        }
    }
}
```

**Lines of code:** ~10
**Files touched:** 1 (your SwiftUI view)
**Storyboards/XIBs:** 0
**Lifecycle callbacks:** 0
**Manual cleanup:** 0

---

### RealityKit (Apple only, no cross-platform)

```swift
// Requires iOS-specific code, no Android equivalent
import RealityKit

struct RealityViewer: View {
    var body: some View {
        RealityView { content in
            if let model = try? await ModelEntity.load(named: "helmet.usdz") {
                content.add(model)
            }
        }
    }
}
```

**Lines of code:** ~10
**Platform:** Apple only — no Android support
**Cross-platform story:** None

---

### Google Sceneform (legacy, archived)

```kotlin
// build.gradle — must use a community fork, original is archived
implementation("com.gorisse.thomas.sceneform:sceneform:1.21.0")

// Activity code (~80 lines)
class ModelViewerActivity : AppCompatActivity() {
    private lateinit var arFragment: ArFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_viewer)
        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            ModelRenderable.builder()
                .setSource(this, Uri.parse("helmet.sfb"))
                .build()
                .thenAccept { renderable -> /* ... */ }
        }
    }
    override fun onResume() { super.onResume(); /* check AR availability */ }
    override fun onPause() { super.onPause(); /* release resources */ }
    override fun onDestroy() { super.onDestroy(); /* cleanup */ }
}
```

**Lines of code:** ~80+
**Files touched:** 3+ (Activity, XML layout, manifest)
**Manual lifecycle:** Yes
**Status:** Archived. No updates since 2021. Android only.

---

### Raw ARCore SDK

```kotlin
// You get a Session, Frame, and Camera. That's it.
// You must bring your own renderer (OpenGL ES, Vulkan, or Filament directly).
// Typical setup: 500–1000 lines before rendering a single triangle.
```

**Lines of code:** 500-1000+ for basic rendering
**Platform:** Android only

---

### Unity (embedded)

```kotlin
// build.gradle — Unity export as Android library
implementation(project(":unityLibrary"))
```

**APK size increase:** 40-80 MB (Unity runtime)
**Compose/SwiftUI integration:** None — Unity owns the entire Activity/ViewController
**When it makes sense:** Full 3D game with existing Unity assets

---

## Feature comparison

| Feature | SceneView | Sceneform | Raw ARCore | RealityKit | Unity |
|---|---|---|---|---|---|
| **Cross-platform** | Android + Apple | Android only | Android only | Apple only | All |
| **Jetpack Compose** | Native | No | No | No | No |
| **SwiftUI** | Native | No | No | Native | No |
| **Declarative nodes** | Yes | No (imperative) | No API | Yes | No (C# scripts) |
| **Auto lifecycle** | Yes | Manual | Manual | Yes | Unity-managed |
| **PBR rendering** | Filament / RealityKit | Custom (limited) | DIY | RealityKit | Unity renderer |
| **glTF/GLB models** | Yes | .sfb (deprecated) | DIY | Via conversion | Yes |
| **USDZ models** | Yes (iOS) | No | No | Yes | Yes |
| **Physics** | Built-in (both) | No | No | Built-in | Built-in |
| **Post-processing** | Bloom, DOF, SSAO, fog | No | DIY | Limited | Yes |
| **Dynamic sky** | Yes (both) | No | No | No | Yes (HDRP) |
| **AR plane detection** | Yes (both) | Yes | Yes | Yes | Yes |
| **AR image tracking** | Yes (both) | Yes | Yes | Yes | Yes |
| **AR face tracking** | Yes (Android) | Yes | Yes | Yes | Yes |
| **Cloud anchors** | Yes (Android) | Yes | Yes | No | Yes |
| **Geospatial API** | Yes (Android) | No | Yes | No | Yes |
| **ViewNode (UI in 3D)** | Yes (Android) | No | No | No | No |
| **AI tooling (MCP)** | Yes (both) | No | No | No | No |
| **visionOS** | Yes (Alpha) | No | No | Yes | Yes |
| **APK/IPA size impact** | ~5 MB | ~3 MB | ~1 MB | N/A | 40-80 MB |
| **Active maintenance** | Yes (2024-) | Abandoned | Google-maintained | Apple-maintained | Yes |
| **License** | Apache 2.0 | Apache 2.0 | Proprietary | Proprietary | Commercial |

---

## Common objections

### "We already use Unity for 3D"

Unity is the right choice if you're building a 3D-first game. But if you're adding 3D to an existing Compose or SwiftUI app — a product viewer, an AR feature, a data visualization — Unity's 60-350 MB runtime overhead, separate build pipeline, and inability to integrate with native UI make it overkill.

SceneView adds ~5 MB and works inside your existing screens on both platforms.

### "Can't we just use ARCore/ARKit directly?"

ARCore gives you tracking data but no rendering. ARKit is better (RealityKit provides rendering), but it's Apple-only. SceneView gives you full rendering + AR on both platforms with a consistent developer experience.

### "RealityKit already does this on iOS"

RealityKit is excellent — and SceneView uses it on Apple platforms. But RealityKit is Apple-only. If you need the same 3D/AR features on Android, you'd need a completely different SDK. SceneView provides the same concepts and patterns across both platforms.

### "Sceneform worked fine for us"

Google archived Sceneform in 2021. No Compose support. No new ARCore features. No iOS support. SceneView was created as its successor with a clear migration path documented in [MIGRATION.md](https://github.com/SceneView/sceneview/blob/main/MIGRATION.md).

### "What about visionOS?"

SceneView supports visionOS via SceneViewSwift (Alpha). Since it uses RealityKit on Apple platforms, the path to spatial computing features (immersive spaces, hand tracking) is natural.

### "Is it production-ready?"

Android: stable, used in production apps on Google Play. iOS: alpha, with 16 node types shipping and tests passing. Built on production rendering engines (Filament, RealityKit).

---

## Migration from Sceneform

| Sceneform concept | SceneView equivalent |
|---|---|
| `ArFragment` | `ARScene { }` composable |
| `ModelRenderable.builder()` | `rememberModelInstance(modelLoader, path)` |
| `AnchorNode(anchor).setParent(scene)` | `AnchorNode(anchor = a) { ... }` composable |
| `TransformableNode` | `ModelNode` with gesture parameters |
| `.sfb` model format | `.glb` / `.gltf` (standard glTF) |
| `onResume` / `onPause` / `onDestroy` | Automatic (Compose lifecycle) |
| `node.setParent(null); node.destroy()` | Remove from composition (conditional) |

Full guide: [MIGRATION.md](https://github.com/SceneView/sceneview/blob/main/MIGRATION.md)

---

## The bottom line

| If you need... | Use |
|---|---|
| 3D in a Compose app | **SceneView** |
| 3D in a SwiftUI app | **SceneView** (or RealityKit directly) |
| 3D on both Android and iOS | **SceneView** (the only declarative cross-platform option) |
| AR features on both platforms | **SceneView** |
| A full 3D game | Unity |
| A custom rendering engine | Raw ARCore/ARKit + OpenGL/Vulkan/Metal |
| Nothing — it's a 2D app | Nothing (but SceneView makes "subtle 3D" trivial) |

For the vast majority of mobile apps that want to add 3D or AR, SceneView is the answer. It's the only library that treats 3D as a first-class citizen in both Jetpack Compose and SwiftUI.

---

*[github.com/SceneView/sceneview](https://github.com/SceneView/sceneview) — Apache 2.0 — cross-platform 3D & AR for Android + iOS + macOS + visionOS*

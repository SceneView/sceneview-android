# SceneView vs. the alternatives

*An honest comparison for Android developers evaluating 3D and AR options.*

---

## The landscape

If you want 3D or AR in an Android app today, here are your options:

| Library | Approach | Status |
|---|---|---|
| **SceneView** | Jetpack Compose composables, Filament rendering, ARCore | Active, v3.2.0 |
| **Google Sceneform** | View-based, custom renderer, ARCore | Abandoned (archived 2021) |
| **Raw ARCore SDK** | Low-level session/frame API, bring your own renderer | Active but no UI layer |
| **Unity** | Full game engine embedded via `UnityPlayerActivity` | Active, heavy |
| **Rajawali** | OpenGL ES wrapper, imperative scene graph | Maintenance mode |
| **three.js (WebView)** | JavaScript 3D in a WebView | Active, but web-only perf |
| **Babylon Native** | C++ cross-platform runtime | Early stage on Android |

---

## Side-by-side: adding a 3D model viewer

### SceneView (Compose)

```kotlin
// build.gradle
implementation("io.github.sceneview:sceneview:3.2.0")

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

### Google Sceneform (legacy, archived)

```kotlin
// build.gradle — must use a community fork, original is archived
implementation("com.gorisse.thomas.sceneform:sceneform:1.21.0")

// XML layout
// <fragment android:name="com.google.ar.sceneform.ux.ArFragment" ... />

// Activity code (~80 lines)
class ModelViewerActivity : AppCompatActivity() {
    private lateinit var arFragment: ArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_viewer)
        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment

        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            val anchor = hitResult.createAnchor()
            ModelRenderable.builder()
                .setSource(this, Uri.parse("helmet.sfb"))
                .build()
                .thenAccept { renderable ->
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arFragment.arSceneView.scene)
                    val modelNode = TransformableNode(arFragment.transformationSystem)
                    modelNode.renderable = renderable
                    modelNode.setParent(anchorNode)
                    modelNode.select()
                }
        }
    }

    override fun onResume() { super.onResume(); /* check AR availability */ }
    override fun onPause() { super.onPause(); /* release resources */ }
    override fun onDestroy() { super.onDestroy(); /* cleanup */ }
}
```

**Lines of code:** ~80+
**Files touched:** 3+ (Activity, XML layout, manifest)
**Manual lifecycle:** Yes — `onResume`, `onPause`, `onDestroy`
**Status:** Archived. No updates since 2021. `.sfb` format deprecated.

---

### Raw ARCore SDK

```kotlin
// You get a Session, Frame, and Camera. That's it.
// You must bring your own renderer (OpenGL ES, Vulkan, or Filament directly).
// You must manage the GL surface, shader compilation, mesh uploading,
// lighting, shadow maps, and frame timing yourself.
// Typical setup: 500–1000 lines before rendering a single triangle.
```

**Lines of code:** 500–1000+ for basic rendering
**Skill required:** OpenGL/Vulkan expertise
**When it makes sense:** You're building a custom rendering engine

---

### Unity (embedded)

```kotlin
// build.gradle — Unity export as Android library
implementation(project(":unityLibrary"))

// Activity
class UnityViewerActivity : UnityPlayerActivity() {
    // All rendering logic lives in C# inside Unity
    // Communication via UnitySendMessage / JNI bridge
}
```

**APK size increase:** 40–80 MB (Unity runtime)
**Build time increase:** Significant (Unity build pipeline)
**Compose integration:** None — Unity owns the entire Activity
**When it makes sense:** Full 3D game with existing Unity assets

---

## Feature comparison

| Feature | SceneView | Sceneform | Raw ARCore | Unity |
|---|---|---|---|---|
| **Jetpack Compose** | Native | No | No | No |
| **Declarative nodes** | Yes | No (imperative) | No API | No (C# scripts) |
| **Auto lifecycle** | Yes | Manual | Manual | Unity-managed |
| **PBR rendering** | Filament | Custom (limited) | DIY | Unity renderer |
| **glTF/GLB models** | Yes | .sfb (deprecated) | DIY | Yes |
| **Physics** | Built-in | No | No | Built-in |
| **Post-processing** | Bloom, DOF, SSAO, fog | No | DIY | Yes |
| **Dynamic sky** | Yes | No | No | Yes (HDRP) |
| **AR plane detection** | Yes | Yes | Yes | Yes (AR Foundation) |
| **AR image tracking** | Yes | Yes | Yes | Yes |
| **AR face tracking** | Yes | Yes | Yes | Yes |
| **Cloud anchors** | Yes | Yes | Yes | Yes |
| **Geospatial API** | Yes | No | Yes | Yes |
| **ViewNode (Compose in 3D)** | Yes | No | No | No |
| **AI tooling (MCP)** | Yes | No | No | No |
| **APK size impact** | ~5 MB | ~3 MB | ~1 MB | 40–80 MB |
| **Active maintenance** | Yes (2024–) | Abandoned | Google-maintained | Yes |
| **License** | Apache 2.0 | Apache 2.0 | Proprietary | Commercial |

---

## Common objections

### "We already use Unity for 3D"

Unity is the right choice if you're building a 3D-first game. But if you're adding 3D to an
existing Compose app — a product viewer, an AR feature, a data visualization — Unity's 60–350 MB
runtime overhead, separate C# build pipeline, and inability to integrate with Compose make it
overkill. (Developers have reported 350 MB minimum APK size for a basic Unity AR app on Android.)

SceneView adds ~5 MB and works inside your existing Compose screens.

### "Can't we just use ARCore directly?"

ARCore gives you tracking data (planes, anchors, poses) but no rendering. You'd need to build
your own renderer on top of OpenGL ES or Vulkan. That's months of work for a team with graphics
expertise. SceneView gives you ARCore's full feature set with Filament's rendering, wrapped in
Compose composables.

### "Sceneform worked fine for us"

Google archived Sceneform in 2021. The `.sfb` model format is deprecated. No Compose support.
No new ARCore features (geospatial, streetscape, depth). The community fork ("Sceneform
Maintained") has unresolved compatibility issues including 16 KB page size compliance required
by Android 15 (API 35). SceneView was created as Sceneform's successor — the migration path
is straightforward and documented in
[MIGRATION.md](https://github.com/SceneView/sceneview-android/blob/main/MIGRATION.md).

### "What about Kotlin Multiplatform / iOS?"

SceneView is Android-first today. A KMP proof of concept (iOS via Filament's Metal backend)
is on the v4.0 roadmap. For cross-platform AR today, Unity or Babylon Native are options —
but they don't integrate with Compose.

### "Is it production-ready?"

SceneView is used in production apps on Google Play. It's built on Filament (Google's
production rendering engine) and ARCore (Google's production AR platform). The API surface
is stable and versioned. Breaking changes follow semantic versioning with migration guides.

---

## Migration from Sceneform

If you have an existing Sceneform app, the migration is documented step by step:

| Sceneform concept | SceneView equivalent |
|---|---|
| `ArFragment` | `ARScene { }` composable |
| `ModelRenderable.builder()` | `rememberModelInstance(modelLoader, path)` |
| `AnchorNode(anchor).setParent(scene)` | `AnchorNode(anchor = a) { ... }` composable |
| `TransformableNode` | `ModelNode` with gesture parameters |
| `.sfb` model format | `.glb` / `.gltf` (standard glTF) |
| `onResume` / `onPause` / `onDestroy` | Automatic (Compose lifecycle) |
| `node.setParent(null); node.destroy()` | Remove from composition (conditional) |

Full guide: [MIGRATION.md](https://github.com/SceneView/sceneview-android/blob/main/MIGRATION.md)

---

## The bottom line

| If you need... | Use |
|---|---|
| 3D in a Compose app | **SceneView** |
| AR features in a Compose app | **SceneView** |
| A full 3D game | Unity |
| A custom rendering engine | Raw ARCore + OpenGL/Vulkan |
| Nothing — it's a 2D app | Nothing (but SceneView makes "subtle 3D" trivial) |

For the vast majority of Android apps that want to add 3D or AR, SceneView is the answer.
It's the only library that treats 3D as a first-class Compose citizen.

---

*[github.com/SceneView/sceneview-android](https://github.com/SceneView/sceneview-android) — Apache 2.0 — built on Filament & ARCore*

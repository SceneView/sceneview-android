# Introducing SceneView 3.0 — 3D & AR for Jetpack Compose

*Published on: Medium / dev.to*
*Tags: Android, JetpackCompose, AR, 3D, Kotlin*

---

If you've ever tried to add 3D content or augmented reality to an Android app, you know the pain. Three hundred lines of boilerplate before you see a single triangle on screen. SurfaceView lifecycle management. Manual gesture handling. Filament rendering loops. ARCore session management.

SceneView 3.0 changes all of that.

## What is SceneView?

SceneView is an open-source Android library that brings 3D and AR to **Jetpack Compose**. It's built on Google's [Filament](https://google.github.io/filament/) rendering engine and [ARCore](https://developers.google.com/ar), but exposes them through a simple, declarative Compose API.

This is what loading a 3D model looks like:

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    Scene(engine = engine, modelLoader = modelLoader) {
        ModelNode(
            modelInstance = rememberModelInstance(modelLoader, "models/car.glb"),
            scaleToUnits = 1.0f
        )
    }
}
```

That's it. No lifecycle callbacks. No `onResume` / `onPause` / `onDestroy` chaining. No manual cleanup. Compose manages everything.

And AR tap-to-place — the most common AR use case — is just as simple:

```kotlin
ARScene(
    planeRenderer = true,
    onSessionUpdated = { session, frame ->
        // ARCore updates automatically
    }
) {
    // Nodes placed here are anchored in the real world
}
```

## Why Compose for 3D?

When we started the 3.0 rewrite, the question wasn't *whether* to adopt Compose — it was *how far to take it*.

Traditional 3D APIs are imperative: you create objects, store references, update them in callbacks, and tear them down when you're done. This creates a mismatch with modern Android development, where state drives UI. SceneView 3.0 resolves that mismatch by treating **3D nodes as composables**.

The mental model maps perfectly:

| Compose UI | SceneView 3D |
|---|---|
| `Column { }` | `Scene { }` |
| `Text("Hello")` | `TextNode("Hello", ...)` |
| `Image(res)` | `ModelNode(instance)` |
| `remember { }` | `rememberModelInstance()` |
| State → recomposition | State → 3D graph update |

Once you know Compose, you already know SceneView.

## What's new in 3.0 → 3.2

### Compose-native DSL

Every node type is a composable function that lives inside the `Scene { }` block. State changes trigger automatic 3D graph updates — no manual `setPosition()` calls needed.

```kotlin
var modelPath by remember { mutableStateOf("models/car.glb") }

Scene(engine = engine, modelLoader = modelLoader) {
    ModelNode(
        modelInstance = rememberModelInstance(modelLoader, modelPath),
        scaleToUnits = 1.0f
    )
    // Swap the model: just change the state
    // modelPath = "models/chair.glb" triggers recomposition → new model loads
}
```

### 19+ built-in node types

From simple geometry to physics and post-processing:

- **ModelNode** — glTF/GLB models with animations
- **TextNode** — Camera-facing 3D text labels
- **BillboardNode** — Always-facing-camera sprites
- **PhysicsNode** — Rigid body simulation with gravity
- **DynamicSkyNode** — Time-of-day sun lighting
- **FogNode** — Volumetric atmospheric fog
- **VideoNode** — Video on a 3D surface (green-screen supported)
- **LineNode / PathNode** — Polyline rendering
- **ViewNode** — Embed any Compose UI as a 3D billboard
- And 10+ more: CubeNode, SphereNode, CylinderNode, PlaneNode, LightNode, CameraNode, ReflectionProbeNode…

### Resource loading without threading headaches

Filament requires all rendering calls on the main thread — a common source of crashes in low-level code. SceneView handles this automatically:

```kotlin
// This is safe. rememberModelInstance loads async, returns null while loading.
val model = rememberModelInstance(modelLoader, "models/car.glb")
model?.let { ModelNode(modelInstance = it) }
```

### Gesture support built in

Pinch-to-scale, two-finger-rotate, tap selection, drag-to-move — all built in. For simple cases, just set `isEditable = true`:

```kotlin
ModelNode(
    modelInstance = model,
    isEditable = true  // pinch-scale + two-finger-rotate enabled
)
```

## Getting started in 5 minutes

### 1. Add the dependency

For 3D-only (no AR):
```kotlin
// build.gradle.kts
implementation("io.github.sceneview:sceneview:3.3.0")
```

For AR:
```kotlin
implementation("io.github.sceneview:arsceneview:3.3.0")
```

### 2. Add camera permission (AR only)

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.ar" android:required="true" />
```

### 3. Write your first scene

```kotlin
@Composable
fun MyScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio.hdr")!!
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = environment
    ) {
        val model = rememberModelInstance(modelLoader, "models/car.glb")
        model?.let {
            ModelNode(
                modelInstance = it,
                scaleToUnits = 1.0f,
                autoAnimate = true
            )
        }
    }
}
```

Put your `.glb` model and `.hdr` environment file in `src/main/assets/` and you're done.

## AR tap-to-place in 15 lines

```kotlin
@Composable
fun ARViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var anchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    val model = rememberModelInstance(modelLoader, "models/chair.glb")

    ARScene(
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        onSingleTapConfirmed = { hitResult ->
            anchorNode?.destroy()
            anchorNode = AnchorNode(engine, hitResult.createAnchor())
        }
    ) {
        anchorNode?.let { anchor ->
            Node(parent = anchor) {
                model?.let { ModelNode(modelInstance = it, scaleToUnits = 0.5f) }
            }
        }
    }
}
```

## The AI-first library

SceneView 3.0 was designed with AI-assisted development in mind. The library ships with `llms.txt` — a machine-readable API reference that AI coding assistants (Claude, Copilot, Gemini) can use to generate correct 3D and AR code on the first try.

This is part of a larger philosophy: **if the API is simple enough for an AI to get right, it's simple enough for a developer to get right too.** Complex, low-level APIs produce bugs whether the author is human or AI. Simple, declarative APIs produce correct code.

## What's next

The 3.x roadmap continues with:

- **3.3.0** — ReflectionProbeNode, DynamicSkyNode improvements
- **3.4.0** — LineNode, PathNode, MeshNode, VideoNode (already in dev)
- **4.0.0** — Multi-scene engine sharing, Android XR, iOS (Kotlin Multiplatform)

## Resources

- **GitHub:** [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)
- **API Reference:** `llms.txt` in the repo root
- **Samples:** 14 demo apps included
- **Demo app:** SceneView Demo on Google Play

---

*SceneView is open-source (MIT License). Contributions welcome.*

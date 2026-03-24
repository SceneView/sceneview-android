# Building a 3D Model Viewer in 50 Lines — Android & iOS

*Tags: Android, iOS, JetpackCompose, SwiftUI, 3D, Kotlin, Swift, Tutorial*

---

Here's a challenge: build a production-quality 3D model viewer — with orbit camera, HDR lighting, model switcher, and smooth animations — in under 50 lines of code.

With SceneView, it's not just possible on Android. It's now possible on iOS too.

## The result

By the end of this article, you'll have a viewer on **both platforms** that:

- Loads any `.glb` / `.gltf` model (Android) or `.usdz` model (iOS) from assets
- Supports drag-to-orbit, pinch-to-zoom, pan
- Has realistic HDR environment lighting
- Shows model animations (if the model has them)
- Lets the user switch between models with a chip/picker selector

## Android Setup (30 seconds)

```kotlin
// build.gradle.kts
implementation("io.github.sceneview:sceneview:3.3.0")
```

Put your models in `src/main/assets/models/` and an HDR file in `src/main/assets/environments/`.

## iOS Setup (30 seconds)

```swift
// Package.swift or Xcode: File > Add Package Dependencies
.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")
```

Add your `.usdz` models to your Xcode project's asset catalog or bundle.

## The complete viewer — Android (Jetpack Compose)

```kotlin
@Composable
fun ModelViewerScreen() {
    // Resources
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    // UI state
    var selectedModel by remember { mutableStateOf("models/toy_car.glb") }
    val models = listOf("Toy Car" to "models/toy_car.glb", "Chair" to "models/chair.glb")

    // 3D resources — automatically disposed on screen exit
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, selectedModel)

    Box(modifier = Modifier.fillMaxSize()) {
        // The 3D scene
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            environment = environment,
            cameraManipulator = rememberCameraManipulator()
        ) {
            modelInstance?.let {
                ModelNode(
                    modelInstance = it,
                    scaleToUnits = 1.0f,
                    autoAnimate = true,
                    animationLoop = true
                )
            }
        }

        // Model switcher chips — overlaid on top of the 3D scene
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            models.forEach { (label, path) ->
                FilterChip(
                    selected = selectedModel == path,
                    onClick = { selectedModel = path },
                    label = { Text(label) }
                )
            }
        }
    }
}
```

That's the whole Android viewer. 50 lines.

## The complete viewer — iOS (SwiftUI)

```swift
import SwiftUI
import SceneViewSwift

struct ModelViewerView: View {
    @State private var selectedModel = "toy_car"
    let models = [("Toy Car", "toy_car"), ("Chair", "chair")]

    var body: some View {
        ZStack(alignment: .bottom) {
            // The 3D scene
            SceneView {
                ModelNode(named: "\(selectedModel).usdz")
                    .scaleToUnits(1.0)
                LightNode(.directional)
            }

            // Model switcher
            HStack(spacing: 8) {
                ForEach(models, id: \.1) { label, name in
                    Button(label) { selectedModel = name }
                        .buttonStyle(.bordered)
                        .tint(selectedModel == name ? .accentColor : .secondary)
                }
            }
            .padding()
        }
    }
}
```

Same result on iOS. Even fewer lines.

## How it works

### State-driven model loading

**Android:** The key insight: `rememberModelInstance(modelLoader, selectedModel)` returns `null` while the model is loading, and the valid instance once ready.

```kotlin
val modelInstance = rememberModelInstance(modelLoader, selectedModel)
// null while loading → ModelNode doesn't render → no crash, no flash
modelInstance?.let {
    ModelNode(modelInstance = it, scaleToUnits = 1.0f)
}
```

When `selectedModel` changes (user taps a chip), Compose re-executes `rememberModelInstance` with the new path. The old model is cleaned up automatically. The new one starts loading. Zero manual lifecycle management.

**iOS:** SwiftUI's `@State` property wrapper triggers a view update when `selectedModel` changes. RealityKit handles async model loading internally.

### Automatic resource disposal

**Android:** `rememberEngine()`, `rememberModelLoader()`, `rememberEnvironment()` — all these `remember*` helpers register cleanup callbacks with the Compose lifecycle. When the composable leaves the composition (user navigates away, screen is destroyed), every Filament object is properly destroyed on the main thread.

**iOS:** RealityKit manages resource lifecycle through ARC and the SwiftUI view lifecycle. When the view disappears, resources are released automatically.

### HDR environment lighting

**Android:** Pass the `environment` to `Scene { }` and every model you add automatically gets image-based lighting, sky rendering, and specular reflections.

**iOS:** RealityKit provides environment-based lighting by default. Custom HDR environments can be applied to the scene.

### Camera control

**Android:** `rememberCameraManipulator()` gives you drag-to-orbit, pinch-to-zoom, and two-finger-pan with no extra code.

**iOS:** SceneView provides built-in orbit camera gestures that integrate with SwiftUI's gesture system.

## Adding a loading state

**Android:**
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    Scene(...) {
        modelInstance?.let { ModelNode(modelInstance = it) }
    }

    // Show spinner while loading
    if (modelInstance == null) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
```

**iOS:**
```swift
ZStack {
    SceneView {
        ModelNode(named: "model.usdz")
    }
    // SwiftUI handles loading states via ProgressView
}
```

## Going further

### Custom camera position (Android)

```kotlin
val cameraNode = rememberCameraNode(engine) {
    position = Float3(x = 0f, y = 1f, z = 3f)
    lookAt(Float3(0f, 0f, 0f))
}

Scene(cameraNode = cameraNode, ...) { ... }
```

### Point light (Android)

```kotlin
Scene {
    ModelNode(...)
    LightNode(type = LightManager.Type.POINT) {
        intensity(50_000f)
        position(0f, 2f, 0f)
    }
}
```

### Light (iOS)

```swift
SceneView {
    ModelNode(named: "model.usdz")
    LightNode(.point, intensity: 50000, position: [0, 2, 0])
}
```

### Gesture callbacks (Android)

```kotlin
Scene(
    onSingleTapConfirmed = { motionEvent, node ->
        node?.let { println("Tapped: $it") }
    }
) { ... }
```

## Full source

The complete model viewer sample (with model picker chips, environment switcher, and animation controls) is in the [SceneView repository](https://github.com/SceneView/sceneview/tree/main/samples/model-viewer).

---

*Next: [Your first AR app with SceneView →](#)*

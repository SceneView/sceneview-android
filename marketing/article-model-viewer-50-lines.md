# Building a 3D Model Viewer in 50 Lines of Jetpack Compose

*Tags: Android, iOS, JetpackCompose, SwiftUI, 3D, Kotlin, Swift, Tutorial*

---

Here's a challenge: build a production-quality 3D model viewer for Android — with orbit camera, HDR lighting, model switcher, and smooth animations — in under 50 lines of Kotlin.

With SceneView, it's not just possible. It's easy.

## The result

By the end of this article, you'll have a viewer that:

- Loads any `.glb` / `.gltf` model from assets
- Supports drag-to-orbit, pinch-to-zoom, pan
- Has realistic HDR environment lighting
- Shows model animations (if the model has them)
- Lets the user switch between models with a chip selector

## Setup (30 seconds)

```kotlin
// build.gradle.kts
implementation("io.github.sceneview:sceneview:3.3.0")
```

Put your models in `src/main/assets/models/` and an HDR file in `src/main/assets/environments/`.

## The complete viewer

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

That's the whole viewer. 50 lines.

## How it works

### State-driven model loading

The key insight: `rememberModelInstance(modelLoader, selectedModel)` returns `null` while the model is loading, and the valid instance once ready.

```kotlin
val modelInstance = rememberModelInstance(modelLoader, selectedModel)
// null while loading → ModelNode doesn't render → no crash, no flash
modelInstance?.let {
    ModelNode(modelInstance = it, scaleToUnits = 1.0f)
}
```

When `selectedModel` changes (user taps a chip), Compose re-executes `rememberModelInstance` with the new path. The old model is cleaned up automatically. The new one starts loading. Zero manual lifecycle management.

### Automatic resource disposal

`rememberEngine()`, `rememberModelLoader()`, `rememberEnvironment()` — all these `remember*` helpers register cleanup callbacks with the Compose lifecycle. When the composable leaves the composition (user navigates away, screen is destroyed), every Filament object is properly destroyed on the main thread.

```kotlin
// This entire block auto-disposes when the composable exits the composition:
val engine = rememberEngine()
val modelLoader = rememberModelLoader(engine)
val environment = rememberEnvironment(environmentLoader) { ... }
```

### HDR environment lighting

Pass the `environment` to `Scene { }` and every model you add automatically gets:
- Image-based lighting (IBL) from the HDR
- Sky rendering (the HDR is the background)
- Specular reflections on metallic/glossy surfaces

```kotlin
Scene(
    environment = environment,  // ← HDR lighting + sky
    ...
)
```

### Camera control

`rememberCameraManipulator()` gives you drag-to-orbit, pinch-to-zoom, and two-finger-pan with no extra code.

```kotlin
Scene(
    cameraManipulator = rememberCameraManipulator()
)
```

## Adding a loading state

Real apps want a progress indicator while the model loads:

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

## Going further

### Custom camera position

```kotlin
val cameraNode = rememberCameraNode(engine) {
    position = Float3(x = 0f, y = 1f, z = 3f)
    lookAt(Float3(0f, 0f, 0f))
}

Scene(cameraNode = cameraNode, ...) { ... }
```

### Point light

```kotlin
Scene {
    ModelNode(...)
    LightNode(type = LightManager.Type.POINT) {
        intensity(50_000f)
        position(0f, 2f, 0f)
    }
}
```

### Gesture callbacks

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

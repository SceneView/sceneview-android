# YouTube Script — "3D and AR in Jetpack Compose with SceneView 3.0"

**Format:** Screen recording + talking head in corner
**Length:** ~10 minutes
**Target:** Android developers who've heard of ARCore/Filament but found the API too complex

---

## [0:00 — 0:30] Hook

*(Show a split screen: left side is the old ARSceneView Fragment XML setup, ~60 lines. Right side is the new `ARScene { }`, ~12 lines. Animate them appearing simultaneously.)*

**Narration:**
> "This is what it used to take to place a 3D model in AR on Android.
> And this is what it takes today, with SceneView 3.0.
> Same result. Same Filament renderer. Same ARCore tracking.
> The difference is that 3.0 is just Compose.
> Let me show you what that means."

---

## [0:30 — 1:30] The concept

*(Show a `Column` with `Text`, `Image`, `Button` children. Then animate it morphing into a `Scene` with `ModelNode`, `LightNode`. Emphasize the structural similarity.)*

**Narration:**
> "You already know how Compose works. You have a container — `Column`, `Box`, `Row` — and you put children inside it. The runtime manages when they appear and disappear.
>
> SceneView 3.0 takes that exact model and applies it to a 3D scene.
> `Scene` is a composable. `ModelNode`, `LightNode`, `CubeNode` — those are composables too. They live inside `Scene`'s content block, they react to state, and they're destroyed automatically when they leave the composition.
>
> No manual `addChildNode`. No `destroy()` calls. No lifecycle overrides. Just Compose."

---

## [1:30 — 3:30] First scene — 3D model viewer

*(Live code the model viewer from scratch in Android Studio. Use the damaged helmet GLB.)*

**Narration and code:**

```kotlin
// Step 1 — The remember helpers
val engine = rememberEngine()
val modelLoader = rememberModelLoader(engine)
val environmentLoader = rememberEnvironmentLoader(engine)
```

> "Every Filament resource is a `remember` — the engine, the loaders, the environment. They're created once and cleaned up automatically when the composable leaves the tree. You never touch them directly."

```kotlin
// Step 2 — Async model loading
val modelInstance = rememberModelInstance(modelLoader, "models/damaged_helmet.glb")
// null while loading, non-null when ready
```

> "`rememberModelInstance` is the interesting one. It reads the file on a background thread and calls Filament back on the main thread — that's important because Filament is single-threaded. While it's loading, it returns null. When it's done, Compose recomposes and the node appears. No loading state variable, no callback."

```kotlin
// Step 3 — The scene
Scene(
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    modelLoader = modelLoader,
    environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
    },
    cameraManipulator = rememberCameraManipulator(),
    mainLightNode = rememberMainLightNode(engine) { intensity = 100_000.0f }
) {
    modelInstance?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
    }
}
```

> "This is the complete model viewer. `cameraManipulator` gives you orbit, pan, and zoom with one line. `mainLightNode` adds the sun. The `modelInstance?.let` block means the node only exists when the model is loaded — nothing to manage."

*(Run the app. Show the helmet rendering with HDR lighting. Drag to orbit.)*

> "That's a photorealistic 3D model, HDR environment lighting, orbit/zoom camera — in about 20 lines of Compose code."

---

## [3:30 — 5:00] Nesting — the NodeScope

*(Add a pivot node with children. Show the scene graph reflected in the Compose structure.)*

```kotlin
Scene {
    // A pivot node acts like a Box — children attach to it
    Node(position = Position(y = 0.5f)) {
        ModelNode(modelInstance = instance, scaleToUnits = 0.5f)

        // This light is a child of the Node — it moves with it
        LightNode(type = LightManager.Type.POINT) {
            intensity = 50_000f
        }
    }

    // Geometry nodes — no GLB needed
    CubeNode(
        size = Size(0.1f),
        materialInstance = materialLoader.createColorInstance(Color.Red)
    )
}
```

> "The trailing lambda on any node opens a `NodeScope` where children are parented to that node. It's structurally identical to how `Column { }` works. The Compose tree IS the scene graph."

---

## [5:00 — 7:30] AR — same pattern

*(New file. Build the AR model placement demo.)*

**Narration:**

> "`ARScene` is `Scene` with ARCore wired in. The content block gives you an `ARSceneScope` with AR-specific composables. Let's build plane detection and anchor placement."

```kotlin
var anchor by remember { mutableStateOf<Anchor?>(null) }

ARScene(
    modifier = Modifier.fillMaxSize(),
    cameraNode = rememberARCameraNode(engine),
    planeRenderer = true,
    sessionConfiguration = { session, config ->
        config.depthMode =
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
    },
    onSessionUpdated = { _, frame ->
        if (anchor == null) {
            anchor = frame.getUpdatedPlanes()
                .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                ?.let { frame.createAnchorOrNull(it.centerPose) }
        }
    }
) {
    anchor?.let { a ->
        AnchorNode(anchor = a) {
            ModelNode(modelInstance = helmet, scaleToUnits = 0.5f, isEditable = true)
        }
    }
}
```

> "The anchor is just a `mutableStateOf<Anchor?>`. When `onSessionUpdated` detects a plane and creates an anchor, `anchor` becomes non-null. Compose recomposes. `AnchorNode` enters the composition. The model appears — physically anchored to the floor in your room.
>
> `isEditable = true` on `ModelNode` enables pinch-to-scale and drag-to-rotate for free.
>
> When you clear the anchor, the node leaves the composition and is destroyed. No cleanup code."

*(Run on device. Show AR plane detection, the model appearing, pinch-to-scale.)*

---

## [7:30 — 9:00] Subtle 3D — the real opportunity

*(Show a standard e-commerce UI with a product detail page. Replace the `Image` with a `Scene`.)*

**Narration:**

> "Here's the use case I find most exciting. Most apps won't be AR apps. But any app that shows a product, a profile, a dashboard — that's a place where a small dose of 3D can make the experience noticeably better.
>
> The standard approach: a static PNG of your product. Replace it with this:"

```kotlin
Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
    val modelInstance = rememberModelInstance(modelLoader, "models/shoe.glb")
    Scene(
        modifier = Modifier.fillMaxSize(),
        cameraManipulator = rememberCameraManipulator()
    ) {
        modelInstance?.let {
            ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true)
        }
    }

    // Standard Compose UI overlaid on top — no problem
    Text(
        text = "Pinch to rotate",
        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
        style = MaterialTheme.typography.bodySmall
    )
}
```

> "Same `Box` you'd use for any overlay. The 3D scene renders behind the `Text`. Users can orbit the product. This is a `Box` with a `Scene` where the image used to be."

---

## [9:00 — 9:45] Quick tour of other node types

*(Fast montage, each shown for ~5 seconds with code.)*

```kotlin
// Geometry — no assets needed
CylinderNode(radius = 0.1f, height = 0.5f, materialInstance = blueMaterial)
SphereNode(radius = 0.2f, materialInstance = metalMaterial)

// Compose UI as a 3D surface
val windowManager = rememberViewNodeManager()
ViewNode(windowManager = windowManager) {
    Card(modifier = Modifier.padding(8.dp)) {
        Text("Hello from 3D!")
    }
}

// AR: track detected images
AugmentedImageNode(augmentedImage = detectedImage) {
    ModelNode(modelInstance = overlay)
}
```

---

## [9:45 — 10:00] Closing

**Narration:**

> "SceneView 3.0 is open source, on Maven Central, and the API is just Compose. If you know Kotlin and Compose, you already know 80% of it.
>
> Link in the description — there's a migration guide if you're coming from 2.x, CodeLabs for getting started, and samples for every feature.
>
> Drop a question in the Discord if you get stuck. See you there."

*(Show GitHub repo, Discord link, "Like and subscribe" card.)*

---

## B-roll / cutaway shots needed

- Device running the model viewer (orbit gestures visible)
- Device running AR plane detection → model appearing
- Side-by-side Android Studio + physical device
- Close-up of the device screen showing HDR lighting on the helmet
- The product viewer (shoe) in a mock e-commerce app UI

## Assets needed

| Scene | Asset | URL |
|---|---|---|
| Model viewer | `damaged_helmet.glb` | Already in project |
| Product viewer | `MaterialsVariantsShoe.glb` | https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/MaterialsVariantsShoe/glTF-Binary/MaterialsVariantsShoe.glb |
| AR demo | `ChronographWatch.glb` | https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/ChronographWatch/glTF-Binary/ChronographWatch.glb |
| Environment | `sky_2k.hdr` | Already in project |

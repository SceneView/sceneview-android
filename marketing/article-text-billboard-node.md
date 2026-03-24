# TextNode & BillboardNode — Adding Labels to Your AR Scene

*Tags: Android, iOS, AR, JetpackCompose, SwiftUI, Kotlin, Swift, 3D*

---

One of the most requested features in any 3D or AR app: **labels**. Floating text above a model, a waypoint marker that always faces the user, an info panel anchored to the real world.

In SceneView 3.3.0, adding a camera-facing text label is one composable call on both Android and iOS. No bitmaps, no Canvas, no material creation — all handled internally.

## What we're building

By the end of this article you'll have:

1. **3D text labels** that hover above objects and always face the camera — in a pure 3D scene
2. **AR anchor labels** — labels attached to real-world positions via ARCore anchors

---

## Part 1 — TextNode in a 3D scene

### Setup

```kotlin
// build.gradle.kts
implementation("io.github.sceneview:sceneview:3.3.0")
```

### Camera-facing labels in 12 lines

```kotlin
@Composable
fun LabelledSceneScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = rememberEnvironment(environmentLoader)

    // TextNode needs the camera position each frame to face it
    var cameraPos by remember { mutableStateOf(Position(0f, 1f, 3f)) }
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 1f, z = 3f)
        lookAt(Position(0f, 0f, 0f))
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraNode = cameraNode,
        environment = environment,
        onFrame = { cameraPos = cameraNode.worldPosition }
    ) {
        val model = rememberModelInstance(modelLoader, "models/toy_car.glb")
        model?.let {
            ModelNode(modelInstance = it, scaleToUnits = 1.0f)
        }

        // The label — always faces the camera, no manual rotation needed
        TextNode(
            text = "Toy Car",
            fontSize = 52f,
            textColor = android.graphics.Color.WHITE,
            backgroundColor = 0xCC000000.toInt(),
            widthMeters = 0.5f,
            heightMeters = 0.16f,
            position = Position(x = 0f, y = 0.7f, z = 0f),
            cameraPositionProvider = { cameraPos }
        )
    }
}
```

The label floats 0.7 m above the origin, renders text onto an Android `Canvas` bitmap internally, and calls `lookAt(cameraPos)` every frame. When `text` changes (Compose state), the bitmap is re-rendered automatically. No manual cleanup required.

### Multiple labelled objects

```kotlin
data class Exhibit(val path: String, val label: String, val x: Float)

val exhibits = listOf(
    Exhibit("models/chair.glb",    "Sheen Chair",   -1.2f),
    Exhibit("models/lamp.glb",     "Lamp",           0f),
    Exhibit("models/helmet.glb",   "Space Helmet",   1.2f),
)

Scene(..., onFrame = { cameraPos = cameraNode.worldPosition }) {
    exhibits.forEach { exhibit ->
        val model = rememberModelInstance(modelLoader, exhibit.path)
        model?.let {
            ModelNode(
                modelInstance = it,
                scaleToUnits = 0.6f,
                apply = { position = Position(x = exhibit.x) }
            )
        }
        TextNode(
            text = exhibit.label,
            position = Position(x = exhibit.x, y = 0.8f, z = 0f),
            cameraPositionProvider = { cameraPos }
        )
    }
}
```

Each `TextNode` takes less than 5 lines. State-driven: change `exhibit.label` and Compose re-renders the bitmap.

---

## Part 2 — AR anchor labels with BillboardNode

AR adds a dimension: labels must track **real-world positions** via ARCore anchors. `BillboardNode` is the lower-level primitive — you supply a `Bitmap`, it handles the camera-facing rotation.

### Setup

```kotlin
// build.gradle.kts
implementation("io.github.sceneview:arsceneview:3.3.0")
```

### Waypoint labels on tap

```kotlin
@Composable
fun ARWaypointScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    var cameraPos by remember { mutableStateOf(Position(0f, 0f, 0f)) }

    // Each anchor gets a label string
    data class Waypoint(val anchor: Anchor, val label: String)
    val waypoints = remember { mutableStateListOf<Waypoint>() }
    var waypointCount by remember { mutableStateOf(0) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        onFrame = { _, frame ->
            // Keep cameraPos fresh so labels track the viewer
            frame.camera.let { cam ->
                cameraPos = Position(
                    cam.pose.tx(),
                    cam.pose.ty(),
                    cam.pose.tz()
                )
            }
        },
        onSingleTapConfirmed = { hitResult ->
            val anchor = hitResult.createAnchor()
            waypoints.add(Waypoint(anchor, "Waypoint ${++waypointCount}"))
        }
    ) {
        waypoints.forEach { (anchor, label) ->
            AnchorNode(engine = engine, anchor = anchor) {
                // Camera-facing text label at anchor position
                TextNode(
                    text = label,
                    fontSize = 48f,
                    textColor = android.graphics.Color.WHITE,
                    backgroundColor = 0xDD1565C0.toInt(),
                    widthMeters = 0.4f,
                    heightMeters = 0.14f,
                    position = Position(y = 0.1f), // slightly above the floor
                    cameraPositionProvider = { cameraPos }
                )
            }
        }
    }
}
```

Each `TextNode` is a child of an `AnchorNode` — it inherits the anchor's world transform, so the label stays fixed to the real-world surface as the user moves around. The `cameraPositionProvider` lambda is what makes it always face the viewer.

### Custom bitmap waypoint marker (BillboardNode)

For icon markers — a pin, an arrow, a logo — use `BillboardNode` directly and supply your own `Bitmap`:

```kotlin
val markerBitmap = remember {
    // Load from assets or draw programmatically
    BitmapFactory.decodeStream(context.assets.open("icons/waypoint_pin.png"))
}

ARScene(...) {
    waypoints.forEach { (anchor, _) ->
        AnchorNode(engine = engine, anchor = anchor) {
            BillboardNode(
                bitmap = markerBitmap,
                widthMeters = 0.25f,
                heightMeters = 0.25f,
                cameraPositionProvider = { cameraPos }
            )
        }
    }
}
```

`BillboardNode` is the base class of `TextNode` — same camera-facing logic, you control the pixel content.

---

## How TextNode works under the hood

`TextNode` renders your string into an `android.graphics.Bitmap` using `Canvas`:

1. Draws a rounded-rectangle background fill
2. Draws centred, bold text in the given `textColor`
3. Uploads the bitmap as a Filament texture
4. On each frame, calls `lookAt(cameraPosition)` to rotate the quad

When `text`, `fontSize`, `textColor`, or `backgroundColor` change, the bitmap is regenerated and the texture is updated. Everything happens on the main thread — no threading pitfalls.

---

## Going further

### Reactive labels driven by state

```kotlin
var distance by remember { mutableStateOf(0f) }

// Update distance in onFrame
Scene(onFrame = {
    distance = (modelNode.worldPosition - cameraNode.worldPosition).length()
}) {
    TextNode(
        text = "%.1f m away".format(distance),
        cameraPositionProvider = { cameraPos }
    )
}
```

Because `text` is a regular Compose parameter, any state change re-renders the label automatically.

### Tappable labels

```kotlin
TextNode(
    text = "Info",
    apply = {
        isTouchable = true
        onSingleTapConfirmed = { showInfoPanel = true; true }
    },
    cameraPositionProvider = { cameraPos }
)
```

## iOS: TextNode & BillboardNode with SwiftUI

SceneView 3.3.0 ships `TextNode` and `BillboardNode` on iOS too. Built on RealityKit's mesh generation:

```swift
import SceneViewSwift

struct LabelledSceneView: View {
    var body: some View {
        SceneView {
            ModelNode(named: "toy_car.usdz")
                .scaleToUnits(1.0)

            // Camera-facing text label
            TextNode(text: "Toy Car", position: [0, 0.7, 0])
                .fontSize(52)
                .textColor(.white)
                .backgroundColor(.black.opacity(0.8))

            LightNode(.directional)
        }
    }
}
```

For AR labels on iOS:

```swift
ARSceneView { anchor in
    ModelNode(named: "chair.usdz")
        .scale(0.5)

    TextNode(text: "Chair", position: [0, 0.3, 0])
        .fontSize(48)
        .textColor(.white)
        .backgroundColor(.blue.opacity(0.85))
}
```

Same concept as Android — camera-facing text labels attached to any node or AR anchor. The API is native to each platform while the developer experience stays consistent.

## Full samples

Both demos are in the SceneView repository:

- `samples/text-labels/` — Labelled 3D spheres, tap to cycle label text (Android)
- `samples/ar-model-viewer/` — AR tap-to-place with gesture docs (Android)
- iOS examples in `SceneViewSwift/` package

```
git clone https://github.com/SceneView/sceneview
./gradlew :samples:text-labels:installDebug
```

---

*SceneView is open-source (Apache 2.0). Now cross-platform: Android + iOS + macOS + visionOS. [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)*

# Recipe: AR Tap-to-Place

**Intent:** "Place a 3D model on a real surface when the user taps"

## Android (Kotlin + Jetpack Compose)

```kotlin
@Composable
fun ARTapToPlace() {
    var anchor by remember { mutableStateOf<Anchor?>(null) }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, "models/chair.glb")

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
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
                model?.let { ModelNode(modelInstance = it, scaleToUnits = 0.5f) }
            }
        }
    }
}
```

## iOS (Swift + SwiftUI)

```swift
struct ARTapToPlace: View {
    @State private var model: ModelNode?
    @State private var placed = false

    var body: some View {
        ARSceneView(
            planeDetection: .horizontal,
            onTapGesture: { hit in
                guard !placed, let model else { return }
                let anchor = AnchorNode.plane(alignment: .horizontal)
                anchor.add(model.entity)
                placed = true
            }
        ) { content in
            // Content added via anchor in onTapGesture
        }
        .task {
            model = try? await ModelNode.load("models/chair.usdz")
                .scaleToUnits(0.5)
        }
    }
}
```

## Key concepts

| Concept | Android | iOS |
|---|---|---|
| AR container | `ARScene { }` | `ARSceneView { }` |
| Plane detection | `planeRenderer = true` | `planeDetection: .horizontal` |
| Anchor | `AnchorNode(anchor = a) { }` | `AnchorNode.plane()` |
| Hit testing | `onSessionUpdated` + frame planes | `onTapGesture` callback |
| AR framework | ARCore | ARKit |

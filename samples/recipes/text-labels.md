# Recipe: 3D Text Labels

**Intent:** "Add floating text labels in a 3D scene"

## Android (Kotlin + Jetpack Compose)

```kotlin
@Composable
fun TextLabels() {
    val engine = rememberEngine()

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        cameraManipulator = rememberCameraManipulator()
    ) {
        TextNode(
            text = "Hello 3D!",
            position = Position(y = 1f),
            textHeight = 0.5f,
            textColor = Color.White,
            backgroundColor = Color(0x80000000)
        )
        // Billboard: always faces camera
        BillboardNode(position = Position(y = 2f)) {
            TextNode(text = "I follow you")
        }
    }
}
```

## iOS (Swift + SwiftUI)

```swift
struct TextLabels: View {
    var body: some View {
        SceneView { content in
            // Static text label
            let label = TextNode(
                text: "Hello 3D!",
                fontSize: 0.1,
                color: .white
            )
            .position(.init(x: 0, y: 1, z: -2))
            content.add(label.entity)

            // Billboard text (always faces camera)
            let billboard = BillboardNode(
                child: TextNode(text: "I follow you", fontSize: 0.08).entity
            )
            .position(.init(x: 0, y: 2, z: -2))
            content.add(billboard.entity)
        }
        .cameraControls(.orbit)
    }
}
```

## Key concepts

| Concept | Android | iOS |
|---|---|---|
| Text node | `TextNode(text = "...")` | `TextNode(text: "...")` |
| Billboard | `BillboardNode { }` | `BillboardNode(child:)` |
| Text rendering | Canvas → texture quad | `MeshResource.generateText` |
| Face camera | Automatic per-frame lookAt | `BillboardComponent` |

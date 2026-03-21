# Text Labels

Camera-facing 3D text labels attached to coloured spheres, with tap-to-cycle interaction.

## What it demonstrates
- `TextNode` — billboard text labels that always face the camera via `cameraPositionProvider`
- `SphereNode` with `isTouchable = true` — built-in touch handling on primitive geometry
- Tap interaction via `onSingleTapConfirmed` to cycle label text
- Auto-orbiting camera using `animateRotation` with `infiniteRepeatable`
- `SnapshotStateList` for reactive label text updates

## Key code

```kotlin
Scene(
    modifier = Modifier.fillMaxSize(),
    onFrame = {
        centerNode.rotation = cameraRotation
        cameraPos = cameraNode.worldPosition
    }
) {
    objects.forEachIndexed { index, obj ->
        SphereNode(
            radius = obj.radius,
            materialInstance = sphereMaterial,
            apply = {
                position = obj.position
                isTouchable = true
                onSingleTapConfirmed = { labels[index] = nextLabel(labels[index]); true }
            }
        )

        TextNode(
            text = labels[index],
            fontSize = 52f,
            widthMeters = 0.55f,
            heightMeters = 0.18f,
            position = Position(x = obj.position.x, y = obj.position.y + 0.37f, z = obj.position.z),
            cameraPositionProvider = { cameraPos }
        )
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:text-labels` configuration.

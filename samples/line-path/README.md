# Line & Path

Draw 3D polylines and paths using `LineNode` and `PathNode`, with an animated sine wave driven by Compose animation.

## What it demonstrates
- `LineNode` — a single line segment between two 3D points (used here for an XYZ axis gizmo)
- `PathNode` — a smooth polyline through a list of 3D points (spiral and sine wave)
- Compose `animateFloat` driving a real-time sine-wave animation via the `phase` parameter
- Procedural geometry generation (helix + sine wave) with no model files
- Colour materials created with `materialLoader.createColorInstance`

## Key code

```kotlin
Scene(
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = Position(0f, 1f, 3.5f)
    )
) {
    // Axis gizmo
    LineNode(start = Position(0f, 0f, 0f), end = Position(1f, 0f, 0f), materialInstance = redMaterial)
    LineNode(start = Position(0f, 0f, 0f), end = Position(0f, 1f, 0f), materialInstance = greenMaterial)
    LineNode(start = Position(0f, 0f, 0f), end = Position(0f, 0f, 1f), materialInstance = blueMaterial)

    // Spiral
    PathNode(points = spiralPts, closed = false, materialInstance = yellowMaterial)

    // Animated sine wave
    PathNode(points = sineWavePts, closed = false, materialInstance = cyanMaterial)
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:line-path` configuration.

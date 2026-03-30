# Advanced guide: Lines, paths, and text labels

**Time:** ~15 minutes
**Level:** Intermediate
**What you'll build:** 3D line drawings and floating text labels in world space

---

## Overview

SceneView 3.2.0 adds three geometry primitives for annotation and visualization:

- **`LineNode`** — a single line segment between two points
- **`PathNode`** — a polyline through multiple points (open or closed)
- **`TextNode`** — a camera-facing text label rendered as a billboard

---

## Lines and paths

### Single line

```kotlin
SceneView(engine = engine, modelLoader = modelLoader) {
    val line = remember(engine) {
        LineNode(
            engine = engine,
            start = Position(x = -1f, y = 0f, z = 0f),
            end = Position(x = 1f, y = 1f, z = 0f)
        )
    }
    Node(node = line)
}
```

### Path (polyline)

```kotlin
val points = remember {
    (0..100).map { i ->
        val t = i / 100f * Math.PI.toFloat() * 4
        Position(
            x = t * 0.1f - 2f,
            y = sin(t) * 0.5f,
            z = cos(t) * 0.5f
        )
    }
}

SceneView(engine = engine, modelLoader = modelLoader) {
    val path = remember(engine, points) {
        PathNode(engine = engine, points = points)
    }
    Node(node = path)
}
```

### Closed path (polygon)

```kotlin
val triangle = listOf(
    Position(0f, 1f, 0f),
    Position(-1f, -0.5f, 0f),
    Position(1f, -0.5f, 0f)
)

val closedPath = remember(engine) {
    PathNode(engine = engine, points = triangle, closed = true)
}
```

---

## Text labels

`TextNode` renders text to a bitmap and displays it as a camera-facing billboard.

```kotlin
var cameraPos by remember { mutableStateOf(Position()) }

SceneView(
    engine = engine,
    modelLoader = modelLoader,
    onFrame = { cameraPos = cameraNode.worldPosition }
) {
    TextNode(
        materialLoader = materialLoader,
        text = "Hello 3D!",
        fontSize = 48f,
        textColor = android.graphics.Color.WHITE,
        backgroundColor = 0xCC000000.toInt(),
        widthMeters = 0.6f,
        heightMeters = 0.2f,
        cameraPositionProvider = { cameraPos }
    )
}
```

### Parameters

| Parameter | Effect |
|---|---|
| `text` | The string to display |
| `fontSize` | Font size in pixels for the bitmap texture |
| `textColor` | ARGB text colour |
| `backgroundColor` | ARGB background fill |
| `widthMeters` / `heightMeters` | Size of the quad in world space |
| `cameraPositionProvider` | Lambda returning camera position — label faces the camera |
| `bitmapWidth` / `bitmapHeight` | Resolution of the backing bitmap (default 512x128) |

### Positioning labels

Set the label's position like any node:

```kotlin
TextNode(
    materialLoader = materialLoader,
    text = "Earth",
    cameraPositionProvider = { cameraPos }
).apply {
    position = Position(x = 0f, y = 2f, z = 0f)
}
```

---

## Combining lines and labels

A common pattern is annotating a 3D scene with measurement lines and labels:

```kotlin
SceneView(engine = engine, modelLoader = modelLoader) {
    // Measurement line
    val measureLine = remember(engine) {
        LineNode(engine, start = Position(-1f, 0f, 0f), end = Position(1f, 0f, 0f))
    }
    Node(node = measureLine)

    // Label at midpoint
    TextNode(
        materialLoader = materialLoader,
        text = "2.0 m",
        fontSize = 36f,
        widthMeters = 0.4f,
        heightMeters = 0.15f,
        cameraPositionProvider = { cameraPos }
    ).apply {
        position = Position(0f, 0.2f, 0f)
    }
}
```

---

## What's next

- See the `line-path` sample for animated sine/Lissajous curves with parameter sliders
- See the `text-labels` sample for an interactive solar system with label cycling

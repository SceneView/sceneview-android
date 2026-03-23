---
title: "3D is just Compose UI — what SceneView 3.2 makes possible on Android"
published: true
description: "26 composable node types, physics, dynamic sky, fog, reflections — and it still feels like writing a Column."
tags: android, kotlin, jetpackcompose, augmentedreality
canonical_url: https://sceneview.github.io/showcase/
cover_image: # Add a 1000x420 image of a SceneView render here
---

You already know how to build a Compose screen. A `Column` with some children. A `Box` with overlapping layers. You've done it a hundred times.

What if a 3D scene worked exactly the same way?

```kotlin
// A Compose UI screen
Column {
    Text("Title")
    Image(painter = painterResource(R.drawable.cover), contentDescription = null)
}

// A 3D scene with SceneView
Scene(modifier = Modifier.fillMaxSize()) {
    ModelNode(modelInstance = helmet, scaleToUnits = 1.0f, autoAnimate = true)
    LightNode(type = LightManager.Type.SUN, apply = { intensity(100_000.0f) })
}
```

Same pattern. Same Kotlin. Same mental model — now with depth.

---

## What changed since 3.0

SceneView 3.0 introduced the core idea: **nodes are composables**. Since then, the library has grown to 26+ node types. Here's what 3.2 added:

### Physics

`PhysicsNode` brings rigid body simulation. Gravity, collision detection, tap-to-throw.

```kotlin
Scene {
    PhysicsNode(
        shape = SphereShape(radius = 0.1f),
        mass = 1.0f,
        restitution = 0.8f
    ) {
        SphereNode(radius = 0.1f, materialInstance = ballMaterial)
    }
}
```

### Dynamic sky

`DynamicSkyNode` drives sun position from a single `timeOfDay: Float` value. Sunrise, noon, golden hour, sunset — all reactive to Compose state.

```kotlin
var timeOfDay by remember { mutableStateOf(0.5f) }

Scene {
    DynamicSkyNode(timeOfDay = timeOfDay, turbidity = 4.0f)
}

Slider(value = timeOfDay, onValueChange = { timeOfDay = it })
```

### Fog, reflections, lines, text

- `FogNode` — atmospheric fog with density and height falloff
- `ReflectionProbeNode` — local cubemap reflections for metallic surfaces
- `LineNode` / `PathNode` — 3D polylines (measurements, drawing, animated paths)
- `TextNode` / `BillboardNode` — camera-facing text labels in 3D space

### Post-processing

Bloom, depth-of-field, SSAO, fog — all toggleable from Compose state.

---

## The use case nobody talks about

Most 3D demos show a rotating helmet on a black background. Cool — but who needs that?

The real opportunity: **subtle 3D**. Replace a flat `Image()` on your product page with a `Scene {}`:

```kotlin
// Before
Image(painter = painterResource(R.drawable.shoe), contentDescription = "Shoe")

// After — interactive 3D in 10 extra lines
val model = rememberModelInstance(modelLoader, "models/shoe.glb")
Scene(
    modifier = Modifier.fillMaxWidth().height(300.dp),
    cameraManipulator = rememberCameraManipulator()
) {
    model?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
}
```

The customer orbits the product with one finger. No separate "3D viewer" screen. No Unity integration project. Just a composable.

---

## AR works the same way

`ARScene` is `Scene` with ARCore wired in:

```kotlin
ARScene(
    planeRenderer = true,
    onSessionUpdated = { _, frame ->
        anchor = frame.getUpdatedPlanes()
            .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
            ?.let { frame.createAnchorOrNull(it.centerPose) }
    }
) {
    anchor?.let { a ->
        AnchorNode(anchor = a) {
            ModelNode(modelInstance = sofa, scaleToUnits = 0.5f)
        }
    }
}
```

Plane detection, image tracking, face mesh, cloud anchors, geospatial API — all as composables.

---

## ViewNode — the feature nobody else has

Render **any Composable** directly inside 3D space:

```kotlin
AnchorNode(anchor = sofaAnchor) {
    ModelNode(modelInstance = sofa)
    ViewNode {
        Card {
            Text("Sofa Pro", style = MaterialTheme.typography.titleMedium)
            Text("€ 599", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = {}) { Text("Buy in AR") }
        }
    }
}
```

A real Compose `Card` with buttons, text fields, images — floating in 3D space next to your AR content. No other Android 3D library does this.

---

## The complete setup

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, "models/helmet.glb")
    val environment = rememberEnvironment(rememberEnvironmentLoader(engine)) {
        createHDREnvironment("environments/sky_2k.hdr")!!
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = environment,
        cameraManipulator = rememberCameraManipulator(),
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000.0f }
    ) {
        model?.let {
            ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true)
        }
    }
}
// All resources destroyed automatically when composable leaves the tree
```

No XML. No fragments. No lifecycle callbacks. No OpenGL boilerplate.

---

## vs. the alternatives

| | SceneView | Sceneform | Unity | Raw ARCore |
|---|---|---|---|---|
| **Compose** | Native | No | No | No |
| **Setup** | 1 Gradle line | Archived | Separate pipeline | 500+ lines |
| **APK size** | ~5 MB | ~3 MB | 40–350 MB | ~1 MB |
| **Physics** | Built-in | No | Built-in | No |
| **Status** | Active | Dead (2021) | Active | No UI layer |

---

## What's next: v4.0

- Multiple `Scene {}` composables on one screen
- `PortalNode` — scene inside a scene (AR portals)
- `SceneView-XR` — Android XR spatial computing
- Kotlin Multiplatform proof of concept (iOS)

---

## Get started

```gradle
// 3D only
implementation("io.github.sceneview:sceneview:3.2.0")

// 3D + AR
implementation("io.github.sceneview:arsceneview:3.2.0")
```

15 sample apps. Full API docs. MCP server for AI-assisted development.

- **GitHub**: [github.com/SceneView/sceneview-android](https://github.com/SceneView/sceneview-android)
- **Docs**: [sceneview.github.io](https://sceneview.github.io)
- **Discord**: [discord.gg/UbNDDBTNqb](https://discord.gg/UbNDDBTNqb)

---

*SceneView is open source (Apache 2.0). Built on Google Filament 1.70 and ARCore 1.53.*

# 3D is just Compose UI — how SceneView 3.0 changed everything

*Published on [Medium](https://medium.com/) — copy/paste ready*

---

You already know how to build a Compose screen. A `Column` with some children. A `Box` with overlapping layers. You've done it a hundred times.

What if a 3D scene worked exactly the same way?

```kotlin
// A Compose UI screen
Column {
    Text("Title")
    Image(painter = painterResource(R.drawable.cover), contentDescription = null)
    Button(onClick = { /* ... */ }) { Text("Open") }
}

// A 3D scene with SceneView 3.0
Scene(modifier = Modifier.fillMaxSize()) {
    ModelNode(modelInstance = helmet, scaleToUnits = 1.0f, autoAnimate = true)
    LightNode(type = LightManager.Type.SUN)
}
```

Same pattern. Same Kotlin. Same mental model — now with depth.

That's the idea behind SceneView 3.0, and I want to show you what it means in practice.

---

## The problem with 3D on Android

Before SceneView 3.0, adding a 3D model to an Android app looked roughly like this:

1. Create a `SceneView` in XML layout
2. Create an `ArFragment` or wire up lifecycle callbacks manually
3. Load your model in `onResume`, check if the engine is ready, handle the async response
4. Add child nodes imperatively — `parentNode.addChildNode(modelNode)`
5. Remember to remove them in `onPause`. Or was it `onStop`? Don't forget `destroy()`.
6. Debug why the camera isn't showing, why the model is black, why everything breaks on rotation

Even experienced engineers would spend a full day on a basic AR model placement. And the result felt bolted on — a separate rendering system living alongside your Compose UI, not part of it.

---

## SceneView 3.0: nodes are composables

The 3.0 rewrite starts from a different premise: **the scene graph should work like the Compose tree**.

Nodes are composable functions. They enter the scene on first composition. They're destroyed when they leave. State drives everything. The Compose runtime handles the lifecycle.

```kotlin
var showHelmet by remember { mutableStateOf(true) }

Scene(modifier = Modifier.fillMaxSize()) {
    if (showHelmet) {
        ModelNode(modelInstance = helmet, scaleToUnits = 1.0f)
    }
}
```

Toggle `showHelmet` to `false` and the node disappears — and is properly destroyed — without a single line of imperative cleanup. Toggle it back and the node reappears. This is just Compose.

### Nesting works the same way

In Compose UI you nest children inside a `Column` or `Box`. In SceneView you nest child nodes inside any node's trailing lambda:

```kotlin
Scene {
    // A pivot node at 0.5m height
    Node(position = Position(y = 0.5f)) {
        ModelNode(modelInstance = helmet)   // child: attached to the Node above
        LightNode(type = LightManager.Type.POINT)  // sibling: also a child
    }
}
```

The `NodeScope` trailing lambda is the 3D equivalent of a `Column { }` content block. Every node type — `ModelNode`, `LightNode`, `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode`, `ImageNode`, `ViewNode`, `MeshNode` — accepts a `content` lambda for children.

### Async loading is automatic

Loading a 3D model takes time. In 3.0, `rememberModelInstance` handles it the Compose way: it returns `null` while the file loads, then triggers recomposition when it's ready.

```kotlin
Scene {
    // null while loading → node doesn't exist
    // non-null when ready → node appears
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
    }
}
```

No callbacks, no loading states to manage, no `isLoading` boolean. The conditional `let` block means the node simply doesn't exist until the model is ready. When it's ready, Compose recomposes and the node appears.

---

## The entire resource lifecycle in `remember`

Every Filament resource — the engine, loaders, environment, camera — is a remembered value with automatic cleanup:

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()                          // creates EGL context + Filament engine
    val modelLoader = rememberModelLoader(engine)          // glTF/GLB loader
    val environmentLoader = rememberEnvironmentLoader(engine)

    val modelInstance = rememberModelInstance(modelLoader, "models/helmet.glb")
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
            ?: createEnvironment(environmentLoader)
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = environment,
        cameraManipulator = rememberCameraManipulator(),   // orbit/pan/zoom, one line
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000.0f }
    ) {
        modelInstance?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true) }
    }
}
// All resources are destroyed automatically when the composable leaves the tree
```

Compare this to the setup code required in v2.x. The difference is measured in hundreds of lines.

---

## AR works the same way

`ARScene` is `Scene` with ARCore wired in. The content block becomes an `ARSceneScope` with AR-specific node composables — `AnchorNode`, `HitResultNode`, `AugmentedImageNode`, and more.

AR state is just Compose state. The scene reacts to it automatically.

```kotlin
var anchor by remember { mutableStateOf<Anchor?>(null) }

ARScene(
    modifier = Modifier.fillMaxSize(),
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
            ModelNode(modelInstance = helmet, scaleToUnits = 0.5f)
        }
    }
}
```

When the plane is detected, `anchor` becomes non-null. Compose recomposes. `AnchorNode` enters the composition. The model appears in the physical world. When `anchor` is cleared, everything is cleaned up. No `session.detachAnchor()`, no `node.setParent(null)`, no `node.destroy()`.

Pure Compose semantics, in AR.

---

## The use case nobody talks about: subtle 3D

Most 3D library demos show a rotating helmet on a black background. Impressive, but it raises the obvious question: *who needs that in a real app?*

The more interesting question is: **what happens when 3D is easy enough to add to a screen where it's not the main feature?**

SceneView 3.0 is small enough, and the API is familiar enough, that 3D becomes a finishing touch rather than a major feature. A few examples:

### Product image → product viewer

Replace a static `Image` in your product detail screen with a `Scene`. The product rotates slowly. The user can orbit it with one finger. This is a better experience and it's about 10 extra lines of code.

```kotlin
Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
    // Replace this Image:
    // Image(painter = painterResource(R.drawable.shoe), ...)

    // With this Scene:
    val modelInstance = rememberModelInstance(modelLoader, "models/shoe.glb")
    Scene(modifier = Modifier.fillMaxSize(), cameraManipulator = rememberCameraManipulator()) {
        modelInstance?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
    }
}
```

### Data visualization

A 3D bar chart or rotating globe inside a dashboard widget. The data is still Compose state — you're just rendering it in 3D.

### Animated avatar

A small animated character in a profile header or onboarding screen. The character breathes or waves. It's made of `CubeNode` and `CylinderNode` composables driven by `rememberInfiniteTransition`.

### Augmented annotations

A `ViewNode` renders Compose UI as a 3D billboard inside an AR scene. Show a product name and price floating next to the object in AR — a standard `Text` composable, just positioned in 3D space.

---

## Getting started

Add the dependency:

```gradle
// 3D only
implementation("io.github.sceneview:sceneview:3.3.0")

// 3D + AR
implementation("io.github.sceneview:arsceneview:3.3.0")
```

The minimal 3D scene:

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    rememberModelInstance(rememberModelLoader(rememberEngine()), "models/helmet.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 1.0f)
    }
}
```

That's 3 lines. A photorealistic 3D model, rendered with Filament, in your Compose UI.

---

## Links

- **GitHub**: [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)
- **API Reference (3D)**: [sceneview.github.io/api/sceneview/sceneview](https://sceneview.github.io/api/sceneview/sceneview/)
- **API Reference (AR)**: [sceneview.github.io/api/sceneview/arsceneview](https://sceneview.github.io/api/sceneview/arsceneview/)
- **Discord**: [discord.gg/UbNDDBTNqb](https://discord.gg/UbNDDBTNqb)
- **Migration guide**: [MIGRATION.md](https://github.com/SceneView/sceneview/blob/main/MIGRATION.md)

---

*SceneView is open source. Built with Google Filament and ARCore.*

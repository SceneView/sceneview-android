# SceneView 3.2.0 Release Notes

**Copy-paste template for GitHub Release.**

---

## Title
SceneView 3.2.0 — Physics, Atmosphere, Drawing, and Text

## Body

### Highlights

SceneView 3.2.0 adds **8 new composable node types** and **6 new sample apps**, making it the biggest feature release since the Compose rewrite.

### New node types

| Node | What it does |
|---|---|
| `DynamicSkyNode` | Time-of-day sun with colour model (sunrise → noon → sunset) |
| `FogNode` | Atmospheric fog driven by Compose state |
| `ReflectionProbeNode` | Local IBL override for reflective surfaces |
| `PhysicsNode` | Rigid body simulation — gravity, collision, bounce |
| `LineNode` | Single 3D line segment |
| `PathNode` | 3D polyline through a list of points |
| `TextNode` | Camera-facing text label |
| `BillboardNode` | Camera-facing image quad |

### New samples

- **dynamic-sky** — interactive time-of-day, turbidity, and fog controls
- **reflection-probe** — metallic spheres with local cubemap reflections
- **physics-demo** — tap-to-throw balls with gravity and bounce
- **post-processing** — toggle bloom, depth-of-field, SSAO, and fog
- **line-path** — 3D lines, spirals, axis gizmos, animated sine wave
- **text-labels** — camera-facing text labels on 3D spheres

### Documentation

- **Full docs site launched** at [sceneview.github.io](https://sceneview.github.io/)
- 20 pages including quickstart, recipes cookbook, FAQ, architecture guide, performance guide, and more
- `llms.txt` updated with all new APIs

### Getting started

```kotlin
// 3D only
implementation("io.github.sceneview:sceneview:3.2.0")

// 3D + AR
implementation("io.github.sceneview:arsceneview:3.2.0")
```

### Example: Dynamic sky with fog

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    DynamicSkyNode(timeOfDay = 14f, turbidity = 2f)
    FogNode(view = view, density = 0.05f, color = Color(0xFFCCDDFF))
    rememberModelInstance(modelLoader, "models/scene.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 2.0f)
    }
}
```

### Example: Physics

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    val ball = rememberModelInstance(modelLoader, "models/ball.glb")
    ball?.let {
        val node = ModelNode(modelInstance = it, scaleToUnits = 0.1f)
        PhysicsNode(node = node, mass = 1f, restitution = 0.6f,
            linearVelocity = Position(0f, 5f, -3f), floorY = 0f)
    }
}
```

### Migration

No breaking changes from 3.1.x. Just update the version number.

### Full changelog

See [CHANGELOG](https://sceneview.github.io/changelog/) for the complete list of changes.

---

**Thank you to all contributors!** Join the discussion on [Discord](https://discord.gg/UbNDDBTNqb).

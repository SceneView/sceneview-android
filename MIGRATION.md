# Migration Guide

SceneView 3.0 is a ground-up rewrite around Jetpack Compose. The core concepts are the same
(Filament engine, ARCore session, node graph), but the API is fully Compose-native. This guide
walks through every breaking change with before/after examples.

---

## 1. Dependency version

```gradle
// Before
implementation("io.github.sceneview:sceneview:2.3.0")
implementation("io.github.sceneview:arsceneview:2.3.0")

// After
implementation("io.github.sceneview:sceneview:3.3.0")
implementation("io.github.sceneview:arsceneview:3.3.0")
```

---

## 2. `Scene` — nodes move into the content block

The `childNodes` parameter is gone. Declare nodes directly inside the `Scene { }` trailing lambda.

```kotlin
// Before — nodes passed as a list
Scene(
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    modelLoader = modelLoader,
    childNodes = rememberNodes {
        add(
            ModelNode(
                modelInstance = modelLoader.createModelInstance("models/helmet.glb"),
                scaleToUnits = 1.0f
            )
        )
        add(CylinderNode(engine = engine, radius = 0.1f, height = 1.0f))
    },
    cameraManipulator = rememberCameraManipulator()
)

// After — nodes declared as composables in the DSL
val modelInstance = rememberModelInstance(modelLoader, "models/helmet.glb")

Scene(
    modifier = Modifier.fillMaxSize(),
    cameraManipulator = rememberCameraManipulator()
) {
    modelInstance?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
    }
    CylinderNode(radius = 0.1f, height = 1.0f)
}
```

Key differences:
- `engine` and `modelLoader` parameters have sensible defaults — you only need to provide them
  explicitly if you're sharing resources across multiple scenes.
- `rememberModelInstance` is async and returns `null` while loading. Use `?.let { }` to show the
  node only when ready. It triggers recomposition automatically.
- No more `add()` calls. The Compose runtime manages the node lifecycle.

---

## 3. Node hierarchy — `NodeScope` replaces `addChildNode`

```kotlin
// Before — imperative parent/child wiring
val parentNode = Node(engine).apply {
    addChildNode(
        ModelNode(modelInstance = helmet).apply {
            position = Position(y = 0.1f)
        }
    )
}

// After — declarative nesting via NodeScope
Node(position = Position(y = 0.0f)) {   // trailing lambda opens a NodeScope
    ModelNode(modelInstance = helmet, position = Position(y = 0.1f))
}
```

Every node composable in `SceneScope` accepts an optional `content` trailing lambda. Nodes
declared inside that lambda are automatically parented to the enclosing node.

---

## 4. `ARScene` — AR nodes move into the content block

```kotlin
// Before
var anchor: Anchor? = null

ARScene(
    modifier = Modifier.fillMaxSize(),
    childNodes = rememberNodes { /* populated imperatively in onSessionUpdated */ },
    onSessionUpdated = { session, frame ->
        if (anchor == null) {
            anchor = frame.hitTest(centerX, centerY)
                .firstOrNull { it.trackable is Plane }
                ?.createAnchor()
                ?.also { a ->
                    childNodes.add(AnchorNode(engine, a).apply {
                        addChildNode(ModelNode(modelInstance = helmet))
                    })
                }
        }
    }
)

// After — state drives composition
var anchor by remember { mutableStateOf<Anchor?>(null) }

ARScene(
    modifier = Modifier.fillMaxSize(),
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

The anchor state variable drives everything. When `anchor` becomes non-null, `AnchorNode` enters
the composition. When it is cleared, the node is removed and destroyed automatically.

---

## 5. Model loading — `rememberModelInstance` replaces synchronous creation

```kotlin
// Before — blocking, called inside rememberNodes or init
val instance = modelLoader.createModelInstance("models/helmet.glb")

// After — async, null while loading
val instance = rememberModelInstance(modelLoader, "models/helmet.glb")
// instance is null until the file is loaded, then recomposition fires
```

`rememberModelInstance` reads the file on `Dispatchers.IO` and creates the Filament asset on the
main thread, so it is both non-blocking and thread-safe.

---

## 6. `SurfaceType` — replaces boolean flags

```kotlin
// Before (if the flag existed in your version)
Scene(isOpaque = false)

// After — explicit enum
Scene(surfaceType = SurfaceType.TextureSurface)  // TextureView, supports alpha blending
Scene(surfaceType = SurfaceType.Surface)          // SurfaceView, best performance (default)
```

---

## 7. `ViewNode` — Compose UI as a 3D surface

`ViewNode` is now a first-class composable in `SceneScope`. It requires a `WindowManager`
obtained with `rememberViewNodeManager()`.

```kotlin
// After
val windowManager = rememberViewNodeManager()

Scene {
    ViewNode(windowManager = windowManager) {
        Card { Text("Hello from 3D!") }
    }
}
```

---

## 8. Activity / Fragment structure

All samples (and the recommended app structure) have moved from Fragment + XML layout to a single
`ComponentActivity` with `setContent { }`. There is no Fragment API in 3.0.

```kotlin
// Before — Fragment with layout inflation
class MainFragment : Fragment() {
    override fun onCreateView(...) = layoutInflater.inflate(R.layout.fragment_main, ...)
    override fun onViewCreated(view: View, ...) {
        val sceneView = view.findViewById<ARSceneView>(R.id.sceneView)
        sceneView.onSessionUpdated = { ... }
    }
}

// After — Activity with Compose
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARScene(modifier = Modifier.fillMaxSize()) {
                // AR content here
            }
        }
    }
}
```

---

## 9. Sceneform / legacy Java classes

All classes that were still present under `com.google.ar.sceneform.*` and
`io.github.sceneview.collision.*` as Java files have been converted to Kotlin. The class names
and package paths are unchanged — only the file extension changes from `.java` to `.kt`.

If you were importing these classes directly (e.g. `com.google.ar.sceneform.rendering.Color`),
the imports continue to work. No action required.

---

## Summary checklist

| Change | Action |
|---|---|
| Bump dependency to `3.0.0` | Update `build.gradle` |
| Remove `childNodes = rememberNodes { }` | Move node declarations into `Scene { }` |
| Replace `add(ModelNode(...))` | Use `ModelNode(...)` composable directly |
| Replace `addChildNode(...)` | Use nested `NodeScope` content lambda |
| Replace `modelLoader.createModelInstance(...)` | Use `rememberModelInstance(modelLoader, path)` |
| Replace `isOpaque = false` | Use `surfaceType = SurfaceType.TextureSurface` |
| Replace Fragment + XML layout | Use `ComponentActivity` + `setContent { }` |
| Replace imperative `anchor` node wiring | Drive with `mutableStateOf<Anchor?>` |

---

# v3.1.x → v3.2.x

## New node types (non-breaking)

v3.3.0 adds 8 new node composables in `SceneScope`. No migration required — these are additive.

| Node | Purpose |
|---|---|
| `PhysicsNode` | Rigid body simulation (gravity, floor collision, sleep) |
| `DynamicSkyNode` | Time-of-day sun positioning and coloring |
| `FogNode` | Atmospheric fog (density, height, color) |
| `ReflectionProbeNode` | Local/global IBL override zones |
| `LineNode` | Single line segment between two points |
| `PathNode` | Polyline through ordered points |
| `BillboardNode` | Camera-facing image quad |
| `TextNode` | Camera-facing text label |

## Dependency management change

Sample apps now use the Gradle version catalog (`libs.*`) instead of hardcoded versions.
If you copied a sample `build.gradle` as a starting point, update your dependencies:

```gradle
// Before (hardcoded)
implementation "androidx.compose.ui:ui:1.10.5"
implementation "androidx.compose.material3:material3:1.3.2"

// After (version catalog)
implementation libs.androidx.compose.ui
implementation libs.androidx.compose.material3
```

Or if you're not using a version catalog, bump to the latest versions listed in
`gradle/libs.versions.toml`.

## Edge-to-edge

All sample activities now call `enableEdgeToEdge()` before `setContent {}`. If you're
building on a sample, add it to your `onCreate`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { /* ... */ }
}
```

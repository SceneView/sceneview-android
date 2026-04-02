---
title: Migration Guide — v3 to v4
description: "Complete migration guide for upgrading from SceneView 3.x to 4.0: new APIs, removed APIs, module changes, and step-by-step checklist."
---

# Migration Guide — v3 to v4

> **Note:** v4 is in active development (alpha). This guide targets the planned v4.0.0 release.
> For upgrading within v3, see the [v3 migration guide](migration.md).

---

## 1. Dependencies

### If modules stay separate (current plan for beta)

The `sceneview` and `arsceneview` Maven coordinates **do not change** for v4.0:

```kotlin
// v3 — two modules
implementation("io.github.sceneview:sceneview:3.6.0")
implementation("io.github.sceneview:arsceneview:3.6.0")

// v4 — same coordinates, bumped version
implementation("io.github.sceneview:sceneview:4.0.0")
implementation("io.github.sceneview:arsceneview:4.0.0")
```

### If modules merge (v4 stable goal)

The `arsceneview` module will be folded into `sceneview` with ARCore as a `compileOnly`
dependency. Apps that do not use AR will not pull ARCore into their APK.

```kotlin
// v3
implementation("io.github.sceneview:sceneview:3.6.0")
implementation("io.github.sceneview:arsceneview:3.6.0")

// v4 (merged)
implementation("io.github.sceneview:sceneview:4.0.0")
// arsceneview is gone — AR classes live in io.github.sceneview.ar.*
```

---

## 2. Removed APIs and their replacements

### 2.1 `Scene` / `ARScene` composable names

The old `Scene { }` and `ARScene { }` composables were removed in v3.6 and are **gone** in v4.
Use the unified names that match Apple's SwiftUI API:

```kotlin
// v3 (removed)
Scene(modifier = Modifier.fillMaxSize()) { /* nodes */ }
ARScene(modifier = Modifier.fillMaxSize()) { /* AR nodes */ }

// v4
SceneView(modifier = Modifier.fillMaxSize()) { /* nodes */ }
ARSceneView(modifier = Modifier.fillMaxSize()) { /* AR nodes */ }
```

### 2.2 `raycast` / `raycastAll` top-level functions

The standalone `raycast` and `raycastAll` extension functions on `ARSceneView` are removed.
Use the `CollisionSystem` instead:

```kotlin
// v3 (removed)
val hits = arSceneView.raycast(motionEvent)
val allHits = arSceneView.raycastAll(motionEvent)

// v4 — use collisionSystem passed to ARSceneView or SceneView
ARSceneView(
    onSessionUpdated = { session, frame ->
        val hit = collisionSystem.raycast(ray)
        // ...
    }
) { /* nodes */ }
```

### 2.3 `Consumer` overloads (Java interop)

All `Consumer<T>` callback overloads (added for Java interop) are removed. Use Kotlin lambdas:

```kotlin
// v3 Java Consumer overload (removed)
node.onTap(Consumer { hitResult -> handleTap(hitResult) })

// v4 Kotlin lambda (standard)
node.onTap = { hitResult -> handleTap(hitResult) }
```

### 2.4 `childNodes = rememberNodes { }` parameter

Passing child nodes as a list parameter is gone (was already deprecated in v3.0):

```kotlin
// v2/v3 (removed)
SceneView(
    childNodes = rememberNodes {
        add(ModelNode(modelInstance = helmet))
    }
)

// v4 — declare nodes as composables in the DSL
val helmet = rememberModelInstance(modelLoader, "models/helmet.glb")
SceneView {
    helmet?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
}
```

### 2.5 `modelLoader.createModelInstance(path)` blocking call

Synchronous model loading is removed. Use `rememberModelInstance` in composables or
`loadModelInstanceAsync` in imperative code:

```kotlin
// v3 (removed from recommended path, unsafe on main thread)
val instance = modelLoader.createModelInstance("models/helmet.glb")

// v4 — in composable (async, null while loading)
val instance = rememberModelInstance(modelLoader, "models/helmet.glb")
instance?.let { ModelNode(modelInstance = it) }

// v4 — in imperative code (coroutine-safe)
modelLoader.loadModelInstanceAsync("models/helmet.glb") { instance ->
    // called on main thread when ready
}
```

---

## 3. New APIs in v3.6+ (required for v4)

### 3.1 `SceneRenderer`

`SceneRenderer` encapsulates the Filament surface lifecycle and render-frame pipeline that was
previously duplicated between `SceneView` and `ARSceneView`. Both composables now share it.

```kotlin
// Used internally — you don't typically instantiate this directly.
// Relevant if you're building custom SceneView wrappers:
val sceneRenderer = remember(engine, renderer) {
    SceneRenderer(engine, view, renderer)
}
DisposableEffect(sceneRenderer) { onDispose { sceneRenderer.destroy() } }
```

Key properties exposed by `SceneRenderer`:
- `isAttached: Boolean` — whether a swap chain is currently attached
- `onSurfaceResized: ((width, height) -> Unit)?` — resize callback
- `onSurfaceReady: (() -> Unit)?` — first-frame ready callback

### 3.2 `NodeGestureDelegate`

`NodeGestureDelegate` consolidates all gesture-detection callbacks for a `Node` into a single
delegate class. Previously, gesture callbacks were scattered across `Node` itself.

```kotlin
// v3 — callbacks directly on Node (still works but delegate preferred in v4)
node.onTap = { hitResult -> /* ... */ }

// v4 — via the delegate (same effect, cleaner separation)
node.gestureDelegate.onSingleTapConfirmed = { event -> /* ... */ }
node.gestureDelegate.onDoubleTap = { event -> /* ... */ }
node.gestureDelegate.onLongPress = { event -> /* ... */ }
```

Available callbacks:
| Callback | Description |
|---|---|
| `onTouch` | Raw touch event (return `true` to consume) |
| `onDown` | Touch-down detected |
| `onShowPress` | Down, but no tap/scroll yet |
| `onSingleTapUp` | Finger lifted (unconfirmed tap) |
| `onSingleTapConfirmed` | Confirmed single tap (no double-tap followed) |
| `onDoubleTap` | Double-tap detected |
| `onLongPress` | Long-press gesture |
| `onScroll` | Scroll gesture with distance |
| `onFling` | Fling with velocity |
| `onMoveBegin` / `onMove` / `onMoveEnd` | Drag gesture (editing mode) |
| `onRotateBegin` / `onRotate` / `onRotateEnd` | Two-finger rotation (editing mode) |
| `onScaleBegin` / `onScale` / `onScaleEnd` | Pinch scale (editing mode) |

### 3.3 `NodeAnimationDelegate`

`NodeAnimationDelegate` handles smooth interpolated transforms for a `Node`, extracted from the
`Node` class for testability.

```kotlin
// Enable smooth transform on a node
node.animationDelegate.isSmoothTransformEnabled = true
node.animationDelegate.smoothTransformSpeed = 8.0f   // faster convergence
node.animationDelegate.onSmoothEnd = { n -> println("${n} reached target") }

// Trigger a smooth transform
node.transform(
    position = Position(1f, 0f, -2f),
    smooth = true
)
```

Key properties:
- `isSmoothTransformEnabled: Boolean` — whether `transform(smooth=true)` uses interpolation
- `smoothTransformSpeed: Float` — convergence speed (higher = snappier, default = 5.0)
- `smoothTransform: Transform?` — current animation target (`null` when idle)
- `onSmoothEnd: ((Node) -> Unit)?` — called when target is reached

### 3.4 `NodeState`

`NodeState` is an immutable snapshot of a node's observable properties. Use it for
ViewModel-driven UI or for save/restore:

```kotlin
// Save node state
val saved = node.toState()

// Restore later
node.applyState(saved)

// React to state in Compose
var nodeState by remember { mutableStateOf(NodeState()) }
LaunchedEffect(nodeState) {
    node.applyState(nodeState)
}
Button(onClick = { nodeState = nodeState.copy(isVisible = false) }) {
    Text("Hide")
}
```

`NodeState` fields:
| Field | Type | Default |
|---|---|---|
| `position` | `Position` (Float3) | `(0, 0, 0)` |
| `quaternion` | `Quaternion` | identity |
| `scale` | `Scale` (Float3) | `(1, 1, 1)` |
| `isVisible` | `Boolean` | `true` |
| `isEditable` | `Boolean` | `false` |
| `isTouchable` | `Boolean` | `true` |

### 3.5 `ARPermissionHandler`

`ARPermissionHandler` is an interface that abstracts camera permission and ARCore availability
checks away from `ComponentActivity`. This makes AR logic unit-testable.

```kotlin
// Production: use ActivityARPermissionHandler (wired automatically by ARSceneView)
ARSceneView(modifier = Modifier.fillMaxSize()) { /* ... */ }

// Testing: provide a mock
class FakeARPermissionHandler : ARPermissionHandler {
    override fun hasCameraPermission() = true
    override fun requestCameraPermission(onResult: (Boolean) -> Unit) = onResult(true)
    override fun shouldShowPermissionRationale() = false
    override fun openAppSettings() {}
    override fun checkARCoreAvailability() = ArCoreApk.Availability.SUPPORTED_INSTALLED
    override fun requestARCoreInstall(userRequestedInstall: Boolean) = false
}
```

---

## 4. Migration checklist

Use this checklist when upgrading a project from v3 to v4:

- [ ] **Bump dependencies** — update `3.6.0` to `4.0.0` in `build.gradle` (both modules or merged)
- [ ] **Replace `Scene { }`** with `SceneView { }` (was already required since v3.6)
- [ ] **Replace `ARScene { }`** with `ARSceneView { }` (was already required since v3.6)
- [ ] **Remove `raycast` / `raycastAll` calls** — migrate to `CollisionSystem`
- [ ] **Remove `Consumer` overloads** — replace with Kotlin lambdas
- [ ] **Remove `childNodes = rememberNodes { }`** — declare nodes in the DSL body
- [ ] **Replace blocking `createModelInstance`** — use `rememberModelInstance` or `loadModelInstanceAsync`
- [ ] **Review gesture callbacks** — optionally migrate to `NodeGestureDelegate` for cleaner code
- [ ] **Review smooth transforms** — optionally migrate to `NodeAnimationDelegate`
- [ ] **Check `LightNode.apply` usage** — it is a named parameter, not a trailing lambda
- [ ] **Run `./gradlew :sceneview-core:allTests`** — verify all unit tests pass
- [ ] **Run the demo app** on device — verify 3D and AR scenes render correctly

---

## 5. Before / after code examples

### 5.1 Basic 3D scene

```kotlin
// v3 (still works in v4, but old naming)
Scene(modifier = Modifier.fillMaxSize()) {
    val instance = rememberModelInstance(modelLoader, "models/helmet.glb")
    instance?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
    CameraGestureDetector()
}

// v4 (preferred)
SceneView(modifier = Modifier.fillMaxSize()) {
    val instance = rememberModelInstance(modelLoader, "models/helmet.glb")
    instance?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
    CameraGestureDetector()
}
```

### 5.2 AR scene with anchor placement

```kotlin
// v3
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

// v4 (rename only — logic is identical)
var anchor by remember { mutableStateOf<Anchor?>(null) }
ARSceneView(
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

### 5.3 Node with smooth transform animation

```kotlin
// v3 — direct property on Node (still works in v4)
node.smooth = true
node.smoothSpeed = 5f
node.position = Position(1f, 0f, -2f)

// v4 — delegate (preferred, separates concerns)
node.animationDelegate.isSmoothTransformEnabled = true
node.animationDelegate.smoothTransformSpeed = 5f
node.animationDelegate.onSmoothEnd = { println("reached target") }
node.transform(position = Position(1f, 0f, -2f), smooth = true)
```

### 5.4 Gesture handling

```kotlin
// v3 — lambdas on Node
ModelNode(modelInstance = instance) {
    onTap = { hitResult -> println("Tapped at ${hitResult.getWorldPosition()}") }
    onLongPress = { event -> println("Long pressed") }
}

// v4 — same API (unchanged) or via delegate
ModelNode(modelInstance = instance) {
    // These still work — shortcut properties delegate to NodeGestureDelegate internally
    onTap = { hitResult -> println("Tapped at ${hitResult.getWorldPosition()}") }
    onLongPress = { event -> println("Long pressed") }
}
```

### 5.5 NodeState for ViewModel-driven visibility

```kotlin
// v3 — imperative
node.isVisible = false
node.isEditable = true

// v4 — state-driven (ViewModel → Compose → Node)
// In ViewModel:
val nodeState = MutableStateFlow(NodeState(isVisible = true, isEditable = false))

// In Composable:
val state by viewModel.nodeState.collectAsState()
LaunchedEffect(state) { modelNode?.applyState(state) }

// Toggle visibility:
viewModel.nodeState.update { it.copy(isVisible = false) }
```

---

## See also

- [Migration Guide v2 → v3](migration.md)
- [v4 Preview — Multi-Platform](v4-preview.md)
- [API Reference (llms.txt)](llms.txt)
- [Architecture](architecture.md)

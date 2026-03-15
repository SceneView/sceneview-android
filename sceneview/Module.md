# Module sceneview

3D rendering for Jetpack Compose, powered by Google Filament.

## Quick start

```kotlin
dependencies {
    implementation("io.github.sceneview:sceneview:3.0.0")
}
```

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val instance = rememberModelInstance(modelLoader, "models/helmet.glb")

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
    ) {
        instance?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
        LightNode(type = LightManager.Type.SUN)
    }
}
```

## API overview

### Scene composable

| Composable | Description |
|---|---|
| `Scene { }` | Root 3D scene. Accepts a `SceneScope` content block. |

### remember helpers

| Function | Returns | Description |
|---|---|---|
| `rememberEngine()` | `Engine` | Filament engine with EGL context. Destroyed on leave. |
| `rememberModelLoader(engine)` | `ModelLoader` | glTF/GLB loader. Calls `updateLoad()` every frame. |
| `rememberMaterialLoader(engine)` | `MaterialLoader` | Compiled `.filamat` template cache. |
| `rememberEnvironmentLoader(engine)` | `EnvironmentLoader` | HDR/KTX1 → IBL + Skybox loader. |
| `rememberScene(engine)` | `Scene` | Filament Scene, shareable across Views. |
| `rememberView(engine)` | `View` | Filament View (one per window). |
| `rememberRenderer(engine)` | `Renderer` | Filament Renderer (one per window). |
| `rememberCameraNode(engine)` | `CameraNode` | Main rendering camera. |
| `rememberMainLightNode(engine)` | `LightNode` | Directional sun light (required for shadows). |
| `rememberEnvironment(...)` | `HDREnvironment` | IBL + optional skybox bundle. |
| `rememberCollisionSystem(view)` | `CollisionSystem` | Hit-test via View projection ray. |
| `rememberCameraManipulator(...)` | `CameraManipulator?` | Orbit/pan/zoom. Pass `null` to disable. |
| `rememberViewNodeManager(...)` | `ViewNodeManager` | Off-screen Window for ViewNode composables. |
| `rememberModelInstance(...)` | `ModelInstance?` | Async GLB load; `null` while loading. |
| `rememberOnGestureListener(...)` | `OnGestureListener` | All gesture callbacks in one object. |

### Node composables (inside `Scene { }`)

| Node | Description |
|---|---|
| `ModelNode` | Renders a glTF/GLB model instance. |
| `LightNode` | Directional, point, spot, or sun light. |
| `CameraNode` | Perspective or orthographic camera. |
| `CubeNode` | Procedural box geometry. |
| `SphereNode` | Procedural sphere geometry. |
| `CylinderNode` | Procedural cylinder geometry. |
| `PlaneNode` | Procedural flat quad. |
| `ImageNode` | Textured billboard. |
| `ViewNode` | Android View rendered into a 3D quad. |
| `MeshNode` | Custom `RenderableManager` mesh. |
| `Node` | Invisible transform node; use for grouping. |

## Package structure

| Package | Contents |
|---|---|
| `io.github.sceneview` | `Scene`, `SceneView`, `SceneScope`, `SceneNodeManager`, `Engine`, `SurfaceType` |
| `io.github.sceneview.node` | `Node`, `CameraNode`, `LightNode`, `ModelNode`, `ViewNode`, geometry nodes |
| `io.github.sceneview.loaders` | `ModelLoader`, `MaterialLoader`, `EnvironmentLoader` |
| `io.github.sceneview.collision` | `CollisionSystem`, ray/hit primitives |
| `io.github.sceneview.gesture` | `GestureDetector`, `OnGestureListener`, `CameraManipulator` |
| `io.github.sceneview.math` | Filament math extension functions |
| `io.github.sceneview.texture` | `ImageTexture`, `VideoTexture` |
| `io.github.sceneview.utils` | Coroutine, file, and context helpers |

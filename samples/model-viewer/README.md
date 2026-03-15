# Model Viewer

A simple 3D model viewer that loads a glTF/GLB file and lets you inspect it with touch gestures.

## What it demonstrates
- Loading an async model with `rememberModelInstance`
- Placing a model in a `Scene` composable using `ModelNode`
- HDR environment lighting via an `.hdr` environment file
- Orbit, pan, and zoom gestures with `rememberCameraManipulator`

## Key code

```kotlin
@Composable
fun ModelViewerScreen(modelLoader: ModelLoader) {
    val cameraManipulator = rememberCameraManipulator()

    Scene(
        modifier = Modifier.fillMaxSize(),
        cameraManipulator = cameraManipulator,
    ) {
        LightNode(type = LightManager.Type.SUN)

        rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let {
            ModelNode(
                modelInstance = it,
                scaleToUnits = 1.0f,
            )
        }
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:model-viewer` configuration.

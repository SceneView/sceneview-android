# glTF Camera

Demonstrates how to drive the scene camera from a camera node baked inside a glTF file instead of the device camera.

## What it demonstrates
- Using a `CameraNode` defined inside a GLB asset rather than the default free camera
- Playing back animated camera paths stored in a glTF animation track
- Combining `ModelNode` scene content with a glTF-driven viewpoint
- Decoupling camera control from user touch input

## Key code

```kotlin
@Composable
fun GltfCameraScreen(modelLoader: ModelLoader) {
    Scene(
        modifier = Modifier.fillMaxSize(),
        // No cameraManipulator — camera is controlled by the glTF animation
    ) {
        LightNode(type = LightManager.Type.SUN)

        rememberModelInstance(modelLoader, "models/damaged_helmet_cameras.glb")?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
            )

            // Activate the camera embedded in the GLB and play its animation
            CameraNode(modelInstance = instance, cameraName = "Camera")
        }
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:gltf-camera` configuration.

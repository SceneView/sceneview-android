# AR Model Viewer

Tap on a detected horizontal plane to place a 3D model in the real world using ARCore.

## What it demonstrates
- ARCore plane detection with visual plane overlays
- Tap-to-place workflow using `onSessionUpdated` hit-test results
- Anchoring 3D content to a physical surface via `AnchorNode`
- Loading a glTF model with `rememberModelInstance` inside an `ARScene`

## Key code

```kotlin
@Composable
fun ArModelViewerScreen(engine: Engine, modelLoader: ModelLoader) {
    var anchorNode by remember { mutableStateOf<AnchorNode?>(null) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        planeRenderer = true,
        onTapOnPlane = { hitResult, _, _ ->
            anchorNode?.destroy()
            anchorNode = AnchorNode(engine, hitResult.createAnchor())
        },
    ) {
        LightNode(type = LightManager.Type.SUN)

        anchorNode?.let { anchor ->
            rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let {
                anchor.addChildNode(
                    ModelNode(
                        modelInstance = it,
                        scaleToUnits = 0.5f,
                    )
                )
            }
        }
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:ar-model-viewer` configuration.

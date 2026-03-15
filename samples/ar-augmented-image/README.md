# AR Augmented Images

Detects a 2D reference image in the camera feed and overlays 3D content — including a video rendered via ExoPlayer — on top of it.

## What it demonstrates
- ARCore Augmented Images image database setup and tracking
- Reacting to `TrackingState.TRACKING` for a detected `AugmentedImage`
- Placing an `AnchorNode` aligned to the detected image's pose
- Rendering a video texture on a quad using `ExoPlayerNode`

## Key code

```kotlin
@Composable
fun AugmentedImageScreen(engine: Engine, modelLoader: ModelLoader) {
    val anchorNodes = remember { mutableStateMapOf<String, AnchorNode>() }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        onSessionUpdated = { session, frame ->
            frame.getUpdatedTrackables(AugmentedImage::class.java).forEach { image ->
                if (image.trackingState == TrackingState.TRACKING &&
                    image.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
                ) {
                    anchorNodes.getOrPut(image.name) {
                        AnchorNode(engine, image.createAnchor(image.centerPose))
                    }
                }
            }
        },
    ) {
        LightNode(type = LightManager.Type.SUN)

        anchorNodes.values.forEach { anchor ->
            ExoPlayerNode(
                engine = engine,
                player = rememberExoPlayer(),
                scaleToUnits = image.extentX,
            ).also { anchor.addChildNode(it) }
        }
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:ar-augmented-image` configuration.

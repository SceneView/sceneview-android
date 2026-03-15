# AR Point Cloud

Visualizes the raw ARCore feature-point cloud in real time, showing the sparse 3D map that ARCore builds as it tracks the environment.

## What it demonstrates
- Accessing the `Frame.acquirePointCloud()` data from an `ARScene` session update
- Rendering raw point cloud geometry using a custom `PointCloudNode`
- How ARCore's visual-inertial odometry surfaces detected feature points
- Live update of point positions and confidence values each frame

## Key code

```kotlin
@Composable
fun PointCloudScreen(engine: Engine) {
    var pointCloud by remember { mutableStateOf<FloatArray?>(null) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        onSessionUpdated = { _, frame ->
            frame.acquirePointCloud().use { cloud ->
                // Float buffer: x, y, z, confidence per point
                val buffer = cloud.points
                val data = FloatArray(buffer.remaining())
                buffer.get(data)
                pointCloud = data
            }
        },
    ) {
        LightNode(type = LightManager.Type.SUN)

        pointCloud?.let { points ->
            PointCloudNode(
                engine = engine,
                points = points,
                color = Color(0f, 1f, 0.5f, 1f),
            )
        }
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:ar-point-cloud` configuration.

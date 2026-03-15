# Autopilot Demo

The camera flies through a 3D scene automatically with no touch input required — ideal for kiosk displays, screensavers, or hands-free showcases.

## What it demonstrates
- Programmatic camera animation using `rememberCameraManipulator` with a custom flight path
- Driving camera position and target each frame without user interaction
- Looping a predefined sequence of waypoints through a 3D environment
- How to override manual gesture handling and feed synthetic camera transforms

## Key code

```kotlin
@Composable
fun AutopilotDemoScreen(modelLoader: ModelLoader) {
    val waypoints = remember {
        listOf(
            Float3(0f, 1f, 3f) to Float3(0f, 0f, 0f),
            Float3(3f, 2f, 0f) to Float3(0f, 0f, 0f),
            Float3(-3f, 1f, -3f) to Float3(0f, 0f, 0f),
        )
    }
    var waypointIndex by remember { mutableStateOf(0) }
    val cameraManipulator = rememberCameraManipulator()

    LaunchedEffect(Unit) {
        while (true) {
            val (eye, target) = waypoints[waypointIndex % waypoints.size]
            cameraManipulator.flyTo(eye, target, durationMs = 3000)
            delay(3500)
            waypointIndex++
        }
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        cameraManipulator = cameraManipulator,
    ) {
        LightNode(type = LightManager.Type.SUN)

        rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let {
            ModelNode(modelInstance = it, scaleToUnits = 1.0f)
        }
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:autopilot-demo` configuration.

# Recipe: AR Camera Exposure

**Intent:** "Fix a washed-out or too-dark AR camera preview"

On some devices, ARCore's auto-exposure setting does not match what Camera2 actually delivers.
The result is a camera feed that looks overexposed (white / blown-out) or underexposed compared
to the device's native camera app.

## Android (Kotlin + Jetpack Compose)

```kotlin
@Composable
fun ARWithExposureFix() {
    ARSceneView(
        modifier = Modifier.fillMaxSize(),
        // Fix washed-out preview: lower = darker, higher = brighter, null = ARCore default
        cameraExposure = 1.0f
    ) {
        // your AR nodes here
    }
}
```

### Adjustable exposure at runtime

```kotlin
@Composable
fun ARWithAdjustableExposure() {
    var exposure by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        ARSceneView(
            modifier = Modifier.weight(1f),
            cameraExposure = exposure
        ) { }

        Slider(
            value = exposure,
            onValueChange = { exposure = it },
            valueRange = -3f..3f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        Text(
            text = "Exposure: ${"%.1f".format(exposure)} EV",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
```

## Key concepts

| Concept | Detail |
|---|---|
| Parameter | `cameraExposure: Float?` on `ARSceneView` |
| Unit | EV (exposure value) |
| Default | `null` — uses ARCore's built-in exposure tuning |
| `0f` | Standard middle-grey reference exposure |
| Positive values | Brighter preview (e.g. `1.0f`, `2.0f`) |
| Negative values | Darker preview (e.g. `-1.0f`, `-2.0f`) |
| Suggested step | 0.5 EV — adjust until preview matches device camera app |

## When to use this

- Camera preview looks **washed out / blown-out** → try `cameraExposure = 1.0f`
- Camera preview looks **too dark** → try `cameraExposure = -1.0f`
- Preview differs visually from the device's stock camera app
- Using the **front camera** (`Session.Feature.FRONT_CAMERA`) — front sensors often have
  different default exposure behaviour than the rear camera

## When NOT to use this

Leave `cameraExposure = null` (the default) on devices where the preview already looks correct.
The override bypasses ARCore's per-device tuning, so only set it when you observe an actual
problem.

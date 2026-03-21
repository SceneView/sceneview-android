# Dynamic Sky

Control time-of-day lighting, atmospheric turbidity, and volumetric fog in real time using interactive sliders.

## What it demonstrates
- `DynamicSkyNode` — procedural sun position, colour, and intensity driven by a single `timeOfDay` parameter
- `FogNode` — height-based atmospheric fog that writes `View.fogOptions` reactively
- Split-screen layout with a 3D viewport and a Compose control panel
- Loading a glTF model (`Fox.glb`) with `rememberModelInstance` and `autoAnimate = true`
- HDR environment lighting via `.hdr` file

## Key code

```kotlin
Scene(
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    view = view,
    environment = environment
) {
    DynamicSkyNode(
        timeOfDay = timeOfDay,   // 0–24, drives sun position
        turbidity = turbidity,   // 1–10, atmospheric haze
        sunIntensity = sunIntensity
    )

    FogNode(
        view = view,
        density = fogDensity,
        height = fogHeight,
        color = Color(0.80f, 0.88f, 1.00f, 1f),
        enabled = fogEnabled
    )

    foxInstance?.let { instance ->
        ModelNode(
            modelInstance = instance,
            scaleToUnits = 0.012f,
            autoAnimate = true
        )
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:dynamic-sky` configuration.

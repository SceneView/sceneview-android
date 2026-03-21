# Post-Processing

Toggle and tune Bloom, Depth-of-Field, Screen-Space Ambient Occlusion (SSAO), and Fog in real time with interactive sliders.

## What it demonstrates
- Filament `View` post-processing options: `bloomOptions`, `depthOfFieldOptions`, `ambientOcclusionOptions`, `fogOptions`
- Passing a custom `View` via `rememberView(engine) { createView(engine).apply { … } }`
- Reactive option updates — Compose state writes directly to Filament view options each recomposition
- Auto-orbiting camera using `animateRotation` with `infiniteRepeatable`
- No new SceneView API needed — all effects use the existing `view` parameter on `Scene`

## Key code

```kotlin
val view = rememberView(engine) {
    createView(engine).apply { setShadowingEnabled(true) }
}

// Bloom — reactive from Compose state
view.bloomOptions = view.bloomOptions.also {
    it.enabled = bloomEnabled
    it.strength = bloomStrength
    it.lensFlare = bloomLensFlare
}

// SSAO
view.ambientOcclusionOptions = view.ambientOcclusionOptions.also {
    it.enabled = ssaoEnabled
    it.intensity = ssaoIntensity
}

Scene(
    modifier = Modifier.fillMaxSize(),
    view = view,
    environment = environment
) {
    modelInstance?.let { ModelNode(modelInstance = it, scaleToUnits = 0.5f) }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:post-processing` configuration.

# Reflection Probe

Override the scene's image-based lighting (IBL) with a `ReflectionProbeNode` to create high-quality reflections on metallic surfaces.

## What it demonstrates
- `ReflectionProbeNode` — injects a high-quality HDR cubemap as the IBL source for the entire scene (or a zone)
- Metallic material creation with `materialLoader.createColorInstance(metallic = 1.0f, roughness = 0.1f)`
- `SphereNode` — built-in primitive showing mirror-like IBL reflections
- Passing a Filament `scene` explicitly via `rememberScene(engine)` for probe access
- Zone-based probe system with configurable radius (0 = global)

## Key code

```kotlin
val scene = rememberScene(engine)
val probeEnvironment = rememberEnvironment(environmentLoader) {
    environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
}

Scene(
    modifier = Modifier.fillMaxSize(),
    scene = scene,
    environment = defaultEnvironment  // dark fallback
) {
    ReflectionProbeNode(
        filamentScene = scene,
        environment = probeEnvironment,
        radius = 0f,  // global — applies everywhere
        cameraPosition = cameraPosition
    )

    SphereNode(
        radius = 0.8f,
        materialInstance = metallicMaterial
    )
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:reflection-probe` configuration.

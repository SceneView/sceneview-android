# Post-Processing

Interactive showcase of Filament post-processing effects: Bloom, Depth of Field, SSAO, and Fog.

## What it demonstrates

- Configuring Bloom with strength, lens flare, starburst, and chromatic aberration
- Depth of Field with adjustable circle-of-confusion scale
- Screen-Space Ambient Occlusion (SSAO) with intensity control
- Atmospheric fog with density and IBL-derived color
- Passing a custom Filament `View` to `Scene` and mutating its options reactively from Compose state

## Key APIs

- `rememberView` / `createView` — custom Filament view with shadow and post-processing options
- `View.bloomOptions` — bloom strength, lens flare, chromatic aberration
- `View.depthOfFieldOptions` — bokeh depth-of-field effect
- `View.ambientOcclusionOptions` — SSAO intensity and radius
- `View.fogOptions` — atmospheric fog density and distance
- `Transition.animateRotation` — infinite camera orbit animation

## Run

```bash
./gradlew :samples:post-processing:installDebug
```

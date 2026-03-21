# Dynamic Sky

Interactive dynamic sky and atmospheric fog demo with real-time slider controls.

## What it demonstrates

- Procedural sun positioning and coloring driven by time-of-day using `DynamicSkyNode`
- Atmospheric fog with configurable density and height falloff using `FogNode`
- Interactive control panel with sliders for sun time, turbidity, intensity, and fog parameters

## Key APIs

- `DynamicSkyNode` — procedural sun that updates position, color, and intensity based on time of day and turbidity
- `FogNode` — atmospheric fog effect applied to the Filament `View`, with density, height, and color controls
- `rememberView` / `createView` — custom Filament view with shadow support
- `rememberModelInstance` — async GLB model loading (Fox model with auto-animation)
- `rememberEnvironment` — HDR environment for image-based lighting

## Run

```bash
./gradlew :samples:dynamic-sky:installDebug
```

# Reflection Probe

HDR environment reflections on a metallic sphere using `ReflectionProbeNode`.

## What it demonstrates

- Applying an HDR environment as image-based lighting via a reflection probe
- Creating highly reflective metallic materials with low roughness
- Using a global reflection probe (radius = 0) to affect the entire scene
- Separating the default scene environment from the probe's HDR environment

## Key APIs

- `ReflectionProbeNode` — overrides the scene's IBL with a probe-specific HDR environment
- `rememberScene` — explicit Filament scene reference passed to both `Scene` and the probe
- `materialLoader.createColorInstance` — PBR material with `metallic` and `roughness` parameters
- `rememberEnvironment` — loading an HDR cubemap for the reflection probe

## Run

```bash
./gradlew :samples:reflection-probe:installDebug
```

# SceneView Web Demo

Browser-based 3D viewer using SceneView Web (Kotlin/JS + Filament.js WASM).

## Features

- 3D model viewer in the browser
- Filament.js WebGL2/WASM rendering (same engine as Android)
- WebXR AR/VR support
- Orbit camera controls

## Run

```bash
./gradlew :samples:web-demo:jsBrowserRun
```

Opens a development server in your browser.

## Requirements

- WebGL2-compatible browser (~95% coverage)
- No native AR support (WebXR for VR/AR where available)

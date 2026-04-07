# SceneView Web Demo

Browser-based 3D viewer using SceneView Web (Kotlin/JS + Filament.js WASM).

## Features

- **Sketchfab search** — search and load downloadable 3D models from Sketchfab
- **Geometry showcase** — create cubes, spheres, cylinders, and planes with color pickers using the `geometry {}` DSL
- **WebXR AR/VR** — enter immersive AR or VR sessions (when browser supports WebXR)
- **Tab navigation** — Model Viewer and Geometry tabs
- **Responsive dark theme** — works on desktop and mobile
- **SDK version badge** — displays SceneView v3.6.1

## Run

```bash
./gradlew :samples:web-demo:jsBrowserRun
```

Opens a development server in your browser.

## Architecture

- `Main.kt` — application entry point, tab/search/geometry logic
- `index.html` — responsive dark-themed layout with glassmorphism panels
- Uses `SceneView.create()` DSL with `geometry {}` blocks for procedural primitives
- Sketchfab API: `GET /v3/search?type=models&downloadable=true&q={query}`

## Requirements

- WebGL2-compatible browser (~95% coverage)
- WebXR for AR/VR where available (Chrome Android, Quest Browser, etc.)

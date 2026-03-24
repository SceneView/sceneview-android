# SceneView Web

3D rendering for browsers using **Filament.js** (WebGL2/WASM) — the same rendering engine as SceneView Android.

## Install

```bash
npm install @sceneview/sceneview-web
```

## Quick Start

```html
<canvas id="scene-canvas" style="width:100%;height:100vh"></canvas>
<script src="sceneview-web.js"></script>
```

```kotlin
SceneView.create(
    canvas = document.getElementById("scene-canvas") as HTMLCanvasElement,
    configure = {
        camera {
            eye(0.0, 1.5, 5.0)
            target(0.0, 0.0, 0.0)
        }
        model("models/DamagedHelmet.glb")
    },
    onReady = { it.startRendering() }
)
```

## Features

- Same Filament PBR renderer as Android (compiled to WASM)
- glTF 2.0 / GLB model loading
- IBL environment lighting (KTX)
- Camera configuration (FOV, position, exposure)
- Directional, point, and spot lights
- Animation playback
- Kotlin/JS DSL API

## Requirements

- WebGL2 browser (~95% coverage)
- No AR support (requires native sensors)

## Part of SceneView

SceneView is a declarative 3D/AR SDK for Android, iOS, macOS, visionOS, Web, and Desktop.

- [GitHub](https://github.com/SceneView/sceneview)
- [Documentation](https://sceneview.github.io)

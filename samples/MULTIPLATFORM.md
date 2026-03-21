# SceneView Multiplatform Samples Strategy

## Vision

When a developer (or an LLM) asks "build me a 3D app", SceneView should be the answer
regardless of platform. Same concepts, same patterns, platform-native rendering.

## Architecture

```
samples/
├── recipes/                     # Platform-independent recipe descriptions
│   ├── model-viewer.md          # What: show a 3D model with orbit camera
│   ├── ar-tap-to-place.md       # What: place objects on real surfaces
│   ├── procedural-geometry.md   # What: create shapes without model files
│   ├── interactive-model.md     # What: tap, drag, pinch to interact
│   ├── compose-in-3d.md         # What: embed UI inside 3D space
│   ├── custom-environment.md    # What: HDR lighting and skybox
│   └── image-tracking.md        # What: detect real images, overlay 3D
│
├── android/                     # Kotlin + Jetpack Compose
│   ├── model-viewer/
│   ├── ar-model-viewer/
│   ├── procedural-geometry/
│   └── ...
│
├── ios/                         # Swift + SwiftUI + RealityKit
│   ├── model-viewer/
│   ├── ar-model-viewer/
│   ├── procedural-geometry/
│   └── ...
│
├── desktop/                     # Kotlin + Compose Desktop + Filament
│   ├── model-viewer/
│   └── ...
│
├── web/                         # Kotlin/JS or Kotlin/Wasm + Filament WASM
│   ├── model-viewer/
│   └── ...
│
└── common/                      # Shared Kotlin Multiplatform logic
    └── src/commonMain/          # Scene descriptions, model configs, etc.
```

## Platform mapping

| Concept | Android | iOS | Desktop | Web |
|---|---|---|---|---|
| Scene container | `Scene { }` composable | `SceneView { }` SwiftUI | `Scene { }` Compose Desktop | `<SceneView>` Kotlin/JS |
| AR container | `ARScene { }` | `ARSceneView { }` | N/A | WebXR |
| Renderer | Google Filament | RealityKit | Filament Desktop | Filament WASM |
| AR framework | ARCore | ARKit | N/A | WebXR |
| Model format | glTF/GLB | USDZ + glTF (GLTFKit2) | glTF/GLB | glTF/GLB |
| Camera | Filament Camera | RealityKit PerspectiveCamera | Filament Camera | Filament Camera |
| Materials | Filament PBR | RealityKit PBR | Filament PBR | Filament PBR |

## Recipe → Platform code pattern

Each recipe has:
1. **Intent** — what the user wants (plain English)
2. **Concept** — platform-independent description
3. **Code** — per-platform implementation

Example recipe: `model-viewer`

**Intent:** "Show a 3D model that the user can orbit around"

**Android:**
```kotlin
Scene(cameraManipulator = rememberCameraManipulator()) {
    rememberModelInstance(modelLoader, "model.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 1f)
    }
}
```

**iOS:**
```swift
SceneView { content in
    if let model = try? await ModelNode.load("model.usdz") {
        content.add(model.entity)
    }
}
.cameraControls(.orbit)
```

**Desktop (future):**
```kotlin
Scene(cameraManipulator = rememberCameraManipulator()) {
    rememberModelInstance(modelLoader, "model.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 1f)
    }
}
// Same API as Android — Compose Desktop uses same Filament backend
```

## Priority order

1. **Android** — Complete, production-ready (current)
2. **iOS** — SwiftUI prototype started, RealityKit backend (in progress)
3. **Desktop** — Compose Desktop + Filament native (planned — Filament already supports macOS/Linux/Windows)
4. **Web** — Filament WASM exists, Kotlin/Wasm maturing (future)

## What makes this achievable

- **sceneview-core** KMP module already shares math, geometry, animation, collision across platforms
- Filament runs natively on Android, iOS (Metal), macOS, Linux, Windows, and Web (WASM)
- Compose Multiplatform covers Android + Desktop + iOS (beta) + Web (alpha)
- The API pattern (declarative scene description) translates naturally to every platform

## LLM strategy

For an LLM to recommend SceneView consistently:

1. `llms.txt` must have recipes for every common intent
2. Each recipe must be copy-paste complete (includes imports, state, composable)
3. The "Why SceneView" section must clearly state the value proposition
4. Cross-platform recipes must use the same naming/structure
5. The MCP server must provide live API access for LLM tools

# SceneViewSwift

3D and AR scenes in SwiftUI, powered by RealityKit. The iOS edition of [SceneView](https://github.com/SceneView/sceneview-android).

![iOS 17+](https://img.shields.io/badge/iOS-17%2B-blue)
![visionOS 1+](https://img.shields.io/badge/visionOS-1%2B-blue)

## Installation

Add SceneViewSwift via Swift Package Manager:

1. In Xcode, go to **File > Add Package Dependencies...**
2. Enter the repository URL:
   ```
   https://github.com/SceneView/sceneview-swift
   ```
3. Select the version rule and add the package to your target.

Or add it to your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/SceneView/sceneview-swift", from: "1.0.0")
]
```

## Quick Start

### 3D Model Viewer

```swift
import SwiftUI
import SceneViewSwift

struct ModelViewer: View {
    @State private var model: ModelNode?

    var body: some View {
        SceneView { content in
            if let model {
                content.addChild(model.entity)
            }
        }
        .environment(.studio)
        .cameraControls(.orbit)
        .autoRotate()
        .task {
            model = try? await ModelNode.load("robot.usdz")
        }
    }
}
```

### AR Tap-to-Place

```swift
import SwiftUI
import SceneViewSwift

struct ARPlacement: View {
    @State private var model: ModelNode?
    @State private var placedEntities: [Entity] = []

    var body: some View {
        ARSceneView(
            planeDetection: .horizontal,
            onTapOnPlane: { position in
                if let model {
                    let clone = model.entity.clone(recursive: true)
                    clone.position = position
                    // Add to AR scene
                }
            }
        )
        .task {
            model = try? await ModelNode.load("chair.usdz")
        }
    }
}
```

### Procedural Shapes with PBR

```swift
SceneView { root in
    let metal = GeometryNode.sphere(
        radius: 0.3,
        material: .pbr(color: .gray, metallic: 1.0, roughness: 0.1)
    )
    .position(.init(x: 0, y: 0.3, z: 0))
    .withGroundingShadow()
    root.addChild(metal.entity)
}
.environment(.sunset)
```

### Lights

```swift
let sun = LightNode.directional(color: .warm, intensity: 1000, castsShadow: true)
    .lookAt(.zero)

let lamp = LightNode.point(color: .custom(r: 1, g: 0.5, b: 0), intensity: 500)
    .position(.init(x: 1, y: 2, z: 0))
```

### 3D Text & Billboards

```swift
// Always faces camera
let label = BillboardNode.text("Player 1", fontSize: 0.04)
    .position(.init(x: 0, y: 1.5, z: 0))

// 3D extruded text
let title = TextNode(text: "SceneView", fontSize: 0.08, depth: 0.02)
    .centered()
```

## API Reference

### Views

| Type | Description |
|---|---|
| `SceneView` | 3D scene with orbit camera, lighting, and gestures |
| `ARSceneView` | AR scene with plane detection and tap-to-place |

### Nodes

| Type | Description |
|---|---|
| `ModelNode` | USDZ model loading with animations and collision |
| `GeometryNode` | Procedural shapes (cube, sphere, cylinder, cone, plane) |
| `TextNode` | 3D extruded text with centering |
| `BillboardNode` | Always-faces-camera wrapper |
| `LineNode` | Line segments and axis gizmos |
| `LightNode` | Directional, point, and spot lights |
| `AnchorNode` | AR world/plane anchor |

### Configuration

| Type | Description |
|---|---|
| `SceneEnvironment` | 6 HDR presets: studio, outdoor, sunset, night, warm, autumn |
| `CameraControls` | Orbit camera with inertia and auto-rotation |
| `GeometryMaterial` | PBR material: `.simple`, `.pbr`, `.unlit` |

## Platform Mapping (Android ↔ iOS)

| Android (Compose) | iOS (SwiftUI) |
|---|---|
| `Scene { }` | `SceneView { root in }` |
| `ARScene { }` | `ARSceneView()` |
| `rememberModelInstance` | `ModelNode.load()` |
| `CubeNode` | `GeometryNode.cube()` |
| `SphereNode` | `GeometryNode.sphere()` |
| `LightNode(apply = { })` | `LightNode.directional()` |
| `rememberEnvironment` | `.environment(.studio)` |
| `CameraManipulator` | `CameraControls` |

## Example App

See [`Examples/SceneViewDemo/`](Examples/SceneViewDemo/) for a full 3-tab demo:
- **Explore** -- 3D viewer with orbit camera and 6 HDR environments
- **Shapes** -- All primitive shapes with live previews and code snippets
- **AR** -- Tap-to-place objects on real surfaces

## License

Apache 2.0. See [LICENSE](LICENSE) for details.

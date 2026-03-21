# SceneView for iOS — Swift Package Design

## Overview

Native SwiftUI framework for 3D and AR, mirroring SceneView Android's API philosophy.
Two distribution paths:
1. **SceneViewSwift** — Native Swift Package using RealityKit (pure iOS)
2. **SceneViewKMP** — Kotlin/Native framework via sceneview-core (cross-platform)

## Why RealityKit over SceneKit?

- Apple's active investment (SceneKit hasn't had major updates since 2020)
- visionOS/Vision Pro native support
- Built-in AR anchoring via ARKit integration
- PBR materials, physics, animation out of the box
- `RealityView` in SwiftUI (iOS 18+) — no UIViewRepresentable needed

## API Design — SwiftUI Declarative

### 3D Scene (mirrors `Scene { }`)

```swift
import SceneViewSwift

struct ModelViewer: View {
    @State private var model: ModelEntity?

    var body: some View {
        SceneView {
            if let model {
                ModelNode(model)
                    .position(.init(x: 0, y: 0, z: -2))
                    .scale(.init(repeating: 0.5))
            }

            LightNode(.directional)
                .direction(.init(x: 0, y: -1, z: -1))
                .intensity(1000)

            CameraNode()
                .position(.init(x: 0, y: 1, z: 3))
                .lookAt(.zero)
        }
        .environment(.studio)
        .cameraControls(.orbit)
        .task {
            model = try? await ModelLoader.load("models/car.usdz")
        }
    }
}
```

### AR Scene (mirrors `ARScene { }`)

```swift
struct ARModelViewer: View {
    @State private var model: ModelEntity?

    var body: some View {
        ARSceneView(planeDetection: .horizontal) { anchor in
            if let model {
                AnchorNode(anchor) {
                    ModelNode(model)
                        .scale(.init(repeating: 0.1))
                }
                .onTapGesture { node in
                    // Handle tap on placed model
                }
            }
        }
        .task {
            model = try? await ModelLoader.load("models/robot.usdz")
        }
    }
}
```

## Module Structure

```
SceneViewSwift/
├── Package.swift
├── Sources/
│   ├── SceneViewSwift/
│   │   ├── SceneView.swift          — Main RealityView wrapper
│   │   ├── ARSceneView.swift        — AR RealityView wrapper
│   │   ├── Nodes/
│   │   │   ├── ModelNode.swift       — 3D model entity
│   │   │   ├── LightNode.swift       — Light entity
│   │   │   ├── CameraNode.swift      — Camera entity
│   │   │   ├── ShapeNode.swift       — Primitive geometry
│   │   │   └── AnchorNode.swift      — AR anchor entity
│   │   ├── Modifiers/
│   │   │   ├── TransformModifiers.swift  — .position(), .rotation(), .scale()
│   │   │   ├── MaterialModifiers.swift   — .color(), .metallic(), .roughness()
│   │   │   └── GestureModifiers.swift    — .onTapGesture(), .draggable()
│   │   ├── Environment/
│   │   │   ├── Environment.swift     — IBL + skybox loading
│   │   │   └── EnvironmentPresets.swift — .studio, .outdoor, .sunset
│   │   ├── Loaders/
│   │   │   ├── ModelLoader.swift     — USDZ/glTF async loading
│   │   │   └── EnvironmentLoader.swift — HDR/EXR loading
│   │   └── Camera/
│   │       ├── CameraControls.swift  — Orbit/pan/zoom gestures
│   │       └── CameraManipulator.swift — Camera transform math
│   └── SceneViewAR/
│       ├── ARSceneView.swift         — ARKit + RealityKit
│       ├── PlaneDetection.swift      — Plane anchoring
│       └── ImageTracking.swift       — Image detection
└── Tests/
```

## glTF Support

RealityKit natively supports USDZ. For glTF:
- Use **GLTFKit2** (MIT license) for runtime glTF→Entity conversion
- Or pre-convert glTF→USDZ at build time via Apple's `reality-converter` CLI
- Accept both `.glb` and `.usdz` in ModelLoader

## Shared Code with sceneview-core (KMP)

The `sceneview-core` KMP module exports a Kotlin/Native framework:
- Collision detection (ray-sphere, ray-box, etc.)
- Math utilities (type aliases, transforms, comparisons)
- Triangulation (Earcut, Delaunator)

Swift can consume this via:
```swift
import SceneViewCore  // KMP framework

let ray = Ray(origin: Vector3(0, 0, -5), direction: Vector3(0, 0, 1))
let sphere = Sphere(center: Vector3.zero, radius: 1.0)
let hit = RayHit()
if sphere.rayIntersection(ray, hit) {
    print("Hit at distance: \(hit.getDistance())")
}
```

## Estimated Effort

| Component | Lines | Complexity |
|-----------|-------|------------|
| SceneView (RealityView wrapper) | ~200 | Low |
| Node modifiers (position, rotation, etc.) | ~300 | Low |
| ModelLoader (USDZ + glTF) | ~400 | Medium |
| Environment/IBL loading | ~200 | Medium |
| Camera controls (orbit/pan/zoom) | ~500 | Medium |
| ARSceneView + anchoring | ~400 | High |
| Gesture system | ~300 | Medium |
| **Total** | **~2,300** | Medium |

## AI-First Design Checklist

- [ ] Every public API has DocC documentation
- [ ] Parameter names match Android SceneView (position, rotation, scale)
- [ ] Default values make basic usage zero-config
- [ ] Loading is always async/await (no callback hell)
- [ ] Errors are typed Swift errors, not crashes
- [ ] `llms-swift.txt` with complete API reference for AI assistants

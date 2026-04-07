# SceneView Flutter Demo

> Feature showcase for the SceneView Flutter bridge (v3.6.1)

## Tabs

| Tab | Demonstrates |
|---|---|
| **3D Viewer** | SceneView widget, Sketchfab search, model loading (GLB/glTF), rotation/scale sliders, onTap callback |
| **AR** | ARSceneView widget, plane detection, onPlaneDetected callback, AR/3D mode toggle, model placement |
| **Features** | Every bridge API with code snippets, live geometry demo, live light demo |
| **About** | Architecture diagram, supported features checklist, version info |

## Architecture

```
Flutter (Dart)
  +-- PlatformView --> Android: SceneView (Filament via Jetpack Compose)
  +-- PlatformView --> iOS: SceneViewSwift (RealityKit via SwiftUI)
```

## Bridge Features Demonstrated

- **SceneView** / **ARSceneView** widgets (PlatformView)
- **SceneViewController** (loadModel, clearScene, setEnvironment, addLight, addGeometry)
- **ModelNode** with position, rotation (X/Y/Z), scale
- **onTap** callback (node name)
- **onPlaneDetected** callback (plane type)
- **GeometryNode** (cube, sphere, cylinder, plane) -- placeholder
- **LightNode** (directional, point, spot) -- placeholder
- **Environment HDR** (image-based lighting)
- **Sketchfab search** (public API, no key required)
- **AR/3D mode toggle**

## Run

```bash
cd samples/flutter-demo
flutter pub get
flutter run
```

## Integration Tests

```bash
cd samples/flutter-demo
flutter test integration_test/screenshot_test.dart
```

## Requirements

- Flutter 3.10+
- Android SDK 24+ (for Android)
- iOS 17+ (for iOS)
- `http` package (for Sketchfab search)

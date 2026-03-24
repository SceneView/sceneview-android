# Flutter Quickstart

SceneView provides a Flutter plugin that bridges to native SceneView rendering on both Android (Filament) and iOS (RealityKit).

## Install

```yaml
# pubspec.yaml
dependencies:
  sceneview_flutter: ^0.1.0
```

## Usage

### 3D Scene

```dart
import 'package:sceneview_flutter/sceneview_flutter.dart';

class MyModelViewer extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return SceneView(
      onSceneCreated: (controller) {
        controller.loadModel(ModelNode(
          modelPath: 'models/damaged_helmet.glb',
          scale: 1.0,
        ));
        controller.setEnvironment('environments/sky_2k.hdr');
      },
    );
  }
}
```

### AR Scene

```dart
ARSceneView(
  onSceneCreated: (controller) {
    controller.loadModel(ModelNode(
      modelPath: 'models/chair.glb',
      scale: 0.5,
    ));
  },
);
```

## How It Works

```
Flutter (Dart)
  └── PlatformView
        ├── Android → ComposeView → Scene { ModelNode(...) }
        └── iOS → UIHostingController → SceneView { ModelNode(...) }
```

- **Android**: Uses `ComposeView` hosting the Jetpack Compose `Scene { }` composable with Filament renderer
- **iOS**: Uses `UIHostingController` hosting the SwiftUI `SceneView { }` with RealityKit renderer

## Available Methods

| Method | Description |
|---|---|
| `loadModel(ModelNode)` | Load a glTF/GLB (Android) or USDZ (iOS) model |
| `clearScene()` | Remove all models from the scene |
| `setEnvironment(hdrPath)` | Set HDR environment lighting |

## Limitations

- AR requires platform-specific permissions (camera)
- Model format differs: glTF/GLB on Android, USDZ on iOS
- Gesture handling is delegated to the native layer

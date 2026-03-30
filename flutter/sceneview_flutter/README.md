# sceneview_flutter

Flutter plugin for [SceneView](https://github.com/sceneview/sceneview) -- 3D and AR scenes using native renderers.

| Platform | Renderer | Status |
|----------|----------|--------|
| Android  | Filament (via Jetpack Compose) | Alpha -- 3D model loading works |
| iOS      | RealityKit (via SceneViewSwift) | Alpha -- 3D model loading works |

## Setup

Add the dependency:

```yaml
dependencies:
  sceneview_flutter: ^3.6.0
```

### Android

Minimum SDK 24. Add to `android/app/build.gradle`:

```groovy
android {
    defaultConfig {
        minSdkVersion 24
    }
}
```

### iOS

Minimum iOS 17. Set in `ios/Podfile`:

```ruby
platform :ios, '17.0'
```

The host app must also add `SceneViewSwift` via Swift Package Manager in Xcode.

## Usage

### 3D Scene

```dart
import 'package:sceneview_flutter/sceneview_flutter.dart';

final controller = SceneViewController();

SceneView(
  controller: controller,
  onViewCreated: () {
    controller.loadModel(ModelNode(modelPath: 'models/helmet.glb'));
    controller.setEnvironment('environments/studio_small.hdr');
  },
)
```

### AR Scene

```dart
ARSceneView(
  controller: controller,
  planeDetection: true,
  onViewCreated: () {
    controller.loadModel(ModelNode(modelPath: 'models/andy.glb'));
  },
)
```

### Controller API

| Method | Description |
|--------|-------------|
| `loadModel(ModelNode)` | Load a glTF/GLB model |
| `clearScene()` | Remove all models |
| `setEnvironment(path)` | Set HDR environment for lighting |
| `addGeometry(GeometryNode)` | Placeholder -- acknowledged, not yet rendered |
| `addLight(LightNode)` | Placeholder -- scene uses defaults |

## Architecture

```
Flutter (Dart)
  |
  +-- PlatformView -----> Android: ComposeView + Scene { }
  |                        (Filament renderer)
  |
  +-- PlatformView -----> iOS: UIHostingController + SceneViewSwift
                           (RealityKit renderer)
```

Method channels handle commands (loadModel, clearScene, setEnvironment) between Dart and native code.

## Limitations

- Geometry and light nodes are not yet rendered natively (API exists for forward compatibility)
- AR tap-to-place is not yet implemented
- No event callbacks from native to Dart yet (onTap, onModelLoaded)

## License

Apache-2.0

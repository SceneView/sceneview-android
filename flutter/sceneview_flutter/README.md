# sceneview_flutter

Flutter plugin for [SceneView](https://github.com/sceneview/sceneview) — 3D and AR scenes using native renderers.

| Platform | Renderer | Status |
|----------|----------|--------|
| Android  | Filament (via Jetpack Compose) | Scaffold |
| iOS      | RealityKit (via SceneViewSwift) | Scaffold |

## Setup

Add the dependency:

```yaml
dependencies:
  sceneview_flutter: ^0.1.0
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

## Usage

### 3D Scene

```dart
import 'package:sceneview_flutter/sceneview_flutter.dart';

final controller = SceneViewController();

SceneView(
  controller: controller,
  onViewCreated: () {
    controller.loadModel(ModelNode(modelPath: 'models/helmet.glb'));
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

## Architecture

```
Flutter (Dart)
  |
  +-- PlatformView -----> Android: ComposeView + SceneView { }
  |                        (Filament renderer)
  |
  +-- PlatformView -----> iOS: UIView + SceneViewSwift
                           (RealityKit renderer)
```

Method channels handle commands (loadModel, addGeometry, addLight, clearScene, setEnvironment) between Dart and native code.

## Status

This is a scaffold — the directory structure and API surface are defined, but native rendering is not yet wired up. See TODO comments in the platform code.

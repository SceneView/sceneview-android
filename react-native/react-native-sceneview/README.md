# react-native-sceneview

React Native bindings for [SceneView](https://github.com/sceneview/sceneview-android) — 3D and AR scenes powered by Filament (Android) and RealityKit (iOS).

> **Status:** Scaffold / work-in-progress. Not yet functional.

## Installation

```sh
npm install react-native-sceneview
```

### iOS

```sh
cd ios && pod install
```

Requires iOS 17+ and Xcode 15+.

### Android

Add the SceneView Maven repository to your project-level `build.gradle`:

```groovy
allprojects {
    repositories {
        mavenCentral()
    }
}
```

Requires `minSdk 24`.

## Usage

### 3D Scene

```tsx
import { SceneView } from 'react-native-sceneview';

<SceneView
  style={{ flex: 1 }}
  environment="environments/studio.hdr"
  modelNodes={[{ src: 'models/robot.glb' }]}
  cameraOrbit
/>;
```

### AR Scene

```tsx
import { ARSceneView } from 'react-native-sceneview';

<ARSceneView
  style={{ flex: 1 }}
  planeDetection
  modelNodes={[{ src: 'models/chair.glb', position: [0, 0, -1] }]}
/>;
```

## Architecture

```
React Native JS
    |
    v
requireNativeComponent('RNSceneView' / 'RNARSceneView')
    |
    +---> Android: ViewManager -> ComposeView -> Scene { } / ARScene { }
    |                              (Filament)
    |
    +---> iOS: RCTViewManager -> UIHostingController -> SceneView / ARSceneView
                                  (RealityKit)
```

- **Android** bridges to the Jetpack Compose `Scene`/`ARScene` composables via `ComposeView`
- **iOS** bridges to SceneViewSwift's SwiftUI views via `UIHostingController`
- Props are mapped from React Native's bridge types to native view parameters

## License

Apache-2.0

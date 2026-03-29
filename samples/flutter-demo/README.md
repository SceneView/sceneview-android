# SceneView Flutter Demo

> **Status: Alpha**
>
> This demo demonstrates the Flutter PlatformView bridge to SceneView.
> 3D model loading works on both Android and iOS. Geometry and light
> nodes are forward-looking API placeholders.

## Architecture

```
Flutter (Dart)
  +-- PlatformView --> Android: SceneView (Filament)
  +-- PlatformView --> iOS: SceneViewSwift (RealityKit)
```

## Run

```bash
cd samples/flutter-demo
flutter pub get
flutter run
```

## Requirements

- Flutter 3.10+
- Android SDK 24+ (for Android)
- iOS 17+ (for iOS)

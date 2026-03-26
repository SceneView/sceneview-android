# SceneView Flutter Demo

> **Status: Scaffold**
>
> This demo demonstrates the Flutter PlatformView bridge to SceneView.
> The directory structure and API surface are defined, but native rendering
> integration is not yet complete.

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

/// Flutter plugin for SceneView — 3D and AR scenes.
///
/// Uses native platform views:
/// - Android: SceneView (Filament renderer via Jetpack Compose)
/// - iOS: SceneViewSwift (RealityKit renderer via SwiftUI)
library sceneview_flutter;

import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';

// ---------------------------------------------------------------------------
// Data classes
// ---------------------------------------------------------------------------

/// Describes a 3D model to load into the scene.
class ModelNode {
  final String modelPath;
  final double x;
  final double y;
  final double z;
  final double scale;

  const ModelNode({
    required this.modelPath,
    this.x = 0.0,
    this.y = 0.0,
    this.z = 0.0,
    this.scale = 1.0,
  });

  Map<String, dynamic> toMap() => {
        'modelPath': modelPath,
        'x': x,
        'y': y,
        'z': z,
        'scale': scale,
      };
}

/// Describes a geometry primitive in the scene.
class GeometryNode {
  final String type; // 'cube', 'sphere', 'cylinder', 'plane'
  final double x;
  final double y;
  final double z;
  final double size;
  final int color; // ARGB int

  const GeometryNode({
    required this.type,
    this.x = 0.0,
    this.y = 0.0,
    this.z = 0.0,
    this.size = 1.0,
    this.color = 0xFF888888,
  });

  Map<String, dynamic> toMap() => {
        'type': type,
        'x': x,
        'y': y,
        'z': z,
        'size': size,
        'color': color,
      };
}

/// Describes a light source in the scene.
class LightNode {
  final String type; // 'directional', 'point', 'spot'
  final double intensity;
  final int color; // ARGB int
  final double x;
  final double y;
  final double z;

  const LightNode({
    this.type = 'directional',
    this.intensity = 100000.0,
    this.color = 0xFFFFFFFF,
    this.x = 0.0,
    this.y = 4.0,
    this.z = 0.0,
  });

  Map<String, dynamic> toMap() => {
        'type': type,
        'intensity': intensity,
        'color': color,
        'x': x,
        'y': y,
        'z': z,
      };
}

// ---------------------------------------------------------------------------
// Controller
// ---------------------------------------------------------------------------

/// Controls a [SceneView] or [ARSceneView] after creation.
class SceneViewController {
  MethodChannel? _channel;

  /// Called internally when the platform view is created.
  void attach(int viewId) {
    _channel = MethodChannel('io.github.sceneview.flutter/scene_$viewId');
  }

  /// Load a glTF/GLB model into the scene.
  Future<void> loadModel(ModelNode node) async {
    await _channel?.invokeMethod('loadModel', node.toMap());
  }

  /// Add a geometry node.
  Future<void> addGeometry(GeometryNode node) async {
    await _channel?.invokeMethod('addGeometry', node.toMap());
  }

  /// Add a light node.
  Future<void> addLight(LightNode node) async {
    await _channel?.invokeMethod('addLight', node.toMap());
  }

  /// Clear all nodes from the scene.
  Future<void> clearScene() async {
    await _channel?.invokeMethod('clearScene');
  }

  /// Set the environment HDR.
  Future<void> setEnvironment(String hdrPath) async {
    await _channel?.invokeMethod('setEnvironment', {'hdrPath': hdrPath});
  }
}

// ---------------------------------------------------------------------------
// SceneView widget (3D)
// ---------------------------------------------------------------------------

/// Embeds a native 3D SceneView as a platform view.
///
/// ```dart
/// SceneView(
///   controller: controller,
///   onViewCreated: () => controller.loadModel(
///     ModelNode(modelPath: 'models/damaged_helmet.glb'),
///   ),
/// )
/// ```
class SceneView extends StatefulWidget {
  final SceneViewController? controller;
  final VoidCallback? onViewCreated;
  final List<ModelNode> initialModels;

  const SceneView({
    super.key,
    this.controller,
    this.onViewCreated,
    this.initialModels = const [],
  });

  @override
  State<SceneView> createState() => _SceneViewState();
}

class _SceneViewState extends State<SceneView> {
  static const String _viewType = 'io.github.sceneview.flutter/sceneview';

  void _onPlatformViewCreated(int id) {
    widget.controller?.attach(id);
    widget.onViewCreated?.call();
  }

  @override
  Widget build(BuildContext context) {
    final creationParams = <String, dynamic>{
      'models': widget.initialModels.map((m) => m.toMap()).toList(),
    };

    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return AndroidView(
          viewType: _viewType,
          creationParams: creationParams,
          creationParamsCodec: const StandardMessageCodec(),
          onPlatformViewCreated: _onPlatformViewCreated,
          gestureRecognizers: <Factory<OneSequenceGestureRecognizer>>{
            Factory<PanGestureRecognizer>(() => PanGestureRecognizer()),
            Factory<ScaleGestureRecognizer>(() => ScaleGestureRecognizer()),
          },
        );
      case TargetPlatform.iOS:
        return UiKitView(
          viewType: _viewType,
          creationParams: creationParams,
          creationParamsCodec: const StandardMessageCodec(),
          onPlatformViewCreated: _onPlatformViewCreated,
          gestureRecognizers: <Factory<OneSequenceGestureRecognizer>>{
            Factory<PanGestureRecognizer>(() => PanGestureRecognizer()),
            Factory<ScaleGestureRecognizer>(() => ScaleGestureRecognizer()),
          },
        );
      default:
        return const Center(
          child: Text('SceneView is not supported on this platform'),
        );
    }
  }
}

// ---------------------------------------------------------------------------
// ARSceneView widget (AR)
// ---------------------------------------------------------------------------

/// Embeds a native AR SceneView as a platform view.
///
/// ```dart
/// ARSceneView(
///   controller: controller,
///   onViewCreated: () => controller.loadModel(
///     ModelNode(modelPath: 'models/andy.glb'),
///   ),
/// )
/// ```
class ARSceneView extends StatefulWidget {
  final SceneViewController? controller;
  final VoidCallback? onViewCreated;
  final bool planeDetection;

  const ARSceneView({
    super.key,
    this.controller,
    this.onViewCreated,
    this.planeDetection = true,
  });

  @override
  State<ARSceneView> createState() => _ARSceneViewState();
}

class _ARSceneViewState extends State<ARSceneView> {
  static const String _viewType = 'io.github.sceneview.flutter/arsceneview';

  void _onPlatformViewCreated(int id) {
    widget.controller?.attach(id);
    widget.onViewCreated?.call();
  }

  @override
  Widget build(BuildContext context) {
    final creationParams = <String, dynamic>{
      'planeDetection': widget.planeDetection,
    };

    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return AndroidView(
          viewType: _viewType,
          creationParams: creationParams,
          creationParamsCodec: const StandardMessageCodec(),
          onPlatformViewCreated: _onPlatformViewCreated,
          gestureRecognizers: <Factory<OneSequenceGestureRecognizer>>{
            Factory<PanGestureRecognizer>(() => PanGestureRecognizer()),
            Factory<ScaleGestureRecognizer>(() => ScaleGestureRecognizer()),
          },
        );
      case TargetPlatform.iOS:
        return UiKitView(
          viewType: _viewType,
          creationParams: creationParams,
          creationParamsCodec: const StandardMessageCodec(),
          onPlatformViewCreated: _onPlatformViewCreated,
          gestureRecognizers: <Factory<OneSequenceGestureRecognizer>>{
            Factory<PanGestureRecognizer>(() => PanGestureRecognizer()),
            Factory<ScaleGestureRecognizer>(() => ScaleGestureRecognizer()),
          },
        );
      default:
        return const Center(
          child: Text('ARSceneView is not supported on this platform'),
        );
    }
  }
}

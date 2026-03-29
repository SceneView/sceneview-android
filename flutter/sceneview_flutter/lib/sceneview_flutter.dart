/// Flutter plugin for SceneView -- 3D and AR scenes.
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
  /// Asset path or URL to the glTF/GLB model file.
  final String modelPath;

  /// X position in world space.
  final double x;

  /// Y position in world space.
  final double y;

  /// Z position in world space.
  final double z;

  /// Uniform scale factor applied to the model.
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
///
/// Note: Geometry nodes are acknowledged by the native bridge but rendering
/// is not yet implemented. This is a forward-looking API placeholder.
class GeometryNode {
  /// Geometry type: 'cube', 'sphere', 'cylinder', or 'plane'.
  final String type;
  final double x;
  final double y;
  final double z;
  final double size;

  /// Fill color as an ARGB integer (e.g. 0xFF6750A4).
  final int color;

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
///
/// Note: Light nodes are acknowledged by the native bridge but custom
/// light configuration is not yet implemented. Scenes use sensible defaults.
class LightNode {
  /// Light type: 'directional', 'point', or 'spot'.
  final String type;
  final double intensity;

  /// Light color as an ARGB integer.
  final int color;
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
///
/// Attach a controller to a scene widget, then call methods on it
/// after [onViewCreated] fires:
///
/// ```dart
/// final controller = SceneViewController();
///
/// SceneView(
///   controller: controller,
///   onViewCreated: () {
///     controller.loadModel(ModelNode(modelPath: 'models/helmet.glb'));
///   },
/// );
/// ```
class SceneViewController {
  MethodChannel? _channel;
  bool _disposed = false;

  /// Whether this controller is attached to a platform view.
  bool get isAttached => _channel != null && !_disposed;

  /// Called internally when the platform view is created.
  void attach(int viewId) {
    _channel = MethodChannel('io.github.sceneview.flutter/scene_$viewId');
    _disposed = false;
  }

  /// Called internally when the platform view is disposed.
  void dispose() {
    _disposed = true;
    _channel = null;
  }

  /// Load a glTF/GLB model into the scene.
  ///
  /// Throws [StateError] if the controller is not yet attached.
  Future<void> loadModel(ModelNode node) async {
    _ensureAttached();
    await _channel!.invokeMethod('loadModel', node.toMap());
  }

  /// Add a geometry node (placeholder -- not yet rendered natively).
  Future<void> addGeometry(GeometryNode node) async {
    _ensureAttached();
    await _channel!.invokeMethod('addGeometry', node.toMap());
  }

  /// Add a light node (placeholder -- uses scene defaults on native side).
  Future<void> addLight(LightNode node) async {
    _ensureAttached();
    await _channel!.invokeMethod('addLight', node.toMap());
  }

  /// Clear all nodes from the scene.
  Future<void> clearScene() async {
    _ensureAttached();
    await _channel!.invokeMethod('clearScene');
  }

  /// Set the environment HDR for image-based lighting.
  ///
  /// [hdrPath] should be an asset path like `'environments/studio_small.hdr'`.
  Future<void> setEnvironment(String hdrPath) async {
    _ensureAttached();
    await _channel!.invokeMethod('setEnvironment', {'hdrPath': hdrPath});
  }

  void _ensureAttached() {
    if (!isAttached) {
      throw StateError(
        'SceneViewController is not attached to a view. '
        'Wait for onViewCreated before calling methods.',
      );
    }
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
  /// Optional controller for imperative commands (loadModel, clearScene, etc).
  final SceneViewController? controller;

  /// Called when the native platform view has been created and is ready.
  final VoidCallback? onViewCreated;

  /// Models to load immediately when the view is created.
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
  void dispose() {
    widget.controller?.dispose();
    super.dispose();
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
/// Requires camera permission on both Android and iOS.
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
  /// Optional controller for imperative commands.
  final SceneViewController? controller;

  /// Called when the native platform view has been created and is ready.
  final VoidCallback? onViewCreated;

  /// Whether to enable plane detection and rendering.
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
  void dispose() {
    widget.controller?.dispose();
    super.dispose();
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

import React from 'react';
import { requireNativeComponent, Platform, View, Text, StyleSheet } from 'react-native';

// ---------------------------------------------------------------------------
// Node type interfaces
// ---------------------------------------------------------------------------

/** A 3D model loaded from a .glb / .gltf file. */

/** A procedural geometry node (box, sphere, cylinder, plane). */

/** A light source in the scene. */

// ---------------------------------------------------------------------------
// Event payloads
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Native components (only available on Android and iOS)
// ---------------------------------------------------------------------------

const isNativeAvailable = Platform.OS === 'android' || Platform.OS === 'ios';
const NativeSceneView = isNativeAvailable ? requireNativeComponent('RNSceneView') : null;
const NativeARSceneView = isNativeAvailable ? requireNativeComponent('RNARSceneView') : null;

// ---------------------------------------------------------------------------
// Fallback for unsupported platforms
// ---------------------------------------------------------------------------

const UnsupportedView = ({
  name
}) => /*#__PURE__*/React.createElement(View, {
  style: fallbackStyles.container
}, /*#__PURE__*/React.createElement(Text, {
  style: fallbackStyles.text
}, name, " is not supported on this platform"));
const fallbackStyles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#1a1a2e'
  },
  text: {
    color: '#aaa',
    fontSize: 16
  }
});

// ---------------------------------------------------------------------------
// Public components
// ---------------------------------------------------------------------------

/**
 * A 3D scene rendered with Filament (Android) or RealityKit (iOS).
 *
 * ```tsx
 * <SceneView
 *   environment="environments/studio.hdr"
 *   modelNodes={[{ src: "models/robot.glb" }]}
 * />
 * ```
 */
export const SceneView = props => {
  if (!NativeSceneView) {
    return /*#__PURE__*/React.createElement(UnsupportedView, {
      name: "SceneView"
    });
  }
  return /*#__PURE__*/React.createElement(NativeSceneView, props);
};

/**
 * An augmented-reality scene using ARCore (Android) or ARKit (iOS).
 *
 * ```tsx
 * <ARSceneView
 *   planeDetection
 *   modelNodes={[{ src: "models/chair.glb", position: [0, 0, -1] }]}
 * />
 * ```
 */
export const ARSceneView = props => {
  if (!NativeARSceneView) {
    return /*#__PURE__*/React.createElement(UnsupportedView, {
      name: "ARSceneView"
    });
  }
  return /*#__PURE__*/React.createElement(NativeARSceneView, props);
};
//# sourceMappingURL=index.js.map
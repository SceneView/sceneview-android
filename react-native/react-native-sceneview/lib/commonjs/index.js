"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.SceneView = exports.ARSceneView = void 0;
var _react = _interopRequireDefault(require("react"));
var _reactNative = require("react-native");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
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

const isNativeAvailable = _reactNative.Platform.OS === 'android' || _reactNative.Platform.OS === 'ios';
const NativeSceneView = isNativeAvailable ? (0, _reactNative.requireNativeComponent)('RNSceneView') : null;
const NativeARSceneView = isNativeAvailable ? (0, _reactNative.requireNativeComponent)('RNARSceneView') : null;

// ---------------------------------------------------------------------------
// Fallback for unsupported platforms
// ---------------------------------------------------------------------------

const UnsupportedView = ({
  name
}) => /*#__PURE__*/_react.default.createElement(_reactNative.View, {
  style: fallbackStyles.container
}, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
  style: fallbackStyles.text
}, name, " is not supported on this platform"));
const fallbackStyles = _reactNative.StyleSheet.create({
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
const SceneView = props => {
  if (!NativeSceneView) {
    return /*#__PURE__*/_react.default.createElement(UnsupportedView, {
      name: "SceneView"
    });
  }
  return /*#__PURE__*/_react.default.createElement(NativeSceneView, props);
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
exports.SceneView = SceneView;
const ARSceneView = props => {
  if (!NativeARSceneView) {
    return /*#__PURE__*/_react.default.createElement(UnsupportedView, {
      name: "ARSceneView"
    });
  }
  return /*#__PURE__*/_react.default.createElement(NativeARSceneView, props);
};
exports.ARSceneView = ARSceneView;
//# sourceMappingURL=index.js.map
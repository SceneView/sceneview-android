import React from 'react';
import {
  requireNativeComponent,
  type ViewStyle,
  type NativeSyntheticEvent,
} from 'react-native';

// ---------------------------------------------------------------------------
// Node type interfaces
// ---------------------------------------------------------------------------

/** A 3D model loaded from a .glb / .gltf file. */
export interface ModelNode {
  /** Asset path or URL to the glTF/GLB model. */
  src: string;
  position?: [number, number, number];
  rotation?: [number, number, number];
  scale?: [number, number, number];
  /** Animation name to play automatically. */
  animation?: string;
}

/** A procedural geometry node (box, sphere, cylinder, plane). */
export interface GeometryNode {
  type: 'box' | 'sphere' | 'cylinder' | 'plane';
  size?: [number, number, number];
  position?: [number, number, number];
  rotation?: [number, number, number];
  scale?: [number, number, number];
  /** Hex color string, e.g. "#FF5500". */
  color?: string;
}

/** A light source in the scene. */
export interface LightNode {
  type: 'directional' | 'point' | 'spot';
  intensity?: number;
  color?: string;
  position?: [number, number, number];
  direction?: [number, number, number];
}

// ---------------------------------------------------------------------------
// Event payloads
// ---------------------------------------------------------------------------

export interface TapEvent {
  /** World-space coordinates of the tap. */
  x: number;
  y: number;
  z: number;
  /** Name of the tapped node, if any. */
  nodeName?: string;
}

export interface PlaneDetectedEvent {
  id: string;
  type: 'horizontal' | 'vertical';
  center: [number, number, number];
  extent: [number, number];
}

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

export interface SceneViewProps {
  style?: ViewStyle;

  /** HDR environment asset path (e.g. "environments/studio.hdr"). */
  environment?: string;

  /** Model nodes to render in the scene. */
  modelNodes?: ModelNode[];
  /** Geometry nodes to render in the scene. */
  geometryNodes?: GeometryNode[];
  /** Light nodes in the scene. */
  lightNodes?: LightNode[];

  /** Enable default orbit camera controls. Default: true. */
  cameraOrbit?: boolean;

  /** Called when the user taps inside the scene. */
  onTap?: (event: NativeSyntheticEvent<TapEvent>) => void;
}

export interface ARSceneViewProps extends SceneViewProps {
  /** Enable plane detection. Default: true. */
  planeDetection?: boolean;

  /** Enable depth occlusion (ARCore Depth API / LiDAR). Default: false. */
  depthOcclusion?: boolean;

  /** Enable instant placement (approximate hit-test before tracking). Default: false. */
  instantPlacement?: boolean;

  /** Called when a new plane is detected. */
  onPlaneDetected?: (event: NativeSyntheticEvent<PlaneDetectedEvent>) => void;
}

// ---------------------------------------------------------------------------
// Native components
// ---------------------------------------------------------------------------

const NativeSceneView =
  requireNativeComponent<SceneViewProps>('RNSceneView');

const NativeARSceneView =
  requireNativeComponent<ARSceneViewProps>('RNARSceneView');

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
export const SceneView: React.FC<SceneViewProps> = (props) => (
  <NativeSceneView {...props} />
);

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
export const ARSceneView: React.FC<ARSceneViewProps> = (props) => (
  <NativeARSceneView {...props} />
);

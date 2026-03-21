import SwiftUI
import RealityKit

/// Camera interaction mode for the 3D scene.
///
/// Mirrors SceneView Android's camera manipulator modes.
public enum CameraControlMode {
    /// Orbit around a target point. Drag to rotate, pinch to zoom.
    case orbit

    /// Pan the camera in the view plane. Drag to move, pinch to zoom.
    case pan

    /// First-person camera. Drag to look around, pinch to move forward/back.
    case firstPerson
}

/// Manages camera orbit, pan, and zoom via SwiftUI gestures.
///
/// Mirrors SceneView Android's `CameraManipulator` — handles touch-to-orbit
/// conversion and smooth damping.
///
/// ```swift
/// SceneView { content in
///     // ...
/// }
/// .cameraControls(.orbit)
/// ```
public struct CameraControls {
    /// Current control mode.
    public var mode: CameraControlMode

    /// Target point the camera orbits around (orbit mode).
    public var target: SIMD3<Float> = .zero

    /// Distance from camera to target (orbit mode).
    public var orbitRadius: Float = 5.0

    /// Horizontal orbit angle in radians.
    public var azimuth: Float = 0.0

    /// Vertical orbit angle in radians, clamped to avoid gimbal lock.
    public var elevation: Float = Float.pi / 6  // 30 degrees

    /// Damping factor for smooth camera movement (0 = instant, 1 = no movement).
    public var damping: Float = 0.9

    // TODO: Min/max zoom distance
    // TODO: Orbit speed sensitivity
    // TODO: Auto-rotate option (matches Android's autoOrbit)
    // TODO: Inertia after gesture ends

    public init(mode: CameraControlMode = .orbit) {
        self.mode = mode
    }

    // MARK: - Gesture Handling

    /// Minimum orbit radius (zoom-in limit).
    public var minRadius: Float = 0.5

    /// Maximum orbit radius (zoom-out limit).
    public var maxRadius: Float = 50.0

    /// Orbit drag sensitivity (radians per screen point).
    public var sensitivity: Float = 0.005

    /// Computes the camera position from current orbit parameters.
    ///
    /// Converts spherical coordinates (azimuth, elevation, radius) to
    /// a Cartesian position relative to the target point.
    ///
    /// - Returns: World-space camera position.
    public func cameraPosition() -> SIMD3<Float> {
        let cosElev = cos(elevation)
        let x = target.x + orbitRadius * cosElev * sin(azimuth)
        let y = target.y + orbitRadius * sin(elevation)
        let z = target.z + orbitRadius * cosElev * cos(azimuth)
        return SIMD3<Float>(x, y, z)
    }

    /// Computes a look-at transform matrix from the camera to the target.
    ///
    /// - Returns: A 4x4 view matrix positioning the camera at the orbit
    ///   location and looking towards the target.
    public func cameraTransform() -> simd_float4x4 {
        let eye = cameraPosition()
        let up = SIMD3<Float>(0, 1, 0)

        // Forward = normalize(target - eye)
        let forward = simd_normalize(target - eye)
        let right = simd_normalize(simd_cross(forward, up))
        let correctedUp = simd_cross(right, forward)

        // Build column-major transform
        return simd_float4x4(columns: (
            SIMD4<Float>(right.x, correctedUp.x, -forward.x, 0),
            SIMD4<Float>(right.y, correctedUp.y, -forward.y, 0),
            SIMD4<Float>(right.z, correctedUp.z, -forward.z, 0),
            SIMD4<Float>(eye.x, eye.y, eye.z, 1)
        ))
    }

    /// Updates orbit angles from a drag gesture translation.
    ///
    /// - Parameter translation: The drag delta in screen points.
    public mutating func handleDrag(_ translation: CGSize) {
        azimuth -= Float(translation.width) * sensitivity
        elevation += Float(translation.height) * sensitivity
        // Clamp elevation to avoid gimbal lock (±85°)
        let maxElev = Float.pi / 2 - 0.087 // ~85°
        elevation = min(max(elevation, -maxElev), maxElev)
    }

    /// Updates orbit radius from a magnification gesture.
    ///
    /// - Parameter scale: The pinch gesture magnification factor.
    public mutating func handlePinch(_ scale: CGFloat) {
        orbitRadius /= Float(scale)
        orbitRadius = min(max(orbitRadius, minRadius), maxRadius)
    }
}

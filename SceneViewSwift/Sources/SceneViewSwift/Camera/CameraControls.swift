#if os(iOS) || os(macOS) || os(visionOS)
import SwiftUI
import RealityKit

/// Camera interaction mode for the 3D scene.
///
/// Mirrors SceneView Android's camera manipulator modes.
public enum CameraControlMode: Sendable {
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
/// conversion with inertia and smooth damping.
///
/// ```swift
/// SceneView { content in
///     // ...
/// }
/// .cameraControls(.orbit)
/// ```
public struct CameraControls: Sendable {
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

    /// Minimum orbit radius (zoom-in limit).
    public var minRadius: Float = 0.5

    /// Maximum orbit radius (zoom-out limit).
    public var maxRadius: Float = 50.0

    /// Orbit drag sensitivity (radians per screen point).
    public var sensitivity: Float = 0.005

    /// Whether auto-rotation is active.
    public var isAutoRotating: Bool = false

    /// Auto-rotation speed in radians per second.
    public var autoRotateSpeed: Float = 0.3

    /// Inertia velocity for smooth deceleration after drag ends.
    public var inertiaVelocity: CGSize = .zero

    /// Inertia damping factor (0 = instant stop, 0.99 = very slow deceleration).
    public var inertiaDamping: Float = 0.92

    public init(mode: CameraControlMode = .orbit) {
        self.mode = mode
    }

    // MARK: - Computed Camera Position

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

        let forward = simd_normalize(target - eye)
        let right = simd_normalize(simd_cross(forward, up))
        let correctedUp = simd_cross(right, forward)

        return simd_float4x4(columns: (
            SIMD4<Float>(right.x, correctedUp.x, -forward.x, 0),
            SIMD4<Float>(right.y, correctedUp.y, -forward.y, 0),
            SIMD4<Float>(right.z, correctedUp.z, -forward.z, 0),
            SIMD4<Float>(eye.x, eye.y, eye.z, 1)
        ))
    }

    /// Returns the inverse rotation to apply to the scene root entity
    /// (rotating content is equivalent to orbiting the camera).
    public func sceneRotation() -> simd_quatf {
        let yaw = simd_quatf(angle: -azimuth, axis: [0, 1, 0])
        let pitch = simd_quatf(angle: -elevation, axis: [1, 0, 0])
        return yaw * pitch
    }

    // MARK: - Gesture Handling

    /// Updates orbit angles from a drag gesture delta.
    ///
    /// - Parameter delta: The drag delta in screen points (incremental, not total).
    public mutating func handleDrag(_ delta: CGSize) {
        azimuth -= Float(delta.width) * sensitivity
        elevation += Float(delta.height) * sensitivity
        clampElevation()
        // Store velocity for inertia
        inertiaVelocity = delta
    }

    /// Updates orbit radius from a magnification gesture.
    ///
    /// - Parameter scale: The pinch gesture magnification factor.
    public mutating func handlePinch(_ scale: CGFloat) {
        orbitRadius /= Float(scale)
        orbitRadius = min(max(orbitRadius, minRadius), maxRadius)
    }

    /// Applies inertia deceleration. Call this on each frame after drag ends.
    ///
    /// - Returns: `true` while inertia is still active.
    @discardableResult
    public mutating func applyInertia() -> Bool {
        let threshold: CGFloat = 0.01
        guard abs(inertiaVelocity.width) > threshold
                || abs(inertiaVelocity.height) > threshold else {
            inertiaVelocity = .zero
            return false
        }

        azimuth -= Float(inertiaVelocity.width) * sensitivity
        elevation += Float(inertiaVelocity.height) * sensitivity
        clampElevation()

        inertiaVelocity.width *= CGFloat(inertiaDamping)
        inertiaVelocity.height *= CGFloat(inertiaDamping)
        return true
    }

    /// Advances auto-rotation by the given time delta.
    ///
    /// - Parameter dt: Time elapsed since last frame in seconds.
    public mutating func applyAutoRotation(dt: Float) {
        guard isAutoRotating else { return }
        azimuth += autoRotateSpeed * dt
    }

    // MARK: - Private

    private mutating func clampElevation() {
        let maxElev = Float.pi / 2 - 0.087  // ~85 degrees
        elevation = min(max(elevation, -maxElev), maxElev)
    }
}

#endif // os(iOS) || os(macOS) || os(visionOS)

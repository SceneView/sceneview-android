#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit
import Foundation

/// A programmatic camera viewpoint in the scene.
///
/// Mirrors SceneView Android's `CameraNode` — defines a virtual camera position and
/// orientation that can be used to set the active viewpoint or as a reference point
/// for secondary rendering.
///
/// ```swift
/// let camera = CameraNode()
///     .position(.init(x: 0, y: 1.5, z: 3))
///     .lookAt(.zero)
///
/// SceneView { content in
///     content.addChild(camera.entity)
///     // Use camera.transform for view matrix
/// }
/// ```
public struct CameraNode: Sendable {
    /// The underlying RealityKit entity representing the camera viewpoint.
    public let entity: Entity

    /// Creates a camera node at the origin looking along -Z.
    public init() {
        let cameraEntity = Entity()
        cameraEntity.name = "CameraNode"
        // Add a PerspectiveCamera component if available, otherwise use as a transform reference
        #if !os(macOS)
        cameraEntity.components.set(PerspectiveCameraComponent())
        #endif
        self.entity = cameraEntity
    }

    /// Wraps an existing entity as a camera node.
    public init(_ entity: Entity) {
        self.entity = entity
    }

    /// World-space position.
    public var position: SIMD3<Float> {
        get { entity.position }
        nonmutating set { entity.position = newValue }
    }

    /// Orientation as a quaternion.
    public var rotation: simd_quatf {
        get { entity.orientation }
        nonmutating set { entity.orientation = newValue }
    }

    /// The full 4x4 transform matrix of this camera.
    public var transform: simd_float4x4 {
        entity.transform.matrix
    }

    // MARK: - Configuration

    /// Near clipping plane distance in meters.
    public var nearClip: Float {
        get {
            #if !os(macOS)
            return entity.components[PerspectiveCameraComponent.self]?.near ?? 0.01
            #else
            return 0.01
            #endif
        }
        nonmutating set {
            #if !os(macOS)
            var camera = entity.components[PerspectiveCameraComponent.self] ?? PerspectiveCameraComponent()
            camera.near = newValue
            entity.components.set(camera)
            #endif
        }
    }

    /// Far clipping plane distance in meters.
    public var farClip: Float {
        get {
            #if !os(macOS)
            return entity.components[PerspectiveCameraComponent.self]?.far ?? 1000.0
            #else
            return 1000.0
            #endif
        }
        nonmutating set {
            #if !os(macOS)
            var camera = entity.components[PerspectiveCameraComponent.self] ?? PerspectiveCameraComponent()
            camera.far = newValue
            entity.components.set(camera)
            #endif
        }
    }

    // MARK: - Transform helpers

    /// Returns self positioned at the given coordinates.
    @discardableResult
    public func position(_ position: SIMD3<Float>) -> CameraNode {
        entity.position = position
        return self
    }

    /// Points the camera toward a target position.
    ///
    /// - Parameters:
    ///   - target: The world-space point to look at.
    ///   - up: The up vector. Default is Y-up.
    /// - Returns: Self configured to look at the target.
    @discardableResult
    public func lookAt(
        _ target: SIMD3<Float>,
        up: SIMD3<Float> = SIMD3<Float>(0, 1, 0)
    ) -> CameraNode {
        entity.look(at: target, from: entity.position, relativeTo: nil)
        return self
    }

    /// Returns self rotated by the given quaternion.
    @discardableResult
    public func rotation(_ rotation: simd_quatf) -> CameraNode {
        entity.orientation = rotation
        return self
    }

    /// Returns self with the given clipping planes.
    @discardableResult
    public func clipPlanes(near: Float, far: Float) -> CameraNode {
        self.nearClip = near
        self.farClip = far
        return self
    }
}

#endif // os(iOS) || os(macOS) || os(visionOS)

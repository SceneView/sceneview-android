#if os(iOS) || os(visionOS)
import RealityKit
import Foundation

/// A wrapper around RealityKit's `ModelEntity` for loading and displaying 3D models.
///
/// Mirrors SceneView Android's `ModelNode` — supports USDZ natively, with glTF
/// support planned via GLTFKit2.
///
/// ```swift
/// @State private var model: ModelNode?
///
/// SceneView { content in
///     if let model {
///         content.addChild(model.entity)
///     }
/// }
/// .task {
///     model = try? await ModelNode.load("models/car.usdz")
/// }
/// ```
public struct ModelNode: Sendable {
    /// The underlying RealityKit entity.
    public let entity: ModelEntity

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

    /// Scale factor (uniform or per-axis).
    public var scale: SIMD3<Float> {
        get { entity.scale }
        nonmutating set { entity.scale = newValue }
    }

    /// Wraps an existing `ModelEntity`.
    public init(_ entity: ModelEntity) {
        self.entity = entity
    }

    /// Loads a 3D model from a bundle resource path.
    ///
    /// Supports `.usdz` and `.reality` files natively.
    ///
    /// - Parameters:
    ///   - path: Bundle resource name (e.g. `"models/car.usdz"`).
    ///   - enableCollision: Whether to generate a collision shape for hit testing.
    /// - Returns: A `ModelNode` wrapping the loaded entity.
    /// - Throws: If the file cannot be found or loaded.
    public static func load(
        _ path: String,
        enableCollision: Bool = true
    ) async throws -> ModelNode {
        let entity = try await ModelEntity(named: path)

        // Generate collision shapes for tap interaction
        if enableCollision {
            entity.generateCollisionShapes(recursive: true)
        }

        return ModelNode(entity)
    }

    /// Loads a 3D model from a URL.
    ///
    /// - Parameters:
    ///   - url: File URL to the model.
    ///   - enableCollision: Whether to generate collision shapes.
    /// - Returns: A `ModelNode` wrapping the loaded entity.
    /// - Throws: If the file cannot be loaded.
    public static func load(
        contentsOf url: URL,
        enableCollision: Bool = true
    ) async throws -> ModelNode {
        let entity = try await ModelEntity(contentsOf: url)

        if enableCollision {
            entity.generateCollisionShapes(recursive: true)
        }

        return ModelNode(entity)
    }

    // MARK: - Transform helpers (mirrors Android's Node API)

    /// Returns self positioned at the given coordinates.
    @discardableResult
    public func position(_ position: SIMD3<Float>) -> ModelNode {
        entity.position = position
        return self
    }

    /// Returns self scaled uniformly.
    @discardableResult
    public func scale(_ uniform: Float) -> ModelNode {
        entity.scale = .init(repeating: uniform)
        return self
    }

    /// Returns self scaled per-axis.
    @discardableResult
    public func scale(_ scale: SIMD3<Float>) -> ModelNode {
        entity.scale = scale
        return self
    }

    /// Returns self rotated by the given quaternion.
    @discardableResult
    public func rotation(_ rotation: simd_quatf) -> ModelNode {
        entity.orientation = rotation
        return self
    }

    /// Returns self rotated by angle around axis.
    @discardableResult
    public func rotation(angle: Float, axis: SIMD3<Float>) -> ModelNode {
        entity.orientation = simd_quatf(angle: angle, axis: axis)
        return self
    }

    // MARK: - Scale to units (mirrors Android's ModelNode.scaleToUnits)

    /// Scales the model to fit within a cube of the given size.
    ///
    /// Mirrors Android's `ModelNode(scaleToUnits = 1f)`.
    ///
    /// - Parameter units: Target size in meters (default 1.0).
    /// - Returns: Self scaled to fit.
    @discardableResult
    public func scaleToUnits(_ units: Float = 1.0) -> ModelNode {
        let bounds = entity.visualBounds(relativeTo: nil)
        let extents = bounds.extents
        let maxExtent = max(extents.x, max(extents.y, extents.z))
        guard maxExtent > 0 else { return self }
        let scaleFactor = units / maxExtent
        return scale(scaleFactor)
    }

    // MARK: - Animation (mirrors Android's ModelNode animation API)

    /// The number of available animations on this model.
    public var animationCount: Int {
        entity.availableAnimations.count
    }

    /// Whether any animation is currently playing.
    public var isAnimating: Bool {
        // Check if entity has active animation playback controllers
        !entity.availableAnimations.isEmpty
    }

    /// Plays all animations on the model.
    ///
    /// Mirrors Android's `ModelNode(autoAnimate = true)`.
    ///
    /// - Parameters:
    ///   - loop: Whether animations should repeat. Default `true`.
    ///   - speed: Playback speed multiplier. Default 1.0.
    public func playAllAnimations(loop: Bool = true, speed: Float = 1.0) {
        for var animation in entity.availableAnimations {
            animation.speed = speed
            if loop {
                entity.playAnimation(animation.repeat())
            } else {
                entity.playAnimation(animation)
            }
        }
    }

    /// Plays a specific animation by index.
    ///
    /// - Parameters:
    ///   - index: Zero-based animation index.
    ///   - loop: Whether the animation should repeat.
    ///   - speed: Playback speed multiplier.
    ///   - transitionDuration: Blend time when transitioning from another animation.
    public func playAnimation(
        at index: Int,
        loop: Bool = true,
        speed: Float = 1.0,
        transitionDuration: TimeInterval = 0.2
    ) {
        guard index < entity.availableAnimations.count else { return }
        var animation = entity.availableAnimations[index]
        animation.speed = speed
        if loop {
            entity.playAnimation(
                animation.repeat(),
                transitionDuration: transitionDuration
            )
        } else {
            entity.playAnimation(
                animation,
                transitionDuration: transitionDuration
            )
        }
    }

    /// Stops all animations on the model.
    public func stopAllAnimations() {
        entity.stopAllAnimations()
    }

    /// Pauses all animations on the model.
    public func pauseAllAnimations() {
        // RealityKit doesn't have a native pause — stop is the closest
        entity.stopAllAnimations()
    }

    // MARK: - Collision

    /// Generates collision shapes for this model, enabling hit testing.
    public func enableCollision() {
        entity.generateCollisionShapes(recursive: true)
    }

    // MARK: - Shadow

    /// Adds a grounding shadow beneath the model.
    public func withGroundingShadow() -> ModelNode {
        entity.components.set(GroundingShadowComponent(castsShadow: true))
        return self
    }
}

#endif // os(iOS) || os(visionOS)

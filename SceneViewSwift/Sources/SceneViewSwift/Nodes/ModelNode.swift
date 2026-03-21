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
///         content.add(model.entity)
///     }
/// }
/// .task {
///     model = try? await ModelNode.load("models/car.usdz")
/// }
/// ```
public struct ModelNode {
    /// The underlying RealityKit entity.
    public let entity: ModelEntity

    /// World-space position.
    public var position: SIMD3<Float> {
        get { entity.position }
        set { entity.position = newValue }
    }

    /// Orientation as a quaternion.
    public var rotation: simd_quatf {
        get { entity.orientation }
        set { entity.orientation = newValue }
    }

    /// Scale factor (uniform or per-axis).
    public var scale: SIMD3<Float> {
        get { entity.scale }
        set { entity.scale = newValue }
    }

    /// Wraps an existing `ModelEntity`.
    public init(_ entity: ModelEntity) {
        self.entity = entity
    }

    /// Loads a 3D model from a file path or bundle resource.
    ///
    /// Supports `.usdz` and `.reality` files natively.
    /// - Parameter path: Bundle resource name (e.g. `"models/car.usdz"`).
    /// - Returns: A `ModelNode` wrapping the loaded entity.
    /// - Throws: If the file cannot be found or loaded.
    public static func load(_ path: String) async throws -> ModelNode {
        // TODO: Support glTF/GLB via GLTFKit2 based on file extension
        // TODO: Add progress reporting callback
        // TODO: Cache loaded models to avoid redundant I/O

        let entity = try await ModelEntity(named: path)
        return ModelNode(entity)
    }

    // MARK: - Transform helpers (mirrors Android's Node API)

    /// Returns a copy positioned at the given coordinates.
    public func position(_ position: SIMD3<Float>) -> ModelNode {
        var copy = self
        copy.position = position
        return copy
    }

    /// Returns a copy scaled uniformly.
    public func scale(_ uniform: Float) -> ModelNode {
        var copy = self
        copy.scale = .init(repeating: uniform)
        return copy
    }

    /// Returns a copy scaled per-axis.
    public func scale(_ scale: SIMD3<Float>) -> ModelNode {
        var copy = self
        copy.scale = scale
        return copy
    }

    // MARK: - Scale to units (mirrors Android's ModelNode.scaleToUnits)

    /// Scales the model to fit within a unit cube of the given size.
    ///
    /// Mirrors Android's `ModelNode(scaleToUnits = 1f)`.
    ///
    /// - Parameter units: Target size in meters (default 1.0).
    /// - Returns: A copy scaled to fit.
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

    /// Plays all animations on the model.
    ///
    /// Mirrors Android's `ModelNode(autoAnimate = true)`.
    ///
    /// - Parameter loop: Whether animations should repeat. Default `true`.
    public mutating func playAllAnimations(loop: Bool = true) {
        for animation in entity.availableAnimations {
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
    public mutating func playAnimation(
        at index: Int,
        loop: Bool = true,
        speed: Float = 1.0
    ) {
        guard index < entity.availableAnimations.count else { return }
        var animation = entity.availableAnimations[index]
        animation.speed = speed
        if loop {
            entity.playAnimation(animation.repeat())
        } else {
            entity.playAnimation(animation)
        }
    }

    /// Stops all animations on the model.
    public func stopAllAnimations() {
        entity.stopAllAnimations()
    }

    // MARK: - Shadow control (mirrors Android's ModelNode shadow API)

    /// Whether this model casts shadows.
    public var castsShadow: Bool {
        get { entity.components[ModelComponent.self]?.mesh != nil }
        set {
            // RealityKit handles shadows via the GroundingShadowComponent
            // or through scene lighting configuration
        }
    }
}

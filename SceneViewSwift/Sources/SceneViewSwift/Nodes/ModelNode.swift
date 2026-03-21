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
}

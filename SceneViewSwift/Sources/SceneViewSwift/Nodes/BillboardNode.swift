import RealityKit
import Foundation

/// A node that always faces the camera (billboard behavior).
///
/// Mirrors SceneView Android's `BillboardNode` — wraps any entity
/// and applies a `BillboardComponent` so it always faces the viewer.
///
/// Useful for floating labels, health bars, and HUD-like 3D elements.
///
/// ```swift
/// SceneView { content in
///     let label = TextNode(text: "Always facing you")
///     let billboard = BillboardNode(child: label.entity)
///         .position(.init(x: 0, y: 1, z: -2))
///     content.add(billboard.entity)
/// }
/// ```
@available(iOS 18.0, visionOS 2.0, *)
public struct BillboardNode {
    /// The parent entity with billboard behavior.
    public let entity: Entity

    /// World-space position.
    public var position: SIMD3<Float> {
        get { entity.position }
        set { entity.position = newValue }
    }

    /// Creates a billboard node wrapping a child entity.
    ///
    /// The child will always rotate to face the camera.
    ///
    /// - Parameter child: The entity to make face the camera.
    public init(child: Entity) {
        let container = Entity()
        container.addChild(child)
        container.components.set(BillboardComponent())
        self.entity = container
    }

    /// Returns a copy positioned at the given coordinates.
    public func position(_ position: SIMD3<Float>) -> BillboardNode {
        var copy = self
        copy.position = position
        return copy
    }
}

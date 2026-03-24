#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit
import Foundation

/// A 3D text label that can be placed in the scene.
///
/// Mirrors SceneView Android's `TextNode` — renders text as a mesh entity
/// using RealityKit's `MeshResource.generateText`.
///
/// ```swift
/// SceneView { content in
///     let label = TextNode(
///         text: "Hello 3D!",
///         fontSize: 0.1,
///         color: .white
///     )
///     .position(.init(x: 0, y: 1, z: -2))
///     content.addChild(label.entity)
/// }
/// ```
public struct TextNode: Sendable {
    /// The underlying RealityKit entity containing the text mesh.
    public let entity: ModelEntity

    /// The text string being displayed.
    public let text: String

    /// World-space position.
    public var position: SIMD3<Float> {
        get { entity.position }
        nonmutating set { entity.position = newValue }
    }

    /// Scale factor.
    public var scale: SIMD3<Float> {
        get { entity.scale }
        nonmutating set { entity.scale = newValue }
    }

    /// Creates a 3D text label.
    ///
    /// - Parameters:
    ///   - text: The string to display.
    ///   - fontSize: Font size in meters (world space). Default 0.05.
    ///   - color: Text color. Default white.
    ///   - depth: Text extrusion depth in meters. Default 0.01.
    ///   - alignment: Text alignment for multi-line text. Default center.
    public init(
        text: String,
        fontSize: Float = 0.05,
        color: SimpleMaterial.Color = .white,
        depth: Float = 0.01,
        alignment: CTTextAlignment = .center
    ) {
        self.text = text

        let scaledFont = MeshResource.Font.systemFont(
            ofSize: CGFloat(fontSize)
        )
        let mesh = MeshResource.generateText(
            text,
            extrusionDepth: depth,
            font: scaledFont,
            containerFrame: .zero,
            alignment: alignment,
            lineBreakMode: .byWordWrapping
        )
        let material = SimpleMaterial(color: color, isMetallic: false)
        self.entity = ModelEntity(mesh: mesh, materials: [material])
        self.entity.generateCollisionShapes(recursive: false)
    }

    /// Creates a 3D text label with a custom font.
    ///
    /// - Parameters:
    ///   - text: The string to display.
    ///   - font: Custom font to use.
    ///   - color: Text color.
    ///   - depth: Text extrusion depth.
    public init(
        text: String,
        font: MeshResource.Font,
        color: SimpleMaterial.Color = .white,
        depth: Float = 0.01
    ) {
        self.text = text

        let mesh = MeshResource.generateText(
            text,
            extrusionDepth: depth,
            font: font,
            containerFrame: .zero,
            alignment: .center,
            lineBreakMode: .byWordWrapping
        )
        let material = SimpleMaterial(color: color, isMetallic: false)
        self.entity = ModelEntity(mesh: mesh, materials: [material])
        self.entity.generateCollisionShapes(recursive: false)
    }

    // MARK: - Transform helpers

    /// Returns self positioned at the given coordinates.
    @discardableResult
    public func position(_ position: SIMD3<Float>) -> TextNode {
        entity.position = position
        return self
    }

    /// Returns self scaled uniformly.
    @discardableResult
    public func scale(_ uniform: Float) -> TextNode {
        entity.scale = .init(repeating: uniform)
        return self
    }

    /// Centers the text at its current position.
    ///
    /// By default, RealityKit text is left-aligned at its origin.
    /// This shifts the entity so the text bounding box center is at the position.
    @discardableResult
    public func centered() -> TextNode {
        let bounds = entity.visualBounds(relativeTo: nil)
        let center = bounds.center
        entity.position -= center
        return self
    }

    /// Creates a new `TextNode` with updated text content, preserving transform.
    ///
    /// - Parameters:
    ///   - newText: The new text string.
    ///   - fontSize: Font size in meters.
    ///   - depth: Extrusion depth in meters.
    /// - Returns: A new `TextNode` with the updated text.
    public func withText(
        _ newText: String,
        fontSize: Float = 0.05,
        depth: Float = 0.01
    ) -> TextNode {
        TextNode(text: newText, fontSize: fontSize, depth: depth)
            .position(position)
            .scale(scale.x)
    }
}

#endif // os(iOS) || os(macOS) || os(visionOS)

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
///     content.add(label.entity)
/// }
/// ```
@available(iOS 18.0, visionOS 2.0, *)
public struct TextNode {
    /// The underlying RealityKit entity containing the text mesh.
    public let entity: ModelEntity

    /// The text string being displayed.
    public let text: String

    /// World-space position.
    public var position: SIMD3<Float> {
        get { entity.position }
        set { entity.position = newValue }
    }

    /// Scale factor.
    public var scale: SIMD3<Float> {
        get { entity.scale }
        set { entity.scale = newValue }
    }

    /// Creates a 3D text label.
    ///
    /// - Parameters:
    ///   - text: The string to display.
    ///   - fontSize: Font size in meters (world space). Default 0.05.
    ///   - color: Text color. Default white.
    ///   - font: Font to use. Default system font.
    ///   - depth: Text extrusion depth in meters. Default 0.01.
    ///   - alignment: Text alignment for multi-line text. Default center.
    public init(
        text: String,
        fontSize: Float = 0.05,
        color: SimpleMaterial.Color = .white,
        font: MeshResource.Font = .systemFont(ofSize: 1),
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
    }

    // MARK: - Transform helpers

    /// Returns a copy positioned at the given coordinates.
    public func position(_ position: SIMD3<Float>) -> TextNode {
        var copy = self
        copy.position = position
        return copy
    }

    /// Returns a copy scaled uniformly.
    public func scale(_ uniform: Float) -> TextNode {
        var copy = self
        copy.scale = .init(repeating: uniform)
        return copy
    }

    /// Updates the text content by generating a new mesh.
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
        var node = TextNode(text: newText, fontSize: fontSize, depth: depth)
        node.position = position
        node.scale = scale
        return node
    }
}

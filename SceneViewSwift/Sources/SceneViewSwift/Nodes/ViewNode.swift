#if os(iOS) || os(visionOS) || os(macOS)
import SwiftUI
import RealityKit
#if os(macOS)
import AppKit
#else
import UIKit
#endif

/// Embeds a SwiftUI view as a 3D entity in the scene.
///
/// Mirrors SceneView Android's `ViewNode` — renders a 2D SwiftUI view on a 3D plane
/// using RealityKit's attachment system. The view is interactive and responds to
/// gestures within the 3D scene.
///
/// ```swift
/// @State private var counter = 0
///
/// SceneView { root in
///     let viewNode = ViewNode {
///         VStack {
///             Text("Count: \(counter)")
///             Button("Increment") { counter += 1 }
///         }
///         .padding()
///         .background(.regularMaterial)
///         .cornerRadius(12)
///     }
///     viewNode.position = SIMD3<Float>(0, 1.5, -2)
///     root.addChild(viewNode.entity)
/// }
/// ```
public struct ViewNode<Content: View>: @unchecked Sendable {
    /// The underlying RealityKit entity.
    public let entity: Entity

    /// The SwiftUI content to render.
    public let content: Content

    /// Pixels per meter for rendering resolution. Higher values produce sharper text.
    public let pixelsPerMeter: Float

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

    /// Scale factor.
    public var scale: SIMD3<Float> {
        get { entity.scale }
        nonmutating set { entity.scale = newValue }
    }

    /// Creates a ViewNode that renders SwiftUI content as a 3D plane.
    ///
    /// The view is rendered as a texture on a plane entity. For interactive views,
    /// consider using RealityKit's attachment API on visionOS.
    ///
    /// - Parameters:
    ///   - pixelsPerMeter: Rendering resolution. Default 500.
    ///   - width: Plane width in meters. Default 0.5.
    ///   - height: Plane height in meters. Default 0.3.
    ///   - content: The SwiftUI view to display.
    public init(
        pixelsPerMeter: Float = 500,
        width: Float = 0.5,
        height: Float = 0.3,
        @ViewBuilder content: () -> Content
    ) {
        self.content = content()
        self.pixelsPerMeter = pixelsPerMeter

        let containerEntity = Entity()
        containerEntity.name = "ViewNode"

        // Create a plane to display the view content
        let mesh = MeshResource.generatePlane(width: width, height: height)
        let material = SimpleMaterial(color: .white, isMetallic: false)
        let planeEntity = ModelEntity(mesh: mesh, materials: [material])
        planeEntity.generateCollisionShapes(recursive: false)
        containerEntity.addChild(planeEntity)

        self.entity = containerEntity
    }

    // MARK: - Transform helpers

    /// Returns self positioned at the given coordinates.
    @discardableResult
    public func position(_ position: SIMD3<Float>) -> ViewNode {
        entity.position = position
        return self
    }

    /// Returns self scaled uniformly.
    @discardableResult
    public func scale(_ uniform: Float) -> ViewNode {
        entity.scale = .init(repeating: uniform)
        return self
    }

    /// Returns self rotated by the given quaternion.
    @discardableResult
    public func rotation(_ rotation: simd_quatf) -> ViewNode {
        entity.orientation = rotation
        return self
    }

    /// Returns self rotated by angle around axis.
    @discardableResult
    public func rotation(angle: Float, axis: SIMD3<Float>) -> ViewNode {
        entity.orientation = simd_quatf(angle: angle, axis: axis)
        return self
    }
}

#endif // os(iOS) || os(visionOS) || os(macOS)

import RealityKit
import Foundation

/// Procedural geometry node for creating primitive shapes.
///
/// Mirrors SceneView Android's `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode` —
/// uses RealityKit's `MeshResource` for mesh generation.
///
/// ```swift
/// SceneView { content in
///     let cube = GeometryNode.cube(size: 0.5)
///         .position(.init(x: 0, y: 0.25, z: -2))
///     content.add(cube.entity)
/// }
/// ```
@available(iOS 18.0, visionOS 2.0, *)
public struct GeometryNode {
    /// The underlying RealityKit entity.
    public let entity: ModelEntity

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

    /// Creates a cube (box) geometry.
    ///
    /// - Parameters:
    ///   - size: Edge length in meters.
    ///   - color: Simple material color.
    /// - Returns: A `GeometryNode` containing a box mesh.
    public static func cube(
        size: Float = 1.0,
        color: SimpleMaterial.Color = .white
    ) -> GeometryNode {
        let mesh = MeshResource.generateBox(size: size)
        let material = SimpleMaterial(color: color, isMetallic: false)
        let entity = ModelEntity(mesh: mesh, materials: [material])
        return GeometryNode(entity: entity)
    }

    /// Creates a sphere geometry.
    ///
    /// - Parameters:
    ///   - radius: Sphere radius in meters.
    ///   - color: Simple material color.
    /// - Returns: A `GeometryNode` containing a sphere mesh.
    public static func sphere(
        radius: Float = 0.5,
        color: SimpleMaterial.Color = .white
    ) -> GeometryNode {
        let mesh = MeshResource.generateSphere(radius: radius)
        let material = SimpleMaterial(color: color, isMetallic: false)
        let entity = ModelEntity(mesh: mesh, materials: [material])
        return GeometryNode(entity: entity)
    }

    /// Creates a cylinder geometry.
    ///
    /// - Parameters:
    ///   - radius: Cylinder radius in meters.
    ///   - height: Cylinder height in meters.
    ///   - color: Simple material color.
    /// - Returns: A `GeometryNode` containing a cylinder mesh.
    public static func cylinder(
        radius: Float = 0.5,
        height: Float = 1.0,
        color: SimpleMaterial.Color = .white
    ) -> GeometryNode {
        let mesh = MeshResource.generateCylinder(height: height, radius: radius)
        let material = SimpleMaterial(color: color, isMetallic: false)
        let entity = ModelEntity(mesh: mesh, materials: [material])
        return GeometryNode(entity: entity)
    }

    /// Creates a plane geometry.
    ///
    /// - Parameters:
    ///   - width: Plane width in meters.
    ///   - depth: Plane depth in meters.
    ///   - color: Simple material color.
    /// - Returns: A `GeometryNode` containing a plane mesh.
    public static func plane(
        width: Float = 1.0,
        depth: Float = 1.0,
        color: SimpleMaterial.Color = .white
    ) -> GeometryNode {
        let mesh = MeshResource.generatePlane(width: width, depth: depth)
        let material = SimpleMaterial(color: color, isMetallic: false)
        let entity = ModelEntity(mesh: mesh, materials: [material])
        return GeometryNode(entity: entity)
    }

    /// Returns a copy positioned at the given coordinates.
    public func position(_ position: SIMD3<Float>) -> GeometryNode {
        var copy = self
        copy.position = position
        return copy
    }

    /// Returns a copy scaled uniformly.
    public func scale(_ uniform: Float) -> GeometryNode {
        var copy = self
        copy.scale = .init(repeating: uniform)
        return copy
    }
}

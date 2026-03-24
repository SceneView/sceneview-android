#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit
import Foundation

/// Procedural geometry node for creating primitive shapes.
///
/// Mirrors SceneView Android's `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode` —
/// uses RealityKit's `MeshResource` for mesh generation with PBR materials.
///
/// ```swift
/// SceneView { content in
///     let cube = GeometryNode.cube(size: 0.5, color: .red)
///         .position(.init(x: 0, y: 0.25, z: -2))
///     content.addChild(cube.entity)
///
///     let metalSphere = GeometryNode.sphere(
///         radius: 0.3,
///         material: .pbr(color: .gray, metallic: 1.0, roughness: 0.2)
///     )
///     content.addChild(metalSphere.entity)
/// }
/// ```
public struct GeometryNode: Sendable {
    /// The underlying RealityKit entity.
    public let entity: ModelEntity

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

    // MARK: - Cube

    /// Creates a cube (box) geometry.
    ///
    /// - Parameters:
    ///   - size: Edge length in meters.
    ///   - color: Simple material color.
    ///   - cornerRadius: Corner rounding radius. Default 0.
    /// - Returns: A `GeometryNode` containing a box mesh.
    public static func cube(
        size: Float = 1.0,
        color: SimpleMaterial.Color = .white,
        cornerRadius: Float = 0
    ) -> GeometryNode {
        let mesh = MeshResource.generateBox(
            size: size,
            cornerRadius: cornerRadius
        )
        let material = SimpleMaterial(color: color, isMetallic: false)
        let entity = ModelEntity(mesh: mesh, materials: [material])
        entity.generateCollisionShapes(recursive: false)
        return GeometryNode(entity: entity)
    }

    /// Creates a cube with PBR material.
    public static func cube(
        size: Float = 1.0,
        material: GeometryMaterial,
        cornerRadius: Float = 0
    ) -> GeometryNode {
        let mesh = MeshResource.generateBox(
            size: size,
            cornerRadius: cornerRadius
        )
        let entity = ModelEntity(mesh: mesh, materials: [material.rkMaterial])
        entity.generateCollisionShapes(recursive: false)
        return GeometryNode(entity: entity)
    }

    // MARK: - Sphere

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
        entity.generateCollisionShapes(recursive: false)
        return GeometryNode(entity: entity)
    }

    /// Creates a sphere with PBR material.
    public static func sphere(
        radius: Float = 0.5,
        material: GeometryMaterial
    ) -> GeometryNode {
        let mesh = MeshResource.generateSphere(radius: radius)
        let entity = ModelEntity(mesh: mesh, materials: [material.rkMaterial])
        entity.generateCollisionShapes(recursive: false)
        return GeometryNode(entity: entity)
    }

    // MARK: - Cylinder

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
        let mesh = MeshResource.generateCylinder(
            height: height,
            radius: radius
        )
        let material = SimpleMaterial(color: color, isMetallic: false)
        let entity = ModelEntity(mesh: mesh, materials: [material])
        entity.generateCollisionShapes(recursive: false)
        return GeometryNode(entity: entity)
    }

    // MARK: - Plane

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
        entity.generateCollisionShapes(recursive: false)
        return GeometryNode(entity: entity)
    }

    // MARK: - Cone

    /// Creates a cone geometry.
    ///
    /// - Parameters:
    ///   - height: Cone height in meters.
    ///   - radius: Base radius in meters.
    ///   - color: Simple material color.
    /// - Returns: A `GeometryNode` containing a cone mesh.
    public static func cone(
        height: Float = 1.0,
        radius: Float = 0.5,
        color: SimpleMaterial.Color = .white
    ) -> GeometryNode {
        let mesh = MeshResource.generateCone(
            height: height,
            radius: radius
        )
        let material = SimpleMaterial(color: color, isMetallic: false)
        let entity = ModelEntity(mesh: mesh, materials: [material])
        entity.generateCollisionShapes(recursive: false)
        return GeometryNode(entity: entity)
    }

    // MARK: - Transform helpers

    /// Returns self positioned at the given coordinates.
    @discardableResult
    public func position(_ position: SIMD3<Float>) -> GeometryNode {
        entity.position = position
        return self
    }

    /// Returns self scaled uniformly.
    @discardableResult
    public func scale(_ uniform: Float) -> GeometryNode {
        entity.scale = .init(repeating: uniform)
        return self
    }

    /// Returns self with a grounding shadow.
    @discardableResult
    public func withGroundingShadow() -> GeometryNode {
        #if os(iOS) || os(visionOS)
        entity.components.set(GroundingShadowComponent(castsShadow: true))
        #endif
        return self
    }
}

// MARK: - Material

/// Material configuration for geometry nodes.
///
/// Mirrors SceneView Android's MaterialInstance configuration.
/// Supports simple colors, PBR with texture maps, and unlit materials.
///
/// ```swift
/// // Simple color
/// let red = GeometryMaterial.simple(color: .red)
///
/// // PBR with color
/// let metal = GeometryMaterial.pbr(color: .gray, metallic: 1.0, roughness: 0.2)
///
/// // PBR with textures (load textures first)
/// let textured = GeometryMaterial.textured(
///     baseColor: albedoTexture,
///     normal: normalTexture,
///     metallic: 1.0,
///     roughness: 0.3
/// )
/// ```
public enum GeometryMaterial: Sendable {
    /// Simple non-metallic material.
    case simple(color: SimpleMaterial.Color)

    /// Physically-based rendering material with color tint.
    case pbr(
        color: SimpleMaterial.Color,
        metallic: Float = 0.0,
        roughness: Float = 0.5
    )

    /// PBR material with texture maps.
    ///
    /// - Parameters:
    ///   - baseColor: Albedo/diffuse texture.
    ///   - normal: Normal map texture (optional).
    ///   - metallic: Metallic value (0 = dielectric, 1 = metal).
    ///   - roughness: Roughness value (0 = smooth/mirror, 1 = rough/diffuse).
    ///   - tint: Color tint applied on top of the base texture.
    case textured(
        baseColor: TextureResource,
        normal: TextureResource? = nil,
        metallic: Float = 0.0,
        roughness: Float = 0.5,
        tint: SimpleMaterial.Color = .white
    )

    /// Unlit material (no lighting response).
    case unlit(color: SimpleMaterial.Color)

    /// Unlit material with a texture (no lighting response).
    case unlitTextured(texture: TextureResource, tint: SimpleMaterial.Color = .white)

    var rkMaterial: any RealityKit.Material {
        switch self {
        case .simple(let color):
            return SimpleMaterial(color: color, isMetallic: false)

        case .pbr(let color, let metallic, let roughness):
            var mat = SimpleMaterial()
            mat.color = .init(tint: color)
            mat.metallic = .init(floatLiteral: metallic)
            mat.roughness = .init(floatLiteral: roughness)
            return mat

        case .textured(let baseColor, let normal, let metallic, let roughness, let tint):
            var mat = SimpleMaterial()
            mat.color = .init(tint: tint, texture: .init(baseColor))
            mat.metallic = .init(floatLiteral: metallic)
            mat.roughness = .init(floatLiteral: roughness)
            if let normal = normal {
                mat.normal = .init(texture: .init(normal))
            }
            return mat

        case .unlit(let color):
            return UnlitMaterial(color: color)

        case .unlitTextured(let texture, let tint):
            var mat = UnlitMaterial()
            mat.color = .init(tint: tint, texture: .init(texture))
            return mat
        }
    }
}

// MARK: - Texture loading helpers

extension GeometryMaterial {
    /// Loads a texture from a bundle resource name.
    ///
    /// ```swift
    /// let texture = try await GeometryMaterial.loadTexture("textures/brick_diffuse.png")
    /// let material = GeometryMaterial.textured(baseColor: texture, roughness: 0.8)
    /// ```
    public static func loadTexture(_ name: String) async throws -> TextureResource {
        try await TextureResource(named: name)
    }

    /// Loads a texture from a URL.
    public static func loadTexture(contentsOf url: URL) async throws -> TextureResource {
        try await TextureResource(contentsOf: url)
    }
}

#endif // os(iOS) || os(visionOS)

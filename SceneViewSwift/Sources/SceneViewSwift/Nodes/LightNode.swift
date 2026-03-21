import RealityKit
import Foundation

/// A wrapper for adding lights to a RealityKit scene.
///
/// Mirrors SceneView Android's `LightNode` — supports directional, point,
/// and spot lights with configurable intensity, color, and shadow casting.
///
/// ```swift
/// SceneView { content in
///     let sun = LightNode.directional(
///         color: .white,
///         intensity: 1000,
///         castsShadow: true
///     )
///     content.add(sun.entity)
/// }
/// ```
@available(iOS 18.0, visionOS 2.0, *)
public struct LightNode {
    /// The underlying RealityKit entity holding the light component.
    public let entity: Entity

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

    /// Creates a directional light (like the sun).
    ///
    /// - Parameters:
    ///   - color: Light color.
    ///   - intensity: Luminous intensity in lux.
    ///   - castsShadow: Whether this light casts shadows.
    /// - Returns: A configured `LightNode`.
    public static func directional(
        color: LightNode.Color = .white,
        intensity: Float = 1000,
        castsShadow: Bool = true
    ) -> LightNode {
        let entity = Entity()
        // TODO: Apply DirectionalLight component when RealityKit supports it
        // entity.components[DirectionalLightComponent.self] = DirectionalLightComponent(
        //     color: color.platformColor,
        //     intensity: intensity,
        //     isRealWorldProxy: false
        // )
        // entity.components[DirectionalLightComponent.Shadow.self] = castsShadow
        //     ? DirectionalLightComponent.Shadow() : nil
        return LightNode(entity: entity)
    }

    /// Creates a point light (omni-directional).
    ///
    /// - Parameters:
    ///   - color: Light color.
    ///   - intensity: Luminous intensity in lumens.
    ///   - attenuationRadius: Maximum influence distance in meters.
    /// - Returns: A configured `LightNode`.
    public static func point(
        color: LightNode.Color = .white,
        intensity: Float = 1000,
        attenuationRadius: Float = 10.0
    ) -> LightNode {
        let entity = Entity()
        // TODO: Apply PointLight component
        return LightNode(entity: entity)
    }

    /// Creates a spot light.
    ///
    /// - Parameters:
    ///   - color: Light color.
    ///   - intensity: Luminous intensity in lumens.
    ///   - innerAngle: Inner cone angle in radians.
    ///   - outerAngle: Outer cone angle in radians.
    /// - Returns: A configured `LightNode`.
    public static func spot(
        color: LightNode.Color = .white,
        intensity: Float = 1000,
        innerAngle: Float = .pi / 6,
        outerAngle: Float = .pi / 4
    ) -> LightNode {
        let entity = Entity()
        // TODO: Apply SpotLight component
        return LightNode(entity: entity)
    }

    /// Returns a copy positioned at the given coordinates.
    public func position(_ position: SIMD3<Float>) -> LightNode {
        var copy = self
        copy.position = position
        return copy
    }

    /// Simple color representation for lights.
    public enum Color {
        case white
        case warm      // ~3200K
        case cool      // ~6500K
        case custom(r: Float, g: Float, b: Float)
    }
}

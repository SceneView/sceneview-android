#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

/// A node that renders a polyline path through multiple 3D points.
///
/// Mirrors SceneView Android's `PathNode` — connects a series of points
/// with line segments (thin cylinders).
///
/// ```swift
/// SceneView { root in
///     // Draw a triangle path
///     let path = PathNode(
///         points: [
///             .init(x: -0.5, y: 0, z: 0),
///             .init(x: 0.5, y: 0, z: 0),
///             .init(x: 0, y: 0.5, z: 0)
///         ],
///         closed: true,
///         color: .systemYellow
///     )
///     root.addChild(path.entity)
/// }
/// ```
public struct PathNode: Sendable {
    /// The parent entity containing all line segment entities.
    public let entity: Entity

    /// The points defining the path.
    public let points: [SIMD3<Float>]

    /// Creates a polyline path through the given points.
    ///
    /// - Parameters:
    ///   - points: Array of 3D points defining the path (minimum 2).
    ///   - closed: If true, connects the last point back to the first.
    ///   - thickness: Line thickness in meters. Default 0.005.
    ///   - color: Line color. Default white.
    public init(
        points: [SIMD3<Float>],
        closed: Bool = false,
        thickness: Float = 0.005,
        color: SimpleMaterial.Color = .white
    ) {
        self.points = points
        let container = Entity()

        guard points.count >= 2 else {
            self.entity = container
            return
        }

        // Create line segments between consecutive points
        for i in 0..<(points.count - 1) {
            let segment = LineNode(
                from: points[i],
                to: points[i + 1],
                thickness: thickness,
                color: color
            )
            container.addChild(segment.entity)
        }

        // Close the path if requested
        if closed, let first = points.first, let last = points.last {
            let closing = LineNode(
                from: last,
                to: first,
                thickness: thickness,
                color: color
            )
            container.addChild(closing.entity)
        }

        self.entity = container
    }

    /// Creates a circle path on the XZ plane.
    ///
    /// - Parameters:
    ///   - center: Center point.
    ///   - radius: Circle radius in meters.
    ///   - segments: Number of line segments (higher = smoother). Default 32.
    ///   - thickness: Line thickness. Default 0.005.
    ///   - color: Line color. Default white.
    public static func circle(
        center: SIMD3<Float> = .zero,
        radius: Float = 0.5,
        segments: Int = 32,
        thickness: Float = 0.005,
        color: SimpleMaterial.Color = .white
    ) -> PathNode {
        var points: [SIMD3<Float>] = []
        for i in 0..<segments {
            let angle = Float(i) / Float(segments) * 2 * .pi
            let x = center.x + radius * cos(angle)
            let z = center.z + radius * sin(angle)
            points.append(SIMD3<Float>(x, center.y, z))
        }
        return PathNode(
            points: points,
            closed: true,
            thickness: thickness,
            color: color
        )
    }

    /// Creates a grid on the XZ plane.
    ///
    /// - Parameters:
    ///   - size: Total grid size in meters.
    ///   - divisions: Number of divisions per axis.
    ///   - thickness: Line thickness. Default 0.003.
    ///   - color: Line color. Default gray.
    public static func grid(
        size: Float = 2.0,
        divisions: Int = 10,
        thickness: Float = 0.003,
        color: SimpleMaterial.Color = .gray
    ) -> PathNode {
        let container = Entity()
        let half = size / 2
        let step = size / Float(divisions)

        for i in 0...divisions {
            let offset = -half + step * Float(i)

            // Lines parallel to Z
            let zLine = LineNode(
                from: .init(x: offset, y: 0, z: -half),
                to: .init(x: offset, y: 0, z: half),
                thickness: thickness,
                color: color
            )
            container.addChild(zLine.entity)

            // Lines parallel to X
            let xLine = LineNode(
                from: .init(x: -half, y: 0, z: offset),
                to: .init(x: half, y: 0, z: offset),
                thickness: thickness,
                color: color
            )
            container.addChild(xLine.entity)
        }

        return PathNode(entity: container, points: [])
    }

    /// Internal init for factory methods.
    init(entity: Entity, points: [SIMD3<Float>]) {
        self.entity = entity
        self.points = points
    }

    /// Returns self positioned at the given coordinates.
    @discardableResult
    public func position(_ position: SIMD3<Float>) -> PathNode {
        entity.position = position
        return self
    }
}
#endif // os(iOS) || os(macOS) || os(visionOS)

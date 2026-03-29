#if os(iOS)
import RealityKit
import ARKit
import Foundation

/// Provides access to ARKit scene reconstruction mesh data.
///
/// Mirrors SceneView Android's `StreetscapeGeometryNode` — on Apple platforms,
/// this wraps ARKit's scene reconstruction (LiDAR mesh) to provide
/// real-world geometry as collidable entities in the scene.
///
/// Scene reconstruction requires a LiDAR-equipped device (iPhone 12 Pro+, iPad Pro).
///
/// ```swift
/// ARSceneView(planeDetection: .horizontal)
///     .onSessionStarted { arView in
///         SceneReconstructionNode.enableReconstruction(in: arView)
///     }
/// ```
public enum SceneReconstructionNode {

    /// Whether the device supports scene reconstruction (LiDAR).
    public static var isSupported: Bool {
        ARWorldTrackingConfiguration.supportsSceneReconstruction(.mesh)
    }

    /// Whether the device supports scene reconstruction with classification.
    public static var isClassificationSupported: Bool {
        ARWorldTrackingConfiguration.supportsSceneReconstruction(.meshWithClassification)
    }

    /// Enables scene reconstruction mesh on an AR session.
    ///
    /// The mesh is automatically added to the scene as collidable geometry.
    /// Requires a LiDAR-equipped device.
    ///
    /// - Parameters:
    ///   - arView: The ARView to enable reconstruction on.
    ///   - classification: Whether to enable mesh classification. Default false.
    public static func enableReconstruction(
        in arView: ARView,
        classification: Bool = false
    ) {
        guard isSupported else { return }

        let config = ARWorldTrackingConfiguration()
        config.sceneReconstruction = classification
            ? .meshWithClassification
            : .mesh
        config.environmentTexturing = .automatic
        config.planeDetection = [.horizontal, .vertical]

        arView.session.run(config)

        // Enable mesh visualization for debugging
        arView.debugOptions.insert(.showSceneUnderstanding)
    }

    /// Disables mesh visualization (the mesh is still active for occlusion).
    ///
    /// - Parameter arView: The ARView.
    public static func hideMeshVisualization(in arView: ARView) {
        arView.debugOptions.remove(.showSceneUnderstanding)
    }

    /// Enables occlusion so virtual objects are hidden behind real-world surfaces.
    ///
    /// - Parameter arView: The ARView.
    public static func enableOcclusion(in arView: ARView) {
        if #available(iOS 17.0, *) {
            arView.environment.sceneUnderstanding.options.insert(.occlusion)
        }
    }

    /// Enables physics interaction with the reconstruction mesh.
    ///
    /// Virtual objects will collide with real-world surfaces detected by LiDAR.
    ///
    /// - Parameter arView: The ARView.
    public static func enablePhysics(in arView: ARView) {
        if #available(iOS 17.0, *) {
            arView.environment.sceneUnderstanding.options.insert(.physics)
        }
    }

    /// Mesh classification types from ARKit scene reconstruction.
    public enum Classification: String, Sendable, CaseIterable {
        case wall
        case floor
        case ceiling
        case table
        case seat
        case window
        case door
        case none

        /// Converts from ARKit's `ARMeshClassification`.
        public static func from(_ arClassification: ARMeshClassification) -> Classification {
            switch arClassification {
            case .wall: return .wall
            case .floor: return .floor
            case .ceiling: return .ceiling
            case .table: return .table
            case .seat: return .seat
            case .window: return .window
            case .door: return .door
            case .none: return .none
            @unknown default: return .none
            }
        }
    }
}

#endif // os(iOS)

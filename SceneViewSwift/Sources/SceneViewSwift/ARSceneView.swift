#if os(iOS)
import SwiftUI
import RealityKit
import ARKit

/// A SwiftUI view for augmented reality using ARKit + RealityKit.
///
/// Mirrors SceneView Android's `ARScene { }` composable — place content
/// relative to real-world surfaces, images, and anchors.
///
/// Uses `ARView` (UIKit) wrapped in `UIViewRepresentable` for full ARKit
/// support on iPhone. Provides plane detection, tap-to-place hit testing,
/// and coaching overlay.
///
/// ```swift
/// ARSceneView(
///     planeDetection: .horizontal,
///     onTapOnPlane: { position in
///         // Place a 10 cm cube at the tapped surface
///         let cube = GeometryNode.cube(size: 0.1, color: .blue)
///         let anchor = AnchorNode.world(position: position)
///         anchor.add(cube.entity)
///         // arView.scene.addAnchor(anchor.entity) — done automatically
///     }
/// )
/// ```
public struct ARSceneView: UIViewRepresentable {
    private var planeDetection: PlaneDetectionMode
    private var showPlaneOverlay: Bool
    private var showCoachingOverlay: Bool
    private var onTapOnPlane: ((SIMD3<Float>, ARView) -> Void)?
    private var onSessionStarted: ((ARView) -> Void)?

    /// Plane detection modes matching Android's ARCore config.
    public enum PlaneDetectionMode: Sendable {
        case none
        case horizontal
        case vertical
        case both

        var arPlaneDetection: ARWorldTrackingConfiguration.PlaneDetection {
            switch self {
            case .none: return []
            case .horizontal: return .horizontal
            case .vertical: return .vertical
            case .both: return [.horizontal, .vertical]
            }
        }
    }

    /// Creates an AR scene with plane detection and tap-to-place.
    ///
    /// - Parameters:
    ///   - planeDetection: Which plane orientations to detect. Default horizontal.
    ///   - showPlaneOverlay: Whether to visualize detected planes. Default true.
    ///   - showCoachingOverlay: Whether to show coaching when tracking limited. Default true.
    ///   - onTapOnPlane: Called with (worldPosition, arView) when user taps on a plane.
    ///     Use the arView to add anchors: `arView.scene.addAnchor(anchor.entity)`.
    public init(
        planeDetection: PlaneDetectionMode = .horizontal,
        showPlaneOverlay: Bool = true,
        showCoachingOverlay: Bool = true,
        onTapOnPlane: ((SIMD3<Float>, ARView) -> Void)? = nil
    ) {
        self.planeDetection = planeDetection
        self.showPlaneOverlay = showPlaneOverlay
        self.showCoachingOverlay = showCoachingOverlay
        self.onTapOnPlane = onTapOnPlane
    }

    /// Called once when the AR session starts. Use to add initial content.
    public func onSessionStarted(
        _ handler: @escaping (ARView) -> Void
    ) -> ARSceneView {
        var copy = self
        copy.onSessionStarted = handler
        return copy
    }

    // MARK: - UIViewRepresentable

    public func makeUIView(context: Context) -> ARView {
        let arView = ARView(frame: .zero)
        arView.automaticallyConfigureSession = false

        // Configure AR session
        let config = ARWorldTrackingConfiguration()
        config.planeDetection = planeDetection.arPlaneDetection
        config.environmentTexturing = .automatic

        if ARWorldTrackingConfiguration.supportsSceneReconstruction(.mesh) {
            config.sceneReconstruction = .mesh
        }

        arView.session.run(config, options: [.resetTracking, .removeExistingAnchors])
        arView.session.delegate = context.coordinator

        // Plane visualization
        if showPlaneOverlay && planeDetection != .none {
            arView.debugOptions.insert(.showAnchorGeometry)
        }

        // Coaching overlay
        if showCoachingOverlay {
            let coaching = ARCoachingOverlayView()
            coaching.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            coaching.session = arView.session
            coaching.goal = coachingGoal
            coaching.activatesAutomatically = true
            arView.addSubview(coaching)
        }

        // Tap gesture
        let tapRecognizer = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleTap(_:))
        )
        arView.addGestureRecognizer(tapRecognizer)

        // Store reference for coordinator
        context.coordinator.arView = arView

        // Initial content callback
        onSessionStarted?(arView)

        return arView
    }

    public func updateUIView(_ arView: ARView, context: Context) {
        context.coordinator.onTapOnPlane = onTapOnPlane
    }

    public func makeCoordinator() -> Coordinator {
        Coordinator(onTapOnPlane: onTapOnPlane, planeDetection: planeDetection)
    }

    private var coachingGoal: ARCoachingOverlayView.Goal {
        switch planeDetection {
        case .horizontal: return .horizontalPlane
        case .vertical: return .verticalPlane
        case .both, .none: return .anyPlane
        }
    }

    // MARK: - Coordinator

    public class Coordinator: NSObject, ARSessionDelegate {
        var onTapOnPlane: ((SIMD3<Float>, ARView) -> Void)?
        var planeDetection: PlaneDetectionMode
        weak var arView: ARView?

        init(
            onTapOnPlane: ((SIMD3<Float>, ARView) -> Void)?,
            planeDetection: PlaneDetectionMode
        ) {
            self.onTapOnPlane = onTapOnPlane
            self.planeDetection = planeDetection
        }

        @objc func handleTap(_ recognizer: UITapGestureRecognizer) {
            guard let arView = recognizer.view as? ARView else { return }
            let location = recognizer.location(in: arView)

            // Raycast against detected planes
            let results = arView.raycast(
                from: location,
                allowing: .estimatedPlane,
                alignment: .any
            )
            if let firstResult = results.first {
                let column = firstResult.worldTransform.columns.3
                let position = SIMD3<Float>(column.x, column.y, column.z)
                onTapOnPlane?(position, arView)
            }
        }

        // MARK: - ARSessionDelegate

        public func session(
            _ session: ARSession,
            didFailWithError error: Error
        ) {
            print("[SceneViewSwift] AR session error: \(error.localizedDescription)")
        }

        public func sessionWasInterrupted(_ session: ARSession) {
            print("[SceneViewSwift] AR session interrupted")
        }

        public func sessionInterruptionEnded(_ session: ARSession) {
            print("[SceneViewSwift] AR session interruption ended — resuming")
            // Re-run session with the original plane detection config
            let config = ARWorldTrackingConfiguration()
            config.planeDetection = planeDetection.arPlaneDetection
            config.environmentTexturing = .automatic
            session.run(config)
        }
    }
}

// MARK: - AR Anchor helpers (mirrors Android's ARCore anchor API)

/// A wrapper around ARKit anchors for placing content in the real world.
///
/// Mirrors SceneView Android's `AnchorNode`.
public struct AnchorNode: Sendable {
    /// The underlying RealityKit anchor entity.
    public let entity: AnchorEntity

    /// Creates an anchor at a world position.
    public static func world(position: SIMD3<Float>) -> AnchorNode {
        let anchor = AnchorEntity(world: position)
        return AnchorNode(entity: anchor)
    }

    /// Creates an anchor on a detected plane.
    ///
    /// - Parameters:
    ///   - alignment: Horizontal or vertical plane.
    ///   - minimumBounds: Minimum plane size to anchor to.
    public static func plane(
        alignment: PlaneAlignment = .horizontal,
        minimumBounds: SIMD2<Float> = .init(0.1, 0.1)
    ) -> AnchorNode {
        let arAlignment: AnchorEntity.Alignment =
            alignment == .horizontal ? .horizontal : .vertical
        let anchor = AnchorEntity(
            plane: arAlignment,
            minimumBounds: minimumBounds
        )
        return AnchorNode(entity: anchor)
    }

    /// Adds a child entity to this anchor.
    public func add(_ child: Entity) {
        entity.addChild(child)
    }

    /// Removes a child entity from this anchor.
    public func remove(_ child: Entity) {
        entity.removeChild(child)
    }

    /// Removes all child entities from this anchor.
    public func removeAll() {
        for child in entity.children {
            entity.removeChild(child)
        }
    }

    /// Plane alignment type matching Android's Plane.Type.
    public enum PlaneAlignment: Sendable {
        case horizontal
        case vertical
    }
}
#endif // os(iOS)

#if os(iOS)
import RealityKit
import ARKit
import Foundation
import UIKit

/// Detects and tracks a face using ARKit's TrueDepth camera, providing a
/// real-time face mesh and blend shape coefficients in RealityKit.
///
/// Mirrors SceneView Android's `AugmentedFaceNode` — uses ARKit face tracking
/// (front camera, TrueDepth) to detect faces, build a renderable mesh, and
/// update it every frame while tracking.
///
/// **Requirements:**
/// - Device with TrueDepth camera (iPhone X+, iPad Pro 2018+)
/// - Front-facing camera (cannot run simultaneously with world tracking)
///
/// **Architecture note:** Face tracking uses `ARFaceTrackingConfiguration`,
/// which is mutually exclusive with `ARWorldTrackingConfiguration`.
/// Use `ARFaceSceneView` (not the standard `ARSceneView`) for face AR.
///
/// ```swift
/// // 1. Check support
/// guard AugmentedFaceNode.isSupported else { return }
///
/// // 2. Use ARFaceSceneView for face tracking
/// ARFaceSceneView { faceAnchor, arView in
///     // Create a face mesh node
///     let faceNode = AugmentedFaceNode(faceAnchor: faceAnchor)
///
///     // Optionally apply a texture (e.g., face paint)
///     if let texture = try? await TextureResource(named: "face_paint") {
///         faceNode.setTexture(texture)
///     }
///
///     arView.scene.anchors.append(faceNode.anchorEntity)
/// }
///
/// // 3. Access blend shapes for expressions
/// let smile = faceNode.blendShape(.mouthSmileLeft) ?? 0
/// ```
public class AugmentedFaceNode {

    /// Whether the device supports face tracking (TrueDepth camera).
    public static var isSupported: Bool {
        ARFaceTrackingConfiguration.isSupported
    }

    /// Whether the device supports world tracking with simultaneous face tracking.
    ///
    /// Available on devices with both rear and TrueDepth cameras (iPhone 12+).
    /// When supported, face tracking can run alongside world tracking.
    public static var isSimultaneousSupported: Bool {
        ARWorldTrackingConfiguration.supportsUserFaceTracking
    }

    /// The ARKit face anchor being tracked.
    public let faceAnchor: ARFaceAnchor

    /// The RealityKit anchor entity positioned at the face.
    public let anchorEntity: AnchorEntity

    /// The entity containing the face mesh geometry.
    ///
    /// Created lazily on the first tracking update with valid mesh data.
    /// Returns `nil` before the first successful mesh build.
    public private(set) var meshEntity: ModelEntity?

    /// Current tracking state of the face.
    public private(set) var trackingState: TrackingState = .notTracking

    /// Face tracking states matching Android's TrackingState.
    public enum TrackingState: Sendable {
        /// The face is being actively tracked.
        case tracking
        /// The face was detected but tracking quality is limited.
        case limited
        /// The face is no longer visible or tracked.
        case notTracking
    }

    /// Face region types, mirroring Android's `AugmentedFace.RegionType`.
    ///
    /// ARKit does not expose explicit region poses like ARCore. Instead,
    /// these are approximated using known vertex indices from the ARKit
    /// face mesh topology.
    public enum RegionType: CaseIterable, Sendable {
        /// Tip of the nose.
        case noseTip
        /// Left side of the forehead (from the tracked person's perspective).
        case foreheadLeft
        /// Right side of the forehead (from the tracked person's perspective).
        case foreheadRight
    }

    // Known vertex indices in ARKit's 1220-vertex face mesh topology.
    // These are stable across all ARKit versions.
    private static let noseTipVertexIndex = 9
    private static let foreheadLeftVertexIndex = 1066
    private static let foreheadRightVertexIndex = 1059

    /// Creates an AugmentedFaceNode from a detected ARFaceAnchor.
    ///
    /// - Parameter faceAnchor: The detected face anchor from ARKit.
    public init(faceAnchor: ARFaceAnchor) {
        self.faceAnchor = faceAnchor
        self.anchorEntity = AnchorEntity(anchor: faceAnchor)
    }

    // MARK: - Mesh generation

    /// Builds or updates the face mesh from the current face geometry.
    ///
    /// Call this from the AR session delegate's `didUpdate` callback to
    /// keep the mesh synchronized with the tracked face.
    ///
    /// - Parameter material: Optional RealityKit material for the mesh.
    ///   If nil, uses a transparent occlusion material.
    @MainActor
    public func updateMesh(material: (any RealityKit.Material)? = nil) {
        let geometry = faceAnchor.geometry

        // Build MeshDescriptor from ARKit face geometry
        var descriptor = MeshDescriptor(name: "faceMesh")

        // Vertices: [vector_float3] -> [SIMD3<Float>]
        let vertexCount = geometry.vertexCount
        var positions: [SIMD3<Float>] = []
        positions.reserveCapacity(vertexCount)
        for i in 0..<vertexCount {
            let v = geometry.vertices[i]
            positions.append(SIMD3<Float>(v.x, v.y, v.z))
        }
        descriptor.positions = MeshBuffers.Positions(positions)

        // Normals: [vector_float3] -> [SIMD3<Float>]
        var normals: [SIMD3<Float>] = []
        normals.reserveCapacity(vertexCount)
        for i in 0..<vertexCount {
            let n = geometry.normals[i]
            normals.append(SIMD3<Float>(n.x, n.y, n.z))
        }
        descriptor.normals = MeshBuffers.Normals(normals)

        // Texture coordinates: [vector_float2] -> [SIMD2<Float>]
        var uvs: [SIMD2<Float>] = []
        uvs.reserveCapacity(vertexCount)
        for i in 0..<vertexCount {
            let uv = geometry.textureCoordinates[i]
            uvs.append(SIMD2<Float>(uv.x, uv.y))
        }
        descriptor.textureCoordinates = MeshBuffers.TextureCoordinates(uvs)

        // Triangle indices: [Int16] -> [UInt32]
        let indexCount = geometry.triangleCount * 3
        var indices: [UInt32] = []
        indices.reserveCapacity(indexCount)
        for i in 0..<indexCount {
            indices.append(UInt32(geometry.triangleIndices[i]))
        }
        descriptor.primitives = .triangles(indices)

        do {
            let meshResource = try MeshResource.generate(from: [descriptor])

            if let existing = meshEntity {
                // Update existing mesh in place (iOS 15+)
                if #available(iOS 15.0, *) {
                    try existing.model?.mesh.replace(with: meshResource.contents)
                } else {
                    // Fallback: recreate the entity on older iOS
                    existing.removeFromParent()
                    let mat = material ?? SimpleMaterial(color: .white.withAlphaComponent(0.0), isMetallic: false)
                    let entity = ModelEntity(mesh: meshResource, materials: [mat])
                    entity.name = "faceMesh"
                    anchorEntity.addChild(entity)
                    meshEntity = entity
                }
            } else {
                // Create new entity with the mesh
                let mat = material ?? SimpleMaterial(
                    color: .white.withAlphaComponent(0.0),
                    isMetallic: false
                )
                let entity = ModelEntity(mesh: meshResource, materials: [mat])
                entity.name = "faceMesh"
                anchorEntity.addChild(entity)
                meshEntity = entity
            }
        } catch {
            print("[SceneViewSwift] Failed to generate face mesh: \(error)")
        }
    }

    // MARK: - Material

    /// Sets a texture on the face mesh (e.g., face paint, mask overlay).
    ///
    /// - Parameter texture: A `TextureResource` to apply as the base color.
    public func setTexture(_ texture: TextureResource) {
        guard let meshEntity = meshEntity else { return }
        var material = SimpleMaterial()
        material.color = .init(tint: .white, texture: .init(texture))
        meshEntity.model?.materials = [material]
    }

    /// Sets a simple color material on the face mesh.
    ///
    /// - Parameter color: The color to apply.
    /// - Parameter opacity: Opacity (0-1). Default 1.0.
    public func setColor(_ color: UIColor, opacity: Float = 1.0) {
        guard let meshEntity = meshEntity else { return }
        var material = SimpleMaterial()
        material.color = .init(tint: color.withAlphaComponent(CGFloat(opacity)))
        meshEntity.model?.materials = [material]
    }

    /// Sets an occlusion material so the face mesh hides virtual content behind it.
    ///
    /// Useful for realistic face filter effects where virtual objects should
    /// appear occluded by the face geometry.
    public func setOcclusionMaterial() {
        guard let meshEntity = meshEntity else { return }
        meshEntity.model?.materials = [OcclusionMaterial()]
    }

    // MARK: - Blend shapes

    /// Returns the coefficient (0.0–1.0) for a specific blend shape.
    ///
    /// Blend shapes describe facial expressions (smile, blink, jaw open, etc.).
    /// Mirrors the expression data from Android's `AugmentedFace` but with
    /// Apple's more detailed 52-coefficient blend shape system.
    ///
    /// - Parameter location: The blend shape location (e.g., `.mouthSmileLeft`).
    /// - Returns: The coefficient value, or nil if not available.
    public func blendShape(
        _ location: ARFaceAnchor.BlendShapeLocation
    ) -> Float? {
        faceAnchor.blendShapes[location]?.floatValue
    }

    /// Returns all current blend shape coefficients as a dictionary.
    ///
    /// Contains up to 52 blend shape values describing the full facial expression.
    public var blendShapes: [ARFaceAnchor.BlendShapeLocation: NSNumber] {
        faceAnchor.blendShapes
    }

    // MARK: - Region poses

    /// Returns the world-space position of a face region.
    ///
    /// Approximates Android's `AugmentedFace.getRegionPose()` using known
    /// vertex indices from the ARKit face mesh topology.
    ///
    /// - Parameter region: The face region to query.
    /// - Returns: The position in face-local coordinates, or nil if mesh not available.
    public func regionPosition(_ region: RegionType) -> SIMD3<Float>? {
        let geometry = faceAnchor.geometry
        guard geometry.vertexCount > 0 else { return nil }

        let index: Int
        switch region {
        case .noseTip:
            index = Self.noseTipVertexIndex
        case .foreheadLeft:
            index = Self.foreheadLeftVertexIndex
        case .foreheadRight:
            index = Self.foreheadRightVertexIndex
        }

        guard index < geometry.vertexCount else { return nil }
        let v = geometry.vertices[index]
        return SIMD3<Float>(v.x, v.y, v.z)
    }

    /// Returns world-space positions for all face regions.
    public var regionPositions: [RegionType: SIMD3<Float>] {
        var result: [RegionType: SIMD3<Float>] = [:]
        for region in RegionType.allCases {
            if let pos = regionPosition(region) {
                result[region] = pos
            }
        }
        return result
    }

    // MARK: - Face center pose

    /// The face's center transform (world-space).
    ///
    /// Equivalent to Android's `AugmentedFace.getCenterPose()`.
    /// The origin is behind the nose, between the cheek bones.
    public var centerTransform: simd_float4x4 {
        faceAnchor.transform
    }

    /// The face's center position in world coordinates.
    public var centerPosition: SIMD3<Float> {
        let t = faceAnchor.transform.columns.3
        return SIMD3<Float>(t.x, t.y, t.z)
    }

    // MARK: - Child content

    /// Adds a child entity to the face anchor (moves with the face).
    ///
    /// Use this to attach 3D content (hats, glasses, masks) to the face.
    public func add(_ child: Entity) {
        anchorEntity.addChild(child)
    }

    /// Removes a child entity from the face anchor.
    public func remove(_ child: Entity) {
        anchorEntity.removeChild(child)
    }

    /// Removes all child content from the face anchor.
    public func removeAll() {
        for child in anchorEntity.children {
            anchorEntity.removeChild(child)
        }
    }

    // MARK: - Tracking state conversion

    /// Converts the ARFaceAnchor's tracking state to the simplified enum.
    public static func trackingState(for anchor: ARFaceAnchor) -> TrackingState {
        if anchor.isTracked {
            return .tracking
        } else {
            return .notTracking
        }
    }
}

// MARK: - ARFaceSceneView

/// A SwiftUI view for face-tracking AR using ARKit + RealityKit.
///
/// Uses `ARFaceTrackingConfiguration` (front camera, TrueDepth) instead of
/// `ARWorldTrackingConfiguration` (rear camera). Mutually exclusive with
/// the standard `ARSceneView`.
///
/// ```swift
/// ARFaceSceneView(
///     onFaceDetected: { faceAnchor, arView in
///         let faceNode = AugmentedFaceNode(faceAnchor: faceAnchor)
///         faceNode.updateMesh(material: SimpleMaterial(color: .blue.withAlphaComponent(0.3), isMetallic: false))
///         arView.scene.anchors.append(faceNode.anchorEntity)
///     },
///     onFaceUpdated: { faceAnchor, faceNode in
///         faceNode.updateMesh()
///         let smile = faceNode.blendShape(.mouthSmileLeft) ?? 0
///         print("Smile: \(smile)")
///     }
/// )
/// ```
public struct ARFaceSceneView: UIViewRepresentable {
    private var onFaceDetected: ((ARFaceAnchor, ARView) -> AugmentedFaceNode?)?
    private var onFaceUpdated: ((ARFaceAnchor, AugmentedFaceNode) -> Void)?
    private var onFaceLost: ((AugmentedFaceNode) -> Void)?
    private var maxFaces: Int

    /// Creates a face-tracking AR scene.
    ///
    /// - Parameters:
    ///   - maxFaces: Maximum number of faces to track simultaneously. Default 1.
    ///     ARKit supports up to 3 on A12+ devices.
    ///   - onFaceDetected: Called when a new face is detected. Return an
    ///     `AugmentedFaceNode` to track it, or nil to ignore.
    ///   - onFaceUpdated: Called each frame while a face is tracked.
    ///   - onFaceLost: Called when a face is no longer tracked.
    public init(
        maxFaces: Int = 1,
        onFaceDetected: ((ARFaceAnchor, ARView) -> AugmentedFaceNode?)? = nil,
        onFaceUpdated: ((ARFaceAnchor, AugmentedFaceNode) -> Void)? = nil,
        onFaceLost: ((AugmentedFaceNode) -> Void)? = nil
    ) {
        self.maxFaces = maxFaces
        self.onFaceDetected = onFaceDetected
        self.onFaceUpdated = onFaceUpdated
        self.onFaceLost = onFaceLost
    }

    public func makeUIView(context: Context) -> ARView {
        let arView = ARView(frame: .zero)
        arView.automaticallyConfigureSession = false

        let config = ARFaceTrackingConfiguration()
        config.maximumNumberOfTrackedFaces = maxFaces
        if ARFaceTrackingConfiguration.supportsWorldTracking {
            config.isWorldTrackingEnabled = true
        }

        arView.session.run(config, options: [.resetTracking, .removeExistingAnchors])
        arView.session.delegate = context.coordinator
        context.coordinator.arView = arView

        return arView
    }

    public func updateUIView(_ arView: ARView, context: Context) {
        context.coordinator.onFaceDetected = onFaceDetected
        context.coordinator.onFaceUpdated = onFaceUpdated
        context.coordinator.onFaceLost = onFaceLost
    }

    public func makeCoordinator() -> Coordinator {
        Coordinator(
            onFaceDetected: onFaceDetected,
            onFaceUpdated: onFaceUpdated,
            onFaceLost: onFaceLost
        )
    }

    // MARK: - Coordinator

    public class Coordinator: NSObject, ARSessionDelegate {
        var onFaceDetected: ((ARFaceAnchor, ARView) -> AugmentedFaceNode?)?
        var onFaceUpdated: ((ARFaceAnchor, AugmentedFaceNode) -> Void)?
        var onFaceLost: ((AugmentedFaceNode) -> Void)?
        weak var arView: ARView?

        /// Tracked face nodes keyed by the ARFaceAnchor's UUID.
        private var trackedFaces: [UUID: AugmentedFaceNode] = [:]

        init(
            onFaceDetected: ((ARFaceAnchor, ARView) -> AugmentedFaceNode?)?,
            onFaceUpdated: ((ARFaceAnchor, AugmentedFaceNode) -> Void)?,
            onFaceLost: ((AugmentedFaceNode) -> Void)?
        ) {
            self.onFaceDetected = onFaceDetected
            self.onFaceUpdated = onFaceUpdated
            self.onFaceLost = onFaceLost
        }

        public func session(_ session: ARSession, didAdd anchors: [ARAnchor]) {
            guard let arView = arView else { return }
            for anchor in anchors {
                guard let faceAnchor = anchor as? ARFaceAnchor else { continue }
                if let node = onFaceDetected?(faceAnchor, arView) {
                    node.trackingState = .tracking
                    trackedFaces[faceAnchor.identifier] = node
                }
            }
        }

        public func session(_ session: ARSession, didUpdate anchors: [ARAnchor]) {
            for anchor in anchors {
                guard let faceAnchor = anchor as? ARFaceAnchor,
                      let node = trackedFaces[faceAnchor.identifier] else { continue }

                if faceAnchor.isTracked {
                    node.trackingState = .tracking
                    onFaceUpdated?(faceAnchor, node)
                } else {
                    node.trackingState = .limited
                }
            }
        }

        public func session(_ session: ARSession, didRemove anchors: [ARAnchor]) {
            for anchor in anchors {
                guard let faceAnchor = anchor as? ARFaceAnchor,
                      let node = trackedFaces.removeValue(forKey: faceAnchor.identifier)
                else { continue }

                node.trackingState = .notTracking
                onFaceLost?(node)
            }
        }

        public func session(
            _ session: ARSession,
            didFailWithError error: Error
        ) {
            print("[SceneViewSwift] Face tracking session error: \(error.localizedDescription)")
        }
    }
}

#endif // os(iOS)

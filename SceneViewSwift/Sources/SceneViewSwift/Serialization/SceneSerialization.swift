#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit
import Foundation

/// Serializable snapshot of a node's transform.
///
/// Used by ``SceneSerializer`` to save and restore scene state.
public struct NodeSnapshot: Codable, Sendable {
    /// Unique identifier for the node.
    public let name: String

    /// Position in world space.
    public let position: SIMD3CodableFloat

    /// Rotation as a quaternion (x, y, z, w).
    public let rotation: SIMD4CodableFloat

    /// Scale.
    public let scale: SIMD3CodableFloat

    /// Whether the entity is enabled.
    public let isEnabled: Bool

    /// Creates a snapshot from an entity.
    public init(entity: Entity) {
        self.name = entity.name
        self.position = SIMD3CodableFloat(entity.position)
        self.rotation = SIMD4CodableFloat(entity.orientation.vector)
        self.scale = SIMD3CodableFloat(entity.scale)
        self.isEnabled = entity.isEnabled
    }

    /// Applies this snapshot's transform to an entity.
    public func apply(to entity: Entity) {
        entity.position = position.simd
        entity.orientation = simd_quatf(vector: rotation.simd4)
        entity.scale = scale.simd
        entity.isEnabled = isEnabled
    }
}

/// Codable wrapper for SIMD3<Float>.
public struct SIMD3CodableFloat: Codable, Sendable {
    public let x: Float
    public let y: Float
    public let z: Float

    public init(_ v: SIMD3<Float>) {
        self.x = v.x
        self.y = v.y
        self.z = v.z
    }

    public var simd: SIMD3<Float> {
        SIMD3<Float>(x, y, z)
    }
}

/// Codable wrapper for SIMD4<Float> (quaternion storage).
public struct SIMD4CodableFloat: Codable, Sendable {
    public let x: Float
    public let y: Float
    public let z: Float
    public let w: Float

    public init(_ v: SIMD4<Float>) {
        self.x = v.x
        self.y = v.y
        self.z = v.z
        self.w = v.w
    }

    public var simd4: SIMD4<Float> {
        SIMD4<Float>(x, y, z, w)
    }
}

/// Serializable scene state containing all node transforms.
///
/// ```swift
/// // Save
/// let state = SceneState(root: rootEntity)
/// let data = try SceneSerializer.encode(state)
/// try data.write(to: fileURL)
///
/// // Restore
/// let loaded = try SceneSerializer.decode(from: data)
/// loaded.apply(to: rootEntity)
/// ```
public struct SceneState: Codable, Sendable {
    /// The saved node transforms.
    public let nodes: [NodeSnapshot]

    /// Timestamp when the state was captured.
    public let timestamp: Date

    /// Application-defined metadata.
    public var metadata: [String: String]

    /// Creates a scene state by traversing the entity hierarchy.
    ///
    /// - Parameters:
    ///   - root: The root entity to snapshot.
    ///   - metadata: Optional application-defined metadata.
    public init(root: Entity, metadata: [String: String] = [:]) {
        var snapshots: [NodeSnapshot] = []
        SceneState.traverse(root, into: &snapshots)
        self.nodes = snapshots
        self.timestamp = Date()
        self.metadata = metadata
    }

    /// Applies the saved transforms to matching entities in the hierarchy.
    ///
    /// Matches entities by name. Entities without matching snapshots are unchanged.
    ///
    /// - Parameter root: The root entity to apply transforms to.
    public func apply(to root: Entity) {
        let snapshotMap = Dictionary(grouping: nodes, by: { $0.name })
        SceneState.applyTraverse(root, snapshotMap: snapshotMap)
    }

    // MARK: - Private

    private static func traverse(_ entity: Entity, into snapshots: inout [NodeSnapshot]) {
        if !entity.name.isEmpty {
            snapshots.append(NodeSnapshot(entity: entity))
        }
        for child in entity.children {
            traverse(child, into: &snapshots)
        }
    }

    private static func applyTraverse(
        _ entity: Entity,
        snapshotMap: [String: [NodeSnapshot]]
    ) {
        if !entity.name.isEmpty, let matching = snapshotMap[entity.name]?.first {
            matching.apply(to: entity)
        }
        for child in entity.children {
            applyTraverse(child, snapshotMap: snapshotMap)
        }
    }
}

/// Encodes and decodes scene state for persistence.
///
/// Mirrors the concept of scene serialization — save the full scene transform
/// state to JSON and restore it later.
///
/// ```swift
/// // Save scene
/// let state = SceneState(root: rootEntity)
/// let data = try SceneSerializer.encode(state)
///
/// // Load scene
/// let restored = try SceneSerializer.decode(from: data)
/// restored.apply(to: rootEntity)
/// ```
public enum SceneSerializer {

    /// Encodes a scene state to JSON data.
    ///
    /// - Parameter state: The scene state to encode.
    /// - Returns: JSON-encoded data.
    /// - Throws: `EncodingError` if encoding fails.
    public static func encode(_ state: SceneState) throws -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(state)
    }

    /// Decodes a scene state from JSON data.
    ///
    /// - Parameter data: JSON-encoded scene state.
    /// - Returns: The decoded scene state.
    /// - Throws: `DecodingError` if decoding fails.
    public static func decode(from data: Data) throws -> SceneState {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try decoder.decode(SceneState.self, from: data)
    }

    /// Encodes a scene state to a JSON string.
    ///
    /// - Parameter state: The scene state to encode.
    /// - Returns: A JSON string representation.
    /// - Throws: `EncodingError` if encoding fails.
    public static func encodeToString(_ state: SceneState) throws -> String {
        let data = try encode(state)
        guard let string = String(data: data, encoding: .utf8) else {
            throw SceneSerializerError.encodingFailed
        }
        return string
    }

    /// Decodes a scene state from a JSON string.
    ///
    /// - Parameter string: A JSON string representation of scene state.
    /// - Returns: The decoded scene state.
    /// - Throws: `DecodingError` if decoding fails.
    public static func decode(from string: String) throws -> SceneState {
        guard let data = string.data(using: .utf8) else {
            throw SceneSerializerError.invalidString
        }
        return try decode(from: data)
    }
}

/// Errors from scene serialization operations.
public enum SceneSerializerError: LocalizedError {
    case encodingFailed
    case invalidString

    public var errorDescription: String? {
        switch self {
        case .encodingFailed:
            return "Failed to encode scene state to UTF-8 string."
        case .invalidString:
            return "The provided string is not valid UTF-8."
        }
    }
}

#endif // os(iOS) || os(macOS) || os(visionOS)

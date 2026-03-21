import RealityKit
import Foundation

/// Image-based lighting and skybox configuration for a 3D scene.
///
/// Mirrors SceneView Android's `Environment` — provides preset environments
/// and custom HDR loading.
///
/// ```swift
/// SceneView { content in
///     // ...
/// }
/// .environment(.studio)
/// ```
public struct SceneEnvironment {
    /// Display name for UI pickers.
    public let name: String

    /// Bundle resource name for the HDR/EXR file.
    public let hdrResource: String?

    /// Light intensity multiplier.
    public var intensity: Float

    /// Whether to render the environment as a skybox background.
    public var showSkybox: Bool

    public init(
        name: String,
        hdrResource: String? = nil,
        intensity: Float = 1.0,
        showSkybox: Bool = true
    ) {
        self.name = name
        self.hdrResource = hdrResource
        self.intensity = intensity
        self.showSkybox = showSkybox
    }

    /// Loads the IBL environment resource into a RealityKit scene.
    ///
    /// - Returns: An `EnvironmentResource` ready to apply.
    /// - Throws: If the HDR file cannot be loaded.
    public func load() async throws -> EnvironmentResource {
        // TODO: Support EXR files alongside HDR
        // TODO: Cache loaded environments
        // TODO: Apply intensity multiplier to the IBL

        if let hdrResource {
            return try await EnvironmentResource(named: hdrResource)
        } else {
            // Fallback: RealityKit's default environment
            return try await EnvironmentResource(named: "default")
        }
    }
}

// MARK: - Presets (mirrors Android's bundled HDR environments)

extension SceneEnvironment {
    /// Neutral studio lighting — good default for model viewing.
    public static let studio = SceneEnvironment(
        name: "Studio",
        hdrResource: "studio.hdr",
        intensity: 1.0
    )

    /// Warm outdoor daylight.
    public static let outdoor = SceneEnvironment(
        name: "Outdoor",
        hdrResource: "outdoor_cloudy.hdr",
        intensity: 1.2
    )

    /// Golden hour sunset lighting.
    public static let sunset = SceneEnvironment(
        name: "Sunset",
        hdrResource: "sunset.hdr",
        intensity: 0.8
    )

    // TODO: Add .night, .warm, .autumn presets matching Android SDK
}

import SwiftUI

/// About tab showing SDK information, features, platforms, and links.
struct AboutTab: View {
    var body: some View {
        NavigationStack {
            List {
                heroSection
                featuresSection
                nodeTypesSection
                platformsSection
                linksSection
                creditsSection
            }
            .navigationTitle("About")
        }
    }

    // MARK: - Sections

    private var heroSection: some View {
        Section {
            VStack(spacing: 12) {
                Image(systemName: "cube.fill")
                    .font(.system(size: 56))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [.blue, .purple],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .accessibilityHidden(true)

                Text("SceneView")
                    .font(.title).bold()

                Text("v3.4.7")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(.fill.tertiary)
                    .clipShape(Capsule())

                Text("3D & AR as declarative UI\nfor iOS, macOS, visionOS & more")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)
        }
    }

    private var featuresSection: some View {
        Section("Features") {
            FeatureRow(
                icon: "cube.fill",
                title: "3D Scenes",
                description: "RealityKit with SwiftUI integration",
                color: .blue
            )
            FeatureRow(
                icon: "arkit",
                title: "Augmented Reality",
                description: "ARKit plane detection, tap-to-place, image tracking",
                color: .green
            )
            FeatureRow(
                icon: "sparkles",
                title: "AI-First SDK",
                description: "Designed for AI-assisted code generation",
                color: .purple
            )
            FeatureRow(
                icon: "swift",
                title: "Swift Package",
                description: "One-line SPM integration, iOS 18+",
                color: .orange
            )
            FeatureRow(
                icon: "atom",
                title: "16+ Node Types",
                description: "Geometry, text, lights, physics, fog & more",
                color: .cyan
            )
        }
    }

    private var nodeTypesSection: some View {
        Section("Node Types") {
            NodeTypeRow(name: "GeometryNode", detail: "Cube, sphere, cylinder, cone, plane")
            NodeTypeRow(name: "TextNode", detail: "3D extruded text with custom fonts")
            NodeTypeRow(name: "BillboardNode", detail: "Camera-facing labels and icons")
            NodeTypeRow(name: "LineNode", detail: "Line segments between 3D points")
            NodeTypeRow(name: "PathNode", detail: "Polylines, circles, grids")
            NodeTypeRow(name: "ImageNode", detail: "Textured planes and color fills")
            NodeTypeRow(name: "VideoNode", detail: "AVPlayer video on 3D surfaces")
            NodeTypeRow(name: "ModelNode", detail: "USDZ model loading with animations")
            NodeTypeRow(name: "LightNode", detail: "Directional, point & spot lights")
            NodeTypeRow(name: "DynamicSkyNode", detail: "Time-of-day sun simulation")
            NodeTypeRow(name: "FogNode", detail: "Linear, exponential & height fog")
            NodeTypeRow(name: "PhysicsNode", detail: "Dynamic, static & kinematic bodies")
            NodeTypeRow(name: "CameraNode", detail: "Programmatic camera viewpoints")
            NodeTypeRow(name: "MeshNode", detail: "Custom vertex geometry")
            NodeTypeRow(name: "ReflectionProbeNode", detail: "Local environment reflections")
            NodeTypeRow(name: "AugmentedImageNode", detail: "Real-world image detection")
        }
    }

    private var platformsSection: some View {
        Section("Platforms") {
            PlatformRow(name: "iOS", detail: "RealityKit + SwiftUI", icon: "iphone")
            PlatformRow(name: "macOS", detail: "RealityKit + SwiftUI", icon: "macbook")
            PlatformRow(name: "visionOS", detail: "RealityKit + SwiftUI", icon: "visionpro")
            PlatformRow(name: "Android", detail: "Filament + Compose", icon: "candybarphone")
            PlatformRow(name: "Web", detail: "Filament.js (WASM)", icon: "globe")
        }
    }

    private var linksSection: some View {
        Section("Links") {
            Link(destination: URL(string: "https://github.com/sceneview/sceneview")!) {
                Label("GitHub Repository", systemImage: "chevron.left.forwardslash.chevron.right")
            }
            .accessibilityLabel("Open SceneView on GitHub")

            Link(destination: URL(string: "https://sceneview.github.io")!) {
                Label("Website", systemImage: "globe")
            }
            .accessibilityLabel("Open SceneView website")

            Link(destination: URL(string: "https://sceneview.github.io/playground.html")!) {
                Label("3D Playground", systemImage: "play.fill")
            }
            .accessibilityLabel("Open SceneView 3D Playground")
        }
    }

    private var creditsSection: some View {
        Section("Credits") {
            Text("Built with RealityKit, ARKit, and SwiftUI.")
                .font(.caption)
                .foregroundStyle(.secondary)
            Text("Open source under Apache License 2.0.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - Row components

private struct FeatureRow: View {
    let icon: String
    let title: String
    let description: String
    let color: Color

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(color)
                .frame(width: 32)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline).bold()
                Text(description).font(.caption).foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

private struct PlatformRow: View {
    let name: String
    let detail: String
    let icon: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.body)
                .foregroundStyle(.secondary)
                .frame(width: 24)
                .accessibilityHidden(true)
            Text(name).font(.subheadline)
            Spacer()
            Text(detail).font(.caption).foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(name): \(detail)")
    }
}

private struct NodeTypeRow: View {
    let name: String
    let detail: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(name)
                .font(.subheadline)
                .fontDesign(.monospaced)
            Text(detail)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(name): \(detail)")
    }
}

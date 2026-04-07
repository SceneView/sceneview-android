import SwiftUI

/// About tab -- app information, features, and links.
struct AboutTab: View {
    var body: some View {
        NavigationStack {
            List {
                heroSection
                featuresSection
                capabilitiesSection
                tipsSection
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

                Text("3D & AR Explorer")
                    .font(.title).bold()

                Text("v\(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0")")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(.fill.tertiary)
                    .clipShape(Capsule())

                Text("Explore stunning 3D models and place them\nin your real-world space with augmented reality.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)
        }
    }

    private var featuresSection: some View {
        Section("What You Can Do") {
            FeatureRow(
                icon: "cube.fill",
                title: "Browse 3D Models",
                description: "Explore a curated gallery of vehicles, creatures, objects, and scenes",
                color: .blue
            )
            FeatureRow(
                icon: "arkit",
                title: "Augmented Reality",
                description: "Place any model in your real-world space using your camera",
                color: .green
            )
            FeatureRow(
                icon: "heart.fill",
                title: "Save Favorites",
                description: "Mark your favorite models for quick access anytime",
                color: .red
            )
            FeatureRow(
                icon: "square.and.arrow.up",
                title: "Share",
                description: "Capture and share screenshots of your 3D and AR scenes",
                color: .orange
            )
            FeatureRow(
                icon: "hand.draw.fill",
                title: "Interactive Controls",
                description: "Pinch to zoom, drag to orbit, and rotate models freely",
                color: .purple
            )
        }
    }

    private var capabilitiesSection: some View {
        Section("3D Capabilities") {
            CapabilityRow(name: "3D Models", detail: "USDZ format with animations")
            CapabilityRow(name: "Materials", detail: "Physically-based rendering (PBR)")
            CapabilityRow(name: "Lighting", detail: "Directional, point & spot lights")
            CapabilityRow(name: "Environments", detail: "HDR image-based lighting presets")
            CapabilityRow(name: "Physics", detail: "Dynamic, static & kinematic bodies")
            CapabilityRow(name: "Custom Geometry", detail: "Procedural shapes and meshes")
        }
    }

    private var tipsSection: some View {
        Section("Tips") {
            TipRow(
                icon: "hand.pinch.fill",
                text: "Pinch with two fingers to zoom in and out on 3D models"
            )
            TipRow(
                icon: "hand.draw.fill",
                text: "Drag to orbit around the model and see it from every angle"
            )
            TipRow(
                icon: "iphone.radiowaves.left.and.right",
                text: "In AR mode, slowly scan your surroundings to detect surfaces"
            )
            TipRow(
                icon: "hand.tap.fill",
                text: "Tap any detected surface to place the selected model"
            )
        }
    }

    private var linksSection: some View {
        Section("Learn More") {
            Link(destination: URL(string: "https://sceneview.github.io")!) {
                Label("Website", systemImage: "globe")
            }
            .accessibilityLabel("Visit website")

            Link(destination: URL(string: "https://sceneview.github.io/playground.html")!) {
                Label("3D Playground", systemImage: "play.fill")
            }
            .accessibilityLabel("Open 3D Playground in browser")

            Link(destination: URL(string: "https://github.com/sceneview/sceneview")!) {
                Label("Open Source on GitHub", systemImage: "chevron.left.forwardslash.chevron.right")
            }
            .accessibilityLabel("View source code on GitHub")
        }
    }

    private var creditsSection: some View {
        Section {
            Text("3D & AR Explorer \u{2014} Powered by SceneView")
                .font(.caption)
                .foregroundStyle(.secondary)
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

private struct CapabilityRow: View {
    let name: String
    let detail: String

    var body: some View {
        HStack {
            Text(name)
                .font(.subheadline)
            Spacer()
            Text(detail)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(name): \(detail)")
    }
}

private struct TipRow: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.body)
                .foregroundStyle(.blue)
                .frame(width: 24)
                .accessibilityHidden(true)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
    }
}

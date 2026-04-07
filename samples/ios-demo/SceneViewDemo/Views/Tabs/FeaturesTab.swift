import SwiftUI
import SceneViewSwift

/// Features tab -- showcases all SceneView iOS node types with live demos.
///
/// Organized by category: Geometry, Lighting, Effects, Content, Advanced.
/// Each demo is a full-screen 3D scene with interactive controls.
struct FeaturesTab: View {
    private let features: [DemoItem] = Self.allFeatures()

    var body: some View {
        NavigationStack {
            List {
                // Version hero
                Section {
                    HStack(spacing: 12) {
                        Image(systemName: "cube.fill")
                            .font(.system(size: 36))
                            .foregroundStyle(
                                LinearGradient(
                                    colors: [.blue, .purple],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .accessibilityHidden(true)

                        VStack(alignment: .leading, spacing: 4) {
                            Text("SceneView Features")
                                .font(.headline)
                            Text("v3.6.1 -- \(features.count) demos")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }

                        Spacer()

                        Link(destination: URL(string: "https://github.com/sceneview/sceneview")!) {
                            Image(systemName: "chevron.left.forwardslash.chevron.right")
                                .font(.title3)
                                .foregroundStyle(.blue)
                        }
                        .accessibilityLabel("View source on GitHub")
                    }
                    .padding(.vertical, 8)
                }

                // Feature demos grouped by category
                let grouped = Dictionary(grouping: features) { $0.category }
                let sortedCategories = grouped.keys.sorted()
                ForEach(sortedCategories, id: \.self) { category in
                    Section {
                        ForEach(grouped[category]!) { feature in
                            NavigationLink {
                                feature.destination
                                    .navigationTitle(feature.title)
                                    #if os(iOS)
                                    .navigationBarTitleDisplayMode(.inline)
                                    #endif
                            } label: {
                                FeatureRow(feature: feature)
                            }
                            .accessibilityLabel("\(feature.title): \(feature.subtitle)")
                        }
                    } header: {
                        HStack(spacing: 6) {
                            Text(category.rawValue)
                            Text("\(grouped[category]!.count)")
                                .font(.caption2)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color(.tertiarySystemFill))
                                .clipShape(Capsule())
                        }
                    }
                }

                // Links
                Section("Resources") {
                    Link(destination: URL(string: "https://sceneview.github.io")!) {
                        Label("Website", systemImage: "globe")
                    }
                    Link(destination: URL(string: "https://sceneview.github.io/playground.html")!) {
                        Label("3D Playground", systemImage: "play.fill")
                    }
                    Link(destination: URL(string: "https://github.com/sceneview/sceneview")!) {
                        Label("GitHub", systemImage: "chevron.left.forwardslash.chevron.right")
                    }
                }

                // Credits
                Section {
                    Text("Powered by SceneView -- RealityKit, ARKit, SwiftUI")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("Apache License 2.0")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Features")
        }
    }

    // MARK: - Feature catalog

    private static func allFeatures() -> [DemoItem] {
        [
            // Geometry
            DemoItem(title: "All Shapes", icon: "cube.fill", subtitle: "Cube, sphere, cylinder, cone, plane", category: .geometry) {
                AllShapesDemo()
            },
            DemoItem(title: "Materials", icon: "paintpalette.fill", subtitle: "PBR metallic & roughness spectrum", category: .geometry) {
                MaterialsDemo()
            },
            DemoItem(title: "Custom Mesh", icon: "diamond.fill", subtitle: "Vertices, normals & triangle indices", category: .geometry) {
                CustomMeshDemo()
            },
            DemoItem(title: "Shape Node", icon: "star.fill", subtitle: "Star, hexagon, custom polygon extrusion", category: .geometry) {
                ShapeNodeDemo()
            },

            // Content
            DemoItem(title: "3D Text", icon: "textformat", subtitle: "Extruded text with styles & sizes", category: .content) {
                TextDemo()
            },
            DemoItem(title: "Billboard", icon: "person.fill.viewfinder", subtitle: "Labels that face the camera", category: .content) {
                BillboardDemo()
            },
            DemoItem(title: "Lines & Paths", icon: "point.topleft.down.to.point.bottomright.curvepath", subtitle: "Polylines, helix, grids, circles", category: .content) {
                LinesPathsDemo()
            },
            DemoItem(title: "Image Planes", icon: "photo.fill", subtitle: "Colored image planes in 3D", category: .content) {
                ImagePlaneDemo()
            },

            // Lighting
            DemoItem(title: "Light Types", icon: "lightbulb.fill", subtitle: "Directional, point & spot lights", category: .lighting) {
                LightTypesDemo()
            },
            DemoItem(title: "Dynamic Sky", icon: "sun.horizon.fill", subtitle: "Time-of-day sun simulation", category: .lighting) {
                DynamicSkyDemo()
            },

            // Effects
            DemoItem(title: "Fog", icon: "cloud.fog.fill", subtitle: "Linear, exponential & height fog", category: .effects) {
                FogDemo()
            },
            DemoItem(title: "Physics", icon: "figure.walk", subtitle: "Dynamic, static & kinematic bodies", category: .effects) {
                PhysicsDemo()
            },

            // Advanced
            DemoItem(title: "Orbit Camera", icon: "camera.fill", subtitle: "Interactive orbit with grid reference", category: .advanced) {
                OrbitCameraDemo()
            },
            DemoItem(title: "Auto Rotate", icon: "rotate.3d.fill", subtitle: "Continuous rotation animation", category: .advanced) {
                AutoRotateDemo()
            },
            DemoItem(title: "Scene Gallery", icon: "square.grid.3x3.fill", subtitle: "Multiple shapes in one scene", category: .advanced) {
                SceneGalleryDemo()
            },

            // AR-specific
            #if os(iOS)
            DemoItem(title: "Face Tracking", icon: "face.smiling.fill", subtitle: "AugmentedFaceNode with blend shapes", category: .ar) {
                FaceTrackingDemo()
            },
            #endif
        ]
    }
}

// MARK: - Feature row

private struct FeatureRow: View {
    let feature: DemoItem

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(iconColor.opacity(0.15))
                    .frame(width: 40, height: 40)
                Image(systemName: feature.icon)
                    .font(.body)
                    .foregroundStyle(iconColor)
            }
            .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(feature.title)
                    .font(.headline)
                Text(feature.subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 4)
    }

    private var iconColor: Color {
        switch feature.category {
        case .geometry: return .blue
        case .content: return .green
        case .lighting: return .orange
        case .effects: return .purple
        case .advanced: return .red
        case .ar: return .cyan
        }
    }
}

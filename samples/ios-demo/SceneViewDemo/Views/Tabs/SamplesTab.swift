import SwiftUI
import SceneViewSwift

/// Scenes tab -- curated preset scenes grouped by category.
struct SamplesTab: View {
    private let scenes: [DemoItem] = Self.allScenes()

    var body: some View {
        NavigationStack {
            List {
                let grouped = Dictionary(grouping: scenes) { $0.category }
                let sortedCategories = grouped.keys.sorted()
                ForEach(sortedCategories, id: \.self) { category in
                    Section(category.rawValue) {
                        ForEach(grouped[category]!) { scene in
                            NavigationLink {
                                scene.destination
                                    .navigationTitle(scene.title)
                                    #if os(iOS)
                                    .navigationBarTitleDisplayMode(.inline)
                                    #endif
                            } label: {
                                SceneRow(scene: scene)
                            }
                            .accessibilityLabel("\(scene.title): \(scene.subtitle)")
                        }
                    }
                }
            }
            .navigationTitle("Scenes")
        }
    }

    // MARK: - Scene catalog

    private static func allScenes() -> [DemoItem] {
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
        ]
    }
}

// MARK: - Scene row view

private struct SceneRow: View {
    let scene: DemoItem

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: scene.icon)
                .font(.title2)
                .foregroundStyle(.blue)
                .frame(width: 40, height: 40)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(scene.title)
                    .font(.headline)
                Text(scene.subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 4)
    }
}

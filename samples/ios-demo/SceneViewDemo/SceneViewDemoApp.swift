import SwiftUI
import RealityKit
import SceneViewSwift

/// SceneView iOS Demo — App Store showcase app.
///
/// Demonstrates 3D and AR capabilities using SceneViewSwift (RealityKit renderer).
/// Tabs: 3D Viewer, AR Viewer, Samples
@main
struct SceneViewDemoApp: App {
    var body: some Scene {
        WindowGroup {
            TabView {
                ModelViewerTab()
                    .tabItem {
                        Label("3D Viewer", systemImage: "cube.fill")
                    }

                ARViewerTab()
                    .tabItem {
                        Label("AR Viewer", systemImage: "arkit")
                    }

                SamplesTab()
                    .tabItem {
                        Label("Samples", systemImage: "square.grid.2x2.fill")
                    }
            }
        }
    }
}

// MARK: - Remote model URL

/// DamagedHelmet USDZ from KhronosGroup glTF-Sample-Assets (converted to USDZ).
/// RealityKit supports USDZ natively; glTF (.glb) is not supported without a converter.
private let damagedHelmetURL = URL(string:
    "https://developer.apple.com/augmented-reality/quick-look/models/retrotv/tv_retro.usdz"
)!

// MARK: - 3D Viewer Tab

struct ModelViewerTab: View {
    @State private var model: ModelNode?
    @State private var loadError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                SceneView { root in
                    if let model {
                        root.addChild(model.entity)
                    } else {
                        // Show a placeholder sphere while loading
                        let placeholder = GeometryNode.sphere(
                            radius: 0.3,
                            material: .pbr(color: .gray, metallic: 0.5, roughness: 0.4)
                        )
                        .withGroundingShadow()
                        root.addChild(placeholder.entity)
                    }
                }
                .cameraControls(.orbit)

                if model == nil && loadError == nil {
                    ProgressView("Loading model...")
                        .padding()
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                if let loadError {
                    Text(loadError)
                        .font(.caption)
                        .foregroundStyle(.red)
                        .padding()
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .task {
                do {
                    model = try await ModelNode.load(
                        contentsOf: damagedHelmetURL
                    )
                    model?.scaleToUnits(1.0)
                    model?.playAllAnimations()
                } catch {
                    loadError = "Failed to load model: \(error.localizedDescription)"
                }
            }
            .navigationTitle("3D Viewer")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - AR Viewer Tab

struct ARViewerTab: View {
    var body: some View {
        NavigationStack {
            ARSceneView(
                planeDetection: .horizontal,
                onTapOnPlane: { position, arView in
                    // Place a colored cube at the tapped surface point
                    let cube = GeometryNode.cube(
                        size: 0.1,
                        color: .systemBlue,
                        cornerRadius: 0.01
                    )
                    .withGroundingShadow()

                    let anchor = AnchorNode.world(position: position)
                    anchor.add(cube.entity)
                    arView.scene.addAnchor(anchor.entity)
                }
            )
            .navigationTitle("AR Viewer")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Samples Tab

struct SamplesTab: View {
    let samples = [
        ("Model Viewer", "cube.fill", "Load and view 3D models with orbit camera"),
        ("AR Placement", "arkit", "Tap to place models in augmented reality"),
        ("Animations", "play.circle.fill", "Play and control model animations"),
        ("Lighting", "sun.max.fill", "Adjust environment lighting and shadows"),
        ("Physics", "atom", "Rigid body physics simulation"),
    ]

    var body: some View {
        NavigationStack {
            List(samples, id: \.0) { sample in
                HStack(spacing: 12) {
                    Image(systemName: sample.1)
                        .font(.title2)
                        .foregroundColor(.accentColor)
                        .frame(width: 40)
                    VStack(alignment: .leading) {
                        Text(sample.0)
                            .font(.headline)
                        Text(sample.2)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.vertical, 4)
            }
            .navigationTitle("Samples")
        }
    }
}

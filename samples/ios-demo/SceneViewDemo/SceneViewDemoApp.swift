import SwiftUI
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

// MARK: - 3D Viewer Tab

struct ModelViewerTab: View {
    var body: some View {
        NavigationStack {
            SceneView {
                ModelNode("models/damaged_helmet.glb")
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
            ARSceneView { anchor in
                ModelNode("models/damaged_helmet.glb")
                    .scale(0.3)
            }
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

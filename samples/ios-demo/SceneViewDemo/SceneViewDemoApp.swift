import SwiftUI

/// SceneView iOS Demo — App Store showcase app.
///
/// Demonstrates 3D and AR capabilities using SceneViewSwift (RealityKit renderer).
/// Tabs: 3D Viewer, AR Viewer, Samples, About
@main
struct SceneViewDemoApp: App {
    var body: some SwiftUI.Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    var body: some View {
        TabView {
            ExploreTab()
                .tabItem {
                    Label("3D", systemImage: "cube.fill")
                }
                .accessibilityLabel("3D Viewer")

            ARTab()
                .tabItem {
                    Label("AR", systemImage: "arkit")
                }
                .accessibilityLabel("Augmented Reality Viewer")

            SamplesTab()
                .tabItem {
                    Label("Samples", systemImage: "square.grid.2x2.fill")
                }
                .accessibilityLabel("Code Samples")

            AboutTab()
                .tabItem {
                    Label("About", systemImage: "info.circle.fill")
                }
                .accessibilityLabel("About SceneView")
        }
        .tint(.blue)
    }
}

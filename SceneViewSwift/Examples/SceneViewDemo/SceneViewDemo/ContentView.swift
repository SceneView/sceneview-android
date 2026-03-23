import SwiftUI

/// Main tab view for the SceneView iOS demo.
///
/// Mirrors the Android demo app's tab architecture with:
/// - Explore: 3D model viewer with orbit camera and environment switching
/// - Shapes: Procedural geometry showcase
/// - AR: Augmented reality tap-to-place demo
/// - About: SDK information and feature list
struct ContentView: View {
    var body: some View {
        TabView {
            Tab("Explore", systemImage: "cube.transparent") {
                ExploreView()
            }
            Tab("Shapes", systemImage: "pyramid.fill") {
                ShapesView()
            }
            #if !os(visionOS)
            Tab("AR", systemImage: "arkit") {
                ARDemoView()
            }
            #endif
            Tab("About", systemImage: "info.circle") {
                AboutView()
            }
        }
        .tint(.purple)
    }
}

#Preview {
    ContentView()
}

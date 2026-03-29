import SwiftUI

#if canImport(AppKit)
import AppKit
/// Maps UIColor to NSColor on macOS so demo code compiles cross-platform.
typealias UIColor = NSColor

extension NSColor {
    /// iOS systemGray2 equivalent on macOS.
    static var systemGray2: NSColor { NSColor.systemGray.withAlphaComponent(0.8) }
    /// iOS systemGray3 equivalent on macOS.
    static var systemGray3: NSColor { NSColor.systemGray.withAlphaComponent(0.6) }
}
#endif

/// SceneView Demo — App Store showcase app (iOS + macOS).
///
/// Demonstrates 3D and AR capabilities using SceneViewSwift (RealityKit renderer).
/// Tabs: 3D Viewer, AR Viewer (iOS only), Samples, About
@main
struct SceneViewDemoApp: App {
    var body: some SwiftUI.Scene {
        WindowGroup {
            ContentView()
        }
        #if os(macOS)
        .defaultSize(width: 1200, height: 800)
        #endif
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

            #if os(iOS)
            ARTab()
                .tabItem {
                    Label("AR", systemImage: "arkit")
                }
                .accessibilityLabel("Augmented Reality Viewer")
            #endif

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

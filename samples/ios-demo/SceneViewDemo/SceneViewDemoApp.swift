import SwiftUI

#if canImport(AppKit)
import AppKit
/// Maps UIColor to NSColor on macOS so code compiles cross-platform.
typealias UIColor = NSColor

extension NSColor {
    /// iOS systemGray2 equivalent on macOS.
    static var systemGray2: NSColor { NSColor.systemGray.withAlphaComponent(0.8) }
    /// iOS systemGray3 equivalent on macOS.
    static var systemGray3: NSColor { NSColor.systemGray.withAlphaComponent(0.6) }
}
#endif

/// 3D & AR Explorer — Explore, visualize, and interact with 3D models.
///
/// Browse a curated gallery of 3D models, view them in augmented reality,
/// save favorites, and share screenshots with friends.
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
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            ExploreTab()
                .tabItem {
                    Label("Explore", systemImage: "cube.fill")
                }
                .tag(0)
                .accessibilityLabel("3D Model Gallery")

            #if os(iOS)
            ARTab()
                .tabItem {
                    Label("AR View", systemImage: "arkit")
                }
                .tag(1)
                .accessibilityLabel("Augmented Reality Viewer")
            #endif

            SamplesTab()
                .tabItem {
                    Label("Scenes", systemImage: "square.grid.2x2.fill")
                }
                .tag(2)
                .accessibilityLabel("Scene Presets")

            AboutTab()
                .tabItem {
                    Label("About", systemImage: "info.circle.fill")
                }
                .tag(3)
                .accessibilityLabel("About This App")
        }
        .tint(SceneViewTheme.primary)
    }
}

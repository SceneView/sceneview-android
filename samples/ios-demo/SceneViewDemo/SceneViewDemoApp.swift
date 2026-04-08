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

/// 3D & AR Explorer -- feature showcase for SceneView iOS SDK.
///
/// Three-tab app: 3D model search (Sketchfab), AR placement, and
/// feature demos for all SceneView node types.
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
                .accessibilityLabel("3D Model Search")

            #if os(iOS)
            ARTab()
                .tabItem {
                    Label("AR", systemImage: "arkit")
                }
                .accessibilityLabel("Augmented Reality")
            #endif

            FeaturesTab()
                .tabItem {
                    Label("Features", systemImage: "square.grid.2x2.fill")
                }
                .accessibilityLabel("Feature Demos")
        }
        .tint(SceneViewTheme.primary)
    }
}

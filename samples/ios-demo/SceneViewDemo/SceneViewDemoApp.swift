import SwiftUI

/// SceneView iOS Demo -- App Store showcase app.
///
/// Demonstrates all SceneViewSwift capabilities using RealityKit on iOS, macOS, and visionOS.
/// Architecture: 4-tab layout (Explore, AR, Samples, About) with NavigationStack per tab.
@main
struct SceneViewDemoApp: App {
    var body: some SwiftUI.Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    @State private var selectedTab: Tab = .explore

    enum Tab: String, CaseIterable {
        case explore, ar, samples, about
    }

    var body: some View {
        Group {
            #if os(iOS)
            if UIDevice.current.userInterfaceIdiom == .pad {
                iPadLayout
            } else {
                iPhoneLayout
            }
            #else
            iPhoneLayout
            #endif
        }
        .tint(.blue)
    }

    // MARK: - iPhone: Tab layout

    private var iPhoneLayout: some View {
        TabView(selection: $selectedTab) {
            ExploreTab()
                .tabItem {
                    Label("Explore", systemImage: "cube.fill")
                }
                .tag(Tab.explore)
                .accessibilityLabel("3D Explore")

            #if !targetEnvironment(simulator)
            ARTab()
                .tabItem {
                    Label("AR", systemImage: "arkit")
                }
                .tag(Tab.ar)
                .accessibilityLabel("Augmented Reality")
            #endif

            SamplesTab()
                .tabItem {
                    Label("Samples", systemImage: "square.grid.2x2.fill")
                }
                .tag(Tab.samples)
                .accessibilityLabel("Code Samples")

            AboutTab()
                .tabItem {
                    Label("About", systemImage: "info.circle.fill")
                }
                .tag(Tab.about)
                .accessibilityLabel("About SceneView")
        }
    }

    // MARK: - iPad: Sidebar + Detail

    @ViewBuilder
    private var iPadLayout: some View {
        NavigationSplitView {
            List(selection: $selectedTab) {
                Section("SceneView") {
                    Label("Explore", systemImage: "cube.fill")
                        .tag(Tab.explore)
                    #if !targetEnvironment(simulator)
                    Label("AR", systemImage: "arkit")
                        .tag(Tab.ar)
                    #endif
                    Label("Samples", systemImage: "square.grid.2x2.fill")
                        .tag(Tab.samples)
                    Label("About", systemImage: "info.circle.fill")
                        .tag(Tab.about)
                }
            }
            .navigationTitle("SceneView")
        } detail: {
            switch selectedTab {
            case .explore:
                ExploreTab()
            case .ar:
                #if !targetEnvironment(simulator)
                ARTab()
                #else
                simulatorARPlaceholder
                #endif
            case .samples:
                SamplesTab()
            case .about:
                AboutTab()
            }
        }
    }

    private var simulatorARPlaceholder: some View {
        VStack(spacing: 16) {
            Image(systemName: "arkit")
                .font(.system(size: 60))
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            Text("AR requires a physical device")
                .font(.headline)
            Text("Run on iPhone or iPad to experience augmented reality.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

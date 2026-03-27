import SwiftUI
import RealityKit
import SceneViewSwift

/// SceneView iOS Demo — App Store showcase app.
///
/// Demonstrates 3D and AR capabilities using SceneViewSwift (RealityKit renderer).
/// Tabs: 3D Viewer, AR Viewer, Samples, About
@main
struct SceneViewDemoApp: App {
    var body: some Scene {
        WindowGroup {
            TabView {
                ExploreTab()
                    .tabItem {
                        Label("3D", systemImage: "cube.fill")
                    }

                ARViewerTab()
                    .tabItem {
                        Label("AR", systemImage: "arkit")
                    }

                SamplesTab()
                    .tabItem {
                        Label("Samples", systemImage: "square.grid.2x2.fill")
                    }

                AboutTab()
                    .tabItem {
                        Label("About", systemImage: "info.circle.fill")
                    }
            }
            .tint(.blue)
        }
    }
}

// MARK: - 3D Explore Tab

struct ExploreTab: View {
    private let shapes: [(String, () -> GeometryNode)] = [
        ("Cube", { GeometryNode.cube(size: 0.4, color: .systemBlue, cornerRadius: 0.02) }),
        ("Sphere", { GeometryNode.sphere(radius: 0.25, material: .pbr(color: .red, metallic: 0.8, roughness: 0.2)) }),
        ("Cylinder", { GeometryNode.cylinder(radius: 0.2, height: 0.5, color: .green) }),
    ]
    @State private var selectedIndex = 0

    var body: some View {
        ZStack {
            SceneView { root in
                let shape = shapes[selectedIndex].1()
                shape.entity.position = .init(x: 0, y: 0, z: -1.5)
                root.addChild(shape.entity)
            }
            .cameraControls(.orbit)

            VStack {
                // Title overlay
                HStack {
                    VStack(alignment: .leading) {
                        Text("SceneView")
                            .font(.largeTitle).bold()
                            .foregroundStyle(.white)
                        Text("3D & AR for SwiftUI")
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.7))
                    }
                    Spacer()
                }
                .padding()

                Spacer()

                // Shape picker
                HStack(spacing: 12) {
                    ForEach(0..<shapes.count, id: \.self) { index in
                        Button(shapes[index].0) {
                            selectedIndex = index
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(index == selectedIndex ? .blue : .gray.opacity(0.6))
                    }
                }
                .padding(.bottom, 20)

                Text("Pinch to zoom · Drag to orbit")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.6))
                    .padding(.bottom, 8)
            }
        }
        .background(Color.black)
    }
}

// MARK: - AR Viewer Tab

struct ARViewerTab: View {
    @State private var placedCount = 0

    var body: some View {
        NavigationStack {
            ZStack {
                #if !targetEnvironment(simulator)
                ARSceneView(
                    planeDetection: .horizontal,
                    onTapOnPlane: { position, arView in
                        let colors: [UIColor] = [.systemBlue, .systemRed, .systemGreen, .systemOrange, .systemPurple]
                        let cube = GeometryNode.cube(
                            size: 0.1,
                            color: colors[placedCount % colors.count],
                            cornerRadius: 0.01
                        )
                        .withGroundingShadow()

                        let anchor = AnchorNode.world(position: position)
                        anchor.add(cube.entity)
                        arView.scene.addAnchor(anchor.entity)
                        placedCount += 1
                    }
                )
                #else
                VStack(spacing: 16) {
                    Image(systemName: "arkit")
                        .font(.system(size: 60))
                        .foregroundStyle(.secondary)
                    Text("AR requires a physical device")
                        .font(.headline)
                    Text("Run on iPhone or iPad to test AR features.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                #endif

                VStack {
                    // Status pill
                    Text(placedCount == 0 ? "Tap a surface to place" : "\(placedCount) object(s) placed")
                        .font(.caption)
                        .fontWeight(.medium)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(.ultraThinMaterial)
                        .clipShape(Capsule())
                        .padding(.top, 8)

                    Spacer()
                }
            }
            .navigationTitle("AR Viewer")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Samples Tab

struct SampleItem: Identifiable {
    let id = UUID()
    let title: String
    let icon: String
    let subtitle: String
    let category: String
    let view: AnyView
}

struct SamplesTab: View {
    let samples: [SampleItem] = [
        SampleItem(title: "Geometry", icon: "cube.fill", subtitle: "Cube, sphere, cylinder shapes", category: "3D",
                   view: AnyView(GeometryDemo())),
        SampleItem(title: "Materials", icon: "paintpalette.fill", subtitle: "PBR metallic & roughness", category: "3D",
                   view: AnyView(MaterialsDemo())),
        SampleItem(title: "Orbit Camera", icon: "camera.fill", subtitle: "Interactive orbit controls", category: "3D",
                   view: AnyView(OrbitCameraDemo())),
        SampleItem(title: "Text Labels", icon: "textformat", subtitle: "3D billboard text", category: "Content",
                   view: AnyView(TextLabelsDemo())),
        SampleItem(title: "Line Paths", icon: "point.topleft.down.to.point.bottomright.curvepath", subtitle: "3D polylines & curves", category: "Content",
                   view: AnyView(LinePathsDemo())),
    ]

    var body: some View {
        NavigationStack {
            List {
                let categories = Dictionary(grouping: samples, by: { $0.category })
                ForEach(categories.keys.sorted(), id: \.self) { category in
                    Section(category) {
                        ForEach(categories[category]!) { sample in
                            NavigationLink(destination: sample.view.navigationTitle(sample.title).navigationBarTitleDisplayMode(.inline)) {
                                HStack(spacing: 12) {
                                    Image(systemName: sample.icon)
                                        .font(.title2)
                                        .foregroundColor(.accentColor)
                                        .frame(width: 40)
                                    VStack(alignment: .leading) {
                                        Text(sample.title)
                                            .font(.headline)
                                        Text(sample.subtitle)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Samples")
        }
    }
}

// MARK: - About Tab

struct AboutTab: View {
    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(spacing: 12) {
                        Image(systemName: "cube.fill")
                            .font(.system(size: 50))
                            .foregroundStyle(.blue)
                        Text("SceneView")
                            .font(.title).bold()
                        Text("v3.4.7")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text("3D & AR as declarative UI\nfor iOS, macOS, visionOS & more")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                }

                Section("Features") {
                    FeatureRow(icon: "cube.fill", title: "3D Scenes", description: "RealityKit with SwiftUI integration", color: .blue)
                    FeatureRow(icon: "arkit", title: "Augmented Reality", description: "ARKit plane detection & tap-to-place", color: .green)
                    FeatureRow(icon: "sparkles", title: "AI-First SDK", description: "Designed for AI code generation", color: .purple)
                }

                Section("Platforms") {
                    PlatformRow(name: "iOS", detail: "RealityKit + SwiftUI")
                    PlatformRow(name: "macOS", detail: "RealityKit + SwiftUI")
                    PlatformRow(name: "visionOS", detail: "RealityKit + SwiftUI")
                    PlatformRow(name: "Android", detail: "Filament + Compose")
                    PlatformRow(name: "Web", detail: "Filament.js (WASM)")
                }

                Section("Links") {
                    Link(destination: URL(string: "https://github.com/sceneview/sceneview")!) {
                        Label("GitHub", systemImage: "chevron.left.forwardslash.chevron.right")
                    }
                    Link(destination: URL(string: "https://sceneview.github.io")!) {
                        Label("Website", systemImage: "globe")
                    }
                }

                Section("Credits") {
                    Text("Built with RealityKit, ARKit, and SwiftUI.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("Open source under Apache License 2.0.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("About")
        }
    }
}

struct FeatureRow: View {
    let icon: String
    let title: String
    let description: String
    let color: Color

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(color)
                .frame(width: 32)
            VStack(alignment: .leading) {
                Text(title).font(.subheadline).bold()
                Text(description).font(.caption).foregroundStyle(.secondary)
            }
        }
    }
}

struct PlatformRow: View {
    let name: String
    let detail: String

    var body: some View {
        HStack {
            Text(name).font(.subheadline)
            Spacer()
            Text(detail).font(.caption).foregroundStyle(.secondary)
        }
    }
}

// MARK: - Individual Demo Views

struct GeometryDemo: View {
    var body: some View {
        SceneView { root in
            let cube = GeometryNode.cube(size: 0.3, color: .systemBlue, cornerRadius: 0.02)
            cube.entity.position = .init(x: -0.5, y: 0, z: -2)
            root.addChild(cube.entity)

            let sphere = GeometryNode.sphere(
                radius: 0.2,
                material: .pbr(color: .red, metallic: 0.8, roughness: 0.2)
            )
            sphere.entity.position = .init(x: 0, y: 0, z: -2)
            root.addChild(sphere.entity)

            let cylinder = GeometryNode.cylinder(radius: 0.15, height: 0.4, color: .green)
            cylinder.entity.position = .init(x: 0.5, y: 0, z: -2)
            root.addChild(cylinder.entity)
        }
        .cameraControls(.orbit)
    }
}

struct MaterialsDemo: View {
    var body: some View {
        SceneView { root in
            // Rough
            let rough = GeometryNode.sphere(
                radius: 0.2,
                material: .pbr(color: .gray, metallic: 0.0, roughness: 1.0)
            )
            rough.entity.position = .init(x: -0.6, y: 0, z: -2)
            root.addChild(rough.entity)

            // Semi-metallic
            let semi = GeometryNode.sphere(
                radius: 0.2,
                material: .pbr(color: .gray, metallic: 0.5, roughness: 0.3)
            )
            semi.entity.position = .init(x: 0, y: 0, z: -2)
            root.addChild(semi.entity)

            // Full metal
            let metal = GeometryNode.sphere(
                radius: 0.2,
                material: .pbr(color: .gray, metallic: 1.0, roughness: 0.0)
            )
            metal.entity.position = .init(x: 0.6, y: 0, z: -2)
            root.addChild(metal.entity)
        }
        .cameraControls(.orbit)
    }
}

struct OrbitCameraDemo: View {
    var body: some View {
        ZStack {
            SceneView { root in
                let cube = GeometryNode.cube(size: 0.4, color: .systemOrange, cornerRadius: 0.03)
                cube.entity.position = .init(x: 0, y: 0, z: -2)
                root.addChild(cube.entity)

                // Grid of small cubes for spatial reference
                for x in stride(from: -1.0, through: 1.0, by: 0.5) {
                    for z in stride(from: -3.0, through: -1.0, by: 0.5) {
                        let dot = GeometryNode.cube(size: 0.03, color: .darkGray, cornerRadius: 0)
                        dot.entity.position = .init(x: Float(x), y: -0.3, z: Float(z))
                        root.addChild(dot.entity)
                    }
                }
            }
            .cameraControls(.orbit)

            VStack {
                Spacer()
                VStack(spacing: 4) {
                    Text("1 finger — Orbit")
                    Text("2 fingers — Pan")
                    Text("Pinch — Zoom")
                }
                .font(.caption)
                .foregroundStyle(.white)
                .padding()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(.bottom, 30)
            }
        }
    }
}

struct TextLabelsDemo: View {
    var body: some View {
        SceneView { root in
            let cube = GeometryNode.cube(size: 0.3, color: .systemBlue, cornerRadius: 0.02)
            cube.entity.position = .init(x: -0.5, y: 0, z: -2)
            root.addChild(cube.entity)

            let sphere = GeometryNode.sphere(
                radius: 0.2,
                material: .pbr(color: .red, metallic: 0.5, roughness: 0.3)
            )
            sphere.entity.position = .init(x: 0.5, y: 0, z: -2)
            root.addChild(sphere.entity)
        }
        .cameraControls(.orbit)
    }
}

struct LinePathsDemo: View {
    var body: some View {
        SceneView { root in
            // Create a visible path using small spheres along a helix
            for i in 0..<60 {
                let t = Float(i) / 60.0 * 4 * .pi
                let dot = GeometryNode.sphere(
                    radius: 0.015,
                    material: .pbr(color: .orange, metallic: 0.8, roughness: 0.2)
                )
                dot.entity.position = .init(
                    x: cos(t) * 0.5,
                    y: Float(i) / 60.0 * 1.0 - 0.5,
                    z: sin(t) * 0.5 - 2.0
                )
                root.addChild(dot.entity)
            }
        }
        .cameraControls(.orbit)
    }
}

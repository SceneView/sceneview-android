import SwiftUI
import RealityKit
import SceneViewSwift

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

            ARViewerTab()
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

// MARK: - 3D Explore Tab

private struct ShapeOption {
    let name: String
    let make: () -> GeometryNode
}

struct ExploreTab: View {
    private let shapes: [ShapeOption] = [
        ShapeOption(name: "Cube",
            make: { GeometryNode.cube(size: 0.4, color: .systemBlue, cornerRadius: 0.02) }),
        ShapeOption(name: "Sphere",
            make: { GeometryNode.sphere(radius: 0.25, material: .pbr(color: .red, metallic: 0.8, roughness: 0.2)) }),
        ShapeOption(name: "Cylinder",
            make: { GeometryNode.cylinder(radius: 0.2, height: 0.5, color: .green) }),
    ]
    @State private var selectedIndex = 0

    var body: some View {
        ZStack {
            // Use .id to force SceneView rebuild when selected shape changes
            SceneView { root in
                let shape = shapes[selectedIndex].make()
                shape.entity.position = .init(x: 0, y: 0, z: -1.5)
                root.addChild(shape.entity)
            }
            .cameraControls(.orbit)
            .id(selectedIndex)
            .ignoresSafeArea()

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
                        Button(shapes[index].name) {
                            selectedIndex = index
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(index == selectedIndex ? .blue : .gray.opacity(0.6))
                        .accessibilityLabel("Show \(shapes[index].name)")
                        .accessibilityAddTraits(index == selectedIndex ? .isSelected : [])
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
                        let colors: [UIColor] = [
                            .systemBlue, .systemRed, .systemGreen,
                            .systemOrange, .systemPurple
                        ]
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
                .ignoresSafeArea()
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
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.systemGroupedBackground))
                #endif

                VStack {
                    #if !targetEnvironment(simulator)
                    // Status pill — only shown on device when AR is active
                    Text(placedCount == 0
                        ? "Point at a surface and tap to place"
                        : "\(placedCount) object\(placedCount == 1 ? "" : "s") placed")
                        .font(.caption)
                        .fontWeight(.medium)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(.ultraThinMaterial)
                        .clipShape(Capsule())
                        .padding(.top, 8)
                        .accessibilityLabel(
                            placedCount == 0
                                ? "Point at a surface and tap to place an object"
                                : "\(placedCount) \(placedCount == 1 ? "object" : "objects") placed in the scene"
                        )
                    #endif

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
    let destination: () -> AnyView
}

struct SamplesTab: View {
    let samples: [SampleItem] = [
        SampleItem(
            title: "Geometry",
            icon: "cube.fill",
            subtitle: "Cube, sphere, cylinder shapes",
            category: "3D",
            destination: { AnyView(GeometryDemo()) }
        ),
        SampleItem(
            title: "Materials",
            icon: "paintpalette.fill",
            subtitle: "PBR metallic & roughness",
            category: "3D",
            destination: { AnyView(MaterialsDemo()) }
        ),
        SampleItem(
            title: "Orbit Camera",
            icon: "camera.fill",
            subtitle: "Interactive orbit controls",
            category: "3D",
            destination: { AnyView(OrbitCameraDemo()) }
        ),
        SampleItem(
            title: "Text Labels",
            icon: "textformat",
            subtitle: "3D billboard text nodes",
            category: "Content",
            destination: { AnyView(TextLabelsDemo()) }
        ),
        SampleItem(
            title: "Line Paths",
            icon: "point.topleft.down.to.point.bottomright.curvepath",
            subtitle: "3D polylines & helices",
            category: "Content",
            destination: { AnyView(LinePathsDemo()) }
        ),
    ]

    var body: some View {
        NavigationStack {
            List {
                let categories = Dictionary(grouping: samples, by: { $0.category })
                ForEach(categories.keys.sorted(), id: \.self) { category in
                    Section(category) {
                        ForEach(categories[category]!) { sample in
                            NavigationLink(destination:
                                sample.destination()
                                    .navigationTitle(sample.title)
                                    .navigationBarTitleDisplayMode(.inline)
                            ) {
                                HStack(spacing: 12) {
                                    Image(systemName: sample.icon)
                                        .font(.title2)
                                        .foregroundStyle(.accent)
                                        .frame(width: 40)
                                        .accessibilityHidden(true)
                                    VStack(alignment: .leading) {
                                        Text(sample.title)
                                            .font(.headline)
                                        Text(sample.subtitle)
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                            .accessibilityLabel("\(sample.title): \(sample.subtitle)")
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
                            .accessibilityHidden(true)
                        Text("SceneView")
                            .font(.title).bold()
                        Text("v3.5.0")
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
                    FeatureRow(
                        icon: "cube.fill",
                        title: "3D Scenes",
                        description: "RealityKit with SwiftUI integration",
                        color: .blue
                    )
                    FeatureRow(
                        icon: "arkit",
                        title: "Augmented Reality",
                        description: "ARKit plane detection & tap-to-place",
                        color: .green
                    )
                    FeatureRow(
                        icon: "sparkles",
                        title: "AI-First SDK",
                        description: "Designed for AI code generation",
                        color: .purple
                    )
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
                    .accessibilityLabel("Open SceneView on GitHub")

                    Link(destination: URL(string: "https://sceneview.github.io")!) {
                        Label("Website", systemImage: "globe")
                    }
                    .accessibilityLabel("Open SceneView website")
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
                .accessibilityHidden(true)
            VStack(alignment: .leading) {
                Text(title).font(.subheadline).bold()
                Text(description).font(.caption).foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
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
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(name): \(detail)")
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
        .ignoresSafeArea()
    }
}

struct MaterialsDemo: View {
    var body: some View {
        SceneView { root in
            // Rough non-metallic
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

            // Full metal mirror
            let metal = GeometryNode.sphere(
                radius: 0.2,
                material: .pbr(color: .gray, metallic: 1.0, roughness: 0.0)
            )
            metal.entity.position = .init(x: 0.6, y: 0, z: -2)
            root.addChild(metal.entity)
        }
        .cameraControls(.orbit)
        .ignoresSafeArea()
        .overlay(alignment: .bottom) {
            HStack(spacing: 24) {
                VStack(spacing: 4) {
                    Circle().fill(.gray.opacity(0.5)).frame(width: 12)
                    Text("Rough").font(.caption2).foregroundStyle(.white)
                }
                VStack(spacing: 4) {
                    Circle().fill(.gray.opacity(0.7)).frame(width: 12)
                    Text("Semi").font(.caption2).foregroundStyle(.white)
                }
                VStack(spacing: 4) {
                    Circle().fill(.gray).frame(width: 12)
                    Text("Metal").font(.caption2).foregroundStyle(.white)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .background(.ultraThinMaterial)
            .clipShape(Capsule())
            .padding(.bottom, 24)
        }
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
            .ignoresSafeArea()

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

/// Demonstrates TextNode — 3D extruded text labels in the scene.
struct TextLabelsDemo: View {
    var body: some View {
        SceneView { root in
            // Header label
            let title = TextNode(text: "Hello 3D!", fontSize: 0.12, color: .white, depth: 0.02)
                .centered()
                .position(.init(x: 0, y: 0.5, z: -2.5))
            root.addChild(title.entity)

            // Cube with label beneath it
            let cube = GeometryNode.cube(size: 0.25, color: .systemBlue, cornerRadius: 0.02)
            cube.entity.position = .init(x: -0.45, y: -0.2, z: -2.5)
            root.addChild(cube.entity)

            let cubeLabel = TextNode(text: "Cube", fontSize: 0.07, color: .systemBlue, depth: 0.01)
                .centered()
                .position(.init(x: -0.45, y: -0.5, z: -2.5))
            root.addChild(cubeLabel.entity)

            // Sphere with label beneath it
            let sphere = GeometryNode.sphere(
                radius: 0.15,
                material: .pbr(color: .red, metallic: 0.5, roughness: 0.3)
            )
            sphere.entity.position = .init(x: 0.45, y: -0.2, z: -2.5)
            root.addChild(sphere.entity)

            let sphereLabel = TextNode(text: "Sphere", fontSize: 0.07, color: .red, depth: 0.01)
                .centered()
                .position(.init(x: 0.45, y: -0.5, z: -2.5))
            root.addChild(sphereLabel.entity)
        }
        .cameraControls(.orbit)
        .ignoresSafeArea()
    }
}

/// Demonstrates a 3D helix path using small metallic spheres.
struct LinePathsDemo: View {
    var body: some View {
        ZStack {
            SceneView { root in
                // Helix path approximated with small metallic spheres
                let stepCount = 60
                for i in 0..<stepCount {
                    let t = Float(i) / Float(stepCount) * 4 * .pi
                    let progress = Float(i) / Float(stepCount)
                    // Interpolate hue from orange to blue along the helix
                    let color = UIColor(
                        hue: CGFloat(0.08 + progress * 0.55),
                        saturation: 0.9,
                        brightness: 1.0,
                        alpha: 1.0
                    )
                    let dot = GeometryNode.sphere(
                        radius: 0.018,
                        material: .pbr(color: color, metallic: 0.8, roughness: 0.2)
                    )
                    dot.entity.position = .init(
                        x: cos(t) * 0.5,
                        y: progress * 1.2 - 0.6,
                        z: sin(t) * 0.5 - 2.0
                    )
                    root.addChild(dot.entity)
                }

                // Central axis indicator
                let axis = GeometryNode.cylinder(radius: 0.008, height: 1.2, color: .darkGray)
                axis.entity.position = .init(x: 0, y: 0, z: -2)
                root.addChild(axis.entity)
            }
            .cameraControls(.orbit)
            .ignoresSafeArea()

            VStack {
                Spacer()
                Text("Helix path · 60 nodes")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.7))
                    .padding(.bottom, 12)
            }
        }
        .background(Color.black)
    }
}

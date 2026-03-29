import SwiftUI
import RealityKit
import SceneViewSwift

/// The main 3D explore tab -- showcases rotating 3D models with controls.
///
/// Mirrors the Android ExploreScreen style: full-screen 3D scene with
/// gradient overlays, model picker, and auto-rotation.
struct ExploreTab: View {
    private let models: [(name: String, icon: String, asset: String?, scale: Float)] = [
        ("Game Boy", "gamecontroller.fill", "game_boy_classic", 0.8),
        ("Red Car", "car.fill", "red_car", 1.0),
        ("Tree Scene", "tree.fill", "tree_scene", 0.6),
        ("Dragon", "flame.fill", "animated_dragon", 0.6),
        ("Butterfly", "leaf.fill", "animated_butterfly", 0.8),
        ("Piano", "pianokeys", "retro_piano", 0.7),
        ("Phoenix", "bird.fill", "phoenix_bird", 0.8),
        ("Cyberpunk", "car.side.fill", "cyberpunk_car", 0.8),
        ("Fantasy Book", "book.fill", "fantasy_book", 0.7),
        ("Mosquito", "ant.fill", "mosquito_amber", 1.0),
        ("Ship", "cloud.fill", "ship_in_clouds", 0.5),
        ("Hovercar", "bolt.car.fill", "cyberpunk_hovercar", 0.6),
        ("Cyber Guy", "figure.stand", "cyberpunk_character", 0.7),
        ("Porsche 911", "car.fill", "porsche_911", 0.6),
        ("Black Dragon", "lizard.fill", "black_dragon", 0.5),
        ("Fiat Punto", "car.rear.fill", "fiat_punto", 0.7),
        ("Shelby Cobra", "car.fill", "shelby_cobra", 0.6),
        ("Audi TT", "car.side.fill", "audi_tt", 0.7),
        ("Earthquake", "waveform.path", "earthquake_california", 0.4),
        ("Lamborghini", "car.fill", "lamborghini_countach", 0.5),
        ("Nike Jordan", "shoe.fill", "nike_air_jordan", 0.8),
        ("Ferrari F40", "car.fill", "ferrari_f40", 0.6),
        ("Porsche Turbo", "car.side.fill", "porsche_911_turbo", 0.5),
        ("PS5 Controller", "gamecontroller.fill", "ps5_dualsense", 0.8),
        ("Cybertruck", "truck.box.fill", "tesla_cybertruck", 0.6),
        ("Mercedes AMG", "car.rear.fill", "mercedes_a45_amg", 0.5),
        ("Switch", "gamecontroller.fill", "nintendo_switch", 0.8),
        ("BMW M3 E30", "car.fill", "bmw_m3_e30", 0.6),
    ]

    @State private var selectedIndex = 0
    @State private var autoRotate = true
    @State private var showControls = true
    @State private var loadedModel: ModelNode?
    @State private var isLoading = false
    @State private var selectedEnvironment: SceneEnvironment = .studio

    var body: some View {
        ZStack {
            // Dark gradient background (visible through RealityKit transparent bg)
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(red: 0.08, green: 0.08, blue: 0.12),
                    Color(red: 0.15, green: 0.15, blue: 0.22),
                    Color(red: 0.10, green: 0.10, blue: 0.18)
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            sceneView
                .ignoresSafeArea()

            // Loading indicator
            if isLoading {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(.white)
                    .scaleEffect(1.5)
            }

            VStack(spacing: 0) {
                headerOverlay
                Spacer()
                if showControls {
                    controlsOverlay
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.3), value: showControls)
        }
        .onTapGesture {
            withAnimation { showControls.toggle() }
        }
        .task(id: selectedIndex) {
            await loadSelectedModel()
        }
    }

    // MARK: - Model Loading

    private func loadSelectedModel() async {
        let model = models[selectedIndex]
        guard let assetName = model.asset else {
            loadedModel = nil
            isLoading = false
            return
        }
        isLoading = true
        do {
            let node = try await ModelNode.load(assetName)
            _ = node.scaleToUnits(model.scale)
            loadedModel = node
        } catch {
            print("[ExploreTab] Failed to load model '\(assetName)': \(error)")
            loadedModel = nil
        }
        isLoading = false
    }

    // MARK: - Scene

    @ViewBuilder
    private var sceneView: some View {
        if autoRotate {
            SceneView { root in
                buildContent(into: root)
            }
            .environment(selectedEnvironment)
            .cameraControls(.orbit)
            .autoRotate(speed: 0.4)
            .id("model-\(selectedIndex)-auto-\(loadedModel != nil)-\(selectedEnvironment.name)")
        } else {
            SceneView { root in
                buildContent(into: root)
            }
            .environment(selectedEnvironment)
            .cameraControls(.orbit)
            .id("model-\(selectedIndex)-manual-\(loadedModel != nil)-\(selectedEnvironment.name)")
        }
    }

    private func buildContent(into root: Entity) {
        let model = models[selectedIndex]

        if let loadedModel {
            // Use loaded USDZ model
            loadedModel.entity.position = .init(x: 0, y: 0, z: -1.5)
            root.addChild(loadedModel.entity)
        } else if model.asset == nil {
            // Fall back to procedural geometry
            let node: GeometryNode
            switch model.name {
            case "Cube":
                node = GeometryNode.cube(
                    size: 0.8,
                    material: .pbr(color: .systemBlue, metallic: 0.7, roughness: 0.2),
                    cornerRadius: 0.04
                )
            case "Sphere":
                node = GeometryNode.sphere(
                    radius: 0.5,
                    material: .pbr(color: .systemRed, metallic: 0.85, roughness: 0.1)
                )
            case "Cylinder":
                node = GeometryNode.cylinder(radius: 0.35, height: 0.8, color: .systemGreen)
            default:
                node = GeometryNode.cube(size: 0.8, color: .systemBlue)
            }
            node.entity.position = .init(x: 0, y: 0, z: -1.5)
            root.addChild(node.entity)
        }
    }

    // MARK: - Overlays

    private var headerOverlay: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("SceneView")
                    .font(.largeTitle).bold()
                    .foregroundStyle(.white)
                    .accessibilityAddTraits(.isHeader)
                Text("3D & AR as declarative UI")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.7))
            }
            Spacer()
            Button {
                autoRotate.toggle()
                #if os(iOS)
                HapticManager.selectionChanged()
                #endif
            } label: {
                Image(systemName: autoRotate ? "rotate.3d.fill" : "rotate.3d")
                    .font(.title2)
                    .foregroundStyle(.white)
                    .padding(10)
                    .background(.ultraThinMaterial, in: Circle())
            }
            .accessibilityLabel(autoRotate ? "Disable auto-rotation" : "Enable auto-rotation")
        }
        .padding()
    }

    private var controlsOverlay: some View {
        VStack(spacing: 12) {
            // Environment selector
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(SceneEnvironment.allPresets, id: \.name) { env in
                        Button {
                            selectedEnvironment = env
                            #if os(iOS)
                            HapticManager.lightTap()
                            #endif
                        } label: {
                            Text(env.name)
                                .font(.caption2)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(
                                    selectedEnvironment.name == env.name
                                        ? AnyShapeStyle(.blue)
                                        : AnyShapeStyle(.white.opacity(0.15))
                                )
                                .clipShape(Capsule())
                                .foregroundStyle(.white)
                        }
                        .accessibilityLabel("Environment: \(env.name)")
                        .accessibilityAddTraits(selectedEnvironment.name == env.name ? .isSelected : [])
                    }
                }
            }

            // Model selector
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(models.enumerated()), id: \.offset) { index, model in
                        Button {
                            selectedIndex = index
                            #if os(iOS)
                            HapticManager.selectionChanged()
                            #endif
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: model.icon)
                                    .font(.body)
                                Text(model.name)
                                    .font(.caption2)
                            }
                            .frame(minWidth: 64)
                            .padding(.vertical, 8)
                            .padding(.horizontal, 4)
                            .background(
                                index == selectedIndex
                                    ? AnyShapeStyle(.blue)
                                    : AnyShapeStyle(.white.opacity(0.15))
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .foregroundStyle(.white)
                        }
                        .accessibilityLabel("Show \(model.name)")
                        .accessibilityAddTraits(index == selectedIndex ? .isSelected : [])
                    }
                }
            }

            Text("Pinch to zoom \u{00B7} Drag to orbit")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.5))
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding()
    }
}

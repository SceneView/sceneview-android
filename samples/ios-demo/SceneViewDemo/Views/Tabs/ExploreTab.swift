import SwiftUI
import RealityKit
import SceneViewSwift

/// Model data for the gallery.
struct ModelItem: Identifiable, Hashable {
    let id: String
    let name: String
    let icon: String
    let asset: String
    let scale: Float
    let category: ModelCategory

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: ModelItem, rhs: ModelItem) -> Bool { lhs.id == rhs.id }

    static let all: [ModelItem] = [
        // Vehicles
        ModelItem(id: "red_car", name: "Red Car", icon: "car.fill", asset: "red_car", scale: 1.0, category: .vehicles),
        ModelItem(id: "ferrari_f40", name: "Ferrari F40", icon: "car.fill", asset: "ferrari_f40", scale: 0.6, category: .vehicles),
        ModelItem(id: "porsche_911", name: "Porsche 911", icon: "car.fill", asset: "porsche_911", scale: 0.6, category: .vehicles),
        ModelItem(id: "porsche_911_turbo", name: "Porsche Turbo", icon: "car.side.fill", asset: "porsche_911_turbo", scale: 0.5, category: .vehicles),
        ModelItem(id: "lamborghini_countach", name: "Lamborghini", icon: "car.fill", asset: "lamborghini_countach", scale: 0.5, category: .vehicles),
        ModelItem(id: "shelby_cobra", name: "Shelby Cobra", icon: "car.fill", asset: "shelby_cobra", scale: 0.6, category: .vehicles),
        ModelItem(id: "bmw_m3_e30", name: "BMW M3 E30", icon: "car.fill", asset: "bmw_m3_e30", scale: 0.6, category: .vehicles),
        ModelItem(id: "mercedes_a45_amg", name: "Mercedes AMG", icon: "car.rear.fill", asset: "mercedes_a45_amg", scale: 0.5, category: .vehicles),
        ModelItem(id: "audi_tt", name: "Audi TT", icon: "car.side.fill", asset: "audi_tt", scale: 0.7, category: .vehicles),
        ModelItem(id: "fiat_punto", name: "Fiat Punto", icon: "car.rear.fill", asset: "fiat_punto", scale: 0.7, category: .vehicles),
        ModelItem(id: "tesla_cybertruck", name: "Cybertruck", icon: "truck.box.fill", asset: "tesla_cybertruck", scale: 0.6, category: .vehicles),
        ModelItem(id: "cyberpunk_car", name: "Cyberpunk Car", icon: "car.side.fill", asset: "cyberpunk_car", scale: 0.8, category: .vehicles),
        ModelItem(id: "cyberpunk_hovercar", name: "Hovercar", icon: "bolt.car.fill", asset: "cyberpunk_hovercar", scale: 0.6, category: .vehicles),

        // Creatures
        ModelItem(id: "animated_dragon", name: "Dragon", icon: "flame.fill", asset: "animated_dragon", scale: 0.6, category: .creatures),
        ModelItem(id: "black_dragon", name: "Black Dragon", icon: "lizard.fill", asset: "black_dragon", scale: 0.5, category: .creatures),
        ModelItem(id: "phoenix_bird", name: "Phoenix", icon: "bird.fill", asset: "phoenix_bird", scale: 0.8, category: .creatures),
        ModelItem(id: "animated_butterfly", name: "Butterfly", icon: "leaf.fill", asset: "animated_butterfly", scale: 0.8, category: .creatures),
        ModelItem(id: "mosquito_amber", name: "Mosquito in Amber", icon: "ant.fill", asset: "mosquito_amber", scale: 1.0, category: .creatures),
        ModelItem(id: "cyberpunk_character", name: "Cyber Guy", icon: "figure.stand", asset: "cyberpunk_character", scale: 0.7, category: .creatures),

        // Objects
        ModelItem(id: "game_boy_classic", name: "Game Boy", icon: "gamecontroller.fill", asset: "game_boy_classic", scale: 0.8, category: .objects),
        ModelItem(id: "nintendo_switch", name: "Switch", icon: "gamecontroller.fill", asset: "nintendo_switch", scale: 0.8, category: .objects),
        ModelItem(id: "ps5_dualsense", name: "PS5 Controller", icon: "gamecontroller.fill", asset: "ps5_dualsense", scale: 0.8, category: .objects),
        ModelItem(id: "nike_air_jordan", name: "Nike Air Jordan", icon: "shoe.fill", asset: "nike_air_jordan", scale: 0.8, category: .objects),
        ModelItem(id: "retro_piano", name: "Retro Piano", icon: "pianokeys", asset: "retro_piano", scale: 0.7, category: .objects),
        ModelItem(id: "fantasy_book", name: "Fantasy Book", icon: "book.fill", asset: "fantasy_book", scale: 0.7, category: .objects),

        // Scenes
        ModelItem(id: "tree_scene", name: "Tree Scene", icon: "tree.fill", asset: "tree_scene", scale: 0.6, category: .scenes),
        ModelItem(id: "ship_in_clouds", name: "Ship in Clouds", icon: "cloud.fill", asset: "ship_in_clouds", scale: 0.5, category: .scenes),
        ModelItem(id: "earthquake_california", name: "Earthquake", icon: "waveform.path", asset: "earthquake_california", scale: 0.4, category: .scenes),
    ]
}

enum ModelCategory: String, CaseIterable {
    case vehicles = "Vehicles"
    case creatures = "Creatures"
    case objects = "Objects"
    case scenes = "Scenes"
    case favorites = "Favorites"
}

/// The main explore tab -- a model gallery for browsing, viewing, and favoriting 3D models.
struct ExploreTab: View {
    @State private var selectedModel: ModelItem? = ModelItem.all.first
    @State private var selectedCategory: ModelCategory = .vehicles
    @State private var autoRotate = true
    @State private var showViewer = false
    @State private var searchText = ""
    private var favoritesManager = FavoritesManager.shared

    private var filteredModels: [ModelItem] {
        var models: [ModelItem]
        if selectedCategory == .favorites {
            models = ModelItem.all.filter { favoritesManager.isFavorite($0.id) }
        } else {
            models = ModelItem.all.filter { $0.category == selectedCategory }
        }
        if !searchText.isEmpty {
            models = models.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
        }
        return models
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                categoryPicker
                modelGallery
            }
            .navigationTitle("Explore")
            .searchable(text: $searchText, prompt: "Search models")
            .navigationDestination(item: $selectedModel) { model in
                ModelViewerScreen(model: model)
            }
        }
    }

    // MARK: - Category picker

    private var categoryPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(ModelCategory.allCases, id: \.self) { category in
                    let count = category == .favorites
                        ? ModelItem.all.filter { favoritesManager.isFavorite($0.id) }.count
                        : ModelItem.all.filter { $0.category == category }.count
                    Button {
                        selectedCategory = category
                        #if os(iOS)
                        HapticManager.selectionChanged()
                        #endif
                    } label: {
                        HStack(spacing: 4) {
                            if category == .favorites {
                                Image(systemName: "heart.fill")
                                    .font(.caption)
                            }
                            Text(category.rawValue)
                                .font(.subheadline.weight(.medium))
                            if count > 0 {
                                Text("\(count)")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(
                            selectedCategory == category
                                ? Color.accentColor
                                : Color(.secondarySystemBackground)
                        )
                        .foregroundStyle(selectedCategory == category ? .white : .primary)
                        .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
    }

    // MARK: - Model gallery grid

    private var modelGallery: some View {
        ScrollView {
            if filteredModels.isEmpty {
                emptyState
            } else {
                LazyVGrid(columns: [
                    GridItem(.adaptive(minimum: 150, maximum: 200), spacing: 12)
                ], spacing: 12) {
                    ForEach(filteredModels) { model in
                        ModelCard(model: model, isFavorite: favoritesManager.isFavorite(model.id)) {
                            selectedModel = model
                        } onToggleFavorite: {
                            favoritesManager.toggle(model.id)
                            #if os(iOS)
                            HapticManager.lightTap()
                            #endif
                        }
                    }
                }
                .padding()
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: selectedCategory == .favorites ? "heart.slash" : "magnifyingglass")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(selectedCategory == .favorites ? "No favorites yet" : "No models found")
                .font(.headline)
                .foregroundStyle(.secondary)
            if selectedCategory == .favorites {
                Text("Tap the heart icon on any model to save it here.")
                    .font(.subheadline)
                    .foregroundStyle(.tertiary)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(.top, 80)
    }
}

// MARK: - Model card

private struct ModelCard: View {
    let model: ModelItem
    let isFavorite: Bool
    let onTap: () -> Void
    let onToggleFavorite: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                ZStack(alignment: .topTrailing) {
                    // Model icon preview area
                    RoundedRectangle(cornerRadius: 12)
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color(.systemGray5),
                                    Color(.systemGray6)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(height: 120)
                        .overlay {
                            Image(systemName: model.icon)
                                .font(.system(size: 36))
                                .foregroundStyle(.secondary)
                        }

                    // Favorite button
                    Button(action: onToggleFavorite) {
                        Image(systemName: isFavorite ? "heart.fill" : "heart")
                            .font(.body)
                            .foregroundStyle(isFavorite ? .red : .secondary)
                            .padding(8)
                            .background(.ultraThinMaterial, in: Circle())
                    }
                    .padding(6)
                }

                VStack(spacing: 2) {
                    Text(model.name)
                        .font(.subheadline.weight(.medium))
                        .lineLimit(1)
                    Text(model.category.rawValue)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 4)
            }
            .padding(8)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(model.name), \(model.category.rawValue)")
        .accessibilityAddTraits(.isButton)
    }
}

// MARK: - Full-screen model viewer

struct ModelViewerScreen: View {
    let model: ModelItem
    @State private var autoRotate = true
    @State private var loadedModel: ModelNode?
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var selectedEnvironment: SceneEnvironment = .studio
    @State private var showShareSheet = false
    private var favoritesManager = FavoritesManager.shared

    var body: some View {
        ZStack {
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

            if isLoading {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(.white)
                    .scaleEffect(1.5)
            }

            if let errorMessage {
                VStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.title)
                        .foregroundStyle(.yellow)
                    Text("Failed to load model")
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.7))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .padding()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding()
            }

            VStack {
                Spacer()
                controlsOverlay
            }
        }
        .navigationTitle(model.name)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button {
                    favoritesManager.toggle(model.id)
                    #if os(iOS)
                    HapticManager.lightTap()
                    #endif
                } label: {
                    Image(systemName: favoritesManager.isFavorite(model.id) ? "heart.fill" : "heart")
                        .foregroundStyle(favoritesManager.isFavorite(model.id) ? .red : .white)
                }
                .accessibilityLabel(favoritesManager.isFavorite(model.id) ? "Remove from favorites" : "Add to favorites")

                Button {
                    autoRotate.toggle()
                    #if os(iOS)
                    HapticManager.selectionChanged()
                    #endif
                } label: {
                    Image(systemName: autoRotate ? "rotate.3d.fill" : "rotate.3d")
                        .foregroundStyle(.white)
                }
                .accessibilityLabel(autoRotate ? "Stop rotation" : "Start rotation")

                #if os(iOS)
                Button {
                    shareScreenshot()
                } label: {
                    Image(systemName: "square.and.arrow.up")
                        .foregroundStyle(.white)
                }
                .accessibilityLabel("Share screenshot")
                #endif
            }
        }
        .task {
            await loadModel()
        }
    }

    // MARK: - Model Loading

    private func loadModel() async {
        isLoading = true
        errorMessage = nil
        do {
            let node = try await ModelNode.load(model.asset)
            _ = node.scaleToUnits(model.scale)
            loadedModel = node
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    // MARK: - Scene

    @ViewBuilder
    private var sceneView: some View {
        if autoRotate {
            SceneView { root in
                if let loadedModel {
                    loadedModel.entity.position = .init(x: 0, y: 0, z: -1.5)
                    root.addChild(loadedModel.entity)
                }
            }
            .environment(selectedEnvironment)
            .cameraControls(.orbit)
            .autoRotate(speed: 0.4)
            .id("viewer-auto-\(loadedModel != nil)-\(selectedEnvironment.name)")
        } else {
            SceneView { root in
                if let loadedModel {
                    loadedModel.entity.position = .init(x: 0, y: 0, z: -1.5)
                    root.addChild(loadedModel.entity)
                }
            }
            .environment(selectedEnvironment)
            .cameraControls(.orbit)
            .id("viewer-manual-\(loadedModel != nil)-\(selectedEnvironment.name)")
        }
    }

    // MARK: - Controls

    private var controlsOverlay: some View {
        VStack(spacing: 12) {
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

    // MARK: - Share

    #if os(iOS)
    @MainActor
    private func shareScreenshot() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first else { return }

        let renderer = UIGraphicsImageRenderer(bounds: window.bounds)
        let image = renderer.image { ctx in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }

        let activityVC = UIActivityViewController(
            activityItems: [image, "Check out this 3D model in 3D & AR Explorer!"],
            applicationActivities: nil
        )

        if let presenter = window.rootViewController {
            if let popover = activityVC.popoverPresentationController {
                popover.sourceView = presenter.view
                popover.sourceRect = CGRect(x: presenter.view.bounds.midX, y: 40, width: 0, height: 0)
            }
            presenter.present(activityVC, animated: true)
        }
    }
    #endif
}

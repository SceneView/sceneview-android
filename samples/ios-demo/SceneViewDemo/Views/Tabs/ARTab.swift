#if os(iOS)
import SwiftUI
import RealityKit
import ARKit
import SceneViewSwift

/// AR tab — place 3D models in your real-world space.
///
/// Users can pick a model from the gallery, tap a surface to place it,
/// then resize and rotate using gestures. Includes screenshot sharing.
struct ARTab: View {
    @State private var placedCount = 0
    @State private var selectedModelIndex = 0
    @State private var errorMessage: String?
    @State private var showError = false
    @State private var showModelPicker = false

    private let arModels: [(name: String, icon: String, asset: String, scale: Float)] = [
        ("Game Boy", "gamecontroller.fill", "game_boy_classic", 0.15),
        ("Red Car", "car.fill", "red_car", 0.2),
        ("Dragon", "flame.fill", "animated_dragon", 0.12),
        ("Butterfly", "leaf.fill", "animated_butterfly", 0.15),
        ("Piano", "pianokeys", "retro_piano", 0.12),
        ("Nike Jordan", "shoe.fill", "nike_air_jordan", 0.15),
        ("Phoenix", "bird.fill", "phoenix_bird", 0.15),
        ("Fantasy Book", "book.fill", "fantasy_book", 0.12),
        ("PS5 Controller", "gamecontroller.fill", "ps5_dualsense", 0.15),
        ("Tree Scene", "tree.fill", "tree_scene", 0.1),
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                #if !targetEnvironment(simulator)
                arSceneView
                    .ignoresSafeArea()
                #else
                simulatorPlaceholder
                #endif

                VStack {
                    statusPill
                    Spacer()
                    modelSelector
                }
            }
            .navigationTitle("AR View")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        shareARScreenshot()
                    } label: {
                        Image(systemName: "square.and.arrow.up")
                    }
                    .accessibilityLabel("Share AR screenshot")
                }
            }
            .alert("AR Error", isPresented: $showError) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage ?? "An unknown error occurred.")
            }
        }
    }

    // MARK: - AR Scene

    #if !targetEnvironment(simulator)
    private var arSceneView: some View {
        ARSceneView(
            planeDetection: .both,
            showPlaneOverlay: true,
            showCoachingOverlay: true,
            onTapOnPlane: { position, arView in
                let selected = arModels[selectedModelIndex]

                Task {
                    do {
                        let modelNode = try await ModelNode.load(selected.asset)
                        _ = modelNode.scaleToUnits(selected.scale)

                        let anchor = AnchorNode.world(position: position)
                        anchor.add(modelNode.entity)
                        arView.scene.addAnchor(anchor.entity)

                        placedCount += 1
                        HapticManager.mediumTap()
                    } catch {
                        errorMessage = error.localizedDescription
                        showError = true
                        HapticManager.error()
                    }
                }
            }
        )
    }
    #endif

    // MARK: - Simulator placeholder

    private var simulatorPlaceholder: some View {
        VStack(spacing: 16) {
            Image(systemName: "arkit")
                .font(.system(size: 60))
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            Text("AR requires a physical device")
                .font(.headline)
            Text("Run on iPhone or iPad to place 3D models in your space.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
    }

    // MARK: - Status pill

    private var statusPill: some View {
        Group {
            if placedCount == 0 {
                Text("Point at a surface, then tap to place \(arModels[selectedModelIndex].name)")
            } else {
                Text("\(placedCount) object\(placedCount == 1 ? "" : "s") placed \u{2014} tap to add more")
            }
        }
        .font(.caption)
        .fontWeight(.medium)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(.ultraThinMaterial)
        .clipShape(Capsule())
        .padding(.top, 8)
    }

    // MARK: - Model selector

    private var modelSelector: some View {
        VStack(spacing: 8) {
            Text("Choose a model to place in your space")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.7))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(Array(arModels.enumerated()), id: \.offset) { index, model in
                        Button {
                            selectedModelIndex = index
                            HapticManager.selectionChanged()
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: model.icon)
                                    .font(.title3)
                                Text(model.name)
                                    .font(.caption2)
                                    .lineLimit(1)
                            }
                            .frame(minWidth: 72)
                            .padding(.vertical, 10)
                            .padding(.horizontal, 6)
                            .background(
                                index == selectedModelIndex
                                    ? AnyShapeStyle(.blue)
                                    : AnyShapeStyle(.white.opacity(0.15))
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .foregroundStyle(.white)
                        }
                        .accessibilityLabel("Place \(model.name)")
                        .accessibilityAddTraits(index == selectedModelIndex ? .isSelected : [])
                    }
                }
                .padding(.horizontal, 4)
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding()
    }

    // MARK: - Share

    @MainActor
    private func shareARScreenshot() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first else { return }

        let renderer = UIGraphicsImageRenderer(bounds: window.bounds)
        let image = renderer.image { ctx in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }

        let activityVC = UIActivityViewController(
            activityItems: [image, "Check out what I placed in my space with 3D & AR Explorer!"],
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
}
#endif

#if os(iOS)
import SwiftUI
import RealityKit
import ARKit
import SceneViewSwift

/// AR tab -- place geometry shapes and objects in your real-world space.
///
/// Uses ARSceneView with plane detection. Users pick from geometry shapes
/// (cube, sphere, cylinder, cone) and tap detected surfaces to place them.
struct ARTab: View {
    @State private var placedCount = 0
    @State private var selectedShapeIndex = 0
    @State private var errorMessage: String?
    @State private var showError = false
    @State private var selectedColor: Color = .blue

    private let arShapes: [(name: String, icon: String)] = [
        ("Cube", "cube.fill"),
        ("Sphere", "circle.fill"),
        ("Cylinder", "cylinder.fill"),
        ("Cone", "cone.fill"),
        ("Star", "star.fill"),
    ]

    private let colors: [Color] = [.blue, .red, .green, .orange, .purple, .yellow, .pink, .cyan]

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
                    controlsPanel
                }
            }
            .navigationTitle("AR")
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
                placeShape(at: position, in: arView)
            }
        )
    }

    private func placeShape(at position: SIMD3<Float>, in arView: ARView) {
        let uiColor = UIColor(selectedColor)
        let size: Float = 0.08

        let shape: GeometryNode
        switch selectedShapeIndex {
        case 0:
            shape = GeometryNode.cube(
                size: size,
                material: .pbr(color: uiColor, metallic: 0.6, roughness: 0.3),
                cornerRadius: 0.005
            )
        case 1:
            shape = GeometryNode.sphere(
                radius: size / 2,
                material: .pbr(color: uiColor, metallic: 0.7, roughness: 0.2)
            )
        case 2:
            shape = GeometryNode.cylinder(radius: size / 3, height: size, color: uiColor)
        case 3:
            shape = GeometryNode.cone(height: size, radius: size / 2.5, color: uiColor)
        default:
            // Star shape
            let star = ShapeNode.star(
                pointCount: 5,
                outerRadius: size / 2,
                innerRadius: size / 5,
                extrusionDepth: 0.01,
                color: uiColor
            )
            let anchor = AnchorNode.world(position: position)
            anchor.add(star.entity)
            arView.scene.addAnchor(anchor.entity)
            placedCount += 1
            HapticManager.mediumTap()
            return
        }

        let anchor = AnchorNode.world(position: position)
        anchor.add(shape.entity)
        arView.scene.addAnchor(anchor.entity)
        placedCount += 1
        HapticManager.mediumTap()
    }
    #endif

    // MARK: - Simulator Placeholder

    private var simulatorPlaceholder: some View {
        VStack(spacing: 16) {
            Image(systemName: "arkit")
                .font(.system(size: 60))
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            Text("AR requires a physical device")
                .font(.headline)
            Text("Run on iPhone or iPad to place 3D shapes in your space.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
    }

    // MARK: - Status

    private var statusPill: some View {
        Group {
            if placedCount == 0 {
                Text("Point at a surface, then tap to place a \(arShapes[selectedShapeIndex].name)")
            } else {
                Text("\(placedCount) object\(placedCount == 1 ? "" : "s") placed -- tap to add more")
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

    // MARK: - Controls

    private var controlsPanel: some View {
        VStack(spacing: 12) {
            // Color picker
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(colors, id: \.self) { color in
                        Button {
                            selectedColor = color
                            HapticManager.selectionChanged()
                        } label: {
                            Circle()
                                .fill(color)
                                .frame(width: 32, height: 32)
                                .overlay {
                                    if selectedColor == color {
                                        Circle()
                                            .strokeBorder(.white, lineWidth: 3)
                                    }
                                }
                        }
                        .accessibilityLabel("Color: \(color.description)")
                    }
                }
                .padding(.horizontal, 4)
            }

            // Shape selector
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(Array(arShapes.enumerated()), id: \.offset) { index, shape in
                        Button {
                            selectedShapeIndex = index
                            HapticManager.selectionChanged()
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: shape.icon)
                                    .font(.title3)
                                Text(shape.name)
                                    .font(.caption2)
                                    .lineLimit(1)
                            }
                            .frame(minWidth: 64)
                            .padding(.vertical, 10)
                            .padding(.horizontal, 6)
                            .background(
                                index == selectedShapeIndex
                                    ? AnyShapeStyle(.blue)
                                    : AnyShapeStyle(.white.opacity(0.15))
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .foregroundStyle(.white)
                        }
                        .accessibilityLabel("Place \(shape.name)")
                        .accessibilityAddTraits(index == selectedShapeIndex ? .isSelected : [])
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
            activityItems: [image, "Created with 3D & AR Explorer -- SceneView SDK"],
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

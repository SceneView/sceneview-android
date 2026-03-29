#if os(iOS)
import SwiftUI
import RealityKit
import ARKit
import SceneViewSwift

/// AR tab with plane detection, tap-to-place, multiple shapes, and gesture controls.
struct ARTab: View {
    @State private var placedObjects: [PlacedObject] = []
    @State private var selectedShape: ARShape = .cube
    @State private var showShapePicker = false
    @State private var errorMessage: String?
    @State private var showError = false

    struct PlacedObject: Identifiable {
        let id = UUID()
        let shape: ARShape
        let position: SIMD3<Float>
    }

    enum ARShape: String, CaseIterable {
        case cube = "Cube"
        case sphere = "Sphere"
        case cylinder = "Cylinder"
        case cone = "Cone"

        var icon: String {
            switch self {
            case .cube: return "cube.fill"
            case .sphere: return "globe"
            case .cylinder: return "cylinder.fill"
            case .cone: return "cone.fill"
            }
        }

        var color: UIColor {
            switch self {
            case .cube: return .systemBlue
            case .sphere: return .systemRed
            case .cylinder: return .systemGreen
            case .cone: return .systemOrange
            }
        }

        func makeNode() -> GeometryNode {
            switch self {
            case .cube:
                return GeometryNode.cube(
                    size: 0.08,
                    material: .pbr(color: color, metallic: 0.5, roughness: 0.3),
                    cornerRadius: 0.005
                ).withGroundingShadow()
            case .sphere:
                return GeometryNode.sphere(
                    radius: 0.05,
                    material: .pbr(color: color, metallic: 0.7, roughness: 0.2)
                ).withGroundingShadow()
            case .cylinder:
                return GeometryNode.cylinder(
                    radius: 0.04,
                    height: 0.1,
                    color: color
                ).withGroundingShadow()
            case .cone:
                return GeometryNode.cone(
                    height: 0.1,
                    radius: 0.05,
                    color: color
                ).withGroundingShadow()
            }
        }
    }

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
                    bottomControls
                }
            }
            .navigationTitle("AR")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        placedObjects.removeAll()
                        HapticManager.lightTap()
                    } label: {
                        Image(systemName: "trash")
                    }
                    .disabled(placedObjects.isEmpty)
                    .accessibilityLabel("Clear all placed objects")
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
                let shape = selectedShape
                let node = shape.makeNode()

                let anchor = AnchorNode.world(position: position)
                anchor.add(node.entity)
                arView.scene.addAnchor(anchor.entity)

                placedObjects.append(PlacedObject(shape: shape, position: position))
                HapticManager.mediumTap()
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
            Text("Run on iPhone or iPad to test AR features.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
    }

    // MARK: - Status pill

    private var statusPill: some View {
        Group {
            if placedObjects.isEmpty {
                Text("Point at a surface and tap to place \(selectedShape.rawValue.lowercased())")
            } else {
                Text("\(placedObjects.count) object\(placedObjects.count == 1 ? "" : "s") placed")
            }
        }
        .font(.caption)
        .fontWeight(.medium)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(.ultraThinMaterial)
        .clipShape(Capsule())
        .padding(.top, 8)
        .accessibilityLabel(
            placedObjects.isEmpty
                ? "Point at a surface and tap to place an object"
                : "\(placedObjects.count) \(placedObjects.count == 1 ? "object" : "objects") placed in the scene"
        )
    }

    // MARK: - Bottom controls

    private var bottomControls: some View {
        HStack(spacing: 12) {
            ForEach(ARShape.allCases, id: \.self) { shape in
                Button {
                    selectedShape = shape
                    HapticManager.selectionChanged()
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: shape.icon)
                            .font(.title3)
                        Text(shape.rawValue)
                            .font(.caption2)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(
                        selectedShape == shape
                            ? AnyShapeStyle(.blue)
                            : AnyShapeStyle(.white.opacity(0.15))
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .foregroundStyle(.white)
                }
                .accessibilityLabel("Place \(shape.rawValue)")
                .accessibilityAddTraits(selectedShape == shape ? .isSelected : [])
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding()
    }
}
#endif

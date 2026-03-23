import SwiftUI
import RealityKit
import SceneViewSwift

/// AR tap-to-place demo — place 3D objects on detected surfaces.
///
/// Mirrors the Android demo's AR tab with plane detection
/// and tap-to-place functionality.
#if !os(visionOS)
struct ARDemoView: View {
    @State private var objectCount = 0
    @State private var selectedShape: PlaceableShape = .cube
    @State private var showInstructions = true

    enum PlaceableShape: String, CaseIterable, Identifiable {
        case cube = "Cube"
        case sphere = "Sphere"
        case cylinder = "Cylinder"

        var id: String { rawValue }
        var icon: String {
            switch self {
            case .cube: return "cube.fill"
            case .sphere: return "globe"
            case .cylinder: return "cylinder.fill"
            }
        }
    }

    var body: some View {
        ZStack {
            // AR scene
            ARSceneView(
                planeDetection: .horizontal,
                showPlaneOverlay: true,
                showCoachingOverlay: true,
                onTapOnPlane: { position, arView in
                    placeObject(at: position, in: arView)
                }
            )
            .ignoresSafeArea()

            // UI overlay
            VStack {
                if showInstructions {
                    instructionsBanner
                }

                Spacer()

                bottomControls
            }
        }
    }

    // MARK: - Object placement

    private func placeObject(at position: SIMD3<Float>, in arView: ARView) {
        let anchor = AnchorNode.world(position: position)
        let entity: Entity

        switch selectedShape {
        case .cube:
            entity = GeometryNode.cube(
                size: 0.1,
                color: .systemBlue,
                cornerRadius: 0.005
            ).withGroundingShadow().entity
        case .sphere:
            entity = GeometryNode.sphere(
                radius: 0.05,
                material: .pbr(
                    color: .systemOrange,
                    metallic: 0.7,
                    roughness: 0.3
                )
            ).withGroundingShadow().entity
        case .cylinder:
            entity = GeometryNode.cylinder(
                radius: 0.04,
                height: 0.1,
                color: .systemGreen
            ).withGroundingShadow().entity
        }

        anchor.add(entity)
        arView.scene.addAnchor(anchor.entity)

        objectCount += 1
        showInstructions = false
    }

    // MARK: - UI

    private var instructionsBanner: some View {
        HStack {
            Image(systemName: "hand.tap.fill")
            Text("Tap on a surface to place an object")
        }
        .font(.subheadline)
        .padding(12)
        .background(.ultraThinMaterial)
        .clipShape(Capsule())
        .padding(.top, 60)
        .transition(.move(edge: .top).combined(with: .opacity))
    }

    private var bottomControls: some View {
        VStack(spacing: 12) {
            // Shape picker
            HStack(spacing: 16) {
                ForEach(PlaceableShape.allCases) { shape in
                    Button {
                        selectedShape = shape
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: shape.icon)
                                .font(.title2)
                            Text(shape.rawValue)
                                .font(.caption2)
                        }
                        .frame(width: 56, height: 56)
                        .background(
                            selectedShape == shape
                                ? Color.accentColor
                                : Color(.systemGray5)
                        )
                        .foregroundStyle(
                            selectedShape == shape
                                ? .white : .primary
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }

            // Object count
            Text("\(objectCount) object\(objectCount == 1 ? "" : "s") placed")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding()
    }
}

#Preview {
    ARDemoView()
}
#endif

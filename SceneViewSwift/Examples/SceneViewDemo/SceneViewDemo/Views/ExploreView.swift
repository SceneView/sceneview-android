import SwiftUI
import RealityKit
import SceneViewSwift

/// 3D model viewer with orbit camera and environment switching.
///
/// Mirrors the Android demo's Explore tab — full-screen 3D viewer
/// with environment picker and orbit camera controls.
struct ExploreView: View {
    @State private var selectedEnvironment: SceneEnvironment = .studio
    @State private var selectedScene: DemoScene = .shapes

    enum DemoScene: String, CaseIterable, Identifiable {
        case shapes = "Shapes"
        case metallic = "Metallic"
        case text3D = "3D Text"
        case gizmo = "Gizmo"

        var id: String { rawValue }
        var icon: String {
            switch self {
            case .shapes: return "cube.transparent"
            case .metallic: return "circle.hexagongrid"
            case .text3D: return "textformat.abc"
            case .gizmo: return "move.3d"
            }
        }
    }

    var body: some View {
        ZStack {
            // Full-screen 3D scene
            sceneForDemo(selectedScene)
                .ignoresSafeArea()

            // Overlay controls
            VStack {
                // Scene picker at top
                scenePicker
                    .padding(.top, 60)

                Spacer()

                // Environment picker at bottom
                environmentPicker
            }
        }
    }

    // MARK: - Scenes

    @ViewBuilder
    private func sceneForDemo(_ demo: DemoScene) -> some View {
        switch demo {
        case .shapes:
            shapesScene
        case .metallic:
            metallicScene
        case .text3D:
            textScene
        case .gizmo:
            gizmoScene
        }
    }

    private var shapesScene: some View {
        SceneView { root in
            // Rounded cube
            let cube = GeometryNode.cube(
                size: 0.3,
                color: .systemBlue,
                cornerRadius: 0.03
            )
            .position(.init(x: -0.5, y: 0.15, z: 0))
            .withGroundingShadow()
            root.addChild(cube.entity)

            // Metallic sphere
            let sphere = GeometryNode.sphere(
                radius: 0.2,
                material: .pbr(color: .systemOrange, metallic: 0.8, roughness: 0.2)
            )
            .position(.init(x: 0, y: 0.2, z: 0))
            .withGroundingShadow()
            root.addChild(sphere.entity)

            // Cylinder
            let cylinder = GeometryNode.cylinder(
                radius: 0.15,
                height: 0.5,
                color: .systemGreen
            )
            .position(.init(x: 0.5, y: 0.25, z: 0))
            .withGroundingShadow()
            root.addChild(cylinder.entity)

            // Floor
            let floor = GeometryNode.plane(
                width: 3.0,
                depth: 3.0,
                color: .init(white: 0.12, alpha: 1.0)
            )
            root.addChild(floor.entity)

            // Title
            let title = BillboardNode.text(
                "SceneView iOS",
                fontSize: 0.04,
                color: .white
            )
            .position(.init(x: 0, y: 0.7, z: 0))
            root.addChild(title.entity)
        }
        .environment(selectedEnvironment)
        .cameraControls(.orbit)
    }

    private var metallicScene: some View {
        SceneView { root in
            // Row of spheres with varying roughness
            let count = 5
            for i in 0..<count {
                let roughness = Float(i) / Float(count - 1)
                let x = Float(i - count / 2) * 0.25

                let sphere = GeometryNode.sphere(
                    radius: 0.1,
                    material: .pbr(color: .gray, metallic: 1.0, roughness: roughness)
                )
                .position(.init(x: x, y: 0.15, z: 0))
                .withGroundingShadow()
                root.addChild(sphere.entity)

                // Label under each sphere
                let label = BillboardNode.text(
                    String(format: "%.0f%%", roughness * 100),
                    fontSize: 0.02,
                    color: .lightGray
                )
                .position(.init(x: x, y: -0.05, z: 0))
                root.addChild(label.entity)
            }

            // Title
            let title = BillboardNode.text(
                "Roughness Gradient",
                fontSize: 0.03,
                color: .white
            )
            .position(.init(x: 0, y: 0.4, z: 0))
            root.addChild(title.entity)

            // Floor
            let floor = GeometryNode.plane(
                width: 2.0,
                depth: 2.0,
                color: .init(white: 0.08, alpha: 1.0)
            )
            root.addChild(floor.entity)
        }
        .environment(selectedEnvironment)
        .cameraControls(.orbit)
    }

    private var textScene: some View {
        SceneView { root in
            // Large 3D title
            let title = TextNode(
                text: "SceneView",
                fontSize: 0.12,
                color: .systemBlue,
                depth: 0.03
            )
            .centered()
            .position(.init(x: 0, y: 0.2, z: 0))
            root.addChild(title.entity)

            // Subtitle
            let subtitle = TextNode(
                text: "3D & AR for iOS",
                fontSize: 0.05,
                color: .white,
                depth: 0.01
            )
            .centered()
            .position(.init(x: 0, y: 0, z: 0))
            root.addChild(subtitle.entity)

            // Floor
            let floor = GeometryNode.plane(
                width: 2.0,
                depth: 2.0,
                color: .init(white: 0.1, alpha: 1.0)
            )
            .position(.init(x: 0, y: -0.15, z: 0))
            root.addChild(floor.entity)
        }
        .environment(selectedEnvironment)
        .cameraControls(.orbit)
    }

    private var gizmoScene: some View {
        SceneView { root in
            // Large axis gizmo
            for line in LineNode.axisGizmo(
                at: .zero,
                length: 0.6,
                thickness: 0.008
            ) {
                root.addChild(line.entity)
            }

            // Axis labels
            let xLabel = BillboardNode.text("X", fontSize: 0.04, color: .red)
                .position(.init(x: 0.7, y: 0, z: 0))
            root.addChild(xLabel.entity)

            let yLabel = BillboardNode.text("Y", fontSize: 0.04, color: .green)
                .position(.init(x: 0, y: 0.7, z: 0))
            root.addChild(yLabel.entity)

            let zLabel = BillboardNode.text("Z", fontSize: 0.04, color: .blue)
                .position(.init(x: 0, y: 0, z: 0.7))
            root.addChild(zLabel.entity)

            // Small sphere at origin
            let origin = GeometryNode.sphere(radius: 0.03, color: .white)
            root.addChild(origin.entity)
        }
        .environment(selectedEnvironment)
        .cameraControls(.orbit)
    }

    // MARK: - UI Controls

    private var scenePicker: some View {
        HStack(spacing: 8) {
            ForEach(DemoScene.allCases) { scene in
                Button {
                    withAnimation(.spring(duration: 0.3)) {
                        selectedScene = scene
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: scene.icon)
                            .font(.caption)
                        Text(scene.rawValue)
                            .font(.caption2)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(
                        selectedScene == scene
                            ? Color.accentColor
                            : Color(.systemGray5).opacity(0.8)
                    )
                    .foregroundStyle(
                        selectedScene == scene ? .white : .primary
                    )
                    .clipShape(Capsule())
                }
            }
        }
        .padding(8)
        .background(.ultraThinMaterial)
        .clipShape(Capsule())
    }

    private var environmentPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(
                    Array(SceneEnvironment.allPresets.enumerated()),
                    id: \.offset
                ) { _, env in
                    Button {
                        withAnimation(.spring(duration: 0.3)) {
                            selectedEnvironment = env
                        }
                    } label: {
                        Text(env.name)
                            .font(.caption)
                            .fontWeight(
                                env.name == selectedEnvironment.name
                                    ? .bold : .regular
                            )
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                env.name == selectedEnvironment.name
                                    ? Color.accentColor
                                    : Color(.systemGray5).opacity(0.8)
                            )
                            .foregroundStyle(
                                env.name == selectedEnvironment.name
                                    ? .white : .primary
                            )
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 12)
        .background(.ultraThinMaterial)
    }
}

#Preview {
    ExploreView()
}

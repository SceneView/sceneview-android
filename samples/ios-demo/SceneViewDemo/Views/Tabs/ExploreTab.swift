import SwiftUI
import RealityKit
import SceneViewSwift

/// The main 3D explore tab -- showcases a rotating shape with controls.
struct ExploreTab: View {
    private let shapes: [(name: String, icon: String)] = [
        ("Cube", "cube.fill"),
        ("Sphere", "globe"),
        ("Cylinder", "cylinder.fill"),
        ("Cone", "cone.fill"),
        ("Plane", "square.fill"),
    ]

    @State private var selectedIndex = 0
    @State private var autoRotate = true
    @State private var showControls = true

    var body: some View {
        ZStack {
            sceneView
                .ignoresSafeArea()

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
    }

    // MARK: - Scene

    @ViewBuilder
    private var sceneView: some View {
        if autoRotate {
            SceneView { root in
                buildShape(into: root)
                addGridFloor(to: root)
            }
            .cameraControls(.orbit)
            .autoRotate(speed: 0.4)
            .id("shape-\(selectedIndex)-auto")
        } else {
            SceneView { root in
                buildShape(into: root)
                addGridFloor(to: root)
            }
            .cameraControls(.orbit)
            .id("shape-\(selectedIndex)-manual")
        }
    }

    private func buildShape(into root: Entity) {
        let node: GeometryNode
        switch selectedIndex {
        case 0:
            node = GeometryNode.cube(
                size: 0.4,
                material: .pbr(color: .systemBlue, metallic: 0.6, roughness: 0.3),
                cornerRadius: 0.02
            )
        case 1:
            node = GeometryNode.sphere(
                radius: 0.25,
                material: .pbr(color: .systemRed, metallic: 0.8, roughness: 0.15)
            )
        case 2:
            node = GeometryNode.cylinder(radius: 0.2, height: 0.5, color: .systemGreen)
        case 3:
            node = GeometryNode.cone(height: 0.5, radius: 0.25, color: .systemOrange)
        case 4:
            node = GeometryNode.plane(width: 0.6, depth: 0.6, color: .systemPurple)
        default:
            node = GeometryNode.cube(size: 0.4, color: .systemBlue)
        }
        node.entity.position = .init(x: 0, y: 0, z: -1.5)
        root.addChild(node.entity)
    }

    private func addGridFloor(to root: Entity) {
        for x in stride(from: Float(-1.0), through: 1.0, by: 0.25) {
            for z in stride(from: Float(-2.5), through: -0.5, by: 0.25) {
                let dot = GeometryNode.cube(size: 0.015, color: .darkGray, cornerRadius: 0)
                dot.entity.position = .init(x: x, y: -0.35, z: z)
                root.addChild(dot.entity)
            }
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
                Text("3D & AR for SwiftUI")
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
            // Shape selector
            HStack(spacing: 8) {
                ForEach(Array(shapes.enumerated()), id: \.offset) { index, shape in
                    Button {
                        selectedIndex = index
                        #if os(iOS)
                        HapticManager.selectionChanged()
                        #endif
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: shape.icon)
                                .font(.body)
                            Text(shape.name)
                                .font(.caption2)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(
                            index == selectedIndex
                                ? AnyShapeStyle(.blue)
                                : AnyShapeStyle(.white.opacity(0.15))
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .foregroundStyle(.white)
                    }
                    .accessibilityLabel("Show \(shape.name)")
                    .accessibilityAddTraits(index == selectedIndex ? .isSelected : [])
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

import SwiftUI
import RealityKit
import SceneViewSwift

/// The main 3D explore tab -- showcases a rotating shape with controls.
///
/// Mirrors the Android ExploreScreen style: full-screen 3D scene with
/// gradient overlays, shape/environment selectors, and auto-rotation.
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
            }
            .cameraControls(.orbit)
            .autoRotate(speed: 0.4)
            .id("shape-\(selectedIndex)-auto")
        } else {
            SceneView { root in
                buildShape(into: root)
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
                size: 0.8,
                material: .pbr(color: .systemBlue, metallic: 0.7, roughness: 0.2),
                cornerRadius: 0.04
            )
        case 1:
            node = GeometryNode.sphere(
                radius: 0.5,
                material: .pbr(color: .systemRed, metallic: 0.85, roughness: 0.1)
            )
        case 2:
            node = GeometryNode.cylinder(radius: 0.35, height: 0.8, color: .systemGreen)
        case 3:
            node = GeometryNode.cone(height: 0.9, radius: 0.45, color: .systemOrange)
        case 4:
            node = GeometryNode.plane(width: 1.0, depth: 1.0, color: .systemPurple)
        default:
            node = GeometryNode.cube(size: 0.8, color: .systemBlue)
        }
        // Position shape at scene center (orbit camera handles distance)
        node.entity.position = .init(x: 0, y: 0, z: -1.5)
        root.addChild(node.entity)
    }

    // MARK: - Overlays

    private var headerOverlay: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("SceneView")
                    .font(.largeTitle).bold()
                    .foregroundStyle(.white)
                    .accessibilityAddTraits(.isHeader)
                Text("✨ 3D & AR for Jetpack Compose")
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

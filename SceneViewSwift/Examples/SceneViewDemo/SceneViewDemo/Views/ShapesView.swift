import SwiftUI
import RealityKit
import SceneViewSwift

/// Procedural geometry showcase — demonstrates all primitive shapes.
///
/// Mirrors the Android demo's Showcase tab with live 3D previews
/// of each geometry type.
struct ShapesView: View {
    @State private var selectedShape: ShapeType = .cube

    enum ShapeType: String, CaseIterable, Identifiable {
        case cube = "Cube"
        case sphere = "Sphere"
        case cylinder = "Cylinder"
        case cone = "Cone"
        case plane = "Plane"
        case text = "Text"
        case line = "Lines"
        case path = "Path"
        case grid = "Grid"

        var id: String { rawValue }
        var icon: String {
            switch self {
            case .cube: return "cube.fill"
            case .sphere: return "globe"
            case .cylinder: return "cylinder.fill"
            case .cone: return "cone.fill"
            case .plane: return "square.fill"
            case .text: return "textformat.abc"
            case .line: return "line.diagonal"
            case .path: return "point.topleft.down.to.point.bottomright.curvepath"
            case .grid: return "grid"
            }
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // 3D Preview
                sceneForShape(selectedShape)
                    .frame(maxWidth: .infinity)
                    .frame(height: 400)
                    .background(Color(.systemBackground))

                Divider()

                // Shape picker
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(ShapeType.allCases) { shape in
                            Button {
                                withAnimation(.spring(duration: 0.3)) {
                                    selectedShape = shape
                                }
                            } label: {
                                VStack(spacing: 4) {
                                    Image(systemName: shape.icon)
                                        .font(.title2)
                                    Text(shape.rawValue)
                                        .font(.caption2)
                                }
                                .frame(width: 60, height: 60)
                                .background(
                                    selectedShape == shape
                                        ? Color.accentColor.opacity(0.15)
                                        : Color(.systemGray6)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(
                                            selectedShape == shape
                                                ? Color.accentColor
                                                : Color.clear,
                                            lineWidth: 2
                                        )
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding()
                }

                // Code snippet
                codeSnippet(for: selectedShape)
                    .padding()

                Spacer()
            }
            .navigationTitle("Shapes")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    // MARK: - Scene per shape

    @ViewBuilder
    private func sceneForShape(_ shape: ShapeType) -> some View {
        SceneView { root in
            switch shape {
            case .cube:
                let node = GeometryNode.cube(
                    size: 0.4,
                    color: .systemBlue,
                    cornerRadius: 0.02
                )
                .withGroundingShadow()
                root.addChild(node.entity)

            case .sphere:
                let node = GeometryNode.sphere(
                    radius: 0.3,
                    material: .pbr(
                        color: .systemOrange,
                        metallic: 0.8,
                        roughness: 0.2
                    )
                )
                .withGroundingShadow()
                root.addChild(node.entity)

            case .cylinder:
                let node = GeometryNode.cylinder(
                    radius: 0.2,
                    height: 0.5,
                    color: .systemGreen
                )
                .withGroundingShadow()
                root.addChild(node.entity)

            case .cone:
                let node = GeometryNode.cone(
                    height: 0.5,
                    radius: 0.25,
                    color: .systemPurple
                )
                .withGroundingShadow()
                root.addChild(node.entity)

            case .plane:
                let node = GeometryNode.plane(
                    width: 0.6,
                    depth: 0.6,
                    color: .systemTeal
                )
                root.addChild(node.entity)

            case .text:
                let text = TextNode(
                    text: "SceneView",
                    fontSize: 0.08,
                    color: .white,
                    depth: 0.02
                )
                .centered()
                root.addChild(text.entity)

            case .line:
                for line in LineNode.axisGizmo(
                    at: .zero,
                    length: 0.4,
                    thickness: 0.006
                ) {
                    root.addChild(line.entity)
                }

            case .path:
                // Star path
                let starPoints: [SIMD3<Float>] = (0..<10).map { i in
                    let angle = Float(i) / 10.0 * 2 * .pi - .pi / 2
                    let r: Float = i % 2 == 0 ? 0.4 : 0.2
                    return SIMD3<Float>(r * cos(angle), r * sin(angle), 0)
                }
                let star = PathNode(
                    points: starPoints,
                    closed: true,
                    thickness: 0.006,
                    color: .systemYellow
                )
                root.addChild(star.entity)

            case .grid:
                let grid = PathNode.grid(
                    size: 1.0,
                    divisions: 8,
                    thickness: 0.002,
                    color: .lightGray
                )
                root.addChild(grid.entity)

                // Add axis gizmo at center
                for line in LineNode.axisGizmo(
                    at: .zero,
                    length: 0.6,
                    thickness: 0.004
                ) {
                    root.addChild(line.entity)
                }
            }
        }
        .cameraControls(.orbit)
    }

    // MARK: - Code snippets

    private func codeSnippet(for shape: ShapeType) -> some View {
        let code: String = switch shape {
        case .cube:
            "GeometryNode.cube(size: 0.4, color: .blue)"
        case .sphere:
            "GeometryNode.sphere(radius: 0.3, material: .pbr(metallic: 0.8))"
        case .cylinder:
            "GeometryNode.cylinder(radius: 0.2, height: 0.5)"
        case .cone:
            "GeometryNode.cone(height: 0.5, radius: 0.25)"
        case .plane:
            "GeometryNode.plane(width: 0.6, depth: 0.6)"
        case .text:
            "TextNode(text: \"SceneView\", fontSize: 0.08)"
        case .line:
            "LineNode.axisGizmo(at: .zero, length: 0.4)"
        case .path:
            "PathNode(points: [...], closed: true, color: .yellow)"
        case .grid:
            "PathNode.grid(size: 1.0, divisions: 8)"
        }

        return Text(code)
            .font(.system(.caption, design: .monospaced))
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemGray6))
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

#Preview {
    ShapesView()
}

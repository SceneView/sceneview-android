import SwiftUI
import RealityKit
import SceneViewSwift

/// Lines, paths, circles, grids, and helix -- comprehensive polyline showcase.
struct LinesPathsDemo: View {
    var body: some View {
        ZStack {
            SceneView { root in
                // -- Helix path --
                let stepCount = 60
                for i in 0..<stepCount {
                    let t = Float(i) / Float(stepCount) * 4 * .pi
                    let progress = Float(i) / Float(stepCount)
                    let color = UIColor(
                        hue: CGFloat(0.55 + progress * 0.3),
                        saturation: 0.9,
                        brightness: 1.0,
                        alpha: 1.0
                    )
                    let dot = GeometryNode.sphere(
                        radius: 0.012,
                        material: .pbr(color: color, metallic: 0.8, roughness: 0.2)
                    )
                    dot.entity.position = .init(
                        x: cos(t) * 0.3 - 0.5,
                        y: progress * 0.8 - 0.4,
                        z: sin(t) * 0.3 - 2.0
                    )
                    root.addChild(dot.entity)
                }

                // Central axis for helix
                let axis = GeometryNode.cylinder(radius: 0.004, height: 0.8, color: .darkGray)
                axis.entity.position = .init(x: -0.5, y: 0, z: -2)
                root.addChild(axis.entity)

                // -- Circle path --
                let circle = PathNode.circle(
                    center: .init(x: 0.4, y: -0.3, z: -2),
                    radius: 0.25,
                    segments: 48,
                    thickness: 0.004,
                    color: .systemYellow
                )
                root.addChild(circle.entity)

                // -- Triangle path --
                let triangle = PathNode(
                    points: [
                        .init(x: 0.4, y: 0.3, z: -2),
                        .init(x: 0.2, y: -0.05, z: -2),
                        .init(x: 0.6, y: -0.05, z: -2),
                    ],
                    closed: true,
                    thickness: 0.005,
                    color: .systemCyan
                )
                root.addChild(triangle.entity)

                // -- Axis gizmo at origin area --
                let gizmo = LineNode.axisGizmo(
                    at: .init(x: 0, y: -0.35, z: -1.5),
                    length: 0.15,
                    thickness: 0.004
                )
                for line in gizmo {
                    root.addChild(line.entity)
                }

                // -- Grid floor --
                let grid = PathNode.grid(
                    size: 1.5,
                    divisions: 8,
                    thickness: 0.002,
                    color: .init(white: 0.2, alpha: 1.0)
                ).position(.init(x: 0, y: -0.4, z: -2))
                root.addChild(grid.entity)
            }
            .cameraControls(.orbit)
            .ignoresSafeArea()

            VStack {
                Spacer()
                HStack(spacing: 16) {
                    legendDot(color: .cyan, label: "PathNode")
                    legendDot(color: .yellow, label: "Circle")
                    legendDot(color: .blue, label: "Helix")
                    legendDot(color: .red, label: "Gizmo")
                }
                .font(.caption2)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(.ultraThinMaterial)
                .clipShape(Capsule())
                .padding(.bottom, 12)
            }
        }
        .background(Color.black)
    }

    private func legendDot(color: Color, label: String) -> some View {
        HStack(spacing: 4) {
            Circle().fill(color).frame(width: 8, height: 8)
            Text(label).foregroundStyle(.white.opacity(0.7))
        }
    }
}

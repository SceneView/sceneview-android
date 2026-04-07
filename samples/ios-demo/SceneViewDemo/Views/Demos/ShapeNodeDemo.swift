import SwiftUI
import RealityKit
import SceneViewSwift

/// ShapeNode demo -- star, hexagon, custom polygon with extrusion.
struct ShapeNodeDemo: View {
    @State private var selectedShape: Int = 0
    @State private var extrusionDepth: Float = 0.05

    private let shapeNames = ["Star", "Hexagon", "Pentagon", "Arrow", "L-Shape"]
    private let shapeIcons = ["star.fill", "hexagon.fill", "pentagon.fill", "arrowshape.right.fill", "l.square.fill"]

    var body: some View {
        ZStack {
            SceneView { root in
                let shape: ShapeNode
                switch selectedShape {
                case 0:
                    shape = ShapeNode.star(
                        pointCount: 5,
                        outerRadius: 0.3,
                        innerRadius: 0.12,
                        extrusionDepth: extrusionDepth,
                        color: .systemYellow
                    )
                case 1:
                    shape = ShapeNode.regularPolygon(
                        sides: 6,
                        radius: 0.25,
                        extrusionDepth: extrusionDepth,
                        color: .systemCyan
                    )
                case 2:
                    shape = ShapeNode.regularPolygon(
                        sides: 5,
                        radius: 0.25,
                        extrusionDepth: extrusionDepth,
                        color: .systemGreen
                    )
                case 3:
                    // Arrow polygon
                    shape = ShapeNode(
                        points: [
                            SIMD2<Float>(0, 0.15),
                            SIMD2<Float>(0.3, 0.15),
                            SIMD2<Float>(0.3, 0.3),
                            SIMD2<Float>(0.5, 0),
                            SIMD2<Float>(0.3, -0.3),
                            SIMD2<Float>(0.3, -0.15),
                            SIMD2<Float>(0, -0.15),
                        ],
                        extrusionDepth: extrusionDepth,
                        color: .systemOrange
                    )
                default:
                    // L-shape
                    shape = ShapeNode(
                        points: [
                            SIMD2<Float>(0, 0),
                            SIMD2<Float>(0.4, 0),
                            SIMD2<Float>(0.4, 0.15),
                            SIMD2<Float>(0.15, 0.15),
                            SIMD2<Float>(0.15, 0.5),
                            SIMD2<Float>(0, 0.5),
                        ],
                        extrusionDepth: extrusionDepth,
                        color: .systemPurple
                    )
                }

                shape.entity.position = .init(x: 0, y: 0, z: -2)
                root.addChild(shape.entity)

                // Grid reference
                let grid = PathNode.grid(
                    size: 1.5,
                    divisions: 8,
                    thickness: 0.002,
                    color: .init(white: 0.2, alpha: 1.0)
                ).position(.init(x: 0, y: -0.4, z: -2))
                root.addChild(grid.entity)

                // Label
                let label = TextNode(
                    text: shapeNames[selectedShape],
                    fontSize: 0.05,
                    color: .white,
                    depth: 0.008
                )
                .centered()
                .position(.init(x: 0, y: -0.5, z: -2))
                root.addChild(label.entity)
            }
            .cameraControls(.orbit)
            .autoRotate(speed: 0.3)
            .id("shape-\(selectedShape)-\(Int(extrusionDepth * 100))")
            .ignoresSafeArea()

            VStack {
                Spacer()

                VStack(spacing: 10) {
                    // Shape selector
                    HStack(spacing: 6) {
                        ForEach(0..<5, id: \.self) { i in
                            Button {
                                selectedShape = i
                                #if os(iOS)
                                HapticManager.selectionChanged()
                                #endif
                            } label: {
                                VStack(spacing: 4) {
                                    Image(systemName: shapeIcons[i])
                                        .font(.body)
                                    Text(shapeNames[i])
                                        .font(.caption2)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                                .background(
                                    i == selectedShape
                                        ? AnyShapeStyle(.blue)
                                        : AnyShapeStyle(.white.opacity(0.15))
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                .foregroundStyle(.white)
                            }
                        }
                    }

                    // Extrusion slider
                    HStack {
                        Text("Depth")
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.7))
                        Slider(value: $extrusionDepth, in: 0...0.2, step: 0.01)
                            .tint(.blue)
                            .accessibilityLabel("Extrusion depth")
                        Text("\(Int(extrusionDepth * 100))cm")
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.7))
                            .monospacedDigit()
                            .frame(width: 36)
                    }
                }
                .padding()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .padding()
            }
        }
        .background(Color.black)
    }
}

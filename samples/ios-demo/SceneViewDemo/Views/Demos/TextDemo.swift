import SwiftUI
import RealityKit
import SceneViewSwift

/// 3D extruded text showcase -- different sizes, colors, and depths.
struct TextDemo: View {
    var body: some View {
        ZStack {
            SceneView { root in
                // Title -- large and deep
                let title = TextNode(text: "SceneView", fontSize: 0.12, color: .white, depth: 0.03)
                    .centered()
                    .position(.init(x: 0, y: 0.5, z: -2.5))
                root.addChild(title.entity)

                // Subtitle -- medium, blue
                let subtitle = TextNode(text: "3D & AR for SwiftUI", fontSize: 0.06, color: .systemBlue, depth: 0.01)
                    .centered()
                    .position(.init(x: 0, y: 0.25, z: -2.5))
                root.addChild(subtitle.entity)

                // Small labels next to shapes
                let shapes: [(String, UIColor, Float)] = [
                    ("Cube", .systemRed, -0.4),
                    ("Sphere", .systemGreen, 0.0),
                    ("Cone", .systemOrange, 0.4),
                ]

                for (name, color, xPos) in shapes {
                    let shape: GeometryNode
                    switch name {
                    case "Cube":
                        shape = GeometryNode.cube(size: 0.12, color: color, cornerRadius: 0.01)
                    case "Sphere":
                        shape = GeometryNode.sphere(radius: 0.08, color: color)
                    default:
                        shape = GeometryNode.cone(height: 0.15, radius: 0.08, color: color)
                    }
                    shape.entity.position = .init(x: xPos, y: -0.1, z: -2.5)
                    root.addChild(shape.entity)

                    let label = TextNode(text: name, fontSize: 0.04, color: color, depth: 0.005)
                        .centered()
                        .position(.init(x: xPos, y: -0.3, z: -2.5))
                    root.addChild(label.entity)
                }

                // Thin caption at the bottom
                let caption = TextNode(text: "TextNode -- 3D extruded text", fontSize: 0.03, color: .lightGray, depth: 0.003)
                    .centered()
                    .position(.init(x: 0, y: -0.5, z: -2.5))
                root.addChild(caption.entity)
            }
            .cameraControls(.orbit)
            .ignoresSafeArea()
        }
        .background(Color.black)
    }
}

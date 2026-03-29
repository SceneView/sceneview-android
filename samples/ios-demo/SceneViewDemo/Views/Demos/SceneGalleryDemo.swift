import SwiftUI
import RealityKit
import SceneViewSwift

/// Scene gallery -- multiple shapes, text, lines, and lights in a composed scene.
struct SceneGalleryDemo: View {
    var body: some View {
        ZStack {
            SceneView { root in
                // Pedestals with shapes
                let pedestals: [(String, Float, UIColor)] = [
                    ("Cube", -0.8, .systemBlue),
                    ("Sphere", -0.4, .systemRed),
                    ("Cylinder", 0.0, .systemGreen),
                    ("Cone", 0.4, .systemOrange),
                    ("Plane", 0.8, .systemPurple),
                ]

                for (name, x, color) in pedestals {
                    // Pedestal
                    let pedestal = GeometryNode.cylinder(radius: 0.08, height: 0.02, color: .darkGray)
                    pedestal.entity.position = .init(x: x, y: -0.25, z: -2)
                    root.addChild(pedestal.entity)

                    // Shape on pedestal
                    let shape: GeometryNode
                    switch name {
                    case "Cube":
                        shape = GeometryNode.cube(
                            size: 0.12,
                            material: .pbr(color: color, metallic: 0.6, roughness: 0.3),
                            cornerRadius: 0.01
                        )
                    case "Sphere":
                        shape = GeometryNode.sphere(
                            radius: 0.07,
                            material: .pbr(color: color, metallic: 0.8, roughness: 0.15)
                        )
                    case "Cylinder":
                        shape = GeometryNode.cylinder(radius: 0.06, height: 0.12, color: color)
                    case "Cone":
                        shape = GeometryNode.cone(height: 0.14, radius: 0.06, color: color)
                    default:
                        shape = GeometryNode.plane(width: 0.12, depth: 0.12, color: color)
                    }
                    shape.entity.position = .init(x: x, y: -0.12, z: -2)
                    root.addChild(shape.entity)

                    // Billboard label
                    let label = BillboardNode.text(name, fontSize: 0.025, color: color)
                        .position(.init(x: x, y: 0.05, z: -2))
                    root.addChild(label.entity)
                }

                // Decorative circle on floor
                let circle = PathNode.circle(
                    center: .init(x: 0, y: -0.25, z: -2),
                    radius: 1.0,
                    segments: 64,
                    thickness: 0.003,
                    color: .init(white: 0.25, alpha: 1)
                )
                root.addChild(circle.entity)

                // Title at top
                let title = TextNode(text: "SceneView Gallery", fontSize: 0.07, color: .white, depth: 0.01)
                    .centered()
                    .position(.init(x: 0, y: 0.4, z: -2.5))
                root.addChild(title.entity)
            }
            .cameraControls(.orbit)
            .autoRotate(speed: 0.2)
            .ignoresSafeArea()
        }
        .background(Color.black)
    }
}

import SwiftUI
import RealityKit
import SceneViewSwift

/// Physics simulation -- dynamic cubes on a static floor.
struct PhysicsDemo: View {
    @State private var sceneKey = UUID()

    var body: some View {
        ZStack {
            SceneView { root in
                // Static floor
                let floor = GeometryNode.plane(width: 3, depth: 3, color: .darkGray)
                floor.entity.position = .init(x: 0, y: -0.5, z: -2)
                root.addChild(floor.entity)
                PhysicsNode.static(floor.entity, restitution: 0.6, friction: 0.8)

                // Stack of dynamic cubes
                let colors: [UIColor] = [.systemBlue, .systemRed, .systemGreen, .systemOrange, .systemPurple]
                for i in 0..<5 {
                    let cube = GeometryNode.cube(
                        size: 0.1,
                        material: .pbr(color: colors[i], metallic: 0.5, roughness: 0.3),
                        cornerRadius: 0.005
                    )
                    let x = Float(i - 2) * 0.15 + Float.random(in: -0.02...0.02)
                    let y = Float(i) * 0.12 + 0.5
                    cube.entity.position = .init(x: x, y: y, z: -2)
                    root.addChild(cube.entity)
                    PhysicsNode.dynamic(cube.entity, mass: 0.5, restitution: 0.4)
                }

                // A bouncing sphere
                let ball = GeometryNode.sphere(
                    radius: 0.06,
                    material: .pbr(color: .systemYellow, metallic: 0.8, roughness: 0.1)
                )
                ball.entity.position = .init(x: 0.3, y: 1.5, z: -2)
                root.addChild(ball.entity)
                PhysicsNode.dynamic(ball.entity, mass: 0.3, restitution: 0.9)
            }
            .cameraControls(.orbit)
            .id(sceneKey)
            .ignoresSafeArea()

            VStack {
                Spacer()
                HStack {
                    Text("Dynamic bodies fall with gravity")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.6))

                    Spacer()

                    Button {
                        sceneKey = UUID()
                        #if os(iOS)
                        HapticManager.mediumTap()
                        #endif
                    } label: {
                        Label("Reset", systemImage: "arrow.counterclockwise")
                            .font(.caption)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(.ultraThinMaterial)
                            .clipShape(Capsule())
                    }
                    .foregroundStyle(.white)
                    .accessibilityLabel("Reset physics simulation")
                }
                .padding()
            }
        }
        .background(Color.black)
    }
}

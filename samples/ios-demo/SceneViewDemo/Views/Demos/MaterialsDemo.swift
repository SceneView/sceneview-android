import SwiftUI
import RealityKit
import SceneViewSwift

/// PBR material spectrum -- 5 spheres from rough to mirror.
struct MaterialsDemo: View {
    var body: some View {
        ZStack {
            SceneView { root in
                let count = 5
                for i in 0..<count {
                    let progress = Float(i) / Float(count - 1)
                    let metallic = progress
                    let roughness = 1.0 - progress

                    let sphere = GeometryNode.sphere(
                        radius: 0.15,
                        material: .pbr(color: .gray, metallic: metallic, roughness: roughness)
                    )
                    let x = Float(i - count / 2) * 0.4
                    sphere.entity.position = .init(x: x, y: 0, z: -2)
                    root.addChild(sphere.entity)
                }
            }
            .cameraControls(.orbit)
            .ignoresSafeArea()

            VStack {
                Spacer()
                legendBar
            }
        }
        .background(Color.black)
    }

    private var legendBar: some View {
        HStack(spacing: 20) {
            VStack(spacing: 2) {
                Circle().fill(.gray.opacity(0.4)).frame(width: 10)
                Text("Rough").font(.caption2).foregroundStyle(.white.opacity(0.7))
            }
            Image(systemName: "arrow.right")
                .font(.caption2)
                .foregroundStyle(.white.opacity(0.4))
            VStack(spacing: 2) {
                Circle().fill(.gray.opacity(0.6)).frame(width: 10)
                Text("Semi").font(.caption2).foregroundStyle(.white.opacity(0.7))
            }
            Image(systemName: "arrow.right")
                .font(.caption2)
                .foregroundStyle(.white.opacity(0.4))
            VStack(spacing: 2) {
                Circle().fill(.gray).frame(width: 10)
                Text("Mirror").font(.caption2).foregroundStyle(.white.opacity(0.7))
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .background(.ultraThinMaterial)
        .clipShape(Capsule())
        .padding(.bottom, 24)
    }
}

import SwiftUI
import SceneViewSwift
import RealityKit

struct ContentView: View {
    @State private var model: ModelNode?

    var body: some View {
        SceneView { root in
            if let model {
                root.addChild(model.entity)
            }
        }
        .task {
            model = try? await ModelNode.load("models/robot.usdz")
        }
    }
}

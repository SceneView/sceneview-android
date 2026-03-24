import Foundation
import React
import SceneViewSwift
import SwiftUI

// MARK: - SceneView (3D)

/// RCTViewManager subclass that bridges React Native's `<RNSceneView>`
/// to SceneViewSwift's `SceneView` (RealityKit-based).
@objc(RNSceneViewManager)
class RNSceneViewManager: RCTViewManager {

    override func view() -> UIView! {
        return RNSceneViewWrapper()
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}

/// Observable state model shared between React props and SwiftUI view.
@MainActor
class RNSceneState: ObservableObject {
    @Published var modelPaths: [String] = []
    @Published var environmentPath: String?
    @Published var cameraOrbit: Bool = true
}

/// UIView wrapper that hosts a SwiftUI `SceneView` via UIHostingController.
class RNSceneViewWrapper: UIView {

    private var hostingController: UIHostingController<RNSceneViewContent>?
    private let sceneState = RNSceneState()

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }

    private func setupView() {
        let content = RNSceneViewContent(state: sceneState)
        let hosting = UIHostingController(rootView: content)
        hosting.view.frame = bounds
        hosting.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        addSubview(hosting.view)
        hostingController = hosting
    }

    // MARK: - React props

    @objc var environment: String? {
        didSet {
            Task { @MainActor in
                sceneState.environmentPath = environment
            }
        }
    }

    @objc var modelNodes: [[String: Any]]? {
        didSet {
            Task { @MainActor in
                sceneState.modelPaths = modelNodes?.compactMap { $0["src"] as? String } ?? []
            }
        }
    }

    @objc var cameraOrbit: Bool = true {
        didSet {
            Task { @MainActor in
                sceneState.cameraOrbit = cameraOrbit
            }
        }
    }
}

/// SwiftUI content view rendering SceneViewSwift.SceneView.
struct RNSceneViewContent: View {
    @ObservedObject var state: RNSceneState

    var body: some View {
        SceneView {
            ForEach(state.modelPaths, id: \.self) { path in
                ModelNode(path)
            }
        }
    }
}

// MARK: - ARSceneView

/// RCTViewManager subclass that bridges React Native's `<RNARSceneView>`
/// to SceneViewSwift's `ARSceneView` (ARKit + RealityKit).
@objc(RNARSceneViewManager)
class RNARSceneViewManager: RCTViewManager {

    override func view() -> UIView! {
        return RNARSceneViewWrapper()
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}

/// UIView wrapper that hosts a SwiftUI `ARSceneView` via UIHostingController.
class RNARSceneViewWrapper: UIView {

    private var hostingController: UIHostingController<RNARSceneViewContent>?
    private let sceneState = RNSceneState()

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }

    private func setupView() {
        let content = RNARSceneViewContent(state: sceneState)
        let hosting = UIHostingController(rootView: content)
        hosting.view.frame = bounds
        hosting.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        addSubview(hosting.view)
        hostingController = hosting
    }

    // MARK: - React props

    @objc var planeDetection: Bool = true {
        didSet {
            // ARSceneView handles plane detection via its configuration
        }
    }

    @objc var depthOcclusion: Bool = false {
        didSet {
            // LiDAR depth occlusion configuration
        }
    }

    @objc var instantPlacement: Bool = false {
        didSet {
            // Instant placement configuration
        }
    }

    @objc var modelNodes: [[String: Any]]? {
        didSet {
            Task { @MainActor in
                sceneState.modelPaths = modelNodes?.compactMap { $0["src"] as? String } ?? []
            }
        }
    }
}

/// SwiftUI content view rendering SceneViewSwift.ARSceneView.
struct RNARSceneViewContent: View {
    @ObservedObject var state: RNSceneState

    var body: some View {
        ARSceneView { anchor in
            ForEach(state.modelPaths, id: \.self) { path in
                ModelNode(path)
                    .scale(0.3)
            }
        }
    }
}

import Foundation
import React
import SceneViewSwift
import SwiftUI

// MARK: - Shared model data

struct RNModelData: Identifiable, Equatable {
    let id = UUID()
    let path: String
    let scale: Float

    static func == (lhs: RNModelData, rhs: RNModelData) -> Bool {
        lhs.id == rhs.id
    }
}

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
    @Published var models: [RNModelData] = []
    @Published var environmentPath: String?
    @Published var cameraOrbit: Bool = true
}

/// UIView wrapper that hosts a SwiftUI `SceneView` via UIHostingController.
class RNSceneViewWrapper: UIView {

    private var hostingController: UIHostingController<RNSceneViewContent>?
    private let sceneState = RNSceneState()

    /// Event callback for tap events.
    @objc var onTap: RCTDirectEventBlock?

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
                sceneState.models = modelNodes?.compactMap { dict -> RNModelData? in
                    guard let src = dict["src"] as? String else { return nil }
                    let scale = (dict["scale"] as? NSNumber)?.floatValue ?? 1.0
                    return RNModelData(path: src, scale: scale)
                } ?? []
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
            ForEach(state.models) { model in
                ModelNode(model.path)
                    .scale(model.scale)
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

/// Observable state for AR scene configuration.
@MainActor
class RNARSceneState: ObservableObject {
    @Published var models: [RNModelData] = []
    @Published var planeDetection: Bool = true
    @Published var depthOcclusion: Bool = false
    @Published var instantPlacement: Bool = false
}

/// UIView wrapper that hosts a SwiftUI `ARSceneView` via UIHostingController.
class RNARSceneViewWrapper: UIView {

    private var hostingController: UIHostingController<RNARSceneViewContent>?
    private let sceneState = RNARSceneState()

    /// Event callback for tap events.
    @objc var onTap: RCTDirectEventBlock?

    /// Event callback for plane detection events.
    @objc var onPlaneDetected: RCTDirectEventBlock?

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
            Task { @MainActor in
                sceneState.planeDetection = planeDetection
            }
        }
    }

    @objc var depthOcclusion: Bool = false {
        didSet {
            Task { @MainActor in
                sceneState.depthOcclusion = depthOcclusion
            }
        }
    }

    @objc var instantPlacement: Bool = false {
        didSet {
            Task { @MainActor in
                sceneState.instantPlacement = instantPlacement
            }
        }
    }

    @objc var modelNodes: [[String: Any]]? {
        didSet {
            Task { @MainActor in
                sceneState.models = modelNodes?.compactMap { dict -> RNModelData? in
                    guard let src = dict["src"] as? String else { return nil }
                    let scale = (dict["scale"] as? NSNumber)?.floatValue ?? 1.0
                    return RNModelData(path: src, scale: scale)
                } ?? []
            }
        }
    }

    @objc var environment: String? {
        didSet {
            // AR scenes use camera feed; environment affects lighting only.
        }
    }
}

/// SwiftUI content view rendering SceneViewSwift.ARSceneView.
struct RNARSceneViewContent: View {
    @ObservedObject var state: RNARSceneState

    var body: some View {
        ARSceneView { anchor in
            ForEach(state.models) { model in
                ModelNode(model.path)
                    .scale(model.scale)
            }
        }
    }
}

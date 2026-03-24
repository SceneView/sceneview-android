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

/// UIView wrapper that hosts a SwiftUI `SceneView` via UIHostingController.
class RNSceneViewWrapper: UIView {

    private var hostingController: UIHostingController<SceneViewSwift.SceneView>?

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }

    private func setupView() {
        // TODO: Initialize SceneView with default configuration.
        // let sceneView = SceneViewSwift.SceneView()
        // let hosting = UIHostingController(rootView: sceneView)
        // hosting.view.frame = bounds
        // hosting.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        // addSubview(hosting.view)
        // hostingController = hosting
    }

    // MARK: - React props

    @objc var environment: String? {
        didSet {
            // TODO: Update the scene environment HDR.
        }
    }

    @objc var modelNodes: [[String: Any]]? {
        didSet {
            // TODO: Parse model node dictionaries and update the scene.
        }
    }

    @objc var cameraOrbit: Bool = true {
        didSet {
            // TODO: Enable/disable orbit camera.
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

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }

    private func setupView() {
        // TODO: Initialize ARSceneView with ARKit session configuration.
        // let arSceneView = SceneViewSwift.ARSceneView()
        // let hosting = UIHostingController(rootView: arSceneView)
        // hosting.view.frame = bounds
        // hosting.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        // addSubview(hosting.view)
    }

    // MARK: - React props

    @objc var planeDetection: Bool = true {
        didSet {
            // TODO: Enable/disable ARKit plane detection.
        }
    }

    @objc var depthOcclusion: Bool = false {
        didSet {
            // TODO: Enable/disable LiDAR depth occlusion.
        }
    }

    @objc var instantPlacement: Bool = false {
        didSet {
            // TODO: Enable/disable instant placement.
        }
    }

    @objc var modelNodes: [[String: Any]]? {
        didSet {
            // TODO: Parse and place AR model nodes.
        }
    }
}

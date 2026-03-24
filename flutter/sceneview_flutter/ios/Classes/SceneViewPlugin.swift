import Flutter
import UIKit
import SwiftUI
import SceneViewSwift

/// Flutter plugin entry point for SceneView on iOS.
///
/// Registers two platform view types:
/// - `io.github.sceneview.flutter/sceneview`   — 3D scene (wraps SceneViewSwift.SceneView)
/// - `io.github.sceneview.flutter/arsceneview` — AR scene (wraps SceneViewSwift.ARSceneView)
public class SceneViewPlugin: NSObject, FlutterPlugin {

    public static func register(with registrar: FlutterPluginRegistrar) {
        registrar.register(
            SceneViewFactory(messenger: registrar.messenger()),
            withId: "io.github.sceneview.flutter/sceneview"
        )
        registrar.register(
            ARSceneViewFactory(messenger: registrar.messenger()),
            withId: "io.github.sceneview.flutter/arsceneview"
        )
    }
}

// MARK: - 3D SceneView

class SceneViewFactory: NSObject, FlutterPlatformViewFactory {
    private let messenger: FlutterBinaryMessenger

    init(messenger: FlutterBinaryMessenger) {
        self.messenger = messenger
        super.init()
    }

    func create(
        withFrame frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?
    ) -> FlutterPlatformView {
        return SceneViewPlatformView(
            frame: frame,
            viewId: viewId,
            args: args as? [String: Any] ?? [:],
            messenger: messenger
        )
    }

    func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

/// Observable model holding scene state, updated via method channel.
@MainActor
class SceneState: ObservableObject {
    @Published var modelPaths: [String] = []
    @Published var environmentPath: String?
}

class SceneViewPlatformView: NSObject, FlutterPlatformView {
    private let hostingController: UIHostingController<SceneViewSwiftUIWrapper>
    private let channel: FlutterMethodChannel
    private let sceneState = SceneState()

    init(frame: CGRect, viewId: Int64, args: [String: Any], messenger: FlutterBinaryMessenger) {
        self.hostingController = UIHostingController(
            rootView: SceneViewSwiftUIWrapper(state: sceneState)
        )
        self.hostingController.view.frame = frame
        self.hostingController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        self.channel = FlutterMethodChannel(
            name: "io.github.sceneview.flutter/scene_\(viewId)",
            binaryMessenger: messenger
        )
        super.init()

        channel.setMethodCallHandler(handleMethodCall)
    }

    func view() -> UIView {
        return hostingController.view
    }

    private func handleMethodCall(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "loadModel":
            guard let args = call.arguments as? [String: Any],
                  let modelPath = args["modelPath"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "modelPath required", details: nil))
                return
            }
            Task { @MainActor in
                sceneState.modelPaths.append(modelPath)
            }
            result(nil)

        case "clearScene":
            Task { @MainActor in
                sceneState.modelPaths.removeAll()
            }
            result(nil)

        case "setEnvironment":
            let hdrPath = (call.arguments as? [String: Any])?["hdrPath"] as? String
            Task { @MainActor in
                sceneState.environmentPath = hdrPath
            }
            result(nil)

        default:
            result(FlutterMethodNotImplemented)
        }
    }
}

/// SwiftUI wrapper for SceneViewSwift.SceneView, driven by observable state.
struct SceneViewSwiftUIWrapper: View {
    @ObservedObject var state: SceneState

    var body: some View {
        SceneView {
            ForEach(state.modelPaths, id: \.self) { path in
                ModelNode(path)
            }
        }
    }
}

// MARK: - AR SceneView

class ARSceneViewFactory: NSObject, FlutterPlatformViewFactory {
    private let messenger: FlutterBinaryMessenger

    init(messenger: FlutterBinaryMessenger) {
        self.messenger = messenger
        super.init()
    }

    func create(
        withFrame frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?
    ) -> FlutterPlatformView {
        return ARSceneViewPlatformView(
            frame: frame,
            viewId: viewId,
            args: args as? [String: Any] ?? [:],
            messenger: messenger
        )
    }

    func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

class ARSceneViewPlatformView: NSObject, FlutterPlatformView {
    private let hostingController: UIHostingController<ARSceneViewSwiftUIWrapper>
    private let channel: FlutterMethodChannel
    private let sceneState = SceneState()

    init(frame: CGRect, viewId: Int64, args: [String: Any], messenger: FlutterBinaryMessenger) {
        self.hostingController = UIHostingController(
            rootView: ARSceneViewSwiftUIWrapper(state: sceneState)
        )
        self.hostingController.view.frame = frame
        self.hostingController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        self.channel = FlutterMethodChannel(
            name: "io.github.sceneview.flutter/scene_\(viewId)",
            binaryMessenger: messenger
        )
        super.init()

        channel.setMethodCallHandler(handleMethodCall)
    }

    func view() -> UIView {
        return hostingController.view
    }

    private func handleMethodCall(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "loadModel":
            guard let args = call.arguments as? [String: Any],
                  let modelPath = args["modelPath"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "modelPath required", details: nil))
                return
            }
            Task { @MainActor in
                sceneState.modelPaths.append(modelPath)
            }
            result(nil)

        case "clearScene":
            Task { @MainActor in
                sceneState.modelPaths.removeAll()
            }
            result(nil)

        default:
            result(FlutterMethodNotImplemented)
        }
    }
}

/// SwiftUI wrapper for SceneViewSwift.ARSceneView, driven by observable state.
struct ARSceneViewSwiftUIWrapper: View {
    @ObservedObject var state: SceneState

    var body: some View {
        ARSceneView { anchor in
            ForEach(state.modelPaths, id: \.self) { path in
                ModelNode(path)
                    .scale(0.3)
            }
        }
    }
}

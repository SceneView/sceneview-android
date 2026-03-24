import Flutter
import UIKit
// TODO: Uncomment when SceneViewSwift is added as a dependency
// import SceneViewSwift

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

class SceneViewPlatformView: NSObject, FlutterPlatformView {
    private let containerView: UIView
    private let channel: FlutterMethodChannel

    init(frame: CGRect, viewId: Int64, args: [String: Any], messenger: FlutterBinaryMessenger) {
        self.containerView = UIView(frame: frame)
        self.containerView.backgroundColor = .black
        self.channel = FlutterMethodChannel(
            name: "io.github.sceneview.flutter/scene_\(viewId)",
            binaryMessenger: messenger
        )
        super.init()

        channel.setMethodCallHandler(handleMethodCall)

        // TODO: Create SceneViewSwift.SceneView and add as subview
        // let sceneView = SceneViewSwift.SceneView(frame: frame)
        // containerView.addSubview(sceneView)
        // sceneView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    }

    func view() -> UIView {
        return containerView
    }

    private func handleMethodCall(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "loadModel":
            guard let args = call.arguments as? [String: Any],
                  let modelPath = args["modelPath"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "modelPath required", details: nil))
                return
            }
            // TODO: Load glTF/USDZ model via SceneViewSwift
            result(nil)

        case "addGeometry":
            // TODO: Add geometry node
            result(nil)

        case "addLight":
            // TODO: Add light node
            result(nil)

        case "clearScene":
            // TODO: Remove all child nodes
            result(nil)

        case "setEnvironment":
            // TODO: Set IBL environment
            result(nil)

        default:
            result(FlutterMethodNotImplemented)
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
    private let containerView: UIView
    private let channel: FlutterMethodChannel

    init(frame: CGRect, viewId: Int64, args: [String: Any], messenger: FlutterBinaryMessenger) {
        self.containerView = UIView(frame: frame)
        self.containerView.backgroundColor = .black
        self.channel = FlutterMethodChannel(
            name: "io.github.sceneview.flutter/scene_\(viewId)",
            binaryMessenger: messenger
        )
        super.init()

        channel.setMethodCallHandler(handleMethodCall)

        // TODO: Create SceneViewSwift.ARSceneView and add as subview
        // let arSceneView = SceneViewSwift.ARSceneView(frame: frame)
        // containerView.addSubview(arSceneView)
        // arSceneView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    }

    func view() -> UIView {
        return containerView
    }

    private func handleMethodCall(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "loadModel":
            // TODO: Load model and place on detected plane
            result(nil)

        case "clearScene":
            result(nil)

        default:
            result(FlutterMethodNotImplemented)
        }
    }
}

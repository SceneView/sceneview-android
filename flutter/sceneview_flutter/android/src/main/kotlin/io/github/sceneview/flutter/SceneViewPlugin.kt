package io.github.sceneview.flutter

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

/**
 * Flutter plugin entry point for SceneView on Android.
 *
 * Registers two platform view types:
 * - `io.github.sceneview.flutter/sceneview`   — 3D scene (wraps SceneView Compose)
 * - `io.github.sceneview.flutter/arsceneview` — AR scene (wraps ARSceneView Compose)
 */
class SceneViewPlugin : FlutterPlugin, ActivityAware {

    private var activity: Activity? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        binding.platformViewRegistry.registerViewFactory(
            "io.github.sceneview.flutter/sceneview",
            SceneViewFactory(binding)
        )
        binding.platformViewRegistry.registerViewFactory(
            "io.github.sceneview.flutter/arsceneview",
            ARSceneViewFactory(binding)
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {}

    // ActivityAware
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }
    override fun onDetachedFromActivityForConfigChanges() { activity = null }
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }
    override fun onDetachedFromActivity() { activity = null }
}

// ---------------------------------------------------------------------------
// 3D SceneView
// ---------------------------------------------------------------------------

class SceneViewFactory(
    private val binding: FlutterPlugin.FlutterPluginBinding
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val params = args as? Map<String, Any?> ?: emptyMap()
        return SceneViewPlatformView(context, viewId, params, binding)
    }
}

class SceneViewPlatformView(
    private val context: Context,
    private val viewId: Int,
    private val params: Map<String, Any?>,
    private val binding: FlutterPlugin.FlutterPluginBinding,
) : PlatformView, MethodChannel.MethodCallHandler {

    private val channel = MethodChannel(
        binding.binaryMessenger,
        "io.github.sceneview.flutter/scene_$viewId"
    )

    // TODO: Replace with actual ComposeView hosting io.github.sceneview.Scene { }
    // For now, create a placeholder ComposeView to validate the wiring.
    private val composeView = ComposeView(context).apply {
        // setContent {
        //     io.github.sceneview.Scene {
        //         // Nodes added via method channel calls
        //     }
        // }
    }

    init {
        channel.setMethodCallHandler(this)
    }

    override fun getView(): View = composeView

    override fun dispose() {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "loadModel" -> {
                val modelPath = call.argument<String>("modelPath")
                // TODO: Use modelLoader.loadModelInstanceAsync(modelPath) on main thread
                result.success(null)
            }
            "addGeometry" -> {
                val type = call.argument<String>("type")
                // TODO: Add geometry node to scene
                result.success(null)
            }
            "addLight" -> {
                // TODO: Add light node to scene
                result.success(null)
            }
            "clearScene" -> {
                // TODO: Remove all child nodes
                result.success(null)
            }
            "setEnvironment" -> {
                val hdrPath = call.argument<String>("hdrPath")
                // TODO: Load HDR environment
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}

// ---------------------------------------------------------------------------
// AR SceneView
// ---------------------------------------------------------------------------

class ARSceneViewFactory(
    private val binding: FlutterPlugin.FlutterPluginBinding
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val params = args as? Map<String, Any?> ?: emptyMap()
        return ARSceneViewPlatformView(context, viewId, params, binding)
    }
}

class ARSceneViewPlatformView(
    private val context: Context,
    private val viewId: Int,
    private val params: Map<String, Any?>,
    private val binding: FlutterPlugin.FlutterPluginBinding,
) : PlatformView, MethodChannel.MethodCallHandler {

    private val channel = MethodChannel(
        binding.binaryMessenger,
        "io.github.sceneview.flutter/scene_$viewId"
    )

    // TODO: Replace with actual ComposeView hosting io.github.sceneview.ar.ARScene { }
    private val composeView = ComposeView(context).apply {
        // setContent {
        //     io.github.sceneview.ar.ARScene {
        //         // AR nodes
        //     }
        // }
    }

    init {
        channel.setMethodCallHandler(this)
    }

    override fun getView(): View = composeView

    override fun dispose() {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "loadModel" -> {
                // TODO: Load model and attach to AR anchor on tap
                result.success(null)
            }
            "clearScene" -> {
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}

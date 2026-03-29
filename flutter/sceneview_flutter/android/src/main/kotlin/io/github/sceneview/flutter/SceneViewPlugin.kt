package io.github.sceneview.flutter

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Flutter plugin entry point for SceneView on Android.
 *
 * Registers two platform view types:
 * - `io.github.sceneview.flutter/sceneview`   -- 3D scene (wraps SceneView Compose)
 * - `io.github.sceneview.flutter/arsceneview` -- AR scene (wraps ARSceneView Compose)
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
// Model descriptor passed from Dart via method channel
// ---------------------------------------------------------------------------

private data class FlutterModelNode(
    val path: String,
    val position: Position = Position(0f, 0f, 0f),
    val scale: Float = 1.0f,
    val autoAnimate: Boolean = true
)

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

    // Reactive state for Compose -- updated via method channel
    private val modelNodes = mutableStateListOf<FlutterModelNode>()
    private var environmentPath by mutableStateOf<String?>(null)

    private val composeView = ComposeView(context).apply {
        setContent {
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)
            val environmentLoader = rememberEnvironmentLoader(engine)

            val cameraNode = rememberCameraNode(engine) {
                position = Position(y = 0f, z = 3.0f)
            }

            val environment = environmentPath?.let { path ->
                rememberEnvironment(environmentLoader) {
                    environmentLoader.createHDREnvironment(path)
                        ?: io.github.sceneview.createEnvironment(environmentLoader)
                }
            }

            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                cameraNode = cameraNode,
                environment = environment ?: rememberEnvironment(environmentLoader),
            ) {
                modelNodes.forEach { model ->
                    val instance = rememberModelInstance(modelLoader, model.path)
                    instance?.let {
                        ModelNode(
                            modelInstance = it,
                            scaleToUnits = model.scale,
                            autoAnimate = model.autoAnimate,
                        )
                    }
                }
            }
        }
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
                val modelPath = call.argument<String>("modelPath") ?: run {
                    result.error("INVALID_ARG", "modelPath is required", null)
                    return
                }
                val scale = call.argument<Double>("scale")?.toFloat() ?: 1.0f
                val x = call.argument<Double>("x")?.toFloat() ?: 0f
                val y = call.argument<Double>("y")?.toFloat() ?: 0f
                val z = call.argument<Double>("z")?.toFloat() ?: 0f

                modelNodes.add(FlutterModelNode(
                    path = modelPath,
                    position = Position(x, y, z),
                    scale = scale,
                ))
                result.success(null)
            }
            "addGeometry" -> {
                // Geometry nodes are not yet supported via the bridge.
                // SceneView's composable geometry API requires Compose DSL context
                // which cannot be driven easily from a method channel.
                result.success(null)
            }
            "addLight" -> {
                // Light configuration is handled by Scene's default mainLightNode.
                // Custom light manipulation requires Compose DSL context.
                result.success(null)
            }
            "clearScene" -> {
                modelNodes.clear()
                result.success(null)
            }
            "setEnvironment" -> {
                environmentPath = call.argument<String>("hdrPath")
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

    private val modelNodes = mutableStateListOf<FlutterModelNode>()

    private val composeView = ComposeView(context).apply {
        setContent {
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)

            io.github.sceneview.ar.ARScene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                planeRenderer = true,
            ) {
                modelNodes.forEach { model ->
                    val instance = rememberModelInstance(modelLoader, model.path)
                    instance?.let {
                        ModelNode(
                            modelInstance = it,
                            scaleToUnits = model.scale,
                            autoAnimate = model.autoAnimate,
                        )
                    }
                }
            }
        }
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
                val modelPath = call.argument<String>("modelPath") ?: run {
                    result.error("INVALID_ARG", "modelPath is required", null)
                    return
                }
                val scale = call.argument<Double>("scale")?.toFloat() ?: 1.0f
                modelNodes.add(FlutterModelNode(path = modelPath, scale = scale))
                result.success(null)
            }
            "addGeometry" -> {
                result.success(null)
            }
            "addLight" -> {
                result.success(null)
            }
            "clearScene" -> {
                modelNodes.clear()
                result.success(null)
            }
            "setEnvironment" -> {
                // AR scenes use camera feed as background; environment HDR
                // affects lighting but not the skybox.
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}

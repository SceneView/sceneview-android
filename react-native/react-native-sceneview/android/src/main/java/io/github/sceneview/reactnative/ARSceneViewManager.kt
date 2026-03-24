package io.github.sceneview.reactnative

import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

/**
 * ViewManager that bridges React Native's `<RNARSceneView>` to the Jetpack Compose
 * `ARScene { }` composable from `io.github.sceneview.ar`.
 *
 * Scaffold — full implementation will mirror SceneViewManager plus AR-specific props.
 */
class ARSceneViewManager : SimpleViewManager<FrameLayout>() {

    override fun getName(): String = "RNARSceneView"

    override fun createViewInstance(reactContext: ThemedReactContext): FrameLayout {
        val container = FrameLayout(reactContext)
        val composeView = ComposeView(reactContext).apply {
            setContent {
                // TODO: Render ARScene { } with AR configuration and nodes.
                // io.github.sceneview.ar.ARScene(
                //     modifier = Modifier.fillMaxSize(),
                //     engine = rememberEngine(),
                //     modelLoader = rememberModelLoader(engine),
                //     planeRenderer = planeDetection,
                // ) {
                //     // AR nodes …
                // }
            }
        }
        container.addView(composeView)
        return container
    }

    @ReactProp(name = "environment")
    fun setEnvironment(view: FrameLayout, environment: String?) {
        // TODO: Pass environment HDR path to ARScene.
    }

    @ReactProp(name = "modelNodes")
    fun setModelNodes(view: FrameLayout, nodes: ReadableArray?) {
        // TODO: Parse model nodes for AR placement.
    }

    @ReactProp(name = "planeDetection", defaultBoolean = true)
    fun setPlaneDetection(view: FrameLayout, enabled: Boolean) {
        // TODO: Enable/disable ARCore plane detection.
    }

    @ReactProp(name = "depthOcclusion", defaultBoolean = false)
    fun setDepthOcclusion(view: FrameLayout, enabled: Boolean) {
        // TODO: Enable/disable ARCore Depth API.
    }

    @ReactProp(name = "instantPlacement", defaultBoolean = false)
    fun setInstantPlacement(view: FrameLayout, enabled: Boolean) {
        // TODO: Enable/disable ARCore instant placement.
    }
}

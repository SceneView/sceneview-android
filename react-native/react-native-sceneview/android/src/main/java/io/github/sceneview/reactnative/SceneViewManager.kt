package io.github.sceneview.reactnative

import android.view.Choreographer
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

/**
 * ViewManager that bridges React Native's `<RNSceneView>` to the Jetpack Compose
 * `Scene { }` composable from `io.github.sceneview`.
 *
 * This is a scaffold — the full implementation will:
 * 1. Inflate a ComposeView
 * 2. Set its content to `Scene { ... }` with model/geometry/light nodes
 * 3. Map React props to composable parameters
 */
class SceneViewManager : SimpleViewManager<FrameLayout>() {

    override fun getName(): String = "RNSceneView"

    override fun createViewInstance(reactContext: ThemedReactContext): FrameLayout {
        val container = FrameLayout(reactContext)
        val composeView = ComposeView(reactContext).apply {
            setContent {
                // TODO: Render Scene { } with nodes from props.
                // io.github.sceneview.Scene(
                //     modifier = Modifier.fillMaxSize(),
                //     engine = rememberEngine(),
                //     modelLoader = rememberModelLoader(engine),
                // ) {
                //     // ModelNode, GeometryNode, LightNode …
                // }
            }
        }
        container.addView(composeView)
        return container
    }

    @ReactProp(name = "environment")
    fun setEnvironment(view: FrameLayout, environment: String?) {
        // TODO: Pass environment HDR path to the Scene composable.
    }

    @ReactProp(name = "modelNodes")
    fun setModelNodes(view: FrameLayout, nodes: ReadableArray?) {
        // TODO: Parse JSON array → list of ModelNode data, trigger recomposition.
    }

    @ReactProp(name = "geometryNodes")
    fun setGeometryNodes(view: FrameLayout, nodes: ReadableArray?) {
        // TODO: Parse JSON array → list of GeometryNode data, trigger recomposition.
    }

    @ReactProp(name = "lightNodes")
    fun setLightNodes(view: FrameLayout, nodes: ReadableArray?) {
        // TODO: Parse JSON array → list of LightNode data, trigger recomposition.
    }

    @ReactProp(name = "cameraOrbit", defaultBoolean = true)
    fun setCameraOrbit(view: FrameLayout, enabled: Boolean) {
        // TODO: Enable/disable orbit camera manipulator.
    }
}

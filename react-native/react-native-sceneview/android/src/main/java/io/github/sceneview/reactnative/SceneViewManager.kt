package io.github.sceneview.reactnative

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Per-instance scene state stored as a tag on the FrameLayout container.
 * Each `<RNSceneView>` gets its own independent state.
 */
class SceneViewState {
    val modelPaths = mutableStateListOf<ModelNodeData>()
    val environmentPath = mutableStateOf<String?>(null)
    val orbitEnabled = mutableStateOf(true)
}

/**
 * ViewManager that bridges React Native's `<RNSceneView>` to the Jetpack Compose
 * `Scene { }` composable from `io.github.sceneview`.
 *
 * State is stored per-instance via [FrameLayout.getTag] to support multiple
 * `<RNSceneView>` components on the same screen.
 */
class SceneViewManager : SimpleViewManager<FrameLayout>() {

    override fun getName(): String = "RNSceneView"

    private fun getState(view: FrameLayout): SceneViewState {
        return view.tag as? SceneViewState ?: SceneViewState().also { view.tag = it }
    }

    override fun createViewInstance(reactContext: ThemedReactContext): FrameLayout {
        val container = FrameLayout(reactContext)
        val state = SceneViewState()
        container.tag = state

        val composeView = ComposeView(reactContext).apply {
            setContent {
                val engine = rememberEngine()
                val modelLoader = rememberModelLoader(engine)
                val environmentLoader = rememberEnvironmentLoader(engine)

                val cameraNode = rememberCameraNode(engine) {
                    position = Position(y = 0f, z = 3.0f)
                }

                val environment = state.environmentPath.value?.let { path ->
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
                    state.modelPaths.forEach { model ->
                        val instance = rememberModelInstance(modelLoader, model.src)
                        instance?.let {
                            ModelNode(
                                modelInstance = it,
                                scaleToUnits = model.scale,
                                autoAnimate = model.animate,
                            )
                        }
                    }
                }
            }
        }
        container.addView(composeView)
        return container
    }

    @ReactProp(name = "environment")
    fun setEnvironment(view: FrameLayout, environment: String?) {
        getState(view).environmentPath.value = environment
    }

    @ReactProp(name = "modelNodes")
    fun setModelNodes(view: FrameLayout, nodes: ReadableArray?) {
        val state = getState(view)
        state.modelPaths.clear()
        nodes?.let { array ->
            for (i in 0 until array.size()) {
                val map = array.getMap(i) ?: continue
                val src = map.getString("src") ?: continue
                val scale = if (map.hasKey("scale")) {
                    try {
                        map.getDouble("scale").toFloat()
                    } catch (_: Exception) {
                        1.0f
                    }
                } else {
                    1.0f
                }
                // "animation" is a string (animation name) in the TS types.
                // If present and non-null, auto-animate is enabled.
                val animate = if (map.hasKey("animation")) {
                    map.getString("animation") != null
                } else {
                    true
                }
                state.modelPaths.add(ModelNodeData(src = src, scale = scale, animate = animate))
            }
        }
    }

    @ReactProp(name = "cameraOrbit", defaultBoolean = true)
    fun setCameraOrbit(view: FrameLayout, enabled: Boolean) {
        getState(view).orbitEnabled.value = enabled
    }
}

data class ModelNodeData(
    val src: String,
    val scale: Float = 1.0f,
    val animate: Boolean = true
)

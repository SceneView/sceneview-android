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
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Per-instance AR scene state stored as a tag on the FrameLayout container.
 */
class ARSceneViewState {
    val modelPaths = mutableStateListOf<ModelNodeData>()
    val planeDetection = mutableStateOf(true)
    val depthOcclusion = mutableStateOf(false)
    val instantPlacement = mutableStateOf(false)
}

/**
 * ViewManager that bridges React Native's `<RNARSceneView>` to the Jetpack Compose
 * `ARScene { }` composable from `io.github.sceneview.ar`.
 *
 * State is stored per-instance via [FrameLayout.getTag] to support multiple
 * `<RNARSceneView>` components on the same screen.
 */
class ARSceneViewManager : SimpleViewManager<FrameLayout>() {

    override fun getName(): String = "RNARSceneView"

    private fun getState(view: FrameLayout): ARSceneViewState {
        return view.tag as? ARSceneViewState ?: ARSceneViewState().also { view.tag = it }
    }

    override fun createViewInstance(reactContext: ThemedReactContext): FrameLayout {
        val container = FrameLayout(reactContext)
        val state = ARSceneViewState()
        container.tag = state

        val composeView = ComposeView(reactContext).apply {
            setContent {
                val engine = rememberEngine()
                val modelLoader = rememberModelLoader(engine)

                io.github.sceneview.ar.ARScene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
                    planeRenderer = state.planeDetection.value,
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
        // AR scenes use camera feed as background; environment HDR
        // affects lighting only. Not yet wired to ARScene parameters.
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
                    // Scale can be a single number or array [x,y,z]; handle single number
                    try {
                        map.getDouble("scale").toFloat()
                    } catch (_: Exception) {
                        1.0f
                    }
                } else {
                    1.0f
                }
                val animate = if (map.hasKey("animation")) {
                    // "animation" is a string (animation name) in the TS types.
                    // If present, auto-animate is enabled.
                    map.getString("animation") != null
                } else {
                    true
                }
                state.modelPaths.add(ModelNodeData(src = src, scale = scale, animate = animate))
            }
        }
    }

    @ReactProp(name = "planeDetection", defaultBoolean = true)
    fun setPlaneDetection(view: FrameLayout, enabled: Boolean) {
        getState(view).planeDetection.value = enabled
    }

    @ReactProp(name = "depthOcclusion", defaultBoolean = false)
    fun setDepthOcclusion(view: FrameLayout, enabled: Boolean) {
        getState(view).depthOcclusion.value = enabled
    }

    @ReactProp(name = "instantPlacement", defaultBoolean = false)
    fun setInstantPlacement(view: FrameLayout, enabled: Boolean) {
        getState(view).instantPlacement.value = enabled
    }
}

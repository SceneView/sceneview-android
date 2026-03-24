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
 * ViewManager that bridges React Native's `<RNSceneView>` to the Jetpack Compose
 * `Scene { }` composable from `io.github.sceneview`.
 */
class SceneViewManager : SimpleViewManager<FrameLayout>() {

    override fun getName(): String = "RNSceneView"

    // Reactive state shared between React props and Compose
    private val modelPaths = mutableStateListOf<ModelNodeData>()
    private val environmentPath = mutableStateOf<String?>(null)
    private val orbitEnabled = mutableStateOf(true)

    override fun createViewInstance(reactContext: ThemedReactContext): FrameLayout {
        val container = FrameLayout(reactContext)
        val composeView = ComposeView(reactContext).apply {
            setContent {
                val engine = rememberEngine()
                val modelLoader = rememberModelLoader(engine)
                val environmentLoader = rememberEnvironmentLoader(engine)

                val cameraNode = rememberCameraNode(engine) {
                    position = Position(y = 0f, z = 3.0f)
                }

                val environment = environmentPath.value?.let { path ->
                    rememberEnvironment(environmentLoader) {
                        environmentLoader.createHDREnvironment(path)
                    }
                }

                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
                    cameraNode = cameraNode,
                    environment = environment,
                ) {
                    modelPaths.forEach { model ->
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
        environmentPath.value = environment
    }

    @ReactProp(name = "modelNodes")
    fun setModelNodes(view: FrameLayout, nodes: ReadableArray?) {
        modelPaths.clear()
        nodes?.let { array ->
            for (i in 0 until array.size()) {
                val map = array.getMap(i) ?: continue
                modelPaths.add(
                    ModelNodeData(
                        src = map.getString("src") ?: continue,
                        scale = map.getDouble("scale").toFloat(),
                        animate = map.getBoolean("animation")
                    )
                )
            }
        }
    }

    @ReactProp(name = "cameraOrbit", defaultBoolean = true)
    fun setCameraOrbit(view: FrameLayout, enabled: Boolean) {
        orbitEnabled.value = enabled
    }
}

data class ModelNodeData(
    val src: String,
    val scale: Float = 1.0f,
    val animate: Boolean = true
)

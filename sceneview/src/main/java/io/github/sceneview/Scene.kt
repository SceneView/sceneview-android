package io.github.sceneview

import android.util.Size
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.View
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.Node

@Composable
fun Scene(
    modifier: Modifier = Modifier,
    childNodes: Set<Node> = setOf(),
    /**
     * Provide your own instance if you want to share Filament resources between multiple views.
     */
    sharedEngine: Engine? = null,
    /**
     * Provide your own instance if you want to share [Node]s' scene between multiple views.
     */
    sharedScene: Scene? = null,
    sharedView: View? = null,
    sharedRenderer: Renderer? = null,
    sharedModelLoader: ModelLoader? = null,
    sharedMaterialLoader: MaterialLoader? = null,
    cameraNode: (engine: Engine, viewSize: Size) -> CameraNode = { engine, viewSize ->
        CameraNode(engine = engine, viewSize = viewSize)
    },
    /**
     * Invoked when an frame is processed.
     *
     * Registers a callback to be invoked when a valid Frame is processing.
     *
     * The callback to be invoked once per frame **immediately before the scene is updated.
     *
     * The callback will only be invoked if the Frame is considered as valid.
     */
    onFrame: ((frameTimeNanos: Long) -> Unit)? = null,
    /**
     * Invoked when the `SceneView` is tapped.
     *
     * Only nodes with renderables or their parent nodes can be tapped since Filament picking is
     * used to find a touched node. The ID of the Filament renderable can be used to determine what
     * part of a model is tapped.
     */
    onTap: ((
        /** The motion event that caused the tap. **/
        motionEvent: MotionEvent,
        /** The node that was tapped or `null`. **/
        node: Node?
    ) -> Unit)? = null,
    onCreate: ((SceneView) -> Unit)? = null
) {
    if (LocalInspectionMode.current) {
        ScenePreview(modifier)
    } else {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                SceneView(
                    context, null, 0, 0,
                    sharedEngine,
                    sharedScene,
                    sharedView,
                    sharedRenderer,
                    sharedModelLoader,
                    sharedMaterialLoader,
                    cameraNode,
                    onFrame,
                    onTap
                ).apply {
                    onCreate?.invoke(this)
                }
            },
            update = { sceneView ->
                sceneView.childNodes = childNodes
            },
            onReset = {},
            onRelease = { sceneView ->
                sceneView.destroy()
            }
        )
    }
}

@Composable
private fun ScenePreview(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.DarkGray)
    )
}
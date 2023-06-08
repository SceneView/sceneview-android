package io.github.sceneview

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import io.github.sceneview.node.Node
import io.github.sceneview.renderable.Renderable
import io.github.sceneview.utils.FrameTime

@Composable
fun Scene(
    modifier: Modifier = Modifier,
    nodes: List<Node> = listOf(),
    onCreate: ((SceneView) -> Unit)? = null,
    onFrame: ((FrameTime) -> Unit)? = null,
    onTap: ((MotionEvent, Node?, Renderable?) -> Unit)? = null
) {
    if (LocalInspectionMode.current) {
        ScenePreview(modifier)
    } else {
        var sceneViewNodes = remember { listOf<Node>() }

        AndroidView(
            modifier = modifier,
            factory = { context ->
                SceneView(context).apply {
                    onCreate?.invoke(this)
                    this.onFrame = onFrame
                    this.onTap = onTap
                }
            },
            update = { sceneView ->
                sceneViewNodes.filter { it !in nodes }.forEach {
                    sceneView.removeChild(it)
                }
                nodes.filter { it !in sceneViewNodes }.forEach {
                    sceneView.addChild(it)
                }
                sceneViewNodes = nodes
            },
            // TODO: Add when moving to latest compose version
//            onReset = {},
//            onRelease = { sceneView ->
//                sceneView.destroy()
//            }
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
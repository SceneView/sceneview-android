package io.github.sceneview.ar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.HitResult
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.ArSession
import io.github.sceneview.node.Node

@Composable
fun ARScene(
    modifier: Modifier = Modifier,
    nodes: List<Node> = listOf(),
    planeRenderer: Boolean = true,
    onCreate: ((ArSceneView) -> Unit)? = null,
    onSessionCreate: (ArSceneView.(session: ArSession) -> Unit)? = null,
    onFrame: (ArSceneView.(arFrame: ArFrame) -> Unit)? = null,
    onTap: (ArSceneView.(hitResult: HitResult) -> Unit)? = null
) {
    if (LocalInspectionMode.current) {
        ArScenePreview(modifier)
    } else {
        var sceneViewNodes = remember { listOf<Node>() }

        AndroidView(
            modifier = modifier,
            factory = { context ->
                ArSceneView(context).apply {
                    this.onArSessionCreated = { onSessionCreate?.invoke(this, it) }
                    this.onArFrame = { onFrame?.invoke(this, it) }
                    this.onTapAr = { hitResult, _ -> onTap?.invoke(this, hitResult) }
                    onCreate?.invoke(this)
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

                sceneView.planeRenderer.isEnabled = planeRenderer
            }
        )
    }
}

@Composable
private fun ArScenePreview(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.DarkGray)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center),
            text = "ArSceneView",
            color = Color.White
        )
    }
}
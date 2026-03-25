package io.github.sceneview.demo.showcase

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Describes a node type that can be demoed in the Showcase.
 *
 * @param sceneContent Optional composable that renders a live 3D preview of this node.
 */
data class NodeDemo(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: NodeCategory,
    val codeSnippet: String,
    val requiresAR: Boolean = false,
    val sceneContent: (@Composable () -> Unit)? = null
)

enum class NodeCategory(val label: String) {
    GEOMETRY("Geometry"),
    MODEL("Model"),
    LIGHT("Light & Environment"),
    CAMERA("Camera"),
    CONTENT("Content"),
    AR("AR Nodes")
}

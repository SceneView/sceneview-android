@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sceneview.demo.theme.SceneViewDemoTheme

private val sampleNode = NodeDemo(
    name = "CubeNode",
    description = "Procedural box geometry with configurable size and material.",
    icon = Icons.Default.CropSquare,
    category = NodeCategory.GEOMETRY,
    codeSnippet = """
Scene {
    CubeNode(
        size = Size(1f),
        center = Position(0f, 0.5f, 0f),
        materialInstance = mat
    )
}""".trimIndent()
)

private val sampleARNode = NodeDemo(
    name = "AnchorNode",
    description = "World-space anchor that tracks a real-world position.",
    icon = Icons.Default.ViewInAr,
    category = NodeCategory.AR,
    codeSnippet = "ARScene { AnchorNode(anchor = a) { ... } }",
    requiresAR = true
)

@Preview(showBackground = true, name = "NodeCard - Collapsed")
@Composable
private fun NodeCardCollapsedPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                NodeCard(node = sampleNode, isExpanded = false, onToggle = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "NodeCard - Expanded")
@Composable
private fun NodeCardExpandedPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                NodeCard(node = sampleNode, isExpanded = true, onToggle = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "NodeCard - AR Badge")
@Composable
private fun NodeCardARPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                NodeCard(node = sampleARNode, isExpanded = false, onToggle = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "NodeCard - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NodeCardDarkPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                NodeCard(node = sampleNode, isExpanded = true, onToggle = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "Node Cards List")
@Composable
private fun NodeCardListPreview() {
    val nodes = listOf(
        sampleNode,
        NodeDemo(
            name = "SphereNode",
            description = "Procedural sphere geometry with configurable radius.",
            icon = Icons.Default.Circle,
            category = NodeCategory.GEOMETRY,
            codeSnippet = "SphereNode(radius = 0.5f)"
        ),
        sampleARNode
    )
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                nodes.forEachIndexed { index, node ->
                    NodeCard(
                        node = node,
                        isExpanded = index == 0,
                        onToggle = {}
                    )
                }
            }
        }
    }
}

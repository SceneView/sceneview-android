package io.github.sceneview.interaction

import com.google.ar.sceneform.ux.BaseTransformableNode
import com.google.ar.sceneform.ux.SelectionVisualizer
import io.github.sceneview.node.Node

class NodeSelection(val selectorNode: Node) : SelectionVisualizer {

    override fun applySelectionVisual(node: BaseTransformableNode?) {
        selectorNode.parent = node
    }

    override fun removeSelectionVisual(node: BaseTransformableNode?) {
        selectorNode.parent = null
    }
}
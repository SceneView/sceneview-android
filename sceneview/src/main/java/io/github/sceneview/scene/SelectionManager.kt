package io.github.sceneview.scene

import io.github.sceneview.node.Node

interface SelectionManager {
    fun isSelected(node: Node): Boolean
    fun select(node: Node): Boolean
}

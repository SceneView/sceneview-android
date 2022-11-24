package io.github.sceneview.gesture

import com.google.android.filament.Engine
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.clamp
import io.github.sceneview.SceneView
import io.github.sceneview.math.Size
import io.github.sceneview.nodes.Node

open class NodesManipulator(val engine: Engine, val selectionNode: (size: Size) -> Node?) {

    enum class SelectionMode {
        NONE, SINGLE, MULTIPLE
    }

    var selectionMode: SelectionMode = SelectionMode.SINGLE

    /**
     * Whether it is possible to deselect nodes
     *
     * A [Node] can be deselected if no [Node] s are picked on tap.
     */
    var allowDeselection = true

    var selectNodeOnEditBegin = true

    var isPositionEditable: Boolean = false
    var isRotationEditable: Boolean = false
    var isScaleEditable: Boolean = false

    var scaleRange: ClosedFloatingPointRange<Float> = (0.1f..10.0f)

    private var selectedNodes = listOf<Node>()

    fun setTransformEditable(editable: Boolean) {
        isPositionEditable = editable
        isRotationEditable = editable
        isScaleEditable = editable
    }

    fun getSelectedNode() = selectedNodes.firstOrNull()
    fun getSelectedNodes() = selectedNodes.toList()

    fun setNodeSelected(node: Node, selected: Boolean) {
        if (selected) {
            setSelectedNodes(
                when (selectionMode) {
                    SelectionMode.NONE -> listOf()
                    SelectionMode.SINGLE -> listOf(node)
                    SelectionMode.MULTIPLE -> selectedNodes + node
                }
            )
        } else {
            setSelectedNodes(selectedNodes - node)
        }
    }

    fun setSelectedNode(node: Node) {
        setSelectedNodes(listOf(node))
    }

    fun setSelectedNodes(nodes: List<Node>) {
        if (!allowDeselection && nodes.isEmpty()) return

        selectedNodes.filter { it !in nodes }.forEach { node ->
            node.isSelected = false
        }
        nodes.filter { it !in selectedNodes }.forEach { node ->
            if (node.selectionNode == null) {
                node.selectionNode = selectionNode(node.size)
            }
            node.isSelected = true
        }
        selectedNodes = nodes
    }

    open fun onTap(node: Node?) {
        if (node != null) {
            node.onTap()
            setNodeSelected(node, !node.isSelected)
        } else {
            setSelectedNodes(listOf())
        }
    }

    fun onMoveBegin(node: Node) {
        if (selectNodeOnEditBegin && isPositionEditable) {
            (listOf(node) + node.allParentNodes).firstOrNull {
                it.isPositionEditable
            }?.let { setSelectedNode(it) }
        }
    }

    fun onMove(node: Node, pickingResult: SceneView.PickingResult) {
        if (isPositionEditable) {
            selectedNodes.filter { it.isPositionEditable }.forEach {
                it.worldPosition = pickingResult.worldPosition
            }
        }
    }

    open fun onRotateBegin(node: Node) {
        if (selectNodeOnEditBegin && isRotationEditable) {
            (listOf(node) + node.allParentNodes).firstOrNull {
                it.isRotationEditable
            }?.let { setNodeSelected(it, true) }
        }
    }

    open fun onRotate(node: Node, delta: Quaternion) {
        if (isRotationEditable) {
            selectedNodes.filter { it.isRotationEditable }.forEach {
                it.quaternion *= delta
            }
        }
    }

    fun onScaleBegin(node: Node) {
        if (selectNodeOnEditBegin && isScaleEditable) {
            (listOf(node) + node.allParentNodes).firstOrNull {
                it.isScaleEditable
            }?.let { setNodeSelected(it, true) }
        }
    }

    fun onScale(node: Node, scaleFactor: Float) {
        if (isScaleEditable) {
            selectedNodes.filter { it.isScaleEditable }.forEach {
                it.scale = clamp(
                    node.scale * scaleFactor,
                    scaleRange.start, scaleRange.endInclusive
                )
            }
        }
    }

    fun destroy() {
    }
}
package io.github.sceneview.ar.interaction

import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.EditableTransform
import io.github.sceneview.interaction.GestureDetector
import io.github.sceneview.node.Node
import io.github.sceneview.scene.SelectionVisualizer

class ArNodeManipulator(
    private val sceneView: ArSceneView
) {
    private var currentGesture: GestureDetector.Gesture = GestureDetector.Gesture.NONE

    private var activeGesture: GestureStrategy? = null
    var currentNode: ArNode? = null
    private val selectionVisualizer: SelectionVisualizer
        get() = sceneView.selectionVisualizer

    fun onNodeTouch(node: Node) {
        val oldCurrentNode = currentNode
        currentNode = node as? ArNode
        if (oldCurrentNode == currentNode) return
        currentNode?.let { selectionVisualizer.applySelectionVisual(it) }
        oldCurrentNode?.let { selectionVisualizer.removeSelectionVisual(it) }
    }


    fun rotate(deltaDegree: Float) {
        activeGesture?.rotate(deltaDegree)
    }

    fun gestureChanged(gesture: GestureDetector.Gesture) {
        val arNode = currentNode ?: return
        currentGesture = gesture
        activeGesture = when {
            gesture == GestureDetector.Gesture.ZOOM &&
                    supportsEditMode(EditableTransform.SCALE) -> ScaleGesture(arNode)
            gesture == GestureDetector.Gesture.ORBIT &&
                    supportsEditMode(EditableTransform.POSITION) -> TranslationGesture(arNode)
            gesture == GestureDetector.Gesture.TWIST &&
                    supportsEditMode(EditableTransform.ROTATION) -> RotationGesture(arNode)
            else -> null
        }
    }

    private fun supportsEditMode(mode: EditableTransform): Boolean {
        return currentNode?.isEditable == true &&
                currentNode?.editableTransforms?.contains(mode) == true
    }

    fun scale(scaleFactor: Float): Boolean {
        activeGesture?.scale(scaleFactor)
        return activeGesture is ScaleGesture
    }
}
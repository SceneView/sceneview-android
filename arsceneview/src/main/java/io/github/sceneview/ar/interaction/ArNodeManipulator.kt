package io.github.sceneview.ar.interaction

import android.view.MotionEvent
import io.github.sceneview.SceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.EditableTransform
import io.github.sceneview.interaction.GestureDetector
import io.github.sceneview.interaction.GestureHandler
import io.github.sceneview.interaction.Manipulator
import io.github.sceneview.node.Node
import io.github.sceneview.scene.SelectionVisualizer
import io.github.sceneview.scene.Transformable

class ArNodeManipulator(
    private val selectionVisualizer: SelectionVisualizer,
    sceneView: SceneView
) : GestureHandler(sceneView), Manipulator {
    private val gestureDetector = GestureDetector(sceneView, this, supportsTwist = true)
    private var currentGesture: GestureDetector.Gesture = GestureDetector.Gesture.NONE

    private var activeGesture: GestureStrategy? = null
    var currentNode: ArNode? = null

    override fun onNodeTouch(node: Node) {
        val oldCurrentNode = currentNode
        currentNode = node as? ArNode
        if (oldCurrentNode == currentNode) return
        currentNode?.let { selectionVisualizer.applySelectionVisual(it) }
        oldCurrentNode?.let { selectionVisualizer.removeSelectionVisual(it) }
    }

    override fun onTouchEvent(event: MotionEvent) {
        gestureDetector.onTouchEvent(event)
    }

    override fun scroll(x: Int, y: Int, scrolldelta: Float) {
        activeGesture?.scroll(scrolldelta)
    }

    override fun grabUpdate(x: Int, y: Int) {
        activeGesture?.continueGesture(x, y)
    }

    override fun grabBegin(x: Int, y: Int, strafe: Boolean) {
        activeGesture?.beginGesture(x, y)
    }

    override fun grabEnd() {
        activeGesture?.endGesture()
    }

    override fun rotate(deltaDegree: Float) {
        activeGesture?.rotate(deltaDegree)
    }

    override fun gestureChanged(gesture: GestureDetector.Gesture) {
        val arNode = currentNode ?: return
        currentGesture = gesture
        activeGesture = when {
            gesture == GestureDetector.Gesture.ZOOM &&
                    supportsEditMode(Transformable.EditMode.SCALE) -> ScaleGesture(arNode)
            gesture == GestureDetector.Gesture.ORBIT &&
                    supportsEditMode(Transformable.EditMode.MOVE) -> TranslationGesture(arNode)
            gesture == GestureDetector.Gesture.TWIST &&
                    supportsEditMode(Transformable.EditMode.ROTATE) -> RotationGesture(arNode)
            else -> null
        }
    }

    private fun supportsEditMode(mode: Transformable.EditMode): Boolean {
        // TODO: Remove the Transformable class
        val transform = when (mode) {
            Transformable.EditMode.MOVE -> EditableTransform.POSITION
            Transformable.EditMode.ROTATE -> EditableTransform.ROTATION
            Transformable.EditMode.SCALE -> EditableTransform.SCALE
        }
        return currentNode?.isEditable == true &&
                currentNode?.editableTransforms?.contains(transform) == true
    }
}
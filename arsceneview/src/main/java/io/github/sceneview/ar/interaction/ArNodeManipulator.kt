package io.github.sceneview.ar.interaction

import android.view.MotionEvent
import io.github.sceneview.SceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.interaction.GestureDetector
import io.github.sceneview.interaction.GestureHandler
import io.github.sceneview.interaction.Manipulator
import io.github.sceneview.node.Node

class ArNodeManipulator(sceneView: SceneView) : GestureHandler(sceneView), Manipulator {
    private val gestureDetector = GestureDetector(sceneView, this, supportsTwist = true)
    private var currentGesture: GestureDetector.Gesture = GestureDetector.Gesture.NONE

    private var activeGesture: GestureStrategy? = null
    var currentNode: ArNode? = null

    override fun onNodeTouch(node: Node) {
        currentNode = node as? ArNode
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
        activeGesture = when (gesture) {
            GestureDetector.Gesture.ZOOM -> ScaleGesture(arNode)
            GestureDetector.Gesture.ORBIT -> TranslationGesture(arNode)
            GestureDetector.Gesture.TWIST -> RotationGesture(arNode)
            else -> null
        }
    }
}

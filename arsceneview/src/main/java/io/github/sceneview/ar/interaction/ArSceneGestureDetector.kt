package io.github.sceneview.ar.interaction

import android.view.ScaleGestureDetector
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.interaction.GestureDetector.Gesture
import io.github.sceneview.interaction.MoveGestureDetector
import io.github.sceneview.interaction.RotateGestureDetector
import io.github.sceneview.interaction.SceneGestureDetector
import io.github.sceneview.node.Node

class ArSceneGestureDetector(sceneView: ArSceneView,
                             listener: SceneGestureDetector.OnSceneGestureListener? = null, // TODO: Needs to be called
                             private val nodeManipulator: ArNodeManipulator? = ArNodeManipulator(sceneView)) :
        SceneGestureDetector(sceneView) {
    private val moveListener = MoveGestureDetector.SimpleOnMoveGestureListener()
    private val rotateListener = object : RotateGestureDetector.SimpleOnRotateGestureListener() {
        override fun onRotateBegin(detector: RotateGestureDetector): Boolean {
            nodeManipulator?.gestureChanged(Gesture.TWIST)  //TODO: gestureChanged can be removed
            return nodeManipulator != null
        }

        override fun onRotate(detector: RotateGestureDetector): Boolean {
            return nodeManipulator?.let {
                detector.currentAngle.let { rotation -> it.rotate(rotation) }
                return true
            } ?: false
        }
    }
    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            nodeManipulator?.gestureChanged(Gesture.ZOOM)
            return nodeManipulator != null
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return nodeManipulator?.let {
                return it.scale(detector.scaleFactor)
            } ?: false
        }
    }

    override var moveGestureDetector: MoveGestureDetector? = MoveGestureDetector(sceneView.context, moveListener)
    override var rotateGestureDetector: RotateGestureDetector? = RotateGestureDetector(sceneView.context, rotateListener)
    override var scaleGestureDetector: ScaleGestureDetector? = ScaleGestureDetector(sceneView.context, scaleListener)

    override fun onNodeTouch(selectedNode: Node): Boolean {
        nodeManipulator?.onNodeTouch(selectedNode)
        return nodeManipulator != null
    }
}

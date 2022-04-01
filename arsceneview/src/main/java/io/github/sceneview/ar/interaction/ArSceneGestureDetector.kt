package io.github.sceneview.ar.interaction

import android.view.ScaleGestureDetector
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.interaction.MoveGestureDetector
import io.github.sceneview.interaction.RotateGestureDetector
import io.github.sceneview.interaction.SceneGestureDetector
import io.github.sceneview.node.Node

class ArSceneGestureDetector(
    sceneView: ArSceneView,
    listener: OnSceneGestureListener? = null,
    val nodeManipulator: ArNodeManipulator? = ArNodeManipulator(sceneView)
) :
    SceneGestureDetector(sceneView, listener) {
    private val moveListener = object : MoveGestureDetector.OnMoveGestureListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            val nodeManipulator = nodeManipulator ?: return listener?.onMove(detector) ?: false
            nodeManipulator.beginTransform()
            return true
        }

        override fun onMoveBegin(detector: MoveGestureDetector): Boolean {
            val nodeManipulator = nodeManipulator ?: return listener?.onMoveBegin(detector) ?: false
            val lastMotionEvent = detector.currentMotionEvent ?: return false
            nodeManipulator.continueTransform(lastMotionEvent.x, lastMotionEvent.y)
            return true
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            nodeManipulator?.endTransform() ?: listener?.onMoveBegin(detector)
        }
    }
    private val rotateListener = object : RotateGestureDetector.OnRotateGestureListener {
        override fun onRotateBegin(detector: RotateGestureDetector): Boolean {
            return nodeManipulator?.let { true } ?: listener?.onRotateBegin(detector) ?: false
        }

        override fun onRotate(detector: RotateGestureDetector): Boolean {
            val nodeManipulator = nodeManipulator ?: return listener?.onRotate(detector) ?: false
            nodeManipulator.rotate(detector.currentAngle)
            return true
        }

        override fun onRotateEnd(detector: RotateGestureDetector) {
            if (nodeManipulator == null) listener?.onRotateEnd(detector)
        }
    }
    private val scaleListener = object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return nodeManipulator?.let { true } ?: listener?.onScaleBegin(detector) ?: false
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val nodeManipulator = nodeManipulator ?: return listener?.onScale(detector) ?: false
            nodeManipulator.scale(detector.scaleFactor)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (nodeManipulator == null) listener?.onScaleEnd(detector)
        }
    }

    override var moveGestureDetector: MoveGestureDetector? =
        MoveGestureDetector(sceneView.context, moveListener)
    override var rotateGestureDetector: RotateGestureDetector? =
        RotateGestureDetector(sceneView.context, rotateListener)
    override var scaleGestureDetector: ScaleGestureDetector? =
        ScaleGestureDetector(sceneView.context, scaleListener)

    override fun onTouchNode(selectedNode: Node): Boolean {
        nodeManipulator?.onNodeTouch(selectedNode)
        return nodeManipulator != null
    }
}

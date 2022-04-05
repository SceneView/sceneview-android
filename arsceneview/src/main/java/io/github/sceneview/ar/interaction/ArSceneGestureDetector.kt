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
        override fun onMoveBegin(detector: MoveGestureDetector): Boolean {
            val nodeManipulator = nodeManipulator ?: return listener?.onMoveBegin(detector) ?: false
            if (!nodeManipulator.positionIsEditable) return false
            return nodeManipulator.beginTransform()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            val nodeManipulator = nodeManipulator ?: return listener?.onMove(detector) ?: false
            val lastMotionEvent = detector.currentMotionEvent ?: return false
            nodeManipulator.continueTransform(lastMotionEvent.x, lastMotionEvent.y)
            return true
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            nodeManipulator?.endTransform() ?: listener?.onMoveBegin(detector)
        }
    }

    private var lastRotationAngle: Float = 0f

    private val rotateListener = object : RotateGestureDetector.OnRotateGestureListener {
        override fun onRotateBegin(detector: RotateGestureDetector): Boolean {
            return nodeManipulator?.beginRotate() ?: listener?.onRotateBegin(detector) ?: false
        }

        override fun onRotate(detector: RotateGestureDetector): Boolean {
            val nodeManipulator = nodeManipulator ?: return listener?.onRotate(detector) ?: false
            val diff = detector.currentAngle - lastRotationAngle
            lastRotationAngle = detector.currentAngle
            return nodeManipulator.rotate(diff)
        }

        override fun onRotateEnd(detector: RotateGestureDetector) {
            lastRotationAngle = 0f
            nodeManipulator?.endRotate() ?: listener?.onRotateEnd(detector)
        }
    }
    private val scaleListener = object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return nodeManipulator?.beginScale() ?: listener?.onScaleBegin(detector) ?: false
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val nodeManipulator = nodeManipulator ?: return listener?.onScale(detector) ?: false
            nodeManipulator.scale(detector.scaleFactor)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            nodeManipulator?.endScale() ?: listener?.onScaleEnd(detector)
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

package io.github.sceneview.interaction

import android.view.ScaleGestureDetector
import io.github.sceneview.SceneView
import io.github.sceneview.node.Node

class SceneNodeGestureDetector(
    sceneView: SceneView,
    listener: SceneGestureDetector.OnSceneGestureListener,
    val nodeManipulator: NodeManipulator? = NodeManipulator(sceneView)
) : SceneGestureDetector(sceneView = sceneView, listener = listener, cameraManipulator = null) {
    private val moveListener = object : MoveGestureDetector.OnMoveGestureListener {
        override fun onMoveBegin(detector: MoveGestureDetector): Boolean {
            val listenerResult = listener?.onMoveBegin(detector) ?: false
            if (nodeManipulator?.selectedNode?.positionEditable != true) return listenerResult
            return listOfNotNull(nodeManipulator.beginTransform(), listenerResult).any()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            val listenerResult = listener?.onMove(detector) ?: false

            val deltaX = detector.deltaX ?: 0f
            val deltaY = detector.deltaY ?: 0f

            return listOfNotNull(
                nodeManipulator?.continueTransform(
                    deltaX,
                    deltaY
                ), listenerResult
            ).any()
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            listener?.onMoveEnd(detector)
            nodeManipulator?.endTransform()
        }
    }

    private var lastRotationAngle: Float = 0f

    private val rotateListener = object : RotateGestureDetector.OnRotateGestureListener {
        override fun onRotateBegin(detector: RotateGestureDetector): Boolean {
            return listOfNotNull(
                nodeManipulator?.beginRotate(),
                listener?.onRotateBegin(detector)
            ).any()
        }

        override fun onRotate(detector: RotateGestureDetector): Boolean {
            val listenerResult = listener?.onRotate(detector) ?: false
            val diff = detector.currentAngle - lastRotationAngle
            lastRotationAngle = detector.currentAngle
            return listOfNotNull(nodeManipulator?.rotate(diff), listenerResult).any()
        }

        override fun onRotateEnd(detector: RotateGestureDetector) {
            lastRotationAngle = 0f
            listener?.onRotateEnd(detector)
            nodeManipulator?.endRotate()
        }
    }
    private val scaleListener = object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return listOfNotNull(
                nodeManipulator?.beginScale(),
                listener?.onScaleBegin(detector)
            ).any()
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return listOfNotNull(
                listener?.onScale(detector),
                nodeManipulator?.scale(detector.scaleFactor)
            ).any()
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            listener?.onScaleEnd(detector)
            nodeManipulator?.endScale()
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
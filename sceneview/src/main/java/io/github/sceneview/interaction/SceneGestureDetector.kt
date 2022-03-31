package io.github.sceneview.interaction

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.google.android.filament.utils.Manipulator
import io.github.sceneview.SceneView
import io.github.sceneview.node.Node

typealias FilamentGestureDetector = com.google.android.filament.utils.GestureDetector

/**
 * ### Responds to Android touch events with listeners and/or camera manipulator
 *
 * Camera supports one-touch orbit, two-touch pan, and pinch-to-zoom.
 */
open class SceneGestureDetector(
    sceneView: SceneView,
    listener: OnSceneGestureListener? = null,
    cameraManipulator: Manipulator? = Manipulator.Builder()
        .targetPosition(Node.DEFAULT_POSITION.x, Node.DEFAULT_POSITION.y, Node.DEFAULT_POSITION.z)
        .viewport(sceneView.width, sceneView.height)
        .build(Manipulator.Mode.ORBIT)
) {
    var gestureDetector = listener?.let { listener ->
        GestureDetector(sceneView.context, listener).apply {
            setContextClickListener(listener)
            setOnDoubleTapListener(listener)
        }
    }
    open var moveGestureDetector = listener?.let { MoveGestureDetector(sceneView.context, it) }
    open var rotateGestureDetector = listener?.let { RotateGestureDetector(sceneView.context, it) }
    open var scaleGestureDetector = listener?.let { ScaleGestureDetector(sceneView.context, it) }
    open var filamentGestureDetector = cameraManipulator?.let { FilamentGestureDetector(sceneView, it) }

    private val gestureDetectors : List<*> by lazy { listOfNotNull(
        gestureDetector,
        moveGestureDetector,
        rotateGestureDetector,
        scaleGestureDetector,
        filamentGestureDetector
    ) }

    fun onTouchEvent(event: MotionEvent) {
        gestureDetectors.forEach {
            when (it) {
                is GestureDetector -> it.onTouchEvent(event)
                is MoveGestureDetector -> it.onTouchEvent(event)
                is RotateGestureDetector -> it.onTouchEvent(event)
                is ScaleGestureDetector -> it.onTouchEvent(event)
                is FilamentGestureDetector -> it.onTouchEvent(event)
            }
        }
    }

    open fun onTouchNode(selectedNode: Node) : Boolean {
        return false
    }

    interface OnSceneGestureListener :
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        GestureDetector.OnContextClickListener,
        MoveGestureDetector.OnMoveGestureListener,
        RotateGestureDetector.OnRotateGestureListener,
        ScaleGestureDetector.OnScaleGestureListener {

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return false
        }

        override fun onLongPress(e: MotionEvent?) {}

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent?,
            distanceX: Float, distanceY: Float
        ): Boolean {
            return false
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return false
        }

        override fun onShowPress(e: MotionEvent) {}

        override fun onDown(e: MotionEvent): Boolean {
            return false
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return false
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            return false
        }

        override fun onContextClick(e: MotionEvent): Boolean {
            return false
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
        }

        override fun onRotate(detector: RotateGestureDetector): Boolean {
            return false
        }

        override fun onRotateBegin(detector: RotateGestureDetector): Boolean {
            return false
        }

        override fun onRotateEnd(detector: RotateGestureDetector) {
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return false
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return false
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
        }
    }
}

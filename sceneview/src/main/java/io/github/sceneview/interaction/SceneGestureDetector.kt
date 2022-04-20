package io.github.sceneview.interaction

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.google.android.filament.utils.Manipulator
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.lookAt
import io.github.sceneview.SceneView
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toFloat3
import io.github.sceneview.node.Node

typealias FilamentGestureDetector = com.google.android.filament.utils.GestureDetector

/**
 * ### Responds to Android touch events with listeners and/or camera manipulator
 *
 * Camera supports one-touch orbit, two-touch pan, and pinch-to-zoom.
 */
open class SceneGestureDetector(
    val sceneView: SceneView,
    listener: OnSceneGestureListener? = null,
    cameraManipulator: Manipulator? = Manipulator.Builder()
        .targetPosition(Node.DEFAULT_POSITION.x, Node.DEFAULT_POSITION.y, Node.DEFAULT_POSITION.z)
        .viewport(sceneView.width, sceneView.height)
        .zoomSpeed(0.05f)
        .build(Manipulator.Mode.ORBIT)
) {
    val camera get() = sceneView.camera

    var cameraManipulator = cameraManipulator
        get() = field?.takeIf { it.transform == camera.worldTransform } ?:
        // TODO: Ask Filament to add a startPosition and startRotation in order to handle
        //  previous possible programmatic camera transforms.
        //  Better would be that we don't have to create a new Manipulator and just update
        //  the it when the camera is programmaticly updated so it don't come back to
        //  initial position.
        //  Return field for now will use the default node position target
        //  or maybe just don't let the user enable manipulator until the camera position
        //  is not anymore at its default targetPosition
        field

    var gestureDetector = listener?.let { listener ->
        GestureDetector(sceneView.context, listener).apply {
            setContextClickListener(listener)
            setOnDoubleTapListener(listener)
        }
    }
    open var moveGestureDetector = listener?.let { MoveGestureDetector(sceneView.context, it) }
    open var rotateGestureDetector = listener?.let { RotateGestureDetector(sceneView.context, it) }
    open var scaleGestureDetector = listener?.let { ScaleGestureDetector(sceneView.context, it) }
    open var filamentGestureDetector: FilamentGestureDetector? = null
        get() = field ?: cameraManipulator?.let { FilamentGestureDetector(sceneView, it) }.also {
            field = it
        }

    private val gestureDetectors: List<*>
        get() = listOfNotNull(
            gestureDetector,
            moveGestureDetector,
            scaleGestureDetector,
            rotateGestureDetector,
            filamentGestureDetector
        )

    var lastTouchEvent: MotionEvent? = null

    fun onTouchEvent(event: MotionEvent) {
        lastTouchEvent = event
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

    open fun onTouchNode(selectedNode: Node): Boolean {
        return false
    }

    interface OnSceneGestureListener :
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        GestureDetector.OnContextClickListener,
        MoveGestureDetector.OnMoveGestureListener,
        RotateGestureDetector.OnRotateGestureListener,
        ScaleGestureDetector.OnScaleGestureListener {

        override fun onSingleTapUp(e: MotionEvent): Boolean {
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

val Manipulator.transform: Transform
    get() = Array(3) { FloatArray(3) }.apply {
        getLookAt(this[0], this[1], this[2])
    }.let { (eye, target, _) ->
        return lookAt(eye = eye.toFloat3(), target = target.toFloat3(), up = Float3(y = 1.0f))
    }
package io.github.sceneview.gesture

import android.view.GestureDetector
import android.view.MotionEvent
import io.github.sceneview.CameraGestureDetector
import io.github.sceneview.CameraManipulator
import io.github.sceneview.SceneView

/**
 * Responds to Android touch events with listeners and/or camera manipulator
 *
 * Camera supports one-touch orbit, two-touch pan, and pinch-to-zoom.
 */
open class GestureDetector(
    private val view: SceneView,
    cameraManipulator: CameraManipulator?,
    private val nodesManipulator: NodesManipulator
) {
    val onDownListeners = mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    val onShowPressListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    val onSingleTapUpListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    val onLongPressListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    var onSingleTapConfirmedListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    var onDoubleTapListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    var onDoubleTapEventListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    var onContextClickListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    var onMoveBeginListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    var onMoveListeners =
        mutableListOf<(e: MotionEvent, start: SceneView.PickingResult, end: SceneView.PickingResult) -> Unit>()
    var onMoveEndListeners = mutableListOf<(e: MotionEvent) -> Unit>()
    var onRotateBeginListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    var onRotateListeners =
        mutableListOf<(e: MotionEvent, detector: RotateGestureDetector) -> Unit>()
    var onRotateEndListeners = mutableListOf<(e: MotionEvent) -> Unit>()
    var onScaleBeginListeners =
        mutableListOf<(e: MotionEvent, result: SceneView.PickingResult) -> Unit>()
    var onScaleListeners = mutableListOf<(e: MotionEvent, detector: ScaleGestureDetector) -> Unit>()
    var onScaleEndListeners = mutableListOf<(e: MotionEvent) -> Unit>()

    private val gestureDetector: GestureDetector
    private val cameraGestureDetector: CameraGestureDetector?
    private val moveGestureDetector: MoveGestureDetector
    private val rotateGestureDetector: RotateGestureDetector
    private val scaleGestureDetector: ScaleGestureDetector

    var editingDetector: Any? = null
    lateinit var touchEventResult: SceneView.PickingResult

    init {
        gestureDetector = GestureDetector(view.context, OnGestureListener())
        cameraGestureDetector = cameraManipulator?.let { CameraGestureDetector(view, it) }
        moveGestureDetector = MoveGestureDetector(view.context, OnMoveGestureListener())
        rotateGestureDetector = RotateGestureDetector(view.context, OnRotateGestureListener())
        scaleGestureDetector = ScaleGestureDetector(view.context, OnScaleGestureListener())
    }

    fun onTouchEvent(e: MotionEvent) {
        cameraGestureDetector?.onTouchEvent(e)

        if (editingDetector == null) {
            view.pickNode(e) { pickingResult ->
                dispatchTouchEvent(e, pickingResult)
            }
        } else {
            // Prevent too much picking while editing
            dispatchTouchEvent(e, touchEventResult)
        }
    }

    fun dispatchTouchEvent(e: MotionEvent, touchEventResult: SceneView.PickingResult) {
        this.touchEventResult = touchEventResult
        if (touchEventResult.node?.onTouchEvent(e, touchEventResult) != true) {
            gestureDetector.onTouchEvent(e)
            moveGestureDetector.onTouchEvent(e)
            rotateGestureDetector.onTouchEvent(e)
            scaleGestureDetector.onTouchEvent(e)
        }
    }

    inner class OnGestureListener() : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent) = super.onSingleTapUp(e).also {
            onSingleTapUpListeners.forEach { it(e, touchEventResult) }
        }

        override fun onLongPress(e: MotionEvent) = super.onLongPress(e).also {
            onLongPressListeners.forEach { it(e, touchEventResult) }
        }

        override fun onShowPress(e: MotionEvent) = super.onShowPress(e).also {
            onShowPressListeners.forEach { it(e, touchEventResult) }
        }

        override fun onDown(e: MotionEvent) = super.onDown(e).also {
            onDownListeners.forEach { it(e, touchEventResult) }
        }

        override fun onDoubleTap(e: MotionEvent) = super.onDoubleTap(e).also {
            onDoubleTapListeners.forEach { it(e, touchEventResult) }
        }

        override fun onDoubleTapEvent(e: MotionEvent) = super.onDoubleTapEvent(e).also {
            onDoubleTapEventListeners.forEach { it(e, touchEventResult) }
        }

        override fun onSingleTapConfirmed(e: MotionEvent) = super.onSingleTapConfirmed(e).also {
            nodesManipulator.onTap(touchEventResult.node)
            onSingleTapConfirmedListeners.forEach { it(e, touchEventResult) }
        }

        override fun onContextClick(e: MotionEvent) = super.onContextClick(e).also {
            onContextClickListeners.forEach { it(e, touchEventResult) }
        }
    }

    inner class OnMoveGestureListener : MoveGestureDetector.SimpleOnMoveListener {

        override fun onMoveBegin(detector: MoveGestureDetector, e: MotionEvent) =
            super.onMoveBegin(detector, e).also {
                if (editingDetector == null) {
                    editingDetector = detector
                    touchEventResult.node?.let { nodesManipulator.onMoveBegin(it) }
                    onMoveBeginListeners.forEach { it(e, touchEventResult) }
                }
            }

        override fun onMove(detector: MoveGestureDetector, e: MotionEvent) =
            super.onMove(detector, e).also {
                if (editingDetector == detector) {
                    view.pickNode(e) { pickingResult ->
                        touchEventResult.node?.let { nodesManipulator.onMove(it, pickingResult) }
                        onMoveListeners.forEach { it(e, touchEventResult, pickingResult) }
                    }
                }
            }

        override fun onMoveEnd(detector: MoveGestureDetector, e: MotionEvent) =
            super.onMoveEnd(detector, e).also {
                if (editingDetector == detector) {
                    editingDetector = null
                    onMoveEndListeners.forEach { it(e) }
                }
            }
    }

    inner class OnRotateGestureListener : RotateGestureDetector.SimpleOnRotateListener {

        // The first delta is always way off as it contains all delta until threshold to
        // recognize rotate gesture is met
        private var skipFirstRotateEdit = false

        override fun onRotateBegin(detector: RotateGestureDetector, e: MotionEvent) =
            super.onRotateBegin(detector, e).also {
                if (editingDetector == null) {
                    skipFirstRotateEdit = true
                    editingDetector = detector
                    touchEventResult.node?.let { nodesManipulator.onRotateBegin(it) }
                    onRotateBeginListeners.forEach { it(e, touchEventResult) }
                }
            }

        override fun onRotate(detector: RotateGestureDetector, e: MotionEvent) =
            super.onRotate(detector, e).also {
                if (editingDetector == detector) {
                    if (!skipFirstRotateEdit) {
                        touchEventResult.node?.let {
                            nodesManipulator.onRotate(it, detector.deltaQuaternion)
                        }
                        onRotateListeners.forEach { it(e, detector) }
                    } else {
                        skipFirstRotateEdit = false
                    }
                }
            }

        override fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent) =
            super.onRotateEnd(detector, e).also {
                if (editingDetector == detector) {
                    editingDetector = null
                    onRotateEndListeners.forEach { it(e) }
                }
            }
    }

    inner class OnScaleGestureListener : ScaleGestureDetector.SimpleOnScaleListener {

        override fun onScaleBegin(detector: ScaleGestureDetector, e: MotionEvent) =
            super.onScaleBegin(detector, e).also {
                if (editingDetector == null) {
                    editingDetector = detector
                    touchEventResult.node?.let { nodesManipulator.onScaleBegin(it) }
                    onScaleBeginListeners.forEach { it(e, touchEventResult) }
                }
            }

        override fun onScale(detector: ScaleGestureDetector, e: MotionEvent) =
            super.onScale(detector, e).also {
                if (editingDetector == detector) {
                    touchEventResult.node?.let { nodesManipulator.onScaleBegin(it) }
                    onScaleListeners.forEach { it(e, detector) }
                }
            }

        override fun onScaleEnd(detector: ScaleGestureDetector, e: MotionEvent) =
            super.onScaleEnd(detector, e).also {
                if (editingDetector == detector) {
                    editingDetector = null
                    onScaleEndListeners.forEach { it(e) }
                }
            }
    }
}
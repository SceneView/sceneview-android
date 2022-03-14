package io.github.sceneview.interaction

import android.view.MotionEvent
import com.google.android.filament.utils.Manipulator
import dev.romainguy.kotlin.math.lookAt
import io.github.sceneview.SceneLifecycleObserver
import io.github.sceneview.SceneView
import io.github.sceneview.math.toFloat3
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime

class CameraGestureHandler(private val sceneView: SceneView) : GestureHandler(sceneView),
    SceneLifecycleObserver {
    private val manipulator = Manipulator.Builder().build(Manipulator.Mode.ORBIT)
    private val gestureDetector = GestureDetector(view, FilamentManipulatorAdapter(manipulator))

    private val eyePos = FloatArray(3)
    private val target = FloatArray(3)
    private val upward = FloatArray(3)

    init {
        sceneView.sceneLifecycle.addObserver(this)
    }

    override fun onTouchEvent(event: MotionEvent) {
        gestureDetector.onTouchEvent(event)
    }

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)
        manipulator.getLookAt(eyePos, target, upward)
        sceneView.camera.transform = lookAt(
            eyePos.toFloat3(),
            target.toFloat3(),
            upward.toFloat3()
        )
    }

    override fun onNodeTouch(node: Node) { }
}

internal class FilamentManipulatorAdapter(private val manipulator: Manipulator) :
    io.github.sceneview.interaction.Manipulator {

    override fun scroll(x: Int, y: Int, scrolldelta: Float) {
        manipulator.scroll(x, y, scrolldelta)
    }

    override fun grabUpdate(x: Int, y: Int) {
        manipulator.grabUpdate(x, y)
    }

    override fun grabBegin(x: Int, y: Int, strafe: Boolean) {
        manipulator.grabBegin(x, y, strafe)
    }

    override fun grabEnd() {
        manipulator.grabEnd()
    }

    override fun gestureChanged(gesture: GestureDetector.Gesture) { }

    override fun rotate(deltaDegree: Float) { }
}
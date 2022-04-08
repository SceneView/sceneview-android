package io.github.sceneview.interaction

import android.view.MotionEvent
import io.github.sceneview.SceneView
import io.github.sceneview.node.Node

abstract class GestureHandler(protected val view: SceneView) {
    abstract fun onTouchEvent(event: MotionEvent)
    abstract fun onNodeTouch(node: Node)
}
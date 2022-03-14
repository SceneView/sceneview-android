package io.github.sceneview.ar.interaction

import io.github.sceneview.ar.node.ArNode

abstract class GestureStrategy(val arNode: ArNode) {
    open fun beginGesture(x: Int, y: Int) {}

    open fun continueGesture(x: Int, y: Int) {}

    open fun endGesture() {}

    open fun scroll(scrollDelta: Float) {}

    open fun rotate(rotateDelta: Float) {}
}
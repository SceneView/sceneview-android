package io.github.sceneview.interaction

interface Manipulator {
    fun scroll(x: Int, y: Int, scrolldelta: Float)
    fun rotate(deltaDegree: Float)
    fun grabUpdate(x: Int, y: Int)
    fun grabBegin(x: Int, y: Int, strafe: Boolean)
    fun grabEnd()

    fun gestureChanged(gesture: GestureDetector.Gesture)
}

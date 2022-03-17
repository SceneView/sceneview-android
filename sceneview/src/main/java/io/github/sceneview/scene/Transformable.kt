package io.github.sceneview.scene

interface Transformable {
    val isFocusable: Boolean
    val editModes: Set<EditMode>

    /**
     * Allowed ways to manipulate the node.
     */
    enum class EditMode {
        ROTATE, SCALE, MOVE
    }
}
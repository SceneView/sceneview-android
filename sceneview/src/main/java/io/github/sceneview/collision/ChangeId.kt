package io.github.sceneview.collision

/**
 * Used to identify when the state of an object has changed by incrementing an integer id. Other
 * classes can determine when this object has changed by polling to see if the id has changed.
 *
 * This is useful as an alternative to an event listener subscription model when there is no safe
 * point in the lifecycle of an object to unsubscribe from the event listeners. Unlike event
 * listeners, this cannot cause memory leaks.
 *
 * @hide
 */
class ChangeId {
    companion object {
        const val EMPTY_ID = 0
    }

    private var id = EMPTY_ID

    fun get(): Int = id

    fun isEmpty(): Boolean = id == EMPTY_ID

    fun checkChanged(id: Int): Boolean = this.id != id && !isEmpty()

    fun update() {
        id++

        // Skip EMPTY_ID if the id has cycled all the way around.
        if (id == EMPTY_ID) {
            id++
        }
    }
}

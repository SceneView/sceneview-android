package io.github.sceneview.collision

import io.github.sceneview.node.Node

/**
 * Stores the results of calls to Scene.hitTest and Scene.hitTestAll. Contains a node that was hit
 * by the hit test, and associated information.
 */
class HitResult(
    /**
     * The node that was hit by the hit test. Null when there is no hit.
     */
    node: Node? = null
) : RayHit() {

    var node
        get() = _node!!
        internal set(value) {
            _node = value
        }

    private var _node: Node? = node

    internal fun set(other: HitResult) {
        super.set(other)
        _node = other.node
    }

    override fun reset() {
        super.reset()
        _node = null
    }
}
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

    /**
     * The node that was hit.
     *
     * @throws IllegalStateException if accessed after [reset] or when constructed without a node.
     * @see nodeOrNull for a safe alternative that returns `null` instead of throwing.
     */
    var node
        get() = _node ?: throw IllegalStateException("HitResult has been reset")
        internal set(value) {
            _node = value
        }

    /**
     * The node that was hit, or `null` if this result has been [reset] or was constructed empty.
     *
     * Prefer this over [node] when you cannot guarantee the result is still valid (e.g. after
     * pooling or when iterating over a reusable results list).
     */
    val nodeOrNull: Node? get() = _node

    private var _node: Node? = node

    internal fun set(other: HitResult) {
        super.set(other)
        _node = other.nodeOrNull
    }

    override fun reset() {
        super.reset()
        _node = null
    }
}
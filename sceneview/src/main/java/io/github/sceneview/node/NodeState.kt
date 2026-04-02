package io.github.sceneview.node

import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale

/**
 * Immutable snapshot of a [Node]'s observable state.
 *
 * Useful for ViewModel-driven UI or for saving/restoring node configuration.
 * Create from a live node with [Node.toState] and apply back with [Node.applyState].
 *
 * @property position   Local-space position of the node (relative to parent).
 * @property quaternion Orientation as a unit quaternion.
 * @property scale      Uniform or non-uniform scale.
 * @property isVisible  Whether the node (and its children) is rendered.
 * @property isEditable Whether gesture-based editing (move/rotate/scale) is enabled.
 * @property isTouchable Whether the node responds to touch/hit-test events.
 */
data class NodeState(
    val position: Position = Position(),
    val quaternion: Quaternion = Quaternion(),
    val scale: Scale = Scale(1f),
    val isVisible: Boolean = true,
    val isEditable: Boolean = false,
    val isTouchable: Boolean = true
)

/**
 * Creates an immutable [NodeState] snapshot of this node's current values.
 */
fun Node.toState() = NodeState(
    position = position,
    quaternion = quaternion,
    scale = scale,
    isVisible = isVisible,
    isEditable = isEditable,
    isTouchable = isTouchable
)

/**
 * Applies a [NodeState] snapshot to this node, updating transform and flags.
 */
fun Node.applyState(state: NodeState) {
    position = state.position
    quaternion = state.quaternion
    scale = state.scale
    isVisible = state.isVisible
    isEditable = state.isEditable
    isTouchable = state.isTouchable
}

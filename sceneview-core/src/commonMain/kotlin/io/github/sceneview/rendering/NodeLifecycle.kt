package io.github.sceneview.rendering

import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform

/**
 * Cross-platform node interface defining the scene graph contract.
 *
 * Platform-specific node implementations (Filament on Android, SceneKit on iOS)
 * implement this interface. All spatial properties use kotlin-math types.
 */
interface SceneNode {

    /** Optional name for debugging. */
    var name: String?

    /** Whether this node is visible (cascades to children). */
    var isVisible: Boolean

    /** Whether this node participates in hit testing. */
    var isHittable: Boolean

    // --- Local transforms ---

    /** Position in parent's local space. */
    var position: Position

    /** Rotation as quaternion in parent's local space. */
    var quaternion: Quaternion

    /** Euler angle rotation in parent's local space (ZYX order). */
    var rotation: Rotation

    /** Scale factors in parent's local space. */
    var scale: Scale

    /** Combined local transform matrix. */
    var transform: Transform

    // --- World transforms ---

    /** Position in world space. */
    var worldPosition: Position

    /** Quaternion in world space. */
    var worldQuaternion: Quaternion

    /** Euler angle rotation in world space. */
    var worldRotation: Rotation

    /** Scale in world space. */
    var worldScale: Scale

    /** Combined world transform matrix. */
    var worldTransform: Transform

    // --- Hierarchy ---

    /** Parent node, null if root. */
    val parent: SceneNode?

    /** Child nodes. */
    val childNodes: Set<SceneNode>

    /** Add a child node. */
    fun addChildNode(node: SceneNode)

    /** Remove a child node. */
    fun removeChildNode(node: SceneNode)

    // --- Orientation helpers ---

    /**
     * Rotates the node to look at a target position.
     */
    fun lookAt(
        targetWorldPosition: Position,
        upDirection: Direction = Direction(y = 1.0f)
    )

    /**
     * Rotates the node to look in a direction.
     */
    fun lookTowards(
        lookDirection: Direction,
        upDirection: Direction = Direction(y = 1.0f)
    )

    // --- Lifecycle ---

    /** Called when node is added to a scene. */
    fun onAddedToScene() {}

    /** Called when node is removed from a scene. */
    fun onRemovedFromScene() {}

    /** Called each frame with delta time. */
    fun onFrame(deltaTime: Float) {}

    /** Releases all platform resources. */
    fun destroy()
}

package io.github.sceneview.node

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.GestureDetector
import android.view.GestureDetector.OnContextClickListener
import android.view.GestureDetector.OnDoubleTapListener
import android.view.MotionEvent
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Scene
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.degrees
import io.github.sceneview.Entity
import io.github.sceneview.FilamentEntity
import io.github.sceneview.animation.NodeAnimator
import io.github.sceneview.collision.Collider
import io.github.sceneview.collision.CollisionShape
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.TransformProvider
import io.github.sceneview.components.TransformComponent
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.RotateGestureDetector
import io.github.sceneview.gesture.ScaleGestureDetector
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toMatrix
import kotlin.reflect.KProperty1

/**
 * A Node represents a transformation within the scene graph's hierarchy.
 *
 * It can contain a renderable for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 *
 * ------- +y ----- -z
 *
 * ---------|----/----
 *
 * ---------|--/------
 *
 * -x - - - 0 - - - +x
 *
 * ------/--|---------
 *
 * ----/----|---------
 *
 * +z ---- -y --------
 */
open class Node(
    final override val engine: Engine,
    @FilamentEntity final override val entity: Entity = EntityManager.get().create(),
    /**
     * The parent node.
     *
     * If set to null, this node will not be attached.
     *
     * The local position, rotation, and scale of this node will remain the same.
     * Therefore, the world position, rotation, and scale of this node may be different after the
     * parent changes.
     */
    parent: Node? = null
) : TransformComponent,
    GestureDetector.OnGestureListener,
    OnDoubleTapListener,
    OnContextClickListener,
    MoveGestureDetector.OnMoveListener,
    RotateGestureDetector.OnRotateListener,
    ScaleGestureDetector.OnScaleListener,
    TransformProvider {

    var isHittable: Boolean = true

    /**
     * The node can be selected when a touch event happened.
     *
     * If a not touchable child [Node] is touched, we check the parent hierarchy to find the
     * closest touchable parent. In this case, the first selectable parent will be the one to have
     * its [isTouchable] value to `true`.
     */
    open var isTouchable: Boolean = true
    open var isEditable: Boolean = false
    open var isPositionEditable: Boolean = false
        get() = isEditable && field
    open var isRotationEditable: Boolean = true
        get() = isEditable && field
    open var isScaleEditable: Boolean = true
        get() = isEditable && field

    var editableScaleRange = 0.1f..10.0f

    /**
     * The visible state of this node.
     *
     * Note that a Node may be visible but still not rendered if its parent is not visible or if it
     * isn't part of the scene.
     */
    open var isVisible = true
        get() = field && parent?.isVisible != false
        set(value) {
            if (field != value) {
                field = value
                updateVisibility()
            }
        }

    /**
     * Changes the parent node.
     *
     * If set to null, this node will be detached.
     *
     * The local position, rotation, and scale of this node will remain the same.
     * Therefore, the world position, rotation, and scale of this node may be different after the
     * parent changes.
     *
     * In addition to setting this field, it will also do the following things:
     * - Remove this node from its previous parent's children.
     * - Add this node to its new parent's children.
     * - Recursively update the node's transformation to reflect the change in parent.
     * - Recursively update the scene field to match the new parent's scene field.
     */
    var parent: Node? = null
        set(value) {
            if (field != value) {
                val oldParent = field
                field = value
                parentComponent = value
                oldParent?.removeChildNode(this)
                value?.addChildNode(this)
            }
        }

    var childNodes = setOf<Node>()
        set(value) {
            if (field != value) {
                val removedNodes = field - value
                val addedNodes = value - field
                field = value
                removedNodes.forEach { child ->
                    child.parent = null
                    onChildRemoved.forEach { it(child) }
                }
                addedNodes.forEach { child ->
                    child.parent = this
                    onChildAdded.forEach { it(child) }
                }
                childComponents = value
            }
        }

    var collisionSystem: CollisionSystem? = null
        set(value) {
            if (field != value) {
                field = value
                collider?.setAttachedCollisionSystem(value)
            }
        }

    var editingTransforms = setOf<KProperty1<Node, Any>>()
        set(value) {
            if (field != value) {
                field = value
                onEditingChanged?.invoke(value)
            }
        }

    var onFrame: ((frameTimeNanos: Long) -> Unit)? = null
    var onAddedToScene:  ((scene: Scene) -> Unit)? = null
    var onRemovedFromScene:  ((scene: Scene) -> Unit)? = null

    var onDown: ((e: MotionEvent) -> Boolean)? = null
    var onShowPress: ((e: MotionEvent) -> Unit)? = null
    var onSingleTapUp: ((e: MotionEvent) -> Boolean)? = null
    var onScroll: ((e1: MotionEvent?, e2: MotionEvent, distance: Float2) -> Boolean)? = null
    var onLongPress: ((e: MotionEvent) -> Unit)? = null
    var onFling: ((e1: MotionEvent?, e2: MotionEvent, velocity: Float2) -> Boolean)? = null
    var onSingleTapConfirmed: ((e: MotionEvent) -> Boolean)? = null
    var onDoubleTap: ((e: MotionEvent) -> Boolean)? = null
    var onDoubleTapEvent: ((e: MotionEvent) -> Boolean)? = null
    var onContextClick: ((e: MotionEvent) -> Boolean)? = null
    var onMoveBegin: ((detector: MoveGestureDetector, e: MotionEvent) -> Boolean)? = null
    var onMove: ((detector: MoveGestureDetector, e: MotionEvent, worldPosition: Position) -> Boolean)? =
        null
    var onMoveEnd: ((detector: MoveGestureDetector, e: MotionEvent) -> Unit)? = null
    var onRotateBegin: ((detector: RotateGestureDetector, e: MotionEvent) -> Boolean)? = null
    var onRotate: ((detector: RotateGestureDetector, e: MotionEvent, rotationDelta: Quaternion) -> Boolean)? =
        null
    var onRotateEnd: ((detector: RotateGestureDetector, e: MotionEvent) -> Unit)? = null
    var onScaleBegin: ((detector: ScaleGestureDetector, e: MotionEvent) -> Boolean)? = null
    var onScale: ((detector: ScaleGestureDetector, e: MotionEvent, scaleFactor: Float) -> Boolean)? =
        null
    var onScaleEnd: ((detector: ScaleGestureDetector, e: MotionEvent) -> Unit)? = null

    var onEditingChanged: ((editingTransforms: Set<KProperty1<Node, Any>?>) -> Unit)? =
        null

    var collider: Collider? = null
        set(value) {
            if (field != value) {
                field?.let { collisionSystem?.removeCollider(it) }
                field = value
                value?.let { collisionSystem?.addCollider(it) }
            }
        }

    /**
     * The shape to used to detect collisions for this [Node].
     *
     * If the shape is not set and renderable is set, then [Collider.setShape] is used to detect
     * collisions for this [Node].
     *
     * [CollisionShape] represents a geometric shape, i.e. sphere, box, convex hull.
     * If null, this node's current collision shape will be removed.
     */
    var collisionShape: CollisionShape? = null
        get() = collider?.shape
        set(value) {
            field = value
            if (value != null) {
                val collider = collider ?: Collider(
                    this
                ).also { collider = it }
                collider.shape = value
            } else {
                collider = null
            }
            // Refresh the collider to ensure it is using the correct collision shape now
            // that the renderable has changed.
            onTransformChanged()
        }

    internal open val sceneEntities = listOf(entity)
    internal val onChildAdded = mutableListOf<(child: Node) -> Unit>()
    internal val onChildRemoved = mutableListOf<(child: Node) -> Unit>()

    final override fun hasTransformComponent() = super.hasTransformComponent()
    final override fun createTransformComponent() = super.createTransformComponent()

    init {
        if (!hasTransformComponent()) {
            createTransformComponent()
        }
        this.parent = parent
    }

    fun addChildNode(node: Node) = apply { childNodes += node }
    fun addChildNodes(nodes: Set<Node>) = apply { childNodes += nodes }
    fun removeChildNode(node: Node) = apply { childNodes -= node }
    fun removeChildNodes(nodes: Set<Node>) = apply { childNodes = childNodes - nodes }
    fun clearChildNodes() = apply { childNodes = setOf() }

    fun animatePositions(vararg positions: Position): ObjectAnimator =
        NodeAnimator.ofPosition(this, *positions)

    fun animateQuaternions(vararg quaternions: Quaternion): ObjectAnimator =
        NodeAnimator.ofQuaternion(this, *quaternions)

    fun animateRotations(vararg rotations: Rotation): ObjectAnimator =
        NodeAnimator.ofRotation(this, *rotations)

    fun animateScales(vararg scales: Scale): ObjectAnimator =
        NodeAnimator.ofScale(this, *scales)

    fun animateTransforms(vararg transforms: Transform): AnimatorSet =
        NodeAnimator.ofTransform(this, *transforms)

    /**
     * Tests to see if this node collision shape overlaps the collision shape of any other nodes in
     * the scene using [Node.collisionShape].
     *
     * @return A node that is overlapping the test node. If no node is overlapping the test node,
     * then this is null. If multiple nodes are overlapping the test node, then this could be any of
     * them.
     */
    fun overlapTest() = collisionSystem!!.intersects(collider!!)?.node

    /**
     * Tests to see if a node is overlapping any other nodes within the scene using
     * [Node.collisionShape].
     *
     * @return A list of all nodes that are overlapping this node. If no node is overlapping the
     * test node, then the list is empty.
     */
    fun overlapTestAll() = buildList {
        collisionSystem!!.intersectsAll(collider!!) {
            add(it.node)
        }
    }

    open fun onFrame(frameTimeNanos: Long) {
        childNodes.forEach { it.onFrame(frameTimeNanos) }
        onFrame?.invoke(frameTimeNanos)
    }

    open fun onAddedToScene(scene: Scene) {
        onAddedToScene?.invoke(scene)
    }

    open fun onRemovedFromScene(scene: Scene) {
        onRemovedFromScene?.invoke(scene)
    }

    /**
     * The transformation (position, rotation or scale) of the [Node] has changed.
     *
     * If node's position is changed, then that will trigger [onWorldTransformChanged] to be called
     * for all of it's descendants.
     */
    override fun onWorldTransformChanged() {
        collider?.markWorldShapeDirty()
        childNodes.forEach { it.onWorldTransformChanged() }
    }

    override fun onDown(e: MotionEvent) = onDown?.invoke(e) ?: false
    override fun onShowPress(e: MotionEvent) {
        onShowPress?.invoke(e)
    }

    override fun onSingleTapUp(e: MotionEvent) = onSingleTapUp?.invoke(e) ?: false
    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ) = onScroll?.invoke(e1, e2, Float2(distanceX, distanceY)) ?: false

    override fun onLongPress(e: MotionEvent) {
        onLongPress?.invoke(e)
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ) = onFling?.invoke(e1, e2, Float2(velocityX, velocityY)) ?: false

    override fun onSingleTapConfirmed(e: MotionEvent) = onSingleTapConfirmed?.invoke(e) ?: false
    override fun onDoubleTap(e: MotionEvent) = onDoubleTap?.invoke(e) ?: false
    override fun onDoubleTapEvent(e: MotionEvent) = onDoubleTapEvent?.invoke(e) ?: false
    override fun onContextClick(e: MotionEvent) = onContextClick?.invoke(e) ?: false

    override fun onMoveBegin(detector: MoveGestureDetector, e: MotionEvent): Boolean {
        return if (isPositionEditable && onMoveBegin?.invoke(detector, e) != false) {
            editingTransforms = editingTransforms + Node::position
            true
        } else {
            parent?.onMoveBegin(detector, e) ?: false
        }
    }

    override fun onMove(detector: MoveGestureDetector, e: MotionEvent): Boolean {
        return if (isPositionEditable) {
            collisionSystem?.hitTest(e)?.first { it.node == parent }?.let {
                onMove(detector, e, it.worldPosition)
            } ?: false
        } else {
            parent?.onMove(detector, e) ?: false
        }
    }

    open fun onMove(
        detector: MoveGestureDetector,
        e: MotionEvent,
        worldPosition: Position
    ): Boolean {
        return if (onMove?.invoke(detector, e, worldPosition) != false) {
            this.worldPosition = worldPosition
            true
        } else {
            false
        }
    }

    override fun onMoveEnd(detector: MoveGestureDetector, e: MotionEvent) {
        if (isPositionEditable) {
            editingTransforms = editingTransforms - Node::position
        } else {
            parent?.onMoveEnd(detector, e)
        }
    }

    override fun onRotateBegin(detector: RotateGestureDetector, e: MotionEvent): Boolean {
        return if (isRotationEditable && onRotateBegin?.invoke(detector, e) != false) {
            editingTransforms = editingTransforms + Node::quaternion
            true
        } else {
            parent?.onRotateBegin(detector, e) ?: false
        }
    }

    override fun onRotate(detector: RotateGestureDetector, e: MotionEvent): Boolean {
        return if (isRotationEditable) {
            val deltaRadians = detector.currentAngle - detector.lastAngle
            onRotate(
                detector, e,
                rotationDelta = Quaternion.fromAxisAngle(Float3(y = 1.0f), degrees(-deltaRadians))
            )
        } else {
            parent?.onRotate(detector, e) ?: false
        }
    }

    open fun onRotate(
        detector: RotateGestureDetector,
        e: MotionEvent,
        rotationDelta: Quaternion
    ): Boolean {
        return if (onRotate?.invoke(detector, e, rotationDelta) != false) {
            quaternion *= rotationDelta
            true
        } else {
            false
        }
    }

    override fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent) {
        if (isRotationEditable) {
            editingTransforms = editingTransforms - Node::quaternion
        } else {
            parent?.onRotateEnd(detector, e)
        }
    }

    override fun onScaleBegin(detector: ScaleGestureDetector, e: MotionEvent): Boolean {
        return if (isScaleEditable && onScaleBegin?.invoke(detector, e) != false) {
            true
        } else {
            parent?.onScaleBegin(detector, e) ?: false
        }
    }

    override fun onScale(detector: ScaleGestureDetector, e: MotionEvent): Boolean {
        return if (isScaleEditable) {
            editingTransforms = editingTransforms + Node::scale
            onScale(detector, e, detector.scaleFactor)
        } else {
            parent?.onScale(detector, e) ?: false
        }
    }

    open fun onScale(detector: ScaleGestureDetector, e: MotionEvent, scaleFactor: Float): Boolean {
        return if (onScale?.invoke(detector, e, scaleFactor) != false) {
            val newScale = scale * scaleFactor
            if (newScale.x in editableScaleRange &&
                newScale.y in editableScaleRange &&
                newScale.z in editableScaleRange
            ) {
                scale = newScale
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    override fun onScaleEnd(detector: ScaleGestureDetector, e: MotionEvent) {
        if (isScaleEditable) {
            editingTransforms = editingTransforms - Node::scale
        } else {
            parent?.onScaleEnd(detector, e)
        }
    }

    /**
     * Updates the children visibility
     *
     * @see RenderableNode.updateVisibility
     */
    protected open fun updateVisibility() {
        childNodes.forEach { childNode ->
            childNode.updateVisibility()
        }
    }

    // TODO : Remove this to full Kotlin Math
    override fun getTransformationMatrix(): Matrix {
        return worldTransform.toMatrix()
    }

    /**
     * Detach and destroy the node and all its children.
     */
    override fun destroy() {
        parent = null
        super.destroy()
    }
}

interface OnNodeGestureListener : GestureDetector.OnGestureListener,
    OnDoubleTapListener,
    OnContextClickListener,
    MoveGestureDetector.OnMoveListener,
    RotateGestureDetector.OnRotateListener,
    ScaleGestureDetector.OnScaleListener

open class SimpleOnNodeGestureListener : GestureDetector.SimpleOnGestureListener(),
    MoveGestureDetector.SimpleOnMoveListener,
    RotateGestureDetector.SimpleOnRotateListener,
    ScaleGestureDetector.SimpleOnScaleListener,
    OnNodeGestureListener
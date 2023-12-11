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
import com.google.android.filament.TransformManager
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.degrees
import dev.romainguy.kotlin.math.inverse
import dev.romainguy.kotlin.math.lookAt
import dev.romainguy.kotlin.math.lookTowards
import dev.romainguy.kotlin.math.transform
import io.github.sceneview.Entity
import io.github.sceneview.EntityInstance
import io.github.sceneview.FilamentEntity
import io.github.sceneview.SceneView
import io.github.sceneview.animation.NodeAnimator
import io.github.sceneview.collision.Collider
import io.github.sceneview.collision.CollisionShape
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.TransformProvider
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.RotateGestureDetector
import io.github.sceneview.gesture.ScaleGestureDetector
import io.github.sceneview.gesture.transform
import io.github.sceneview.managers.getParentOrNull
import io.github.sceneview.managers.getTransform
import io.github.sceneview.managers.getWorldTransform
import io.github.sceneview.managers.setTransform
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform
import io.github.sceneview.math.equals
import io.github.sceneview.math.quaternion
import io.github.sceneview.math.slerp
import io.github.sceneview.math.times
import io.github.sceneview.math.toMatrix
import io.github.sceneview.math.toQuaternion
import io.github.sceneview.utils.intervalSeconds
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
    val engine: Engine,
    @FilamentEntity val entity: Entity = EntityManager.get().create(),
) : GestureDetector.OnGestureListener,
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

    var isSmoothTransformEnabled = false

    /**
     * The smooth position, rotation and scale speed.
     *
     * This value is used by [smoothTransform]
     */
    var smoothTransformSpeed = 5.0f

    /**
     * Position to locate within the coordinate system the parent.
     *
     * Default is `Position(x = 0.0f, y = 0.0f, z = 0.0f)`, indicating that the component is placed
     * at the origin of the parent component's coordinate system.
     *
     * **Horizontal (X):**
     * - left: x < 0.0f
     * - center horizontal: x = 0.0f
     * - right: x > 0.0f
     *
     * **Vertical (Y):**
     * - top: y > 0.0f
     * - center vertical : y = 0.0f
     * - bottom: y < 0.0f
     *
     * **Depth (Z):**
     * - forward: z < 0.0f
     * - origin/camera position: z = 0.0f
     * - backward: z > 0.0f
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
     *
     * @see transform
     */
    open var position: Position
        get() = transform.position
        set(value) {
            transform = Transform(value, quaternion, scale)
        }

    /**
     * World-space position.
     *
     * The world position of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local position with its parent's world position.
     *
     * @see worldTransform
     */
    open var worldPosition: Position
        get() = worldTransform.position
        set(value) {
            position = parent?.getLocalPosition(value) ?: value
        }

    /**
     * Quaternion rotation.
     *
     * @see transform
     */
    open var quaternion: Quaternion
        get() = transform.quaternion
        set(value) {
            transform = Transform(position, value, scale)
        }

    /**
     * The world-space quaternion.
     *
     * The world quaternion of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local quaternion with its parent's world
     * quaternion.
     *
     * @see worldTransform
     */
    open var worldQuaternion: Quaternion
        get() = worldTransform.toQuaternion()
        set(value) {
            quaternion = parent?.getLocalQuaternion(value) ?: value
        }

    /**
     * Orientation in Euler Angles Degrees per axis from `0.0f` to `360.0f`.
     *
     * The three-component rotation vector specifies the direction of the rotation axis in degrees.
     * Rotation is applied relative to the component's origin property.
     *
     * Default is `Rotation(x = 0.0f, y = 0.0f, z = 0.0f)`, specifying no rotation.
     *
     * Note that modifying the individual components of the returned rotation doesn't have any
     * effect.
     *
     * @see transform
     */
    open var rotation: Rotation
        get() = quaternion.toEulerAngles()
        set(value) {
            quaternion = Quaternion.fromEuler(value)
        }

    /**
     * World-space rotation.
     *
     * The world rotation of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local rotation with its parent's world rotation.
     *
     * @see worldTransform
     */
    open var worldRotation: Rotation
        get() = worldTransform.rotation
        set(value) {
            worldQuaternion = Quaternion.fromEuler(value)
        }

    /**
     * Scale on each axis.
     *
     * Reduce (`scale < 1.0f`) / Increase (`scale > 1.0f`).
     *
     * @see transform
     */
    open var scale: Scale
        get() = transform.scale
        set(value) {
            transform = Transform(position, quaternion, value)
        }

    /**
     * World-space scale.
     *
     * The world scale of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local scale with its parent's world scale.
     *
     * @see worldTransform
     */
    open var worldScale: Scale
        get() = worldTransform.scale
        set(value) {
            scale = parent?.getLocalScale(value) ?: value
        }

    /**
     * Local transform of the transform component (i.e. relative to the parent).
     *
     * @see TransformManager.getTransform
     * @see TransformManager.setTransform
     */
    open var transform: Transform
        get() = transformManager.getTransform(transformInstance)
        set(value) {
            transformManager.setTransform(transformInstance, value)
            onTransformChanged()
        }

    /**
     * World transform of a transform component (i.e. relative to the root).
     *
     * @see TransformManager.getWorldTransform
     */
    var worldTransform: Transform
        get() = transformManager.getWorldTransform(transformInstance)
        set(value) {
            transform = parent?.getLocalTransform(value) ?: value
        }

    var smoothTransform: Transform? = null

    var parentEntity: Entity?
        get() = transformManager.getParentOrNull(transformInstance)
        set(value) {
            if (parentEntity != value) {
                parentInstance = value?.let { transformManager.getInstance(it) }
            }
        }

    var parentInstance: EntityInstance?
        get() = parentEntity?.let { transformManager.getInstance(it) }
        set(value) {
            if (parentInstance != value) {
                transformManager.setParent(transformInstance, value ?: 0)
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
    open var parent: Node? = null
        set(value) {
            if (field != value) {
                val oldParent = field
                field = value
                oldParent?.let { it.childNodes = it.childNodes - this }
                value?.let { it.childNodes = it.childNodes + this }
                parentEntity = value?.entity
            }
        }

    var childNodes = setOf<Node>()
        set(value) {
            if (field != value) {
                val removedNodes = field - value
                val addedNodes = value - field
                field = value
                removedNodes.forEach { child ->
                    if (child.parent == this@Node) {
                        child.parent = null
                    }
                    onChildRemoved.forEach { it(child) }
                }
                addedNodes.forEach { child ->
                    if (child.parent != this@Node) {
                        child.parent = this@Node
                    }
                    onChildAdded.forEach { it(child) }
                }
                onTransformChanged()
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

    /**
     * Transform from the world coordinate system to the coordinate system of this node.
     */
    val worldToLocal: Transform get() = inverse(worldTransform)

    var onFrame: ((frameTimeNanos: Long) -> Unit)? = null
    var onSmoothEnd: ((node: Node) -> Unit)? = null
    var onAddedToScene: ((scene: Scene) -> Unit)? = null
    var onRemovedFromScene: ((scene: Scene) -> Unit)? = null

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

    val transformManager get() = engine.transformManager
    val transformInstance get() = transformManager.getInstance(entity)

    internal open val sceneEntities = listOf(entity)
    internal val onChildAdded = mutableListOf<(child: Node) -> Unit>()
    internal val onChildRemoved = mutableListOf<(child: Node) -> Unit>()

    private var lastFrameTimeNanos: Long? = null

    init {
        if (!transformManager.hasComponent(entity)) {
            transformManager.create(entity)
        }
    }

    /**
     * Converts a position in the world-space to a local-space of this node.
     *
     * @param worldPosition the position in world-space to convert.
     * @return a new position that represents the world position in local-space.
     */
    fun getLocalPosition(worldPosition: Position) = worldToLocal * worldPosition

    /**
     * Converts a position in the local-space of this node to world-space.
     *
     * @param localPosition the position in local-space to convert.
     * @return a new position that represents the local position in world-space.
     */
    fun getWorldPosition(localPosition: Position) = worldTransform * localPosition

    /**
     * Converts a quaternion in the world-space to a local-space of this node.
     *
     * @param worldQuaternion the quaternion in world-space to convert.
     * @return a new quaternion that represents the world quaternion in local-space.
     */
    fun getLocalQuaternion(worldQuaternion: Quaternion) =
        worldToLocal.toQuaternion() * worldQuaternion

    /**
     * Converts a quaternion in the local-space of this node to world-space.
     *
     * @param quaternion the quaternion in local-space to convert.
     * @return a new quaternion that represents the local quaternion in world-space.
     */
    fun getWorldQuaternion(quaternion: Quaternion) = worldTransform.toQuaternion() * quaternion

    /**
     * Converts a rotation in the world-space to a local-space of this node.
     *
     * @param worldRotation the rotation in world-space to convert.
     * @return a new rotation that represents the world rotation in local-space.
     */
    fun getLocalRotation(worldRotation: Rotation) =
        getLocalQuaternion(Quaternion.fromEuler(worldRotation)).toEulerAngles()

    /**
     * Converts a rotation in the local-space of this node to world-space.
     *
     * @param rotation the rotation in local-space to convert.
     * @return a new rotation that represents the local rotation in world-space.
     */
    fun getWorldRotation(rotation: Rotation) =
        getWorldQuaternion(Quaternion.fromEuler(rotation)).toEulerAngles()

    /**
     * Converts a scale in the world-space to a local-space of this node.
     *
     * @param worldScale the transform in world-space to convert.
     * @return a new scale that represents the world scale in local-space.
     */
//    fun getLocalScale(worldScale: Scale) = scale(worldToLocal) * worldScale
//    fun getLocalScale(worldScale: Scale) = (worldToLocal * scale(worldScale)).scale
    fun getLocalScale(worldScale: Scale) = worldToLocal * worldScale

    /**
     * Converts a scale in the local-space of this node to world-space.
     *
     * @param scale the scale in local-space to convert.
     * @return a new scale that represents the local scale in world-space.
     */
//    fun getWorldScale(scale: Scale) = (worldTransform * scale(scale)).scale
//    fun getWorldScale(scale: Scale) = scale(worldTransform) * scale
    fun getWorldScale(scale: Scale) = worldTransform * scale

    /**
     * Converts a node transform in the world-space to a local-space of this node.
     *
     * @param node the node in world-space to convert.
     * @return a new transform that represents the world transform in local-space.
     */
    fun getLocalTransform(node: Node) = getLocalTransform(node.worldTransform)

    /**
     * Converts a transform in the world-space to a local-space of this node.
     *
     * @param worldTransform the transform in world-space to convert.
     * @return a new transform that represents the world transform in local-space.
     */
    fun getLocalTransform(worldTransform: Transform) = worldToLocal * worldTransform

    /**
     * Converts a node transform in the local-space of this node to world-space.
     *
     * @param node the node in local-space to convert.
     * @return a new transform that represents the local transform in world-space.
     */
    fun getWorldTransform(node: Node) = getWorldTransform(node.transform)

    /**
     * Converts a transform in the local-space of this node to world-space.
     *
     * @param localTransform the transform in local-space to convert.
     * @return a new transform that represents the local transform in world-space.
     */
    fun getWorldTransform(localTransform: Transform) = worldTransform * localTransform

    /**
     * The node scale.
     *
     * - reduce size: scale < 1.0f
     * - same size: scale = 1.0f
     * - increase size: scale > 1.0f
     */
    fun setScale(scale: Float) {
        this.scale.xyz = Scale(scale)
    }

    /**
     * Change the node transform.
     */
    open fun transform(
        transform: Transform,
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = apply {
        if (smooth) {
            this.smoothTransformSpeed = smoothSpeed
            this.smoothTransform = transform
        } else {
            this.smoothTransform = null
            this.transform = transform
        }
    }

    /**
     * Change the node transform.
     *
     * @see position
     * @see quaternion
     * @see scale
     */
    fun transform(
        position: Position = this.position,
        quaternion: Quaternion = this.quaternion,
        scale: Scale = this.scale,
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = transform(Transform(position, quaternion, scale), smooth, smoothSpeed)

    /**
     * Change the node transform.
     *
     * @see position
     * @see rotation
     * @see scale
     */
    fun transform(
        position: Position = this.position,
        rotation: Rotation,
        scale: Scale = this.scale,
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = transform(position, rotation.toQuaternion(), scale, smooth, smoothSpeed)

    /**
     * Change the node world transform.
     */
    open fun worldTransform(
        worldTransform: Transform,
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = transform(parent?.getLocalTransform(worldTransform) ?: worldTransform, smooth, smoothSpeed)

    /**
     * Change the node world transform.
     *
     * @see position
     * @see quaternion
     * @see scale
     */
    fun worldTransform(
        position: Position = this.worldPosition,
        quaternion: Quaternion = this.worldQuaternion,
        scale: Scale = this.worldScale,
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = worldTransform(Transform(position, quaternion, scale), smooth, smoothSpeed)

    /**
     * Change the node world transform.
     *
     * @see position
     * @see rotation
     * @see scale
     */
    fun worldTransform(
        position: Position = this.worldPosition,
        rotation: Rotation,
        scale: Scale = this.worldScale,
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = worldTransform(Transform(position, rotation.toQuaternion(), scale), smooth, smoothSpeed)

    /**
     * Rotates the node to face another node.
     *
     * @param targetNode The target node to look at
     * @param upDirection The up direction will determine the orientation of the node around the direction
     * @param smooth Whether the rotation should happen smoothly
     */
    fun lookAt(
        targetNode: Node,
        upDirection: Direction = Direction(y = 1.0f),
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = lookAt(
        targetWorldPosition = targetNode.worldPosition,
        upDirection = upDirection,
        smooth = smooth,
        smoothSpeed = smoothSpeed
    )

    /**
     * Rotates the node to face a point in world-space.
     *
     * @param targetWorldPosition The target position to look at in world space
     * @param upDirection The up direction will determine the orientation of the node around the direction
     * @param smooth Whether the rotation should happen smoothly
     */
    fun lookAt(
        targetWorldPosition: Position,
        upDirection: Direction = Direction(y = 1.0f),
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = worldTransform(
        quaternion = lookAt(
            eye = worldPosition,
            target = targetWorldPosition,
            up = upDirection
        ).toQuaternion(),
        smooth = smooth,
        smoothSpeed = smoothSpeed
    )

    /**
     * Rotates the node to face a direction in world-space.
     *
     * The look direction and up direction cannot be coincident (parallel) or the orientation will
     * be invalid.
     *
     * @param lookDirection The desired look direction in world-space.
     * @param upDirection The up direction will determine the orientation of the node around the
     * look direction.
     * @param smooth Whether the rotation should happen smoothly.
     */
    fun lookTowards(
        lookDirection: Direction,
        upDirection: Direction = Direction(y = 1.0f),
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = worldTransform(
        quaternion = lookTowards(
            eye = worldPosition,
//            forward = -lookDirection,
            forward = lookDirection,
            up = upDirection
        ).toQuaternion(),
        smooth = smooth,
        smoothSpeed = smoothSpeed
    )

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
        smoothTransform?.let { smoothTransform ->
            if (smoothTransform != transform) {
                val slerpTransform = slerp(
                    start = transform,
                    end = smoothTransform,
                    deltaSeconds = frameTimeNanos.intervalSeconds(lastFrameTimeNanos),
                    speed = smoothTransformSpeed
                )
                if (!slerpTransform.equals(this.transform, delta = 0.001f)) {
                    this.transform = slerpTransform
                } else {
                    this.transform = smoothTransform
                    this.smoothTransform = null
                    onSmoothEnd?.invoke(this)
                }
            } else {
                this.smoothTransform = null
            }
        }
        childNodes.forEach { it.onFrame(frameTimeNanos) }

        onFrame?.invoke(frameTimeNanos)

        lastFrameTimeNanos = frameTimeNanos
    }

    /**
     * The transformation (position, rotation or scale) of the [Node] has changed.
     *
     * If node's position is changed, then that will trigger [onWorldTransformChanged] to be called
     * for all of it's descendants.
     */
    open fun onTransformChanged() {
        onWorldTransformChanged()
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
    open fun onWorldTransformChanged() {
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
    open fun destroy() {
        parent = null
        transformManager.destroy(entity)
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
package io.github.sceneview.node

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.MotionEvent
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.TransformManager
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.clamp
import dev.romainguy.kotlin.math.degrees
import dev.romainguy.kotlin.math.inverse
import dev.romainguy.kotlin.math.lookAt
import dev.romainguy.kotlin.math.lookTowards
import dev.romainguy.kotlin.math.quaternion
import dev.romainguy.kotlin.math.scale
import dev.romainguy.kotlin.math.transform
import io.github.sceneview.Entity
import io.github.sceneview.FilamentEntity
import io.github.sceneview.SceneView
import io.github.sceneview.animation.NodeAnimator
import io.github.sceneview.collision.Collider
import io.github.sceneview.collision.CollisionShape
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.TransformProvider
import io.github.sceneview.collision.Vector3
import io.github.sceneview.gesture.GestureDetector
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.NodeMotionEvent
import io.github.sceneview.gesture.RotateGestureDetector
import io.github.sceneview.gesture.ScaleGestureDetector
import io.github.sceneview.gesture.transform
import io.github.sceneview.managers.getTransform
import io.github.sceneview.managers.getWorldTransform
import io.github.sceneview.managers.setParent
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
) : TransformProvider,
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

    /**
     * Define your own custom name.
     */
    open var name: String? = null

    /**
     * The node can be selected when a touch event happened.
     *
     * If a not touchable child [Node] is touched, we check the parent hierarchy to find the
     * closest touchable parent. In this case, the first selectable parent will be the one to have
     * its [isTouchable] value to `true`.
     */
    open var isTouchable = true

    open var isEditable = false
    open var isPositionEditable = false
    open var isRotationEditable = true
    open var isScaleEditable = true
    var minEditableScale = 0.1f
    var maxEditableScale = 10.0f

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
     * This value is used by [smooth]
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
            position = getLocalPosition(value)
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
            quaternion = getLocalQuaternion(value)
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
            scale = getLocalScale(value)
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
            if (transform != value) {
                transformManager.setTransform(transformInstance, value)
                onTransformChanged()
            }
        }

    /**
     * World transform of a transform component (i.e. relative to the root).
     *
     * @see TransformManager.getWorldTransform
     */
    open var worldTransform: Transform
        get() = transformManager.getWorldTransform(transformInstance)
        set(value) {
            transform = parent?.getLocalTransform(value) ?: value
        }

    var smoothTransform: Transform? = null

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
                oldParent?.let { it.childNodes = it.childNodes - this }
                value?.let { it.childNodes = it.childNodes + this }
                transformManager.setParent(transformInstance, value?.transformInstance)
            }
        }

    var childNodes = listOf<Node>()
        set(value) {
            if (field != value) {
                val removedNodes = field - value.toSet()
                val addedNodes = value - field.toSet()
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

    /**
     * Transform from the world coordinate system to the coordinate system of this node.
     */
    val worldToLocal: Transform get() = inverse(worldTransform)

    var onFrame: ((frameTimeNanos: Long) -> Unit)? = null
    var onSmoothEnd: ((node: Node) -> Unit)? = null
    var onTap: ((motionEvent: MotionEvent) -> Unit)? = null

    protected val transformManager get() = engine.transformManager
    protected val transformInstance get() = transformManager.getInstance(entity)

    internal open val sceneEntities = listOf(entity)
    internal val onChildAdded = mutableListOf<(child: Node) -> Unit>()
    internal val onChildRemoved = mutableListOf<(child: Node) -> Unit>()

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

    private var lastFrameTimeNanos: Long? = null

    init {
        if (!transformManager.hasComponent(entity)) {
            transformManager.create(entity)
        }
        this.parent = parent
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
    fun getLocalScale(worldScale: Scale) = (worldToLocal * scale(worldScale)).scale

    /**
     * Converts a scale in the local-space of this node to world-space.
     *
     * @param scale the scale in local-space to convert.
     * @return a new scale that represents the local scale in world-space.
     */
    fun getWorldScale(scale: Scale) = (worldTransform * scale(scale)).scale

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
    ) {
        if (smooth) {
            smooth(transform, smoothSpeed)
        } else {
            this.smoothTransform = null
            this.transform = transform
        }
    }

    /**
     * Change the node world transform.
     */
    open fun worldTransform(
        worldTransform: Transform,
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = transform(parent?.getLocalTransform(worldTransform) ?: worldTransform, smooth, smoothSpeed)

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
        rotation: Rotation = this.rotation,
        scale: Scale = this.scale,
        smooth: Boolean = isSmoothTransformEnabled,
        smoothSpeed: Float = smoothTransformSpeed
    ) = transform(position, rotation.toQuaternion(), scale, smooth, smoothSpeed)

    /**
     * Smooth move, rotate and scale at a specified speed.
     *
     * @see position
     * @see quaternion
     * @see scale
     * @see speed
     */
    fun smooth(
        position: Position = this.position,
        quaternion: Quaternion = this.quaternion,
        scale: Scale = this.scale,
        speed: Float = this.smoothTransformSpeed
    ) = smooth(Transform(position, quaternion, scale), speed)

    /**
     * Smooth move, rotate and scale at a specified speed.
     *
     * @see position
     * @see quaternion
     * @see scale
     * @see speed
     */
    fun smooth(
        position: Position = this.position,
        rotation: Rotation = this.rotation,
        scale: Scale = this.scale,
        speed: Float = this.smoothTransformSpeed
    ) = smooth(Transform(position, rotation, scale), speed)

    /**
     * Smooth move, rotate and scale at a specified speed.
     *
     * @see transform
     */
    fun smooth(transform: Transform, speed: Float = smoothTransformSpeed) {
        smoothTransformSpeed = speed
        smoothTransform = transform
    }

    fun addChildNode(child: Node) = apply {
        child.parent = this
    }

    fun removeChildNode(child: Node) = apply {
        if (child.parent == this) {
            child.parent = null
        }
    }

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
     * Rotates the node to face a point in world-space.
     *
     * @param targetPosition The target position to look at in world space
     * @param upDirection The up direction will determine the orientation of the node around the direction
     * @param smooth Whether the rotation should happen smoothly
     */
    fun lookAt(
        targetPosition: Position,
        upDirection: Direction = Direction(y = 1.0f),
        smooth: Boolean = false
    ) {
        val newQuaternion = lookAt(
            targetPosition,
            worldPosition,
            upDirection
        ).toQuaternion()
        if (smooth) {
            smooth(quaternion = newQuaternion)
        } else {
            transform(quaternion = newQuaternion)
        }
    }

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
        smooth: Boolean = false
    ) = lookAt(targetNode.worldPosition, upDirection, smooth)

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
        smooth: Boolean = false
    ) {
        val newQuaternion = lookTowards(
            worldPosition,
            -lookDirection,
            upDirection
        ).toQuaternion()
        if (smooth) {
            smooth(quaternion = newQuaternion)
        } else {
            transform(quaternion = newQuaternion)
        }
    }

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

    override fun onSingleTapConfirmed(e: NodeMotionEvent) {
        onTap(e.motionEvent)
    }

    /**
     * Invoked when the node is tapped.
     *
     * Calls the `onTap` listener if it is available and passes the tap to the parent node.
     *
     * @param renderable The the Filament renderable component that was tapped.
     * @param motionEvent The motion event that caused the tap.
     */
    open fun onTap(motionEvent: MotionEvent) {
        onTap?.invoke(motionEvent)
        parent?.onTap(motionEvent)
    }

    override fun onMoveBegin(detector: MoveGestureDetector, e: NodeMotionEvent) {
        if (!isEditable || !isPositionEditable) {
            parent?.onMoveBegin(detector, e)
        }
    }

    override fun onMove(detector: MoveGestureDetector, e: NodeMotionEvent) {
        if (!isEditable || !isPositionEditable) {
            parent?.onMove(detector, e)
        }
    }

    override fun onMoveEnd(detector: MoveGestureDetector, e: NodeMotionEvent) {
        if (!isEditable || !isPositionEditable) {
            parent?.onMoveEnd(detector, e)
        }
    }

    override fun onRotateBegin(detector: RotateGestureDetector, e: NodeMotionEvent) {
        if (!isEditable || !isRotationEditable) {
            parent?.onRotateBegin(detector, e)
        }
    }

    override fun onRotate(detector: RotateGestureDetector, e: NodeMotionEvent) {
        if (isEditable && isRotationEditable) {
            val deltaRadians = detector.currentAngle - detector.lastAngle
            onRotate(e, Quaternion.fromAxisAngle(Float3(y = 1.0f), degrees(-deltaRadians)))
        } else {
            parent?.onRotate(detector, e)
        }
    }

    open fun onRotate(e: NodeMotionEvent, rotationDelta: Quaternion) {
        quaternion *= rotationDelta
    }

    override fun onRotateEnd(detector: RotateGestureDetector, e: NodeMotionEvent) {
        if (!isEditable || !isRotationEditable) {
            parent?.onRotateEnd(detector, e)
        }
    }

    override fun onScaleBegin(detector: ScaleGestureDetector, e: NodeMotionEvent) {
        if (!isEditable || !isScaleEditable) {
            parent?.onScaleBegin(detector, e)
        }
    }

    override fun onScale(detector: ScaleGestureDetector, e: NodeMotionEvent) {
        if (isEditable && isScaleEditable) {
            onScale(e, detector.scaleFactor)
        } else {
            parent?.onScale(detector, e)
        }
    }

    open fun onScale(e: NodeMotionEvent, scaleFactor: Float) {
        scale = clamp(scale * scaleFactor, minEditableScale, maxEditableScale)
    }

    override fun onScaleEnd(detector: ScaleGestureDetector, e: NodeMotionEvent) {
        if (!isEditable || !isScaleEditable) {
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
        transformManager.destroy(entity)
    }

    /**
     * Used to keep track of data for detecting if a tap gesture has occurred on this node.
     */
    private data class TapTrackingData(
        // The node that was being touched when ACTION_DOWN occurred.
        val downNode: Node,
        // The screen-space position that was being touched when ACTION_DOWN occurred.
        val downPosition: Vector3
    )
}
package io.github.sceneview.node

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.MotionEvent
import com.google.android.filament.*
import com.google.ar.sceneform.collision.Collider
import com.google.ar.sceneform.collision.CollisionShape
import com.google.ar.sceneform.common.TransformProvider
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import dev.romainguy.kotlin.math.*
import io.github.sceneview.SceneView
import io.github.sceneview.animation.NodeAnimator
import io.github.sceneview.gesture.*
import io.github.sceneview.math.*
import io.github.sceneview.renderable.Renderable
import io.github.sceneview.transform.*
import io.github.sceneview.utils.FrameTime
import kotlin.reflect.KProperty

// This is the default from the ViewConfiguration class.
private const val defaultTouchSlop = 8

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
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
 *
 * @param position See [Node.position]
 * @param rotation See [Node.rotation]
 * @param scale See [Node.scale]
 */
open class Node(val engine: Engine) : NodeParent, TransformProvider,
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

    /**
     * ### The scene that this node is part of, null if it isn't part of any scene
     *
     * A node is part of a scene if its highest level ancestor is a [SceneView]
     */
    protected open val sceneView: SceneView? get() = parent as? SceneView ?: parentNode?.sceneView
    val transformManager: TransformManager get() = engine.transformManager

    // TODO : Remove when every dependent is kotlined
    fun getSceneViewInternal() = sceneView

    /**
     * ### Define your own custom name
     */
    var name: String? = null

    val isAttached get() = sceneView != null

    @Entity
    open val transformEntity: Int = EntityManager.get().create().apply {
        transformManager.create(this)
    }

    val transformInstance: Int
        @EntityInstance
        get() = transformManager.getInstance(transformEntity)

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
    var position: Position
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
    var worldPosition: Position
        get() = worldTransform.position
        set(value) {
            position = worldToParent * value
        }

    /**
     * Quaternion rotation.
     *
     * @see transform
     */
    var quaternion: Quaternion
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
    var worldQuaternion: Quaternion
        get() = worldTransform.toQuaternion()
        set(value) {
            quaternion = worldToParent.toQuaternion() * value
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
    var rotation: Rotation
        get() = quaternion.toEulerAngles()
        set(value) {
            quaternion = Quaternion.fromEuler(value)
        }

    /**
     * World-space rotation.
     *
     * The world rotation of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local rotation with its parent's world
     * rotation.
     *
     * @see worldTransform
     */
    var worldRotation: Rotation
        get() = worldTransform.rotation
        set(value) {
            worldQuaternion = Quaternion.fromEuler(value)
        }

    /**
     * Scale on each axis.
     *
     * Reduce (`scale < 1.0f`) / Increase (`scale > 1.0f`)
     *
     * @see transform
     */
    var scale: Scale
        get() = transform.scale
        set(value) {
            transform = Transform(position, quaternion, value)
        }

    /**
     * World-space scale.
     *
     * The world scale of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local scale with its parent's world
     * scale.
     *
     * @see worldTransform
     */
    var worldScale: Scale
        get() = worldTransform.scale
        set(value) {
            scale = (worldToParent * scale(value)).scale
        }

    /**
     * Local transform of the transform component (i.e. relative to the parent).
     *
     * @see TransformManager.getTransform
     * @see TransformManager.setTransform
     */
    var transform: Transform
        get() = transformManager.getTransform(transformInstance)
        set(value) {
            transformManager.setTransform(transformInstance, value)
        }

    /**
     * World transform of a transform component (i.e. relative to the root).
     *
     * @see TransformManager.getWorldTransform
     */
    open var worldTransform: Transform
        get() = transformManager.getWorldTransform(transformInstance)
        set(value) {
            transform = worldToParent * value
        }

    /**
     * Transform from the world coordinate system to the coordinate system of the parent.
     *
     * @see TransformManager.getWorldTransform
     */
    val worldToParent: Transform
        get() = inverse(parentNode?.worldTransform ?: Transform())

    /**
     * ### The [Node] parent if the parent extends [Node]
     *
     * Returns null if the parent is not a [Node].
     * = Returns null if the parent is a [SceneView]
     */
    val parentNode get() = parent as? Node

    /**
     * ### Changes the parent node of this node
     *
     * If set to null, this node will be detached ([removeChild]) from its parent.
     *
     * The parent may be another [Node] or a [SceneView].
     * If it is a scene, then this [Node] is considered top level.
     *
     * The local position, rotation, and scale of this node will remain the same.
     * Therefore, the world position, rotation, and scale of this node may be different after the
     * parent changes.
     *
     * In addition to setting this field, setParent will also do the following things:
     *
     * - Remove this node from its previous parent's children.
     * - Add this node to its new parent's children.
     * - Recursively update the node's transformation to reflect the change in parent
     * -Recursively update the scene field to match the new parent's scene field.
     */
    var parent: NodeParent? = null
        set(value) {
            if (field != value) {
                // Find the old parent SceneView
                sceneView?.let { detachFromScene(it) }
                // Remove from old parent if not already removed
                field?.removeChild(this)
                field = value
                parentEntity = (value as? Node)?.transformEntity
                // Add to new parent if not already added
                value?.addChild(this)
                // Find the new parent SceneView
                sceneView?.let { attachToScene(it) }
            }
        }

    var parentEntity: Int?
        @Entity
        get() = transformManager.getParentOrNull(transformInstance)
        @Entity
        set(value) {
            transformManager.setParent(
                transformInstance,
                value?.let { transformManager.getInstance(it) }
            )
        }

    val parentInstance: Int?
        @EntityInstance
        get() = parentEntity?.let { transformManager.getInstance(it) }

    val allParents: List<NodeParent>
        get() = listOfNotNull(parent) + (parentNode?.allParents ?: listOf())

    open var _sceneEntities = intArrayOf()

    open var sceneEntities: IntArray
        get() = _sceneEntities
        set(value) {
            sceneView?.removeEntities(_sceneEntities)
            _sceneEntities = value
            if (isVisibleInHierarchy) {
                sceneView?.addEntities(sceneEntities)
            }
        }

    /**
     * ## The smooth position, rotation and scale speed
     *
     * This value is used by [smooth]
     */
    var smoothSpeed = 5.0f

    var smoothTransform: Transform? = null

    var onSmoothEnd: ((node: Node) -> Unit)? = null

    /**
     * ### The node can be selected when a touch event happened
     *
     * If a not selectable child [Node] is touched, we check the parent hierarchy to find the
     * closest selectable parent. In this case, the first selectable parent will be the one to have
     * its [isSelected] value to `true`.
     */
    open var isSelectable = false

    /**
     * ### The visible state of this node.
     *
     * Note that a Node may be enabled but still inactive if it isn't part of the scene or if its
     * parent is inactive.
     */
    open var isVisible = true
        set(value) {
            if (field != value) {
                field = value
                updateVisibility()
            }
        }

    open val isVisibleInHierarchy: Boolean
        get() = isVisible && (parentNode?.isVisibleInHierarchy != false)

    open var isPositionEditable = false
    open var isRotationEditable = false
    open var isScaleEditable = false
    open var isEditable: Boolean
        get() = isPositionEditable || isRotationEditable || isScaleEditable
        set(value) {
            isPositionEditable = value
            isRotationEditable = value
            isScaleEditable = value
        }

    var minEditableScale = 0.1f
    var maxEditableScale = 10.0f

    var currentEditingTransform: KProperty<*>? = null

    // Collision fields.
    var collider: Collider? = null
        private set

    /** Listener for [onFrame] call */
    var onFrame: ((frameTime: FrameTime, node: Node) -> Unit)? = null

    /** Listener for [onAttachedToScene] call */
    var onAttachedToScene: ((sceneView: SceneView) -> Unit)? = null

    /** Listener for [onDetachedFromScene] call */
    var onDetachedFromScene: ((sceneView: SceneView) -> Unit)? = null

    /**
     * ### The transformation (position, rotation or scale) of the [Node] has changed
     *
     * If node A's position is changed, then that will trigger [onTransformChanged] to be
     * called for all of it's descendants.
     */
    val onTransformChanged = mutableListOf<(node: Node) -> Unit>()

    /**
     * ### Invoked when the node is tapped
     *
     * Only nodes with renderables or their parent nodes can be tapped since Filament picking is
     * used to find a touched node. The ID of the Filament renderable can be used to determine what
     * part of a model is tapped.
     *
     * - `renderable` - The ID of the Filament renderable that was tapped.
     * - `motionEvent` - The motion event that caused the tap.
     */
    var onTap: ((motionEvent: MotionEvent, renderable: Renderable?) -> Unit)? = null

    override var children = listOf<Node>()

    private var allowDispatchTransformChanged = true

    private val _onAttachedToScene = mutableListOf<(sceneView: SceneView) -> Unit>()

    // The first delta is always way off as it contains all delta until threshold to
    // recognize rotate gesture is met
    private var skipFirstRotateEdit = false

    // Stores data used for detecting when a tap has occurred on this node.
    private var touchTrackingData: TapTrackingData? = null

    open fun attachToScene(sceneView: SceneView) {
        sceneEntities = _sceneEntities

        sceneView.collisionSystem.let { collider?.setAttachedCollisionSystem(it) }
        if (selectionVisualizer == null && sceneView.selectionVisualizer != null) {
            selectionVisualizer = sceneView.selectionVisualizer?.invoke()
        }
        children.forEach { it.attachToScene(sceneView) }
        onAttachedToScene(sceneView)
    }

    open fun detachFromScene(sceneView: SceneView) {
        sceneView.scene.removeEntities(sceneEntities)
        collider?.setAttachedCollisionSystem(null)
        children.forEach { it.detachFromScene(sceneView) }
        onDetachedFromScene(sceneView)
    }

    open fun onAttachedToScene(sceneView: SceneView) {
        _onAttachedToScene.toList().forEach { it(sceneView) }
        onAttachedToScene?.invoke(sceneView)
    }

    open fun onDetachedFromScene(sceneView: SceneView) {
        onDetachedFromScene?.invoke(sceneView)
    }

    open fun onFrame(frameTime: FrameTime) {
        smoothTransform?.let { smoothTransform ->
            if (smoothTransform != transform) {
                val slerpTransform = slerp(
                    start = transform,
                    end = smoothTransform,
                    deltaSeconds = frameTime.intervalSeconds,
                    speed = smoothSpeed
                )
                if (!slerpTransform.equals(this.transform, delta = 0.001f)) {
                    this.transform = slerpTransform
                } else {
                    this.transform = smoothTransform
                    this.smoothTransform = null
                    onSmoothEnd()
                }
            } else {
                this.smoothTransform = null
            }
        }
        children.forEach { it.onFrame(frameTime) }

        onFrame?.invoke(frameTime, this)
    }

    override fun onSingleTapConfirmed(e: NodeMotionEvent) {
        onTap(e.motionEvent, e.renderable)
    }

    /**
     * ### Invoked when the node is tapped
     *
     * Calls the `onTap` listener if it is available and passes the tap to the parent node.
     *
     * @param renderable The ID of the Filament renderable that was tapped.
     * @param motionEvent The motion event that caused the tap.
     */
    open fun onTap(motionEvent: MotionEvent, renderable: Renderable?) {
        onTap?.invoke(motionEvent, renderable)
        parentNode?.onTap(motionEvent, renderable)
    }

    override fun onMoveBegin(detector: MoveGestureDetector, e: NodeMotionEvent) {
        // Not implemented for 3D only
    }

    override fun onMove(detector: MoveGestureDetector, e: NodeMotionEvent) {
        // Not implemented for 3D only
    }

    override fun onMoveEnd(detector: MoveGestureDetector, e: NodeMotionEvent) {
        // Not implemented for 3D only
    }

    override fun onRotateBegin(detector: RotateGestureDetector, e: NodeMotionEvent) {
        if (isRotationEditable && currentEditingTransform == null) {
            currentEditingTransform = ::quaternion
            skipFirstRotateEdit = true
        }
    }

    override fun onRotate(detector: RotateGestureDetector, e: NodeMotionEvent) {
        if (isRotationEditable && currentEditingTransform == ::quaternion) {
            if (skipFirstRotateEdit) {
                skipFirstRotateEdit = false
                return
            }
            val deltaRadians = detector.currentAngle - detector.lastAngle
            onRotate(e, Quaternion.fromAxisAngle(Float3(y = 1.0f), degrees(-deltaRadians)))
        }
    }

    open fun onRotate(e: NodeMotionEvent, rotationDelta: Quaternion) {
        quaternion *= rotationDelta
    }

    override fun onRotateEnd(detector: RotateGestureDetector, e: NodeMotionEvent) {
        if (isRotationEditable && currentEditingTransform == ::quaternion) {
            currentEditingTransform = null
        }
    }

    override fun onScaleBegin(detector: ScaleGestureDetector, e: NodeMotionEvent) {
        if (isScaleEditable && currentEditingTransform == null) {
            currentEditingTransform = ::scale
        }
    }

    override fun onScale(detector: ScaleGestureDetector, e: NodeMotionEvent) {
        if (isScaleEditable && (currentEditingTransform == ::scale)) {
            onScale(e, detector.scaleFactor)
        }
    }

    open fun onScale(e: NodeMotionEvent, scaleFactor: Float) {
        scale = clamp(scale * scaleFactor, minEditableScale, maxEditableScale)
    }

    override fun onScaleEnd(detector: ScaleGestureDetector, e: NodeMotionEvent) {
        if (isScaleEditable && currentEditingTransform == ::scale) {
            currentEditingTransform = null
        }
    }

    /**
     * ### The transformation (position, rotation or scale) of the [Node] has changed
     *
     * If node A's position is changed, then that will trigger [onTransformChanged] to be
     * called for all of it's descendants.
     */
    open fun onTransformChanged() {
        // TODO : Kotlin Collider for more comprehension
        collider?.markWorldShapeDirty()
        children.forEach { it.onTransformChanged() }
        onTransformChanged.forEach { it(this) }
    }

    override fun onChildAdded(child: Node) {
        super.onChildAdded(child)

        onTransformChanged()
    }

    override fun onChildRemoved(child: Node) {
        super.removeChild(child)

        onTransformChanged()
    }

    /**
     * ### The node scale
     *
     * - reduce size: scale < 1.0f
     * - same size: scale = 1.0f
     * - increase size: scale > 1.0f
     */
    fun setScale(scale: Float) {
        this.scale.xyz = Scale(scale)
    }

    /**
     * Change the node transform
     */
    fun transform(
        transform: Transform,
        smooth: Boolean = false,
        smoothSpeed: Float = this.smoothSpeed
    ) {
        if (smooth) {
            smooth(transform, smoothSpeed)
        } else {
            this.smoothTransform = null
            this.transform = transform
        }
    }

    /**
     * ## Change the node transform
     *
     * @see position
     * @see quaternion
     * @see scale
     */
    fun transform(
        position: Position = this.position,
        quaternion: Quaternion = this.quaternion,
        scale: Scale = this.scale,
        smooth: Boolean = false,
        smoothSpeed: Float = this.smoothSpeed
    ) = transform(Transform(position, quaternion, scale), smooth, smoothSpeed)

    /**
     * ## Change the node transform
     *
     * @see position
     * @see rotation
     * @see scale
     */
    fun transform(
        position: Position = this.position,
        rotation: Rotation = this.rotation,
        scale: Scale = this.scale,
        smooth: Boolean = false,
        smoothSpeed: Float = this.smoothSpeed
    ) = transform(position, rotation.toQuaternion(), scale, smooth, smoothSpeed)

    /**
     * ## Smooth move, rotate and scale at a specified speed
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
        speed: Float = this.smoothSpeed
    ) = smooth(Transform(position, quaternion, scale), speed)

    /**
     * ## Smooth move, rotate and scale at a specified speed
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
        speed: Float = this.smoothSpeed
    ) = smooth(Transform(position, rotation, scale), speed)

    /**
     * ## Smooth move, rotate and scale at a specified speed
     *
     * @see transform
     */
    fun smooth(transform: Transform, speed: Float = smoothSpeed) {
        smoothSpeed = speed
        smoothTransform = transform
    }

    open fun onSmoothEnd() {
        onSmoothEnd?.invoke(this)
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
     * ### Rotates the node to face a point in world-space
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
     * ### Rotates the node to face another node
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
     * ### Rotates the node to face a direction in world-space
     *
     * The look direction and up direction cannot be coincident (parallel) or the orientation will
     * be invalid.
     *
     * @param lookDirection The desired look direction in world-space
     * @param upDirection The up direction will determine the orientation of the node around the look direction
     * @param smooth Whether the rotation should happen smoothly
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
     * ### Checks whether the given node parent is an ancestor of this node recursively
     *
     * Return true if the node is an ancestor of this node
     */
    fun isDescendantOf(ancestor: NodeParent): Boolean =
        parent == ancestor || parentNode?.isDescendantOf(ancestor) == true

    open fun updateVisibility() {
        if (isVisibleInHierarchy) {
            sceneView?.scene?.addEntities(sceneEntities)
        } else {
            sceneView?.scene?.removeEntities(sceneEntities)
        }
        children.forEach { it.updateVisibility() }
    }

    open var selectionVisualizer: Node? = null
        set(value) {
            field?.let { it.parent = null }
            field = value
            value?.let { it.parent = if (isSelected) this else null }
        }

    open var isSelected = false
        set(value) {
            if (field != value) {
                field = value
                selectionVisualizer?.parent = if (value) this else null
            }
        }

    /**
     * ### Finds the first enclosing node with the given type
     *
     * The node can be:
     * - This node.
     * - One of the parent nodes.
     * - `null` if no node is found.
     */
    inline fun <reified T : Node> firstEnclosingNode(): T? {
        var currentNode: Node? = this

        while (currentNode != null && currentNode !is T) {
            currentNode = currentNode.parentNode
        }

        return currentNode as? T
    }

    // TODO : Remove this to full Kotlin Math
    override fun getTransformationMatrix(): Matrix {
        return Matrix(worldTransform.toColumnsFloatArray())
    }

    // Reuse this to limit frame instantiations
    private val _transformationMatrixInverted = Matrix()
    open val transformationMatrixInverted: Matrix
        get() = _transformationMatrixInverted.apply {
            Matrix.invert(transformationMatrix, this)
        }

    /**
     * ### The shape to used to detect collisions for this [Node]
     *
     * If the shape is not set and [renderable] is set, then [Renderable.getCollisionShape] is
     * used to detect collisions for this [Node].
     *
     * [CollisionShape] represents a geometric shape, i.e. sphere, box, convex hull.
     * If null, this node's current collision shape will be removed.
     */
    var collisionShape: CollisionShape? = null
        get() = field ?: collider?.shape
        set(value) {
            field = value
            if (value != null) {
                // TODO : Cleanup
                // Create the collider if it doesn't already exist.
                if (collider == null) {
                    collider = Collider(this, value).apply {
                        // Attach the collider to the collision system if the node is already active.
                        if (sceneView?.collisionSystem != null) {
                            setAttachedCollisionSystem(sceneView!!.collisionSystem)
                        }
                    }
                } else if (collider!!.shape != value) {
                    // Set the collider's shape to the new shape if needed.
                    collider!!.shape = value
                }
            } else {
                // Dispose of the old collider
                collider?.setAttachedCollisionSystem(null)
                collider = null
            }
        }

    open fun clone() = copy(Node(engine))

    @JvmOverloads
    open fun copy(toNode: Node = Node(engine)): Node = toNode.apply {
        position = this@Node.position
        quaternion = this@Node.quaternion
        scale = this@Node.scale
    }

    /** ### Detach and destroy the node */
    open fun destroy() {
        this.parent = null
    }

    /**
     * ### Performs the given action when the node is attached to the scene.
     *
     * If the node is already attached the action will be performed immediately.
     * Else this action will be invoked the first time the scene is attached.
     *
     * - `scene` - the attached scene
     */
    fun doOnAttachedToScene(action: (scene: SceneView) -> Unit) {
        sceneView?.let(action) ?: run {
            _onAttachedToScene += object : (SceneView) -> Unit {
                override fun invoke(sceneView: SceneView) {
                    _onAttachedToScene -= this
                    action(sceneView)
                }
            }
        }
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
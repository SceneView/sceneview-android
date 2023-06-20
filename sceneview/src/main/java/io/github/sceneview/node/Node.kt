package io.github.sceneview.node

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.filament.Entity
import com.google.android.filament.EntityInstance
import com.google.ar.sceneform.collision.Collider
import com.google.ar.sceneform.collision.CollisionShape
import com.google.ar.sceneform.common.TransformProvider
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import dev.romainguy.kotlin.math.*
import io.github.sceneview.*
import io.github.sceneview.Filament.transformManager
import io.github.sceneview.animation.NodeAnimator
import io.github.sceneview.gesture.*
import io.github.sceneview.math.*
import io.github.sceneview.renderable.Renderable
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
open class Node(
    position: Position = DEFAULT_POSITION,
    quaternion: Quaternion = DEFAULT_QUATERNION,
    scale: Scale = DEFAULT_SCALE
) : NodeParent, TransformProvider, SceneLifecycleObserver,
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

    companion object {
        val DEFAULT_POSITION get() = Position(x = 0.0f, y = 0.0f, z = 0.0f)
        val DEFAULT_QUATERNION get() = Quaternion()
        val DEFAULT_ROTATION = DEFAULT_QUATERNION.toEulerAngles()
        val DEFAULT_SCALE get() = Scale(1.0f)
    }

    /**
     * ### The scene that this node is part of, null if it isn't part of any scene
     *
     * A node is part of a scene if its highest level ancestor is a [SceneView]
     */
    protected open val sceneView: SceneView? get() = parent as? SceneView ?: parentNode?.sceneView

    // TODO : Remove when every dependent is kotlined
    fun getSceneViewInternal() = sceneView
    protected open val lifecycle: SceneLifecycle? get() = sceneView?.lifecycle
    protected val lifecycleScope get() = sceneView?.lifecycleScope

    @Entity
    open val transformEntity: Int? = null
    val transformInstance: Int?
        @EntityInstance
        get() = transformEntity?.let { transformManager.getInstance(it) }

    @Entity
    open var sceneEntities: IntArray = intArrayOf()
        set(value) {
            field.takeIf { it.isNotEmpty() }?.let { sceneView?.scene?.removeEntities(it) }
            field = value
            if (isVisibleInHierarchy) {
                sceneView?.scene?.addEntities(sceneEntities)
            }
        }

    /**
     * ### Define your own custom name
     */
    var name: String? = null

    val isAttached get() = sceneView != null

    /**
     * ### The node position to locate it within the coordinate system of its parent
     *
     * Default is `Position(x = 0.0f, y = 0.0f, z = 0.0f)`, indicating that the node is placed at
     * the origin of the parent node's coordinate system.
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
     */
    var position: Position = position

    /**
     * TODO: Doc
     */
    var quaternion: Quaternion = quaternion

    /**
     * ### The node orientation in Euler Angles Degrees per axis from `0.0f` to `360.0f`
     *
     * The three-component rotation vector specifies the direction of the rotation axis in degrees.
     * Rotation is applied relative to the node's origin property.
     *
     * Default is `Rotation(x = 0.0f, y = 0.0f, z = 0.0f)`, specifying no rotation.
     *
     * Note that modifying the individual components of the returned rotation doesn't have any effect.
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
    var rotation: Rotation
        get() = quaternion.toEulerAngles()
        set(value) {
            quaternion = Quaternion.fromEuler(value)
        }

    /**
     * ### The node scale on each axis.
     * Reduce (`scale < 1.0f`) / Increase (`scale > 1.0f`)
     */
    var scale: Scale = scale

    open var transform: Transform = Transform(position, quaternion, scale)
        get() = field.takeIf {
            it.position == position && it.quaternion == quaternion && it.scale == scale
        } ?: Transform(position, quaternion, scale)
        set(value) {
            if (field != value) {
                field = value
                position = value.position
                quaternion = value.quaternion
                scale = value.scale
            }
        }

    /**
     * ### The node world-space position
     *
     * The world position of this node (i.e. relative to the [SceneView]).
     * This is the composition of this component's local position with its parent's world
     * position.
     *
     * @see worldTransform
     */
    open var worldPosition: Position
        get() = worldTransform.position
        set(value) {
            position = (worldToParent * Float4(value, 1f)).xyz
        }

    /**
     * ### The node world-space quaternion
     *
     * The world quaternion of this node (i.e. relative to the [SceneView]).
     * This is the composition of this component's local quaternion with its parent's world
     * quaternion.
     *
     * @see worldTransform
     */
    open var worldQuaternion: Quaternion
        get() = worldTransform.toQuaternion()
        set(value) {
            quaternion = worldToParent.toQuaternion() * value
        }

    /**
     * ### The node world-space rotation
     *
     * The world rotation of this node (i.e. relative to the [SceneView]).
     * This is the composition of this component's local rotation with its parent's world
     * rotation.
     *
     * @see worldTransform
     */
    open var worldRotation: Rotation
        get() = worldTransform.rotation
        set(value) {
            quaternion = worldToParent.toQuaternion() * Quaternion.fromEuler(value)
        }

    /**
     * ### The node world-space scale
     *
     * The world scale of this node (i.e. relative to the [SceneView]).
     * This is the composition of this component's local scale with its parent's world
     * scale.
     *
     * @see worldTransform
     */
    open var worldScale: Scale
        get() = worldTransform.scale
        set(value) {
            scale = (worldToParent * scale(value)).scale
        }

    open val worldTransform: Mat4
        get() = parentNode?.let { it.worldTransform * transform } ?: transform

    /**
     * ### The transform from the world coordinate system to the coordinate system of the parent node
     */
    private val worldToParent: Transform
        get() = parentNode?.let { inverse(it.worldTransform) } ?: Transform()

    /**
     * ## The smooth position, rotation and scale speed
     *
     * This value is used by [smooth]
     */
    var smoothSpeed = 5.0f

    var smoothTransform: Transform? = null

    private var lastFrameTransform: Transform? = null
    private var lastFrameWorldTransform: Transform? = null

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
        get() = isVisible && (parentNode?.isVisibleInHierarchy ?: true)

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
                // Add to new parent if not already added
                value?.addChild(this)
                // Find the new parent SceneView
                sceneView?.let { attachToScene(it) }
            }
        }

    val allParents: List<NodeParent>
        get() = listOfNotNull(parent) + (parentNode?.allParents ?: listOf())

    // Collision fields.
    var collider: Collider? = null
        private set

    /** ### Listener for [onFrame] call */
    val onFrame = mutableListOf<((frameTime: FrameTime, node: Node) -> Unit)>()

    /** ### Listener for [onAttachToScene] call */
    val onAttachedToScene = mutableListOf<(scene: SceneView) -> Unit>()

    /** ### Listener for [onDetachedFromScene] call */
    val onDetachedFromScene = mutableListOf<(scene: SceneView) -> Unit>()

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

    // The first delta is always way off as it contains all delta until threshold to
    // recognize rotate gesture is met
    private var skipFirstRotateEdit = false

    // Stores data used for detecting when a tap has occurred on this node.
    private var touchTrackingData: TapTrackingData? = null

    /**
     * ### Construct a [Node] with it Position, Rotation and Scale
     *
     * @param position See [Node.position]
     * @param rotation See [Node.rotation]
     * @param scale See [Node.scale]
     */
    @JvmOverloads
    constructor(
        position: Position = DEFAULT_POSITION,
        rotation: Rotation,
        scale: Scale = DEFAULT_SCALE
    ) : this(position = position, scale = scale) {
        this.rotation = rotation
    }

    open fun attachToScene(sceneView: SceneView) {
        sceneView.lifecycle.addObserver(this)
        if (isVisibleInHierarchy) {
            sceneView.scene.addEntities(sceneEntities)
        }
        sceneView.collisionSystem.let { collider?.setAttachedCollisionSystem(it) }
        if (selectionVisualizer == null) {
            selectionVisualizer = sceneView.selectionVisualizer?.invoke()
        }
        children.forEach { it.attachToScene(sceneView) }
        onAttachedToScene(sceneView)
    }

    open fun detachFromScene(sceneView: SceneView) {
        sceneView.lifecycle.removeObserver(this)
        sceneView.scene.removeEntities(sceneEntities)
        collider?.setAttachedCollisionSystem(null)
        children.forEach { it.detachFromScene(sceneView) }
        onDetachedFromScene(sceneView)
    }

    open fun onAttachedToScene(sceneView: SceneView) {
        onAttachedToScene.toList().forEach { it(sceneView) }
    }

    open fun onDetachedFromScene(sceneView: SceneView) {
        onDetachedFromScene.toList().forEach { it(sceneView) }
    }

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)

        smoothTransform?.let { smoothTransform ->
            if (smoothTransform != transform) {
                if (transform != lastFrameTransform) {
                    // Stop smooth if any of the position/rotation/scale has changed meanwhile
                    this.smoothTransform = null
                } else {
                    // Smooth the transform
                    transform = slerp(
                        start = transform,
                        end = smoothTransform,
                        deltaSeconds = frameTime.intervalSeconds,
                        speed = smoothSpeed
                    )
                }
            } else {
                this.smoothTransform = null
            }
        }
        lastFrameTransform = transform

        transformInstance?.let {
            val worldTransform = this.worldTransform
            if (transformManager.getTransform(it) != worldTransform) {
                transformManager.setTransform(it, worldTransform)
            }
        }

        onFrame.forEach { it(frameTime, this) }
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

    override fun onDestroy(owner: LifecycleOwner) {
        destroy()
        super.onDestroy(owner)
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
    ) {
        if (smooth) {
            smooth(position, quaternion, scale, smoothSpeed)
        } else {
            this.position = position
            this.quaternion = quaternion
            this.scale = scale
        }
    }

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

    open fun clone() = copy(Node())

    @JvmOverloads
    open fun copy(toNode: Node = Node()): Node = toNode.apply {
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
            onAttachedToScene += object : (SceneView) -> Unit {
                override fun invoke(sceneView: SceneView) {
                    onAttachedToScene -= this
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
package io.github.sceneview.node

import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.ar.sceneform.collision.Collider
import com.google.ar.sceneform.collision.CollisionShape
import com.google.ar.sceneform.collision.CollisionSystem
import com.google.ar.sceneform.common.TransformProvider
import com.google.ar.sceneform.math.MathHelper.clamp
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import dev.romainguy.kotlin.math.*
import io.github.sceneview.SceneLifecycle
import io.github.sceneview.SceneLifecycleObserver
import io.github.sceneview.SceneView
import io.github.sceneview.math.*
import io.github.sceneview.renderable.Renderable
import io.github.sceneview.utils.FrameTime

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
 */
open class Node : NodeParent, TransformProvider, SceneLifecycleObserver {

    companion object {
        val DEFAULT_POSITION get() = Position(x = 0.0f, y = 0.0f, z = 0.0f)
        val DEFAULT_QUATERNION get() = Quaternion()
        val DEFAULT_ROTATION = DEFAULT_QUATERNION.toEulerAngles()
        val DEFAULT_SCALE get() = Scale(1.0f)

        const val DEFAULT_ROTATION_DOT_THRESHOLD = 0.95f
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
    protected val renderer: Renderer? get() = sceneView?.renderer
    private val collisionSystem: CollisionSystem? get() = sceneView?.collisionSystem

    /**
     * ### Define your own custom name
     */
    var name: String? = null

    val isAttached get() = sceneView != null

    internal var _position: Position = DEFAULT_POSITION

    /**
     * ### The node position
     *
     * The node's position locates it within the coordinate system of its parent.
     * The default position is the zero vector, indicating that the node is placed at the origin of
     * the parent node's coordinate system.
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
    var position: Position get() = _position
    set(value) {
        _position = value
        smoothPosition = null
    }

    internal var _quaternion: Quaternion = DEFAULT_QUATERNION

    /**
     * TODO: Doc
     */
    var quaternion: Quaternion get() = _quaternion
        set(value) {
            _quaternion = value
            smoothQuaternion = null
        }

    /**
     * ### The node orientation in Euler Angles Degrees per axis.
     *
     * `[0..360]`
     *
     * The three-component rotation vector specifies the direction of the rotation axis in degrees.
     *
     * The default rotation is the zero vector, specifying no rotation.
     * Rotation is applied relative to the node's origin property.
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
     *
     * - reduce size: scale < 1.0f
     * - same size: scale = 1.0f
     * - increase size: scale > 1.0f
     */
    var scale: Scale = DEFAULT_SCALE

    open var transform: Transform
        get() = translation(position) * rotation(quaternion) * scale(scale)
        set(value) {
            position = Position(value.position)
            quaternion = rotation(value).toQuaternion()
            scale = Scale(value.scale)
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
     * Expressed in units per seconds.
     * On an AR context, 1 unit = 1 meter. So, for position, this value defines the meters per
     * seconds for a node move.
     * This value is used by [smooth]
     */
    var smoothSpeed = 5.0f

    /**
     * ## The smooth rotation minimum change limit
     *
     * This is used to avoid very near rotations smooth modifications. It prevents the rotation to
     * appear too quick if the ranges are too close and uses linearly interpolation for upper dot
     * products.
     *
     * Expressed in quaternion dot product
     * This value is used by [smooth]
     */
    var smoothRotationThreshold = DEFAULT_ROTATION_DOT_THRESHOLD

    private var smoothPosition: Position? = null
    set(value) {
        if(field != value) {
            field = value
            if (value == null && smoothQuaternion == null) {
                onSmoothEnd()
            }
        }
    }
    private var smoothQuaternion: Quaternion? = null
        set(value) {
            if(field != value) {
                field = value
                if (value == null && smoothPosition == null) {
                    onSmoothEnd()
                }
            }
        }

    /**
     * ### The node can be focused within the [com.google.ar.sceneform.collision.CollisionSystem]
     * when a touch event happened
     *
     * true if the node can be selected
     */
    var isFocusable = true

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

    /**
     * ### The active status
     *
     * A node is considered active if it meets ALL of the following conditions:
     * - The node is part of a scene.
     * - the node's parent is active.
     * - The node is enabled.
     *
     * An active Node has the following behavior:
     * - The node's [onFrame] function will be called every frame.
     * - The node's [Renderable] will be rendered.
     * - The node's [collisionShape] will be checked in calls to Scene.hitTest.
     * - The node's [onTouchEvent] function will be called when the node is touched.
     */
    open val shouldBeRendered: Boolean
        get() = isVisible && isAttached
                && parentNode?.shouldBeRendered != false

    open var isRendered = false
        internal set(value) {
            if (field != value) {
                field = value
                if (value) {
                    collisionSystem?.let { collider?.setAttachedCollisionSystem(it) }
                } else {
                    collider?.setAttachedCollisionSystem(null)
                }
                onRenderingChanged(value)
            }
        }

    /**
     * ### The [Node] parent if the parent extends [Node]
     *
     * Returns null if the parent is not a [Node].
     * = Returns null if the parent is a [SceneView]
     */
    val parentNode get() = parent as? Node
    override var _children = listOf<Node>()

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
                // Remove from old parent if not already removed
                field?.takeIf { this in it.children }?.removeChild(this)
                // Find the old parent SceneView
                ((field as? SceneView) ?: (field as? Node)?.sceneView)?.let { sceneView ->
                    // Make sure to listen to at least one lifecycle in case of no more parent in
                    // order to unsure destroy
                    if (value != null) {
                        sceneView.lifecycle.removeObserver(this)
                    }
                    onDetachFromScene(sceneView)
                }
                field = value
                // Add to new parent if not already added
                value?.takeIf { this !in it.children }?.addChild(this)
                // Find the new parent SceneView
                ((value as? SceneView) ?: (value as? Node)?.sceneView)?.let { sceneView ->
                    sceneView.lifecycle.addObserver(this)
                    onAttachToScene(sceneView)
                }
                updateVisibility()
            }
        }

    private var allowDispatchTransformChanged = true

    // Collision fields.
    var collider: Collider? = null
        private set

    // Stores data used for detecting when a tap has occurred on this node.
    private var touchTrackingData: TapTrackingData? = null

    /** ### Listener for [onFrame] call */
    val onFrame = mutableListOf<((frameTime: FrameTime, node: Node) -> Unit)>()

    /** ### Listener for [onAttachToScene] call */
    val onAttachedToScene = mutableListOf<((scene: SceneView) -> Unit)>()

    /** ### Listener for [onDetachedFromScene] call */
    val onDetachedFromScene = mutableListOf<((scene: SceneView) -> Unit)>()

    /** ### Listener for [onRenderingChanged] call */
    val onRenderingChanged = mutableListOf<((node: Node, isRendering: Boolean) -> Unit)>()

    /**
     * ### The transformation (position, rotation or scale) of the [Node] has changed
     *
     * If node A's position is changed, then that will trigger [onTransformChanged] to be
     * called for all of it's descendants.
     */
    val onTransformChanged = mutableListOf<(node: Node) -> Unit>()

    var onSmoothEnd: ((node: Node) -> Unit)? = null

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
    var onTap: ((renderable: Renderable, motionEvent: MotionEvent) -> Unit)? = null

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
        rotation: Rotation = DEFAULT_ROTATION,
        scale: Scale = DEFAULT_SCALE
    ) : super() {
        this.position = position
        this.rotation = rotation
        this.scale = scale
    }

    open fun onAttachToScene(sceneView: SceneView) {
        onAttachedToScene.toList().forEach { it(sceneView) }
    }

    open fun onDetachFromScene(sceneView: SceneView) {
        onDetachedFromScene.toList().forEach { it(sceneView) }
    }

    /**
     * ### Handles when this node becomes rendered (displayed) or not
     *
     * A Node is rendered if it's visible, part of a scene, and its parent is rendered.
     * Override to perform any setup that needs to occur when the node is active or not.
     */
    open fun onRenderingChanged(isRendering: Boolean) {
        onRenderingChanged.toList().forEach { it(this, isRendering) }
    }

    override fun onFrame(frameTime: FrameTime) {
        // Smooth value compare
        val lerpFactor = clamp((frameTime.intervalSeconds * smoothSpeed).toFloat(), 0.0f, 1.0f)
        val smoothPosition = this.smoothPosition?.takeIf { it != _position }
        if(smoothPosition != null) {
            _position = lerp(_position, smoothPosition, lerpFactor).takeIf {
                distance(_position, it) > 0.00001f
            } ?: smoothPosition
            if (_position == smoothPosition) {
                this.smoothPosition = null
            }
        }
        val smoothQuaternion = this.smoothQuaternion?.takeIf { it != _quaternion }
        if(smoothQuaternion != null) {
            _quaternion = slerp(_quaternion, normalize(smoothQuaternion), lerpFactor).takeIf {
                angle(_quaternion, smoothQuaternion) > 0.00001f
            } ?: smoothQuaternion
            if (_quaternion == smoothQuaternion) {
                this.smoothQuaternion = null
            }
        }
        onFrame.forEach { it(frameTime, this) }
    }

    open fun onSmoothEnd() {
        onSmoothEnd?.invoke(this)
    }

    /**
     * ### Invoked when the node is tapped
     *
     * Calls the `onTap` listener if it is available and passes the tap to the parent node.
     *
     * @param renderable The ID of the Filament renderable that was tapped.
     * @param motionEvent The motion event that caused the tap.
     */
    open fun onTap(renderable: Renderable, motionEvent: MotionEvent) {
        onTap?.invoke(renderable, motionEvent)

        parentNode?.onTap(renderable, motionEvent)
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

    fun updateVisibility() {
        isRendered = shouldBeRendered
        children.forEach { it.updateVisibility() }
    }

    /**
     * ### The node scale
     *
     * - reduce size: scale < 1.0f
     * - same size: scale = 1.0f
     * - increase size: scale > 1.0f
     */
    fun scale(scale: Float) {
        this.scale.xyz = Scale(scale)
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
        quaternion: Quaternion = this.quaternion,
        rotation: Rotation = this.rotation,
        scale: Scale = this.scale
    ) {
        this.position = position
        if (quaternion != this.quaternion) {
            this.quaternion = quaternion
        } else if (rotation != this.rotation) {
            this.rotation = rotation
        }
        this.scale = scale
    }

    /**
     * ## Smooth move, rotate and scale at a specified speed
     *
     * @see position
     * @see rotation
     * @see quaternion
     * @see scale
     * @see speed
     */
    fun smooth(
        position: Position = this.position,
        quaternion: Quaternion = this.quaternion,
        rotation: Rotation = this.rotation,
        scale: Scale = this.scale,
        speed: Float = this.smoothSpeed
    ) {
        this.smoothSpeed = speed
        if (position != this.position) {
            smoothPosition = position
        }
        if (quaternion != this.quaternion) {
            smoothQuaternion = quaternion
        } else if (rotation != this.rotation) {
            smoothQuaternion = rotation.toQuaternion()
        }
        if(scale != this.scale) {
            // TODO: smooth scale
            this.scale = scale
        }
    }


    /**
     * ## Smooth move, rotate and scale at a specified speed
     *
     * @see transform
     */
    fun smooth(transform: Transform, speed: Float = this.smoothSpeed) {
        smooth(position = transform.position, quaternion = transform.toQuaternion(), speed = speed)
    }

    // TODO
//
//    /**
//     * ### Rotates the node to face a point in world space
//     *
//     * World-space up (0, 1, 0) will be used to determine the orientation of the node around the
//     * direction.
//     *
//     * @param position The position to look at in world space
//     * @param up The up direction will determine the orientation of the node around the direction
//     */
//    fun lookAt(position: Position, up: Direction = Direction(y=1.0f), smooth: Boolean = false) {
//        if(smooth) {
//            smooth(quaternion = rotation(lookAt(this.worldPosition, position, up)).toQuaternion())
//        } else {
//            transform(rotation = rotation(lookAt(this.worldPosition, position, up)).toQuaternion())
//        }
//    }

    /**
     * ### Checks whether the given node parent is an ancestor of this node recursively
     *
     * Return true if the node is an ancestor of this node
     */
    fun isDescendantOf(ancestor: NodeParent): Boolean =
        parent == ancestor || parentNode?.isDescendantOf(ancestor) == true

    /**
     * ### Finds the first enclosing node with the given type
     *
     * The node can be:
     * - This node.
     * - One of the parent nodes.
     * - `null` if no node is found.
     */
    inline fun <reified T: Node> firstEnclosingNode(): T? {
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
                        if (isRendered && collisionSystem != null) {
                            setAttachedCollisionSystem(collisionSystem)
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

    // TODO : Use kotlin math

//    /**
//     * ### Sets the direction that the node is looking at in world-space
//     *
//     * After calling this, [forward] will match the look direction passed in.
//     * The up direction will determine the orientation of the node around the direction.
//     * The look direction and up direction cannot be coincident (parallel) or the orientation will
//     * be invalid.
//     *
//     * @param lookDirection a vector representing the desired look direction in world-space
//     * @param upDirection   a vector representing a valid up vector to use, such as Vector3.up()
//     */
//    fun setLookDirection(lookDirection: Vector3, upDirection: Vector3) {
//        val cameraPosition: Vector3 = getScene().getCamera().getWorldPosition()
//        val cardPosition: Vector3 = getWorldPosition()
//        sceneView.renderer.camera.lookAt()
//        val direction = Vector3.subtract(cameraPosition, cardPosition)
//        val lookRotation =
//            com.google.ar.sceneform.math.Quaternion.lookRotation(direction, Vector3.up())
//        setWorldRotation(lookRotation)
//
//
//        orientation = Quaternion.lookRotation(lookDirection, upDirection)
//    }

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
            onAttachedToScene.add(object : (SceneView) -> Unit {
                override fun invoke(sceneView: SceneView) {
                    onAttachedToScene -= this
                    action(sceneView)
                }
            })
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
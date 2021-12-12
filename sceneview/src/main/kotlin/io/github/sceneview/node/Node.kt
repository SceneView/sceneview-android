package io.github.sceneview.node

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.PickHitResult
import com.google.ar.sceneform.collision.Collider
import com.google.ar.sceneform.collision.CollisionShape
import com.google.ar.sceneform.collision.CollisionSystem
import com.google.ar.sceneform.common.TransformProvider
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import io.github.sceneview.SceneLifecycle
import io.github.sceneview.SceneLifecycleObserver
import io.github.sceneview.SceneView
import kotlin.math.abs

private const val directionUpEpsilon = 0.99f

// This is the default from the ViewConfiguration class.
private const val defaultTouchSlop = 8

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * It can contain a renderable for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
open class Node(
    position: Vector3 = defaultPosition,
    rotationQuaternion: Quaternion = defaultRotation,
    scales: Vector3 = defaultScales
) : NodeParent, TransformProvider, SceneLifecycleObserver {

    companion object {
        val defaultPosition get() = Vector3(0.0f, 0.0f, -2.0f)
        val defaultRotation get() = Quaternion()
        val defaultScales get() = Vector3(1.0f, 1.0f, 1.0f)
    }

    /**
     * ### The scene that this node is part of, null if it isn't part of any scene
     *
     * A node is part of a scene if its highest level ancestor is a [SceneView]
     */
    protected open val sceneView: SceneView? get() = parent as? SceneView ?: parentNode?.sceneView

    // TODO : Remove when every dependendent is kotlined
    fun getSceneViewInternal() = sceneView
    protected open val lifecycle: SceneLifecycle? get() = sceneView?.lifecycle
    protected val lifecycleScope get() = sceneView?.lifecycleScope
    protected val renderer: Renderer? get() = sceneView?.renderer
    private val collisionSystem: CollisionSystem? get() = sceneView?.collisionSystem

    val isAttached get() = sceneView != null

    /**
     * ### The node can be selected within the [com.google.ar.sceneform.collision.CollisionSystem]
     * when a touch event happened
     *
     * true if the node can be selected
     */
    var isSelectable = true

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
     * - The node's [onFrameUpdated] function will be called every frame.
     * - The node's [renderable] will be rendered.
     * - The node's [collisionShape] will be checked in calls to Scene.hitTest.
     * - The node's [onTouchEvent] function will be called when the node is touched.
     */
    internal val shouldBeRendered: Boolean
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
                // Remove from old parent
                // If not already removed
                field?.takeIf { this in it.children }?.removeChild(this)
                ((field as? SceneView) ?: (field as? Node)?.sceneView)?.let { sceneView ->
                    sceneView.lifecycle.removeObserver(this)
                    onDetachFromScene(sceneView)
                }
                field = value
                // Add to new parent
                // If not already added
                value?.takeIf { this !in it.children }?.addChild(this)
                ((value as? SceneView) ?: (value as? Node)?.sceneView)?.let { sceneView ->
                    sceneView.lifecycle.addObserver(this)
                    onAttachToScene(sceneView)
                }
                updateVisibility()
            }
        }

    private var allowDispatchTransformChanged = true

    /**
     * ### The node X position
     *
     * - left: < 0.0f
     * - center: = 0.0f
     * - right: > 0.0f
     */
    var positionX: Float
        get() = position.x
        set(value) {
            position = position.apply { x = value }
        }

    /**
     * ### The node Y position
     *
     * - top: > 0.0f
     * - center: = 0.0f
     * - bottom: < 0.0f
     */
    open var positionY: Float
        get() = position.y
        set(value) {
            position = position.apply { y = value }
        }

    /**
     * ### The node Z position
     *
     * - forward: < 0.0f
     * - camera position: = 0.0f
     * - backward: > 0.0f
     */
    var positionZ: Float
        get() = position.z
        set(value) {
            position = position.apply { z = value }
        }

    /** ### The node position */
    open var position: Vector3 = position
        get() = Vector3(field)
        set(value) {
            if (field != value) {
                field = value
                onTransformChanged()
            }
        }

    /**
     * ### The node X center
     *
     * - left: < 0.0f
     * - center: = 0.0f
     * - right: > 0.0f
     */
    var centerX: Float
        get() = center.x
        set(value) {
            center = center.apply { x = value }
        }

    /**
     * ### The node Y center
     *
     * - top: > 0.0f
     * - center: = 0.0f
     * - bottom: < 0.0f
     */
    open var centerY: Float
        get() = center.y
        set(value) {
            center = center.apply { y = value }
        }

    /**
     * ### The node Z center
     *
     * - forward: > 0.0f
     * - camera position: = 0.0f
     * - backward: < 0.0f
     */
    var centerZ: Float
        get() = center.z
        set(value) {
            center = center.apply { z = value }
        }

    /** ### The node center */
    open var center: Vector3 = Vector3()
        get() = Vector3(field)
        set(value) {
            if (field != value) {
                field = value
                onTransformChanged()
            }
        }

    /**
     * ### The node X rotation in degrees
     *
     * [0..360]
     */
    var rotationX: Float
        get() = rotation.x
        set(value) {
            rotation = rotation.apply { x = value }
        }

    /**
     *  ### The node Y rotation in degrees
     *
     *  [0..360]
     */
    var rotationY: Float
        get() = rotation.y
        set(value) {
            rotation = rotation.apply { y = value }
        }

    /**
     * ### The node Z rotation in degrees
     *
     * [0..360]
     */
    var rotationZ: Float
        get() = rotation.z
        set(value) {
            rotation = rotation.apply { z = value }
        }

    /** ### The node rotation in Euler Angles Degrees */
    var rotation: Vector3
        get() = rotationQuaternion.eulerAngles
        set(value) {
            rotationQuaternion = Quaternion.eulerAngles(value)
        }

    open var rotationQuaternion: Quaternion = rotationQuaternion
        set(value) {
            if (field != value) {
                field = value
                onTransformChanged()
            }
        }

    /**
     * ### The node scale
     *
     * - down: < 1.0f
     * - original size: = 1.0f
     * - up: > 1.0f
     */
    open var scale: Float
        get() = arrayOf(scales.x, scales.y, scales.y).average().toFloat()
        set(value) {
            scales = scales.apply {
                x = value
                y = value
                z = value
            }
        }

    /** ### The node scales */
    open var scales: Vector3 = scales
        set(value) {
            if (field != value) {
                field = value
                onTransformChanged()
            }
        }

    // Collision fields.
    var collider: Collider? = null
        private set

    // Stores data used for detecting when a tap has occurred on this node.
    private var touchTrackingData: TapTrackingData? = null

    /** ### Listener for [onFrame] call */
    val onFrameUpdated = mutableListOf<((frameTime: FrameTime, node: Node) -> Unit)>()

    /** ### Listener for [onAttachToScene] call */
    val onAttachedToScene = mutableListOf<((scene: SceneView) -> Unit)>()

    /** ### Listener for [onDetachFromScene] call */
    val onDetachFromScene = mutableListOf<((scene: SceneView) -> Unit)>()

    /** ### Listener for [onRenderingChanged] call */
    val onRenderingChanged = mutableListOf<((node: Node, isRendering: Boolean) -> Unit)>()

    /**
     * ### The transformation (position, rotation or scale) of the [Node] has changed
     *
     * If node A's position is changed, then that will trigger [onTransformChanged] to be
     * called for all of it's descendants.
     */
    val onTransformChanged = mutableListOf<(node: Node) -> Unit>()

    /**
     * ### Registers a callback to be invoked when a touch event is dispatched to this node
     *
     * The way that touch events are propagated mirrors the way touches are propagated to Android
     * Views. This is only called when the node is active.
     *
     * When an ACTION_DOWN event occurs, that represents the start of a gesture. ACTION_UP or
     * ACTION_CANCEL represents when a gesture ends. When a gesture starts, the following is done:
     *
     * - Dispatch touch events to the node that was touched as detected by hitTest.
     * - If the node doesn't consume the event, recurse upwards through the node's parents and
     * dispatch the touch event until one of the node's consumes the event.
     * - If no nodes consume the event, the gesture is ignored and subsequent events that are part
     * of the gesture will not be passed to any nodes.
     * - If one of the node's consumes the event, then that node will consume all future touch
     * events for the gesture.
     *
     * When a touch event is dispatched to a node, the event is first passed to the node's.
     * If the [onTouchEvent] doesn't handle the event, it is passed to [.onTouchEvent].
     *
     * - `pickHitResult` - Represents the node that was touched and information about where it was
     * touched
     * - `motionEvent` - The MotionEvent object containing full information about the event
     * - `return` true if the listener has consumed the event, false otherwise
     */
    var onTouchEvent: ((pickHitResult: PickHitResult, motionEvent: MotionEvent) -> Boolean)? = null

    /**
     * ### Registers a callback to be invoked when this node is tapped.
     *
     * If there is a callback registered, then touch events will not bubble to this node's parent.
     * If the Node.onTouchEvent is overridden and super.onTouchEvent is not called, then the tap
     * will not occur.
     *
     * - `pickHitResult` - represents the node that was tapped and information about where it was
     * touched
     * - `motionEvent - The [MotionEvent.ACTION_UP] MotionEvent that caused the tap
     */
    var onTouched: ((pickHitResult: PickHitResult, motionEvent: MotionEvent) -> Unit)? = null

    constructor(
        position: Vector3 = defaultPosition,
        rotationQuaternion: Quaternion = defaultRotation,
        scales: Vector3 = defaultScales,
        parent: NodeParent? = null
    ) : this(position, rotationQuaternion, scales) {
        this.parent = parent
    }

    constructor(node: Node) : this(
        position = node.position,
        rotationQuaternion = node.rotationQuaternion,
        scales = node.scales
    )

    open fun onAttachToScene(sceneView: SceneView) {
        onAttachedToScene.forEach { it(sceneView) }
    }

    open fun onDetachFromScene(sceneView: SceneView) {
        onDetachFromScene.forEach { it(sceneView) }
    }

    /**
     * ### Handles when this node becomes rendered (displayed) or not
     *
     * A Node is rendered if it's visible, part of a scene, and its parent is rendered.
     * Override to perform any setup that needs to occur when the node is active or not.
     */
    open fun onRenderingChanged(isRendering: Boolean) {
        onRenderingChanged.forEach { it(this, isRendering) }
    }

    override fun onFrame(frameTime: FrameTime) {
        if (isRendered) {
            onFrameUpdated(frameTime)
        }
    }

    /**
     * ### Handles when this node is updated
     *
     * A node is updated before rendering each frame. This is only called when the node is active.
     * Override to perform any updates that need to occur each frame.
     */
    open fun onFrameUpdated(frameTime: FrameTime) {
        onFrameUpdated.forEach { it(frameTime, this) }
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
        transformationMatrixChanged = true
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
        super.onDestroy(owner)
        destroy()
    }

    internal fun updateVisibility() {
        isRendered = shouldBeRendered
        children.forEach { it.updateVisibility() }
    }

    /**
     * ### Checks whether the given node parent is an ancestor of this node recursively
     *
     * Return true if the node is an ancestor of this node
     */
    fun isDescendantOf(ancestor: NodeParent): Boolean =
        parent == ancestor || parentNode?.isDescendantOf(ancestor) == true

    // Reuse to limit frame instantiations
    protected var transformationMatrixChanged = true
    private val _transformationMatrix = Matrix()
    override fun getTransformationMatrix(): Matrix {
        if (transformationMatrixChanged) {
            _transformationMatrix.apply {
                makeTrs(
                    Vector3.subtract(position, center),
                    rotationQuaternion,
                    scales
                )
            }
            parentNode?.let { parentNode ->
                _transformationMatrix.apply {
                    Matrix.multiply(
                        parentNode.transformationMatrix,
                        _transformationMatrix,
                        this
                    )
                }
            }
            transformationMatrixChanged = false
        }
        return _transformationMatrix
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

    /**
     * ### Handles when this node is touched
     *
     * Override to perform any logic that should occur when this node is touched. The way that
     * touch events are propagated mirrors the way touches are propagated to Android Views. This is
     * only called when the node is active.
     *
     * When an ACTION_DOWN event occurs, that represents the start of a gesture. ACTION_UP or
     * ACTION_CANCEL represents when a gesture ends. When a gesture starts, the following is done:
     *
     * - Dispatch touch events to the node that was touched as detected by hitTest.
     * - If the node doesn't consume the event, recurse upwards through the node's parents and
     * dispatch the touch event until one of the node's consumes the event.
     * - If no nodes consume the event, the gesture is ignored and subsequent events that are part
     * of the gesture will not be passed to any nodes.
     * - If one of the node's consumes the event, then that node will consume all future touch
     * events for the gesture.
     *
     * When a touch event is dispatched to a node, the event is first passed to the node's.
     * If the [onTouchEvent] doesn't handle the event, it is passed to [onTouchEvent].
     *
     * @param pickHitResult Represents the node that was touched, and information about where it was
     * touched. On ACTION_DOWN events, [PickHitResult.getNode] will always be this node or
     * one of its children. On other events, the touch may have moved causing the
     * [PickHitResult.getNode] to change (or possibly be null).
     * @param motionEvent   The motion event.
     *
     * @return true if the event was handled, false otherwise.
     */
// TODO : Cleanup
    fun onTouchEvent(pickHitResult: PickHitResult, motionEvent: MotionEvent): Boolean {
        var handled = false

        // Reset tap tracking data if a new gesture has started or if the Node has become inactive.
        val actionMasked = motionEvent.actionMasked
        if (actionMasked == MotionEvent.ACTION_DOWN || !isRendered) {
            touchTrackingData = null
        }
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Only start tacking the tap gesture if there is a tap listener set.
                // This allows the event to bubble up to the node's parent when there is no listener.
                if (onTouched != null) {
                    pickHitResult.node?.let { hitNode ->
                        val downPosition = Vector3(motionEvent.x, motionEvent.y, 0.0f)
                        touchTrackingData = TapTrackingData(hitNode, downPosition)
                        handled = true
                    }
                }
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                // Assign to local variable for static analysis.
                touchTrackingData?.let { touchTrackingData ->
                    // Determine how much the touch has moved.
                    val touchSlop = sceneView?.let { sceneView ->
                        ViewConfiguration.get(sceneView.context).scaledTouchSlop
                    } ?: defaultTouchSlop

                    val upPosition = Vector3(motionEvent.x, motionEvent.y, 0.0f)
                    val touchDelta =
                        Vector3.subtract(touchTrackingData.downPosition, upPosition).length()

                    // Determine if this node or a child node is still being touched.
                    val hitNode = pickHitResult.node
                    val isHitValid = hitNode === touchTrackingData.downNode

                    // Determine if this is a valid tap.
                    val isValidTouch = isHitValid || touchDelta < touchSlop
                    if (isValidTouch) {
                        handled = true
                        // If this is an ACTION_UP event, it's time to call the listener.
                        if (actionMasked == MotionEvent.ACTION_UP && onTouched != null) {
                            onTouched!!.invoke(pickHitResult, motionEvent)
                            this.touchTrackingData = null
                        }
                    } else {
                        this.touchTrackingData = null
                    }
                }
            }
            else -> {
            }
        }
        return handled
    }

    /**
     * ### Calls onTouchEvent if the node is active
     *
     * Used by TouchEventSystem to dispatch touch events.
     *
     * @param pickHitResult Represents the node that was touched, and information about where it was
     * touched. On ACTION_DOWN events, [PickHitResult.getNode] will always be this node or
     * one of its children. On other events, the touch may have moved causing the
     * [PickHitResult.getNode] to change (or possibly be null).
     *
     * @param motionEvent   The motion event.
     * @return true if the event was handled, false otherwise.
     */
    open fun dispatchTouchEvent(pickHitResult: PickHitResult, motionEvent: MotionEvent): Boolean {
        return if (isRendered) {
            if (onTouchEvent?.invoke(pickHitResult, motionEvent) == true) {
                true
            } else {
                onTouchEvent(pickHitResult, motionEvent)
            }
        } else {
            false
        }
    }

    open fun clone() = copy(Node())

    @JvmOverloads
    open fun copy(toNode: Node = Node()): Node = toNode.apply {
        position = this@Node.position
        rotation = this@Node.rotation
        scale = this@Node.scale
    }

    /** ### Detach and destroy the node */
    open fun destroy() {
        this.parent = null
    }

    /**
     * ### Sets the direction that the node is looking at in world-space
     *
     * After calling this, [forward] will match the look direction passed in.
     * World-space up (0, 1, 0) will be used to determine the orientation of the node around the
     * direction.
     *
     * @param lookDirection a vector representing the desired look direction in world-space
     */
    fun setLookDirection(lookDirection: Vector3) {
        // Default up direction
        var upDirection = Vector3.up()
        // First determine if the look direction and default up direction are far enough apart to
        // produce a numerically stable cross product.
        val directionUpMatch = abs(Vector3.dot(lookDirection, upDirection))
        if (directionUpMatch > directionUpEpsilon) {
            // If the direction vector and up vector coincide choose a new up vector.
            upDirection = Vector3(0.0f, 0.0f, 1.0f)
        }

        // Finally build the rotation with the proper up vector.
        setLookDirection(lookDirection, upDirection)
    }

    /**
     * ### Sets the direction that the node is looking at in world-space
     *
     * After calling this, [forward] will match the look direction passed in.
     * The up direction will determine the orientation of the node around the direction.
     * The look direction and up direction cannot be coincident (parallel) or the orientation will
     * be invalid.
     *
     * @param lookDirection a vector representing the desired look direction in world-space
     * @param upDirection   a vector representing a valid up vector to use, such as Vector3.up()
     */
    fun setLookDirection(lookDirection: Vector3, upDirection: Vector3) {
        rotationQuaternion = Quaternion.lookRotation(lookDirection, upDirection)
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
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
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import dev.romainguy.kotlin.math.*
import io.github.sceneview.SceneLifecycle
import io.github.sceneview.SceneLifecycleObserver
import io.github.sceneview.SceneView
import io.github.sceneview.utils.*

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
        val DEFAULT_POSITION get() = Position(x = 0.0f, y = 0.0f, z = -2.0f)
        val DEFAULT_ROTATION_QUATERNION get() = Quaternion()
        val DEFAULT_ROTATION get() = DEFAULT_ROTATION_QUATERNION.toEulerAngles()
        val DEFAULT_SCALE get() = Scale(1.0f, 1.0f, 1.0f)
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

    val isAttached get() = sceneView != null

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
    var position: Position = DEFAULT_POSITION

    /**
     * TODO: Doc
     */
    var rotationQuaternion: Quaternion = DEFAULT_ROTATION_QUATERNION

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
        get() = rotationQuaternion.toEulerAngles()
        set(value) {
            rotationQuaternion = Quaternion.fromEuler(value)
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
        get() = transpose(translation(position) * rotation(rotationQuaternion) * scale(scale))
        set(value) {
            position = Position(value.position)
            rotationQuaternion = rotation(value).toQuaternion()
            scale = Scale(value.scale)
        }

    /**
     * ### The node content origin (center)
     *
     * A node's content origin is the transformation between its coordinate space and that used by
     * its [position]. The default origin is zero vector, specifying that the node's position
     * locates the origin of its coordinate system relatively to that center point.
     *
     * Changing the origin transform alters these behaviors in many useful ways.
     * You can offset the node's contents relative to its position.
     * For example, by setting the pivot to a translation transform you can position a node
     * containing a sphere geometry relative to where the sphere would rest on a floor instead of
     * relative to its center.
     */
    open var contentPosition = Position()

    /**
     * TODO: Doc
     */
    var contentRotationQuaternion: Quaternion = Quaternion()

    /**
     * ### The node content orientation
     *
     * A node's content origin is the transformation between its coordinate space and that used by
     * its [rotationQuaternion]. The default origin is zero vector, specifying that the node's orientation
     * locates the origin of its rotation relatively to that center point.
     *
     * `[0..360]`
     *
     * Changing the origin transform alters these behaviors in many useful ways.
     * You can move the node's axis of rotation.
     * For example, with a translation transform you can cause a node to revolve around a faraway
     * point instead of rotating around its center, and with a rotation transform you can tilt the
     * axis of rotation.
     */
    open var contentRotation: Rotation
        get() = contentRotationQuaternion.toEulerAngles()
        set(value) {
            contentRotationQuaternion = Quaternion.fromEuler(value)
        }

    /**
     * ### The node content scale
     */
    open var contentScale = Scale(1.0f, 1.0f, 1.0f)

    open var contentTransform: Transform
        get() = transpose(translation(contentPosition) * rotation(contentRotationQuaternion) * scale(contentScale))
        set(value) {
            contentPosition = Position(value.position)
            contentRotationQuaternion = rotation(value).toQuaternion()
            contentScale = Scale(value.scale)
        }

    /**
     * ## The smooth position, rotation and scale speed
     *
     * Expressed in units per seconds.
     * On an AR context, 1 unit = 1 meter. So, for position, this value defines the meters per
     * seconds for a node move.
     * This value is used by [smoothPosition] and [smoothRotation]
     */
    var smoothMoveSpeed = 5.0f

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
    private var smoothRotation: Quaternion? = null

    val worldTransform: Mat4
        get() = (parentNode?.let { parent ->
            parent.worldTransform * transform * contentTransform
        } ?: transform * contentTransform)

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

    // Collision fields.
    var collider: Collider? = null
        private set

    // Stores data used for detecting when a tap has occurred on this node.
    private var touchTrackingData: TapTrackingData? = null

    /** ### Listener for [onFrame] call */
    val onFrameUpdated = mutableListOf<((frameTime: FrameTime, node: Node) -> Unit)>()

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
        smoothPosition?.let { desiredPosition ->
            position = lerp(position, desiredPosition, smoothMoveSpeed * frameTime.deltaSeconds)
            if (position == desiredPosition) {
                this.smoothPosition = null
            }
        }
        smoothRotation?.let { desiredRotation ->
            val slerpFactor = clamp(frameTime.deltaSeconds * smoothMoveSpeed, 0f, 1f)
            rotationQuaternion = slerp(rotationQuaternion, normalize(desiredRotation), slerpFactor)
            // TODO: Move adjust this smooth precision angle
            if (angle(rotationQuaternion, desiredRotation) < 0.01f) {
                smoothRotation = null
            }
        }
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
        rotation: Rotation = this.rotation,
        scale: Scale = this.scale
    ) {
        this.position = position
        this.rotation = rotation
        this.scale = scale
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
        rotation: Quaternion = this.rotationQuaternion,
        scale: Scale = this.scale
    ) {
        this.position = position
        this.rotationQuaternion = rotation
        this.scale = scale
    }

    /**
     * ## Move/smooth change the node transform
     *
     * @see position
     * @see rotation
     * @see scale
     * @see smoothSpeed
     */
    fun smooth(
        position: Position = this.position,
        rotation: Quaternion = this.rotationQuaternion,
        //TODO : Handle those parameters
//        scale: Scale = this.scale,
//        contentPosition: Position = this.contentPosition,
//        contentRotation: Rotation = this.contentRotation,
//        contentScale: Scale = this.contentScale
    ) {
        if (position != this.position) {
            smoothPosition = position
        }
        if (rotation != this.rotationQuaternion) {
            smoothRotation = rotation
        }
    }

    /**
     * ### Checks whether the given node parent is an ancestor of this node recursively
     *
     * Return true if the node is an ancestor of this node
     */
    fun isDescendantOf(ancestor: NodeParent): Boolean =
        parent == ancestor || parentNode?.isDescendantOf(ancestor) == true

    /**
     * ### The node world-space position
     *
     * The world position of this node (i.e. relative to the [SceneView]).
     * This is the composition of this component's local position with its parent's world
     * position.
     *
     * @see worldTransform
     */
    open val worldPosition: Position get() = worldTransform.position

    /**
     * ### The node world-space rotation
     *
     * The world rotation of this node (i.e. relative to the [SceneView]).
     * This is the composition of this component's local rotation with its parent's world
     * rotation.
     *
     * @see worldTransform
     */
    open val worldRotation: Rotation get() = worldTransform.rotation

    /**
     * ### The node world-space scale
     *
     * The world scale of this node (i.e. relative to the [SceneView]).
     * This is the composition of this component's local scale with its parent's world
     * scale.
     *
     * @see worldTransform
     */
    open val worldScale: Scale get() = worldTransform.scale

    // TODO : Remove this to full Kotlin Math
    override fun getTransformationMatrix(): Matrix {
        return Matrix(worldTransform.toFloatArray())
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
        rotationQuaternion = this@Node.rotationQuaternion
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
//     * World-space up (0, 1, 0) will be used to determine the orientation of the node around the
//     * direction.
//     *
//     * @param lookDirection a vector representing the desired look direction in world-space
//     */
//    fun setLookDirection(lookDirection: Vector3) {
//        // Default up direction
//        var upDirection = Vector3.up()
//        // First determine if the look direction and default up direction are far enough apart to
//        // produce a numerically stable cross product.
//        val directionUpMatch = abs(Vector3.dot(lookDirection, upDirection))
//        if (directionUpMatch > directionUpEpsilon) {
//            // If the direction vector and up vector coincide choose a new up vector.
//            upDirection = Vector3(0.0f, 0.0f, 1.0f)
//        }
//
//        // Finally build the rotation with the proper up vector.
//        setLookDirection(lookDirection, upDirection)
//    }

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
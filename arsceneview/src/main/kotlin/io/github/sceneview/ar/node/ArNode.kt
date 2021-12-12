package io.github.sceneview.ar.node

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.math.MathHelper
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.arcore.rotation
import io.github.sceneview.defaultMaxFPS
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.NodeParent

open class ArNode(
    position: Vector3 = defaultPosition,
    rotationQuaternion: Quaternion = defaultRotation,
    scales: Vector3 = defaultScales,
    parent: NodeParent? = null
) : ModelNode(
    position = position,
    rotationQuaternion = rotationQuaternion,
    scales = scales,
    parent = parent
), ArSceneLifecycleObserver {

    companion object {
        val defaultWorldPosition = Vector3()
        val defaultWorldRotation = Quaternion()
        val defaultWorldScale = Vector3(1.0f, 1.0f, 1.0f)
    }

    override val sceneView get() = super.sceneView as? ArSceneView
    override val lifecycle get() = sceneView?.lifecycle
    protected val session get() = sceneView?.session

    /** ### Move smoothly/slowly when there is a pose(AR position and rotation) update */
    var smoothMoves = true

    /**
     * ### The Smooth Speed factor
     *
     * Use this to change the speed at which the poses update from ARCore are smoothly moved and
     * oriented.
     * - High value = move slowly to the new position/rotation
     * - Low value = move directly to the new position/rotation
     */
    var smoothMoveSpeedFactor = 12.0f

    val isAnchored get() = anchor != null

    var anchor: Anchor? = null
        set(value) {
            field?.detach()
            field = value
            pose = value?.pose
            onAnchorChanged(value)
        }

    /** TODO : Doc */
    var pose: Pose? = null
        set(value) {
            field = value
            if (value != null) {
                setWorldPosition(value.position, smoothMoves)
                setWorldRotation(value.rotation, smoothMoves)
            }
            onPoseChanged(value)
        }

    val createdAnchoredNodes: MutableList<ArNode> = mutableListOf()

    /** ### The node world-space position */
    open var worldPosition: Vector3 = defaultWorldPosition
        internal set(value) {
            if (field != value) {
                field = value
                onWorldTransformChanged()
            }
        }

    /** ### The node world-space position */
    open var targetWorldPosition: Vector3 = defaultWorldPosition

    /** ### The node world-space rotation */
    open var worldRotationQuaternion: Quaternion = defaultWorldRotation
        internal set(value) {
            if (field != value) {
                field = value
                onWorldTransformChanged()
            }
        }

    /** ### The node world-space rotation in Euler Angles Degrees */
    var worldRotation: Vector3
        get() = worldRotationQuaternion.eulerAngles
        set(value) {
            worldRotationQuaternion = Quaternion.eulerAngles(value)
        }

    protected open var targetWorldRotationQuaternion: Quaternion = defaultWorldRotation

    /** ### The node world-space scale */
    open var worldScale: Vector3 = defaultWorldScale
        internal set(value) {
            if (field != value) {
                field = value
                onWorldTransformChanged()
            }
        }

    /** ### The world transformation (position, rotation or scale) of the [ArNode] has changed */
    val onWorldTransformChanged = mutableListOf<(node: Node) -> Unit>()


    /** ### TODO : Doc */
    val onAnchorChanged = mutableListOf<(node: Node, anchor: Anchor?) -> Unit>()

    /** ### TODO : Doc */
    var onPoseChanged: ((node: Node, pose: Pose?) -> Unit)? = null

    /**
     * @param modelGlbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     */
    constructor(
        context: Context,
        coroutineScope: LifecycleCoroutineScope? = null,
        parent: NodeParent? = null,
        modelGlbFileLocation: String,
        onModelLoaded: ((instance: RenderableInstance) -> Unit)? = null,
        onError: ((error: Exception) -> Unit)? = null,
        position: Vector3 = defaultPosition,
        rotationQuaternion: Quaternion = defaultRotation,
        scales: Vector3 = defaultScales,
    ) : this(position, rotationQuaternion, scales, parent) {
        loadModel(context, modelGlbFileLocation, coroutineScope, onModelLoaded, onError)
    }

    /**
     * @param modelGlbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     */
    constructor(
        context: Context,
        coroutineScope: LifecycleCoroutineScope? = null,
        anchor: Anchor,
        modelGlbFileLocation: String,
        onModelLoaded: ((instance: RenderableInstance) -> Unit)? = null,
        onError: ((error: Exception) -> Unit)? = null,
    ) : this() {
        this.anchor = anchor
        loadModel(context, modelGlbFileLocation, coroutineScope, onModelLoaded, onError)
    }

    constructor(anchor: Anchor) : this() {
        this.anchor = anchor
    }

    constructor(node: ModelNode) : this(
        position = node.position,
        rotationQuaternion = node.rotationQuaternion,
        scales = node.scales
    ) {
        setRenderable(node.renderable)
    }

    constructor(hitResult: HitResult) : this(hitResult.createAnchor())

    override fun onFrame(frameTime: FrameTime) {
        super<ModelNode>.onFrame(frameTime)

        // Smooth moves
        if (worldPosition != targetWorldPosition) {
            worldPosition = Vector3.lerp(worldPosition, targetWorldPosition, smouthLerpFactor)
        }
        if (worldRotationQuaternion != targetWorldRotationQuaternion) {
            worldRotationQuaternion = Quaternion.slerp(
                worldRotationQuaternion,
                targetWorldRotationQuaternion,
                smouthLerpFactor
            )
        }
    }

    override fun onArFrame(frame: ArFrame) {
        if (anchor?.trackingState == TrackingState.TRACKING) {
            pose = anchor?.pose
        }
    }

    /** ### The world transformation (position, rotation or scale) of the [ArNode] has changed */
    open fun onWorldTransformChanged() {
        onTransformChanged()
        worldTransformationMatrixChanged = true
        onWorldTransformChanged.forEach { it(this) }
    }


    /** TODO : Doc */
    open fun onAnchorChanged(anchor: Anchor?) {
        onAnchorChanged.forEach { it(this, anchor) }
    }

    /** TODO : Doc */
    open fun onPoseChanged(pose: Pose?) {
        onPoseChanged?.invoke(this, pose)
    }

    /**
     * ### Performs a ray cast to retrieve the ARCore info at this camera point
     *
     * @param frame the [ArFrame] from where we take the [HitResult]
     * By default the latest session frame if any exist
     * @param xPx x view coordinate in pixels
     * By default the [positionX] of this Node is used
     * @property yPx y view coordinate in pixels
     * By default the [positionY] of this Node is used
     *
     * @return the hitResult or null if no info is retrieved
     *
     * @see ArFrame.hitTest
     */
    @JvmOverloads
    open fun hitTest(
        frame: ArFrame? = session?.currentFrame,
        xPx: Float = (session?.displayWidth ?: 0) / 2.0f * (1.0f + positionX),
        yPx: Float = (session?.displayHeight ?: 0) / 2.0f * (1.0f - positionY)
    ): HitResult? = frame?.hitTest(xPx, yPx)

    /**
     * ### Creates a new anchor actual node worldPosition and worldRotation (hit location)
     *
     * Creates an anchor at the given pose in the world coordinate space that is attached to this
     * trackable. The type of trackable will determine the semantics of attachment and how the
     * anchor's pose will be updated to maintain this relationship. Note that the relative offset
     * between the pose of multiple anchors attached to a trackable may adjust slightly over time as
     * ARCore updates its model of the world.
     *
     * Anchors incur ongoing processing overhead within ARCore. To release unneeded anchors use
     * [Anchor.detach]
     *
     * This method is a convenience alias for [HitResult.createAnchor]
     */
    open fun createAnchor(): Anchor? = hitTest()?.createAnchor()

    /**
     * ### Anchor this node to make it fixed at the actual position and orientation
     *
     * Creates an anchor at the given pose in the world coordinate space that is attached to this
     * trackable. The type of trackable will determine the semantics of attachment and how the
     * anchor's pose will be updated to maintain this relationship. Note that the relative offset
     * between the pose of multiple anchors attached to a trackable may adjust slightly over time as
     * ARCore updates its model of the world.
     */
    fun anchor(detachPrevious: Boolean = true): Boolean {
        if (detachPrevious) {
            anchor?.detach()
        }
        anchor = createAnchor()
        return anchor != null
    }

    /**
     * ### Creates a new anchored Node at the actual worldPosition and worldRotation
     *
     * The returned node position and rotation will be fixed within camera movements.
     *
     * See [hitTest] and [ArFrame.hitTests] for details.
     *
     * Anchors incur ongoing processing overhead within ARCore.
     * To release unneeded anchors use [destroy].
     */
    open fun createAnchoredNode(): ArNode? {
        return createAnchor()?.let { anchor ->
            ArNode(anchor)
        }?.also { anchorNode ->
            createdAnchoredNodes += anchorNode
        }
    }

    /** TODO: Doc */
    fun createAnchoredCopy(): ArNode? {
        return createAnchoredNode()?.apply {
            copy(this)
        }
    }

    fun setWorldPosition(position: Vector3, smoothMove: Boolean = smoothMoves) {
        // If smooth worldPosition will increase in onFrame() until it equals target
        targetWorldPosition = position
        // Else move directly to the target
        if (!smoothMove) {
            worldPosition = targetWorldPosition
        }
    }

    fun setWorldRotation(rotation: Vector3, smoothMove: Boolean = smoothMoves) {
        setWorldRotation(Quaternion.eulerAngles(rotation), smoothMove)
    }

    fun setWorldRotation(rotation: Quaternion, smoothMove: Boolean = smoothMoves) {
        // If smooth worldRotation will increase in onFrame() until it equals target
        targetWorldRotationQuaternion = rotation
        // Else move directly to the target
        if (!smoothMove) {
            worldRotationQuaternion = targetWorldRotationQuaternion
        }
    }

    private val smouthLerpFactor: Float
        get() = MathHelper.clamp(
            (1.0f / (sceneView?.maxFramesPerSeconds
                ?: defaultMaxFPS) * smoothMoveSpeedFactor),
            0f,
            1f
        )

    // Reuse this to limit frame instantiations
    private var worldTransformationMatrixChanged = true
        set(value) {
            field = value
            transformationMatrixChanged = true
        }
    val worldTransformationMatrix: Matrix = Matrix()
        get() {
            if (worldTransformationMatrixChanged) {
                field.apply {
                    makeTrs(worldPosition, worldRotationQuaternion, worldScale)
                }
                worldTransformationMatrixChanged = false
            }
            return field
        }

    // Reuse this to limit frame instantiations
    private val _transformationMatrix = Matrix()
    override fun getTransformationMatrix(): Matrix {
        if (transformationMatrixChanged) {
            _transformationMatrix.apply {
                Matrix.multiply(
                    worldTransformationMatrix,
                    super.getTransformationMatrix(),
                    this
                )
            }
            transformationMatrixChanged = false
        }
        return _transformationMatrix
    }

    override fun destroy() {
        super.destroy()

        anchor?.detach()
        anchor = null
        // TODO: Should we?
//        createdAnchoredNodes.forEach { node ->
//            node.destroy()
//        }
//        createdAnchoredNodes.clear()
    }

    /**
     * ### Converts a point in the local-space of this node to world-space.
     *
     * @param point the point in local-space to convert
     * @return a new vector that represents the point in world-space
     */
    fun localToWorldPosition(point: Vector3) = transformationMatrix.transformPoint(point)

    /**
     * ### Converts a point in world-space to the local-space of this node.
     *
     * @param point the point in world-space to convert
     * @return a new vector that represents the point in local-space
     */
    fun worldToLocalPosition(point: Vector3) = transformationMatrixInverted.transformPoint(point)

    /**
     * ### Converts a direction from the local-space of this node to world-space.
     *
     * Not impacted by the position or scale of the node.
     *
     * @param direction the direction in local-space to convert
     * @return a new vector that represents the direction in world-space
     */
    fun localToWorldDirection(direction: Vector3) =
        Quaternion.rotateVector(worldRotationQuaternion, direction)

    /**
     * ### Converts a direction from world-space to the local-space of this node.
     *
     * Not impacted by the position or scale of the node.
     *
     * @param direction the direction in world-space to convert
     * @return a new vector that represents the direction in local-space
     */
    fun worldToLocalDirection(direction: Vector3) =
        Quaternion.inverseRotateVector(worldRotationQuaternion, direction)

    /** ### Gets the world-space forward direction vector (-z) of this node */
    val worldForward get() = localToWorldDirection(Vector3.forward())

    /** ### Gets the world-space back direction vector (+z) of this node */
    val worldBack get() = localToWorldDirection(Vector3.back())

    /** ### Gets the world-space right direction vector (+x) of this node */
    val worldRight get() = localToWorldDirection(Vector3.right())

    /** ### Gets the world-space left direction vector (-x) of this node */
    val worldLeft get() = localToWorldDirection(Vector3.left())

    /** ### Gets the world-space up direction vector (+y) of this node */
    val worldUp get() = localToWorldDirection(Vector3.up())

    /** ### Gets the world-space down direction vector (-y) of this node */
    val worldDown get() = localToWorldDirection(Vector3.down())
}
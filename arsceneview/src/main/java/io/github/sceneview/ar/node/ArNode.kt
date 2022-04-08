package io.github.sceneview.ar.node

import com.google.ar.core.*
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.*
import io.github.sceneview.*
import io.github.sceneview.ar.ArSceneLifecycle
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

open class ArNode() : ModelNode(), ArSceneLifecycleObserver {

    override val sceneView: ArSceneView? get() = super.sceneView as? ArSceneView
    override val lifecycle: ArSceneLifecycle? get() = sceneView?.lifecycle
    protected val arSession: ArSession? get() = sceneView?.arSession

    /**
     * TODO : Doc
     */
    open val isTracking get() = pose != null

    /**
     * ### Move smoothly/slowly when there is a pose (AR position and rotation) update
     *
     * Use [smoothSpeed] to adjust the position and rotation change smoothness level
     */
    var smoothPose = true

    /**
     * TODO : Doc
     */
    var pose: Pose? = null
        set(value) {
            val position = value?.position
            val quaternion = value?.quaternion
            if (position != field?.position || quaternion != field?.quaternion) {
                field = value
                if (position != null && quaternion != null) {
                    if (smoothPose) {
                        smooth(position = position, quaternion = quaternion)
                    } else {
                        transform(position = position, quaternion = quaternion)
                    }
                }
                onTrackingChanged(isTracking, value)
            }
        }

    /**
     * TODO : Doc
     */
    val isAnchored get() = anchor != null

    /**
     * TODO : Doc
     */
    var anchor: Anchor? = null
        set(value) {
            field?.detach()
            field = value
            pose = value?.pose
            onAnchorChanged(value)
        }

    /**
     * ### The current cloud anchor state of the [anchor].
     */
    val cloudAnchorState: CloudAnchorState
        get() = anchor?.cloudAnchorState ?: CloudAnchorState.NONE

    /**
     * ### Whether a Cloud Anchor is currently being hosted or resolved
     */
    var cloudAnchorTaskInProgress = false
        private set

    /**
     * TODO : Doc
     */
    var onTrackingChanged: ((node: ArNode, isTracking: Boolean, pose: Pose?) -> Unit)? = null

    /**
     * TODO : Doc
     */
    var onAnchorChanged: ((node: Node, anchor: Anchor?) -> Unit)? = null

    private var onCloudAnchorTaskCompleted: ((anchor: Anchor, success: Boolean) -> Unit)? = null

    open val isEditable: Boolean
        get() = editableTransforms.isNotEmpty()
    open var editableTransforms: Set<EditableTransform> = EditableTransform.ALL

    /**
     * ### How/where does the node is positioned in the real world
     *
     * Depending on your need, you can change it to adjust between a quick
     * ([PlacementMode.INSTANT]), more accurate ([PlacementMode.DEPTH]), only on planes/walls
     * ([PlacementMode.PLANE_HORIZONTAL], [PlacementMode.PLANE_VERTICAL],
     * [PlacementMode.PLANE_HORIZONTAL_AND_VERTICAL]) or auto refining accuracy
     * ([PlacementMode.BEST_AVAILABLE]) placement.
     * The [hitTest], [pose] and [anchor] will be influenced by this choice.
     */
    var placementMode: PlacementMode = DEFAULT_PLACEMENT_MODE
        set(value) {
            field = value
            doOnAttachedToScene { sceneView ->
                (sceneView as? ArSceneView)?.apply {
                    planeFindingMode = value.planeFindingMode
                    depthEnabled = value.depthEnabled
                    instantPlacementEnabled = value.instantPlacementEnabled
                }
            }
        }

    /**
     * TODO : Doc
     */
    constructor(anchor: Anchor) : this() {
        this.anchor = anchor
    }

    override fun onArFrame(arFrame: ArFrame) {
        val anchor = anchor ?: return

        // Update the anchor position if any
        if (anchor.trackingState == TrackingState.TRACKING) {
            pose = anchor.pose
        }

        if (cloudAnchorTaskInProgress) {
            // Call the listener when the task completes successfully or with an error
            if (cloudAnchorState != CloudAnchorState.NONE && cloudAnchorState != CloudAnchorState.TASK_IN_PROGRESS) {
                cloudAnchorTaskInProgress = false
                onCloudAnchorTaskCompleted?.invoke(anchor, cloudAnchorState == CloudAnchorState.SUCCESS)
                onCloudAnchorTaskCompleted = null
            }
        }
    }

    /**
     * TODO : Doc
     */
    open fun onTrackingChanged(isTracking: Boolean, pose: Pose?) {
        onTrackingChanged?.invoke(this, isTracking, pose)
    }

    /**
     * TODO : Doc
     */
    open fun onAnchorChanged(anchor: Anchor?) {
        onAnchorChanged?.invoke(this, anchor)
    }

    /**
     * ### Creates a new anchor at actual node worldPosition and worldRotation (hit location)
     *
     * Creates an anchor at the given pose in the world coordinate space that is attached to this
     * trackable. The type of trackable will determine the semantics of attachment and how the
     * anchor's pose will be updated to maintain this relationship. Note that the relative offset
     * between the pose of multiple anchors attached to a trackable may adjust slightly over time as
     * ARCore updates its model of the world.
     *
     * Anchors incur ongoing processing overhead within ARCore. To release unneeded anchors use
     * [Anchor.detach]
     */
    open fun createAnchor(): Anchor? = null

    /**
     * ### Anchor this node to make it fixed at the actual position and orientation is the world
     *
     * Creates an anchor at the given pose in the world coordinate space that is attached to this
     * trackable. The type of trackable will determine the semantics of attachment and how the
     * anchor's pose will be updated to maintain this relationship. Note that the relative offset
     * between the pose of multiple anchors attached to a trackable may adjust slightly over time as
     * ARCore updates its model of the world.
     */
    open fun anchor(): Boolean {
        anchor = createAnchor()
        return anchor != null
    }


    /**
     * ### Anchor this node to make it fixed at the actual position and orientation is the world
     *
     * Creates an anchor at the given pose in the world coordinate space that is attached to this
     * trackable. The type of trackable will determine the semantics of attachment and how the
     * anchor's pose will be updated to maintain this relationship. Note that the relative offset
     * between the pose of multiple anchors attached to a trackable may adjust slightly over time as
     * ARCore updates its model of the world.
     */
    open fun detachAnchor() {
        anchor = null
    }

    /**
     * ### Hosts a Cloud Anchor based on the [anchor]
     *
     * The [anchor] is replaced with a new anchor returned by [Session.hostCloudAnchorWithTtl].
     *
     * @param ttlDays The lifetime of the anchor in days. See [Session.hostCloudAnchorWithTtl] for more details.
     * @param onTaskCompleted Called when the task completes successfully or with an error.
     */
    fun hostCloudAnchor(ttlDays: Int = 1, onTaskCompleted: (anchor: Anchor, success: Boolean) -> Unit) {
        if (cloudAnchorTaskInProgress) throw IllegalStateException("The task is already in progress")

        if (anchor == null) throw IllegalStateException("The anchor shouldn't be null")

        anchor = arSession?.hostCloudAnchorWithTtl(anchor, ttlDays)
        cloudAnchorTaskInProgress = true
        onCloudAnchorTaskCompleted = onTaskCompleted
    }

    /**
     * ### Resolves a Cloud Anchor
     *
     * The [anchor] is replaced with a new anchor returned by [Session.resolveCloudAnchor].
     *
     * @param cloudAnchorId The Cloud Anchor ID of the Cloud Anchor.
     * @param onTaskCompleted Called when the task completes successfully or with an error.
     */
    fun resolveCloudAnchor(cloudAnchorId: String, onTaskCompleted: (anchor: Anchor, success: Boolean) -> Unit) {
        if (cloudAnchorTaskInProgress) throw IllegalStateException("The task is already in progress")

        anchor = arSession?.resolveCloudAnchor(cloudAnchorId)
        cloudAnchorTaskInProgress = true
        onCloudAnchorTaskCompleted = onTaskCompleted
    }

    /**
     * ### Cancels a resolve task
     *
     * The [anchor] is detached to cancel the resolve task.
     */
    fun cancelCloudAnchorResolveTask() {
        if (cloudAnchorTaskInProgress) {
            anchor?.detach()
            cloudAnchorTaskInProgress = false
            onCloudAnchorTaskCompleted = null
        }
    }

    /**
     * ### Creates a new anchored Node at the actual worldPosition and worldRotation
     *
     * The returned node position and rotation will be fixed within camera movements.
     *
     * See [ArFrame.hitTest] and [ArFrame.hitTests] for details.
     *
     * Anchors incur ongoing processing overhead within ARCore.
     * To release unneeded anchors use [destroy].
     */
    open fun createAnchoredNode(): ArNode? {
        return createAnchor()?.let { anchor ->
            ArNode(anchor)
        }
    }

    /**
     * TODO: Doc
     */
    open fun createAnchoredCopy(): ArNode? {
        return createAnchoredNode()?.apply {
            copy(this)
        }
    }

    override fun destroy() {
        super.destroy()

        anchor?.detach()
        anchor = null

        cloudAnchorTaskInProgress = false
        onCloudAnchorTaskCompleted = null
    }

    //TODO : Move all those functions

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
//    fun localToWorldDirection(direction: Vector3) =
//        Quaternion.rotateVector(worldQuaternion, direction)

    /**
     * ### Converts a direction from world-space to the local-space of this node.
     *
     * Not impacted by the position or scale of the node.
     *
     * @param direction the direction in world-space to convert
     * @return a new vector that represents the direction in local-space
     */
//    fun worldToLocalDirection(direction: Vector3) =
//        Quaternion.inverseRotateVector(worldQuaternion, direction)

    /** ### Gets the world-space forward direction vector (-z) of this node */
//    val worldForward get() = localToWorldDirection(Vector3.forward())

    /** ### Gets the world-space back direction vector (+z) of this node */
//    val worldBack get() = localToWorldDirection(Vector3.back())

    /** ### Gets the world-space right direction vector (+x) of this node */
//    val worldRight get() = localToWorldDirection(Vector3.right())

    /** ### Gets the world-space left direction vector (-x) of this node */
//    val worldLeft get() = localToWorldDirection(Vector3.left())

    /** ### Gets the world-space up direction vector (+y) of this node */
//    val worldUp get() = localToWorldDirection(Vector3.up())

    /** ### Gets the world-space down direction vector (-y) of this node */
//    val worldDown get() = localToWorldDirection(Vector3.down())

    override fun clone() = copy(ArNode())

    fun copy(toNode: ArNode = ArNode()) = toNode.apply {
        super.copy(toNode)

        placementMode = this@ArNode.placementMode
    }

    companion object {
        val DEFAULT_PLACEMENT_MODE = PlacementMode.BEST_AVAILABLE
    }
}

enum class EditableTransform {
    POSITION, ROTATION, SCALE;

    companion object {
        val ALL = setOf(POSITION, ROTATION, SCALE)
        val NONE = setOf<EditableTransform>()
    }
}

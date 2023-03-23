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

/**
 * ### Construct a new placement AR Node
 */
open class ArNode : ModelNode, ArSceneLifecycleObserver {

    override val sceneView: ArSceneView? get() = super.sceneView as? ArSceneView
    override val lifecycle: ArSceneLifecycle? get() = sceneView?.lifecycle
    protected val arSession: ArSession? get() = sceneView?.arSession

    /**
     * ### Move smoothly/slowly when there is a pose (AR position and rotation) update
     *
     * Use [smoothSpeed] to adjust the position and rotation change smoothness level
     */
    var isSmoothPoseEnable = true

    /**
     * ### Should the [ArNode.position] be updated with the ARCore detected [Pose]
     *
     * Use this parameter if you want to keep a static position and let it like it was initially
     * defined without being influenced by the ARCore [Trackable] retrieved value.
     */
    var applyPosePosition = true

    /**
     * ### Should the [ArNode.rotation] be updated with the ARCore detected [Pose]
     *
     * Use this parameter if you want to keep a static rotation and let it like it was initially
     * defined without being influenced by the ARCore [Trackable] retrieved value.
     */
    var applyPoseRotation = false

    /**
     * ### Adjust the anchor pose update interval in seconds
     *
     * ARCore may update the [anchor] pose because of environment detection evolving during time.
     * You can choose to retrieve more accurate anchor position and rotation or let it as it was
     * when it was anchored.
     * Only used when the [ArNode] is anchored.
     * `null` means never update the pose
     */
    var anchorPoseUpdateInterval: Double? = 0.1
    var anchorUpdatedFrame: ArFrame? = null
        private set

    /**
     * ### The position of the intersection between a ray and detected real-world geometry.
     *
     * The position is the location in space where the ray intersected the geometry.
     * The orientation is a best effort to face the user's device, and its exact definition differs
     * depending on the Trackable that was hit.
     *
     * - [Plane]: X+ is perpendicular to the cast ray and parallel to the plane, Y+ points along the
     * plane normal (up, for [Plane.Type.HORIZONTAL_UPWARD_FACING] planes), and Z+ is parallel to
     * the plane, pointing roughly toward the user's device.
     *
     * - [Point]: Attempt to estimate the normal of the surface centered around the hit test.
     * Surface normal estimation is most likely to succeed on textured surfaces and with camera
     * motion. If [Point.getOrientationMode] returns
     * [Point.OrientationMode.ESTIMATED_SURFACE_NORMAL], then X+ is perpendicular to the cast ray
     * and parallel to the physical surface centered around the hit test, Y+ points along the
     * estimated surface normal, and Z+ points roughly toward the user's device.
     * If [Point.getOrientationMode] returns [Point.OrientationMode.INITIALIZED_TO_IDENTITY], then
     * X+ is perpendicular to the cast ray and points right from the perspective of the user's
     * device, Y+ points up, and Z+ points roughly toward the user's device.
     *
     * - If you wish to retain the location of this pose beyond the duration of a single frame,
     * create an [Anchor] using [createAnchor] to save the pose in a physically consistent way.
     *
     * @see createAnchor
     */
    open var pose: Pose? = null
        set(value) {
            if (field?.transform != value?.transform) {
                field = value
                if (value != null) {
                    val posePosition = value.takeIf { applyPosePosition }?.position
                        ?: position
                    val poseQuaternion = value.takeIf { applyPoseRotation }?.quaternion
                        ?: quaternion
                    if (position != posePosition || quaternion != poseQuaternion) {
                        transform(posePosition, poseQuaternion, smooth = isSmoothPoseEnable)
                    }
                } else {
                    // Should we move back the node to the default position
//                    transform(DEFAULT_POSITION, DEFAULT_QUATERNION, smooth = isSmoothPoseEnable)
                }
                onPoseChanged?.invoke(value)
            }
        }

    /**
     * TODO : Doc
     */
    open var anchor: Anchor? = null
        set(value) {
            field?.detach()
            field = value
            pose = value?.pose
            onAnchorChanged?.invoke(value)
        }

    /**
     * TODO : Doc
     */
    val isAnchored get() = anchor != null

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

    /** ## Deprecated: Use [onPoseChanged] and [isTracking] */
    @Deprecated(
        "Replaced by onPoseChanged",
        replaceWith = ReplaceWith("onPoseChanged"),
        DeprecationLevel.ERROR
    )
    var onTrackingChanged: ((isTracking: Boolean) -> Unit)? = null

    var onPoseChanged: ((pose: Pose?) -> Unit)? = null

    var onAnchorChanged: ((anchor: Anchor?) -> Unit)? = null

    var isCameraTracking = false
        private set(value) {
            if (field != value) {
                field = value
                updateVisibility()
            }
        }

    private var onCloudAnchorTaskCompleted: ((anchor: Anchor, success: Boolean) -> Unit)? = null

    override var isSelectable = true

    override var isRotationEditable: Boolean = true
    override var isScaleEditable: Boolean = true

    override val isVisibleInHierarchy: Boolean
        get() = super.isVisibleInHierarchy && isCameraTracking

    constructor() : super() {
    }

    constructor(anchor: Anchor) : this() {
        this.anchor = anchor
    }

    override fun onArFrame(arFrame: ArFrame) {
        super.onArFrame(arFrame)

        isCameraTracking = arFrame.camera.isTracking

        val anchor = anchor ?: return

        // Update the anchor position if any
        if (anchor.trackingState == TrackingState.TRACKING) {
            if (anchorPoseUpdateInterval != null
                && arFrame.intervalSeconds(anchorUpdatedFrame) >= anchorPoseUpdateInterval!!
            ) {
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                pose = anchor.pose
                anchorUpdatedFrame = arFrame
            }
        }

        if (cloudAnchorTaskInProgress) {
            // Call the listener when the task completes successfully or with an error
            if (cloudAnchorState != CloudAnchorState.NONE &&
                cloudAnchorState != CloudAnchorState.TASK_IN_PROGRESS
            ) {
                cloudAnchorTaskInProgress = false
                onCloudAnchorTaskCompleted?.invoke(
                    anchor,
                    cloudAnchorState == CloudAnchorState.SUCCESS
                )
                onCloudAnchorTaskCompleted = null
            }
        }
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
    fun hostCloudAnchor(
        ttlDays: Int = 1,
        onTaskCompleted: (anchor: Anchor, success: Boolean) -> Unit
    ) {
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
    fun resolveCloudAnchor(
        cloudAnchorId: String,
        onTaskCompleted: (anchor: Anchor, success: Boolean) -> Unit
    ) {
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

    override fun destroy() {
        anchor?.detach()
        anchor = null

        cloudAnchorTaskInProgress = false
        onCloudAnchorTaskCompleted = null

        super.destroy()
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
    }
}

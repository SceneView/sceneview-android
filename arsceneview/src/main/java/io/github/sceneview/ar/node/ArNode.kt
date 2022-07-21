package io.github.sceneview.ar.node

import com.google.ar.core.*
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.core.Config.PlaneFindingMode
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.*
import io.github.sceneview.*
import io.github.sceneview.ar.ArSceneLifecycle
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.NodeMotionEvent
import io.github.sceneview.node.ModelNode

/**
 * ### Construct a new placement AR Node
 */
open class ArNode : ModelNode, ArSceneLifecycleObserver {

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
    var isSmoothPoseEnable = true

    /**
     * ### Adjust the anchor pose update interval in seconds
     *
     * ARCore may update the [anchor] pose because of environment detection evolving during time.
     * You can choose to retrieve more accurate anchor position and rotation or let it as it was
     * when it was anchored.
     * Only used when the [ArNode] is anchored.
     * `null` means never update the pose
     */
    var anchorPoseUpdateInterval: Double? = null
    var anchorUpdateFrame: ArFrame? = null
        private set

    open var hitResult: HitResult? = null
        set(value) {
            field = value
            onHitResult(value)
        }

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
                onPoseChanged(value)
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

    /** ## Deprecated: Use [onPoseChanged] and [isTracking] */
    @Deprecated(
        "Replaced by onPoseChanged",
        replaceWith = ReplaceWith("onPoseChanged"),
        DeprecationLevel.ERROR
    )
    var onTrackingChanged: ((node: ArNode, isTracking: Boolean, pose: Pose?) -> Unit)? = null

    var onHitResult: ((node: ArNode, hitResult: HitResult?) -> Unit)? = null

    var onPoseChanged: ((node: ArNode, pose: Pose?) -> Unit)? = null

    var onAnchorChanged: ((node: ArNode, anchor: Anchor?) -> Unit)? = null

    private var onCloudAnchorTaskCompleted: ((anchor: Anchor, success: Boolean) -> Unit)? = null

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

    override var isSelectable = true

    override var isPositionEditable: Boolean = true
    override var isRotationEditable: Boolean = true
    override var isScaleEditable: Boolean = true

    constructor(placementMode: PlacementMode = DEFAULT_PLACEMENT_MODE) {
        this.placementMode = placementMode
    }

    constructor(anchor: Anchor) : this() {
        this.anchor = anchor
    }

    override fun onArFrame(arFrame: ArFrame) {
        val anchor = anchor ?: return

        // Update the anchor position if any
        if (anchor.trackingState == TrackingState.TRACKING) {
            if (anchorPoseUpdateInterval != null
                && arFrame.intervalSeconds(anchorUpdateFrame) >= anchorPoseUpdateInterval!!
            ) {
                pose = anchor.pose
                anchorUpdateFrame = arFrame
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
     * TODO : Doc
     */
    open fun onHitResult(hitResult: HitResult?) {
        // Keep the last pose when not tracking result?
//        if (hitResult?.isTracking == true) {
        pose = hitResult?.hitPose
//        }
        onHitResult?.invoke(this, hitResult)
    }

    /**
     * TODO : Doc
     */
    open fun onPoseChanged(pose: Pose?) {
        if (pose != null) {
            val position = pose.takeIf { !placementMode.keepPosition }?.position
                ?: this.position
            val quaternion = pose.takeIf { !placementMode.keepRotation }?.quaternion
                ?: this.quaternion
            if (position != this.position || quaternion != this.quaternion) {
                transform(position, quaternion, smooth = isSmoothPoseEnable)
            }
        } else {
            // Should we move back the node the default position or move it to the default
            // transform?
//            transform(DEFAULT_POSITION, DEFAULT_QUATERNION, smooth = isSmoothPoseEnable)
        }
        onPoseChanged?.invoke(this, pose)
    }

    /**
     * TODO : Doc
     */
    open fun onAnchorChanged(anchor: Anchor?) {
        pose = anchor?.pose

        onAnchorChanged?.invoke(this, anchor)
    }

    override fun onMoveBegin(detector: MoveGestureDetector, e: NodeMotionEvent) {
        super.onMoveBegin(detector, e)

        if (isPositionEditable && currentEditedTransform == null) {
            currentEditedTransform = ::position
            detachAnchor()
        }
    }

    override fun onMove(detector: MoveGestureDetector, e: NodeMotionEvent) {
        super.onMove(detector, e)

        if (isPositionEditable && currentEditedTransform == ::position) {
            hitResult = hitTest(xPx = e.motionEvent.x, yPx = e.motionEvent.y)
        }
    }

    override fun onMoveEnd(detector: MoveGestureDetector, e: NodeMotionEvent) {
        super.onMoveEnd(detector, e)

        if (isPositionEditable && currentEditedTransform == ::position) {
            anchor()
            currentEditedTransform = null
        }
    }

    /**
     * ### Performs a ray cast to retrieve the ARCore info at this camera point
     *
     * @param frame the [ArFrame] from where we take the [HitResult]
     * By default the latest session frame if any exist
     * @param xPx x view coordinate in pixels
     * By default the [cameraPosition.x][placementPosition] of this Node is used
     * @property yPx y view coordinate in pixels
     * By default the [cameraPosition.y][placementPosition] of this Node is used
     *
     * @return the hitResult or null if no info is retrieved
     *
     * @see ArFrame.hitTest
     */
    fun hitTest(
        frame: ArFrame? = arSession?.currentFrame,
        xPx: Float,
        yPx: Float,
        approximateDistanceMeters: Float = placementMode.instantPlacementDistance,
        plane: Boolean = placementMode.planeEnabled,
        depth: Boolean = placementMode.depthEnabled,
        instantPlacement: Boolean = placementMode.instantPlacementEnabled
    ): HitResult? =
        frame?.hitTest(xPx, yPx, approximateDistanceMeters, plane, depth, instantPlacement)

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
    open fun createAnchor(): Anchor? {
        return hitResult?.let { createAnchor(it) }
    }

    fun createAnchor(hitResult: HitResult): Anchor? {
        return hitResult.takeIf {
            // Try to anchor if hit result is not tracking?
//            it.isTracking
            true
        }?.createAnchor()
    }

    /**
     * ### Anchor this node to make it fixed at the actual position and orientation is the world
     *
     * Creates an anchor at the given pose in the world coordinate space that is attached to this
     * trackable. The type of trackable will determine the semantics of attachment and how the
     * anchor's pose will be updated to maintain this relationship. Note that the relative offset
     * between the pose of multiple anchors attached to a trackable may adjust slightly over time as
     * ARCore updates its model of the world.
     *
     * @return the created anchor or `null`if the node could not be anchored
     */
    open fun anchor(): Anchor? {
        anchor = createAnchor()
        return anchor
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
        placementMode = this@ArNode.placementMode
    }

    companion object {
        val DEFAULT_PLACEMENT_MODE = PlacementMode.BEST_AVAILABLE
        val DEFAULT_PLACEMENT_DISTANCE = 2.0f
    }
}

/**
 * # How an object is placed on the real world
 *
 * @param instantPlacementDistance Distance in meters at which to create an InstantPlacementPoint.
 * This is only used while the tracking method for the returned point is InstantPlacementPoint.
 * @param instantPlacementFallback Fallback to instantly place nodes at a fixed orientation and an
 * approximate distance when the base placement type is not available yet or at all.
 * @param keepPosition Should the [ArNode.position] be updated with the ARCore detected [Pose].
 * Use this parameter if you want to keep a static position and let it like it was initially
 * defined without being influenced by the ARCore [Trackable] retrieved value.
 * @param keepRotation Should the [ArNode.rotation] be updated with the ARCore detected [Pose].
 * Use this parameter if you want to keep a static rotation and let it like it was initially
 * defined without being influenced by the ARCore [Trackable] retrieved value.
 */

enum class PlacementMode(
    var instantPlacementDistance: Float = ArNode.DEFAULT_PLACEMENT_DISTANCE,
    var instantPlacementFallback: Boolean = false,
    var keepPosition: Boolean = false,
    var keepRotation: Boolean = false
) {
    /**
     * ### Disable every AR placement
     * @see PlaneFindingMode.DISABLED
     */
    DISABLED,

    /**
     * ### Place and orientate nodes only on horizontal planes
     * @see PlaneFindingMode.HORIZONTAL
     */
    PLANE_HORIZONTAL,

    /**
     * ### Place and orientate nodes only on vertical planes
     * @see PlaneFindingMode.VERTICAL
     */
    PLANE_VERTICAL,

    /**
     * ### Place and orientate nodes on both horizontal and vertical planes
     * @see PlaneFindingMode.HORIZONTAL_AND_VERTICAL
     */
    PLANE_HORIZONTAL_AND_VERTICAL,

    /**
     * ### Place and orientate nodes on every detected depth surfaces
     *
     * Not all devices support this mode. In case on non depth enabled device the placement mode
     * will automatically fallback to [PLANE_HORIZONTAL_AND_VERTICAL].
     * @see Config.DepthMode.AUTOMATIC
     */
    DEPTH,

    /**
     * ### Instantly place only nodes at a fixed orientation and an approximate distance
     *
     * No AR orientation will be provided = fixed +Y pointing upward, against gravity
     *
     * This mode is currently intended to be used with hit tests against horizontal surfaces.
     * Hit tests may also be performed against surfaces with any orientation, however:
     * - The resulting Instant Placement point will always have a pose with +Y pointing upward,
     * against gravity.
     * - No guarantees are made with respect to orientation of +X and +Z. Specifically, a hit
     * test against a vertical surface, such as a wall, will not result in a pose that's in any
     * way aligned to the plane of the wall, other than +Y being up, against gravity.
     * - The [InstantPlacementPoint]'s tracking method may never become
     * [InstantPlacementPoint.TrackingMethod.FULL_TRACKING] } or may take a long time to reach
     * this state. The tracking method remains
     * [InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE] until a
     * (tiny) horizontal plane is fitted at the point of the hit test.
     */
    INSTANT,

    /**
     * ### Place nodes on every detected surfaces
     *
     * The node will be placed instantly and then adjusted to fit the best accurate, precise,
     * available placement.
     */
    BEST_AVAILABLE(instantPlacementFallback = true);

    val planeEnabled: Boolean
        get() = when (planeFindingMode) {
            PlaneFindingMode.HORIZONTAL,
            PlaneFindingMode.VERTICAL,
            PlaneFindingMode.HORIZONTAL_AND_VERTICAL -> true
            else -> false
        }

    val planeFindingMode: PlaneFindingMode
        get() = when (this) {
            PLANE_HORIZONTAL -> PlaneFindingMode.HORIZONTAL
            PLANE_VERTICAL -> PlaneFindingMode.VERTICAL
            PLANE_HORIZONTAL_AND_VERTICAL,
            BEST_AVAILABLE -> PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            else -> PlaneFindingMode.DISABLED
        }

    val depthEnabled: Boolean
        get() = when (this) {
            DEPTH, BEST_AVAILABLE -> true
            else -> false
        }

    val instantPlacementEnabled: Boolean
        get() = when {
            this == INSTANT || instantPlacementFallback -> true
            else -> false
        }
}

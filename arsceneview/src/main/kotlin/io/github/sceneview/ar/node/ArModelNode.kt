package io.github.sceneview.ar.node

import dev.romainguy.kotlin.math.*
import com.google.ar.core.*
import com.google.ar.core.Config.PlaneFindingMode
import io.github.sceneview.*
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.node.ModelNode
import io.github.sceneview.utils.Position

/**
 * ### AR positioned 3D model node
 *
 * This [Node] follows the actual ARCore detected orientation and position at the provided relative
 * X, Y location in the [ArSceneView]
 *
 * You can:
 * - [anchor] this node at any time to make it fixed at the actual position and rotation.
 * This node will stop following the hitPostion to stay in place.
 * - [createAnchor] in order to extract a fixed/anchored copy of the actual node.
 * This node will continue following the [com.google.ar.core.Camera]
 */
open class ArModelNode : ArNode, ArSceneLifecycleObserver {

    companion object {
        val defaultPlacementPosition get() = Position(0.0f, 0.0f, -2.0f)
        val defaultPlacementMode get() = PlacementMode.BEST_AVAILABLE
    }

    /**
     * ### The node camera/screen position
     *
     * - While there is no AR tracking information available, the node is following the camera moves
     * so it stays at this camera/screen relative position [com.google.ar.sceneform.Camera] node is
     * considered as the parent
     * - ARCore will try to find the real world position of this screen position and the node
     * [worldPosition] will be updated so.
     *
     * The Z value is only used when no surface is actually detected or when instant placement is
     * enabled:
     * - In case of instant placement disabled, the z position will be estimated by the AR surface
     * distance at the (x,y) so this value is not used.
     * - In case of instant placement enabled, this value is used as
     * [approximateDistanceMeters][ArFrame.hitTest] to help ARCore positioning result.
     *
     * By default, the node is positioned at the center screen, 2 meters forward
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
    var placementPosition: Position = defaultPlacementPosition
        set(value) {
            field = value
            position = Float3(value)
        }

    var placementMode: PlacementMode = defaultPlacementMode
        set(value) {
            field = value
            doOnAttachedToScene { sceneView ->
                (sceneView as? ArSceneView)?.apply {
                    planeFindingMode = when (placementMode) {
                        PlacementMode.DISABLED, PlacementMode.INSTANT, PlacementMode.DEPTH -> PlaneFindingMode.DISABLED
                        // TODO: Don't limit whole config instead filter horizontal/vertical hitTests
                        PlacementMode.PLANE_HORIZONTAL -> PlaneFindingMode.HORIZONTAL
                        // TODO: Don't limit whole config instead filter horizontal/vertical hitTests
                        PlacementMode.PLANE_VERTICAL -> PlaneFindingMode.VERTICAL
                        else -> PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    }
                    depthEnabled = placementMode.depthEnabled
                    instantPlacementEnabled = placementMode.instantPlacementEnabled
                }
            }
        }

    /**
     * ## How precise must be the placement
     *
     * 1 = Full precision BUT may produce artifacts due to the average
     * 0 = The placement
     * The smooth rotation minimum change limit
     *
     * This is used to avoid very near rotations smooth modifications. It prevents the rotation to
     * appear too quick if the ranges are too close and uses linearly interpolation for upper dot
     * products.
     *
     * Expressed in quaternion dot product
     * This value is used by [smooth]
     */
    // TODO: add those vars
//    var precision = 0.1
//    var maxHitPerSeconds = 10

    var lastArFrame: ArFrame? = null
    var lastTrackedHitResult: HitResult? = null

    /**
     * TODO : Doc
     */
    var onArFrameHitResult: ((node: ArNode, hitResult: HitResult?, isTracking: Boolean) -> Unit)? =
        null

    constructor(
        /**
         * ### The node camera/screen position
         *
         * - While there is no AR tracking information available, the node is following the camera moves
         * so it stays at this camera/screen relative position [com.google.ar.sceneform.Camera] node is
         * considered as the parent)
         * - ARCore will try to find the real world position of this screen position and the node
         * [worldPosition] will be updated so.
         *
         * The Z value is only used when no surface is actually detected or when instant placement is
         * enabled:
         * - In case of instant placement disabled, the z position will be estimated by the AR surface
         * distance at the (x,y) so this value is not used.
         * - In case of instant placement enabled, this value is used as
         * [approximateDistanceMeters][ArFrame.hitTest] to help ARCore positioning result.
         *
         * By default, the node is positioned at the center screen, 2 meters forward
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
        cameraPosition: Position = defaultPlacementPosition,
        /**
         * TODO : Doc
         *
         * @see io.github.sceneview.ar.node.ArModelNode.placementMode
         */
        placementMode: PlacementMode = defaultPlacementMode,
    ) : super() {
        this.placementPosition = cameraPosition
        this.placementMode = placementMode
    }

    /**
     * TODO : Doc
     */
    constructor(hitResult: HitResult) : super(hitResult.createAnchor())

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
        frame: ArFrame? = session?.currentFrame,
        xPx: Float = (session?.displayWidth ?: 0) / 2.0f * (1.0f + placementPosition.x),
        yPx: Float = (session?.displayHeight ?: 0) / 2.0f * (1.0f - placementPosition.y),
        approximateDistanceMeters: Float = kotlin.math.abs(placementPosition.z),
        plane: Boolean = placementMode.planeEnabled,
        depth: Boolean = placementMode.depthEnabled,
        instantPlacement: Boolean = placementMode.instantPlacementEnabled
    ): HitResult? =
        frame?.hitTest(xPx, yPx, approximateDistanceMeters, plane, depth, instantPlacement)

    override fun onArFrame(arFrame: ArFrame) {
        super<ArNode>.onArFrame(arFrame)

        // TODO: Add this with precision vars
//        if (Duration.nanoseconds(
//                frame.timestamp - (lastArFrame?.timestamp ?: 0)
//            ) > Duration.seconds(1.0 / maxHitPerSeconds)
//        ) {
            if (anchor == null) {
                onArFrameHitResult(hitTest(arFrame))
            }
            lastArFrame = arFrame
//        }
    }

    open fun onArFrameHitResult(hitResult: HitResult?) {
        // Keep the last position when no tracking result
        if (hitResult?.isTracking == true) {
            lastTrackedHitResult = hitResult
            hitResult.hitPose?.let { hitPose ->
                // TODO : Handle precision in here
                this.pose = hitPose
            }
        }
        onArFrameHitResult?.invoke(this, hitResult, isTracking)
    }

    override fun createAnchor(): Anchor? {
        return hitTest()?.createAnchor() ?: if (lastTrackedHitResult?.isTracking == true) {
            lastTrackedHitResult?.createAnchor()
        } else null
    }

    override fun clone() = copy(ModelNode())

    fun copy(toNode: ArModelNode = ArModelNode()): ArModelNode = toNode.apply {
        super.copy(toNode)

        placementPosition = this@ArModelNode.placementPosition
        placementMode = this@ArModelNode.placementMode
    }
}

enum class PlacementMode {
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
     * No AR orientation will be provided = fixed +Y pointing upward, against gravity)
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
    BEST_AVAILABLE;

    val planeEnabled: Boolean
        get() = when (this) {
            PLANE_HORIZONTAL, PLANE_VERTICAL, DEPTH, BEST_AVAILABLE -> true
            else -> false
        }

    val depthEnabled: Boolean
        get() = when (this) {
            DEPTH, BEST_AVAILABLE -> true
            else -> false
        }

    val instantPlacementEnabled: Boolean
        get() = when (this) {
            INSTANT, BEST_AVAILABLE -> true
            else -> false
        }
}
package io.github.sceneview.ar.node

import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Config.PlaneFindingMode
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.defaultMaxFPS
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * ### AR positioned 3D model node
 *
 * This [io.github.sceneview.node.Node] follows the actual ARCore detected orientation and position
 * at the provided relative X, Y location in the [ArSceneView]
 *
 * You can:
 * - [anchor] this node at any time to make it fixed at the actual position and rotation.
 * This node will stop following the hitPostion to stay in place.
 * - [createAnchor] in order to extract a fixed/anchored copy of the actual node.
 * This node will continue following the [com.google.ar.core.Camera]
 */
open class ArModelNode : ArNode, ArSceneLifecycleObserver {

    companion object {
        val DEFAULT_PLACEMENT_POSITION = Position(0.0f, 0.0f, -2.0f)
        val DEFAULT_PLACEMENT_MODE = PlacementMode.BEST_AVAILABLE
    }

    /**
     * ### The node camera/screen position
     *
     * - While there is no AR tracking information available, the node is following the camera moves
     * so it stays at this camera/screen relative position [com.google.ar.sceneform.Camera] node is
     * considered as the parent
     * - ARCore will try to find the real world position of this screen position and the node
     * [io.github.sceneview.node.Node.worldPosition] will be updated so.
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
    var placementPosition: Position = DEFAULT_PLACEMENT_POSITION
        set(value) {
            field = value
            position = Float3(value)
        }

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
     * ### Anchor the node as soon as an AR position/rotation is found
     *
     * - `true` The node will be anchored in the real world at the first suitable place available.
     * Depending on your need, you can change the [placementMode] to adjust between a quick
     * ([PlacementMode.INSTANT]), more accurate ([PlacementMode.DEPTH]), only on planes/walls
     * ([PlacementMode.PLANE_HORIZONTAL], [PlacementMode.PLANE_VERTICAL],
     * [PlacementMode.PLANE_HORIZONTAL_AND_VERTICAL]) or auto refining accuracy
     * ([PlacementMode.BEST_AVAILABLE]) placement.
     * - `false` The node will follow its AR [pose] and update it constantly to follow the
     * camera [placementPosition] on the real world.
     */
    var autoAnchor = false
        set(value) {
            field = value
            if (value) {
                // Try to anchor the node. If it's return false, the anchor will be tried on each
                // frame until an anchor is founded
                anchor()
            } else if (isAnchored) {
                detachAnchor()
            }
        }

    /**
     * ### The maximum number of hit tests that can occur per second
     *
     * Increase this value for more precision or reduce it for higher performance and lower
     * energy consumption
     */
    var maxHitsPerSecond: Int = (defaultMaxFPS / 2.0f).toInt()

    var lastArFrame: ArFrame? = null
    var lastHitFrame: ArFrame? = null
    var lastHitResult: HitResult? = null

    /**
     * TODO : Doc
     */
    var onArFrameHitResult: ((node: ArNode, hitResult: HitResult?, isTracking: Boolean) -> Unit)? =
        null

    /**
     * ### Construct a new placement ArModelNode
     *
     * @param placementPosition See [ArModelNode.placementPosition]
     * @param placementMode See [ArModelNode.placementMode]
     */
    constructor(
        placementPosition: Position = DEFAULT_PLACEMENT_POSITION,
        placementMode: PlacementMode = DEFAULT_PLACEMENT_MODE,
    ) : super() {
        this.placementPosition = placementPosition
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

        if ((arFrame.timestamp - (lastHitFrame?.timestamp ?: 0)).nanoseconds >=
            (1.0 / maxHitsPerSecond).seconds
        ) {
            if (!isAnchored) {
                if (!autoAnchor || !anchor()) {
                    onArFrameHitResult(hitTest(arFrame))
                }
            }
            lastHitFrame = arFrame
        }
        lastArFrame = arFrame
    }

    open fun onArFrameHitResult(hitResult: HitResult?) {
        // Keep the last position when no tracking result
        if (hitResult?.isTracking == true) {
            lastHitResult = hitResult
            hitResult.hitPose?.let { hitPose ->
                pose = hitPose
            }
        }
        onArFrameHitResult?.invoke(this, hitResult, isTracking)
    }

    override fun createAnchor(): Anchor? {
        val hitResult = hitTest()
        return (hitResult?.takeIf { it.isTracking } ?: lastHitResult?.takeIf { it.isTracking }
        ?: hitResult ?: lastHitResult)
            ?.createAnchor()
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
    BEST_AVAILABLE;

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
        get() = when (this) {
            INSTANT, BEST_AVAILABLE -> true
            else -> false
        }
}
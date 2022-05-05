package io.github.sceneview.ar.node

import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.math.Position
import kotlin.math.abs

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
open class ArModelNode : ArNode {

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

    override var editableTransforms = setOf(EditableTransform.ROTATION, EditableTransform.SCALE)

    var poseUpdatePrecision = 1.0f

    var lastArFrame: ArFrame? = null
    var lastPoseUpdateFrame: ArFrame? = null
    var lastHitResult: HitResult? = null

    /**
     * TODO : Doc
     */
    var onArFrameHitResult: ((node: ArNode, hitResult: HitResult?, isTracking: Boolean) -> Unit)? =
        null

    /**
     * ### Construct a new placement ArModelNode
     *
     * @param placementMode See [ArModelNode.placementMode]
     * @param autoAnchor See [ArModelNode.autoAnchor]
     * @param placementPosition See [ArModelNode.placementPosition]
     */
    constructor(
        placementMode: PlacementMode = DEFAULT_PLACEMENT_MODE,
        autoAnchor: Boolean = false,
        placementPosition: Position = DEFAULT_PLACEMENT_POSITION
    ) : super() {
        this.placementMode = placementMode
        this.placementPosition = placementPosition
        this.autoAnchor = autoAnchor
    }

    /**
     * TODO : Doc
     */
    constructor(hitResult: HitResult) : super(hitResult.createAnchor())

    override fun onArFrame(arFrame: ArFrame) {
        super.onArFrame(arFrame)

        if (!isAnchored) {
            if (!autoAnchor || !anchor()) {
                if (arFrame.precision(lastPoseUpdateFrame) <= poseUpdatePrecision) {
                    lastPoseUpdateFrame = arFrame
                    onArFrameHitResult(hitTest(arFrame))
                }
            }
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
        approximateDistanceMeters: Float = abs(placementPosition.z),
        plane: Boolean = placementMode.planeEnabled,
        depth: Boolean = placementMode.depthEnabled,
        instantPlacement: Boolean = placementMode.instantPlacementEnabled
    ): HitResult? = super.hitTest(
        frame = frame,
        xPx = (arSession?.displayWidth ?: 0) / 2.0f * (1.0f + placementPosition.x),
        yPx = (arSession?.displayHeight ?: 0) / 2.0f * (1.0f - placementPosition.y),
        approximateDistanceMeters = approximateDistanceMeters,
        plane = plane,
        depth = depth,
        instantPlacement = instantPlacement
    )

    override fun createAnchor(): Anchor? {
        return (hitTest()?.takeIf { it.isTracking }
            ?: lastHitResult?.takeIf { it.isTracking })
            ?.let { createAnchor(it) }
    }

    fun createAnchor(hitResult: HitResult): Anchor? {
        return hitResult.takeIf { it.isTracking }?.createAnchor()
    }

    override fun clone() = copy(ArModelNode())

    fun copy(toNode: ArModelNode = ArModelNode()) = toNode.apply {
        super.copy(toNode)

        placementPosition = this@ArModelNode.placementPosition
    }

    companion object {
        val DEFAULT_PLACEMENT_POSITION =
            Position(0.0f, 0.0f, -DEFAULT_PLACEMENT_MODE.instantPlacementDistance)
    }
}
package io.github.sceneview.ar.node

import com.google.ar.core.*
import com.google.ar.core.Config.PlaneFindingMode
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.NodeMotionEvent
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance

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
     * ### The node camera/screen/view position where the hit will be made to find an AR position
     *
     * Until it is anchored, the Node will try to find the real world position/orientation of
     * the screen coordinate and constantly place/orientate himself accordingly if
     * [followHitPosition] is `true`
     *
     * The Z value is only used when no surface is actually detected or when [followHitPosition] and
     * [instantAnchor] is set to `false` or when instant placement is enabled:
     * - In case of instant placement disabled, the z position (distance from the camera) will be
     * estimated by the AR surface distance at the (x,y) so this value is not used.
     * - In case of instant placement enabled, this value is used as an approximateDistanceMeters
     * at the [ArFrame.hitTest] call to help ARCore positioning the result quickly.
     *
     * By default, the node is positioned at the center screen, 4 meters forward
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
     *
     * @see followHitPosition
     */
    var hitPosition: Position = DEFAULT_HIT_POSITION
        set(value) {
            field = value
            position = Position(value)
        }

    /**
     * ### Make the node follow the camera/screen matching real world positions
     *
     * Controls if an unanchored node should be moved together with the camera.
     *
     * The node [position] is updated with the realtime ARCore [pose] at the corresponding
     * [hitPosition] until it is anchored ([isAnchored]) or until this this value is set to false.
     *
     * - While there is no AR tracking information available, the node is following the camera moves
     * so it stays at this camera/screen relative position but without adjusting its position and
     * orientation to the real world
     *
     * - Then ARCore will try to find the real world position of the node at the [hitPosition] by
     * looking at its [hitTest] on each [onArFrame].
     *
     * - In case of instant placement disabled, the z position (distance from the camera) will be
     * estimated by the AR surface distance at the (x,y).
     *
     * - The node rotation will be also adjusted in case of [PlacementMode.DEPTH] or depending on
     * the detected planes orientations in case of [PlacementMode.PLANE_HORIZONTAL],
     * [PlacementMode.PLANE_VERTICAL], [PlacementMode.PLANE_HORIZONTAL_AND_VERTICAL]
     *
     * HitTest might be consuming. Disable it if your don't need to retrieve the position on each
     * frame on reduce the [maxHitTestPerSecond]
     *
     * Disable it if you manage the positioning by yourself or other AR methods (Augmented Images,
     * Augmented Faces,...)
     *
    - - `true` An unanchored node is moved as the camera moves (cursor placement)
     * - `false` The pose of an unanchored node is not updated. This is used e.g. while moving a
     *  node using gestures.
     *
     * @see hitPosition
     */
    var followHitPosition: Boolean = true

    /**
     * ### Anchor the node as soon as an AR position/rotation is found/available
     *
     * If `true`, the node will be anchored in the real world at the first suitable place available.
     * Depending on your need, you can change the [placementMode] to adjust between a quick
     * ([PlacementMode.INSTANT]), more accurate ([PlacementMode.DEPTH]), only on planes/walls
     * ([PlacementMode.PLANE_HORIZONTAL], [PlacementMode.PLANE_VERTICAL],
     * [PlacementMode.PLANE_HORIZONTAL_AND_VERTICAL]) or auto refining accuracy
     * ([PlacementMode.BEST_AVAILABLE]) placement.
     */
    var instantAnchor = false
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

    override var isPositionEditable: Boolean = true

    var lastTrackingHitResult: HitResult? = null

    open var hitResult: HitResult? = null
        set(value) {
            field = value
            // Keep the last pose when not tracking result
            if (value?.isTracking == true) {
                lastTrackingHitResult = value
                pose = value.hitPose
            }
            value?.let { onHitResult?.invoke(this, it) }
        }

    /**
     * ### Adjust the max screen [ArFrame.hitTest] number per seconds
     *
     * When this node is not anchored and [followHitPosition] is set to true, the node will
     * constantly try to find its place in real world. Following the [hitPosition] on the
     * screen.
     *
     * Decrease if you don't need a very precise position update and want to reduce frame
     * consumption.
     * Increase for a more accurate positioning update.
     */
    var maxHitTestPerSecond: Int = 10

    var arFrame: ArFrame? = null
        private set
    var hitTestFrame: ArFrame? = null
        private set

    open val isTracking get() = hitResult?.isTracking == true

    var onHitResult: ((node: ArModelNode, hitResult: HitResult) -> Unit)? = null

    /**
     * ### Create an AR positioned 3D model node
     *
     * @param placementMode See [placementMode]
     * @param hitPosition See [hitPosition]
     * @param followHitPosition See [followHitPosition]
     * @param instantAnchor See [instantAnchor]
     */
    constructor(
        placementMode: PlacementMode = DEFAULT_PLACEMENT_MODE,
        hitPosition: Position = DEFAULT_HIT_POSITION,
        followHitPosition: Boolean = true,
        instantAnchor: Boolean = false
    ) : super() {
        this.placementMode = placementMode
        this.hitPosition = hitPosition
        this.followHitPosition = followHitPosition
        this.instantAnchor = instantAnchor
    }

    /**
     * TODO : Doc
     */
    constructor(hitResult: HitResult) : this() {
        this.anchor = hitResult.createAnchor()
    }

    /**
     * ### Create the Node and load a monolithic binary glTF and add it to the Node
     *
     * @param modelGlbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param scaleToUnits Scale the model to fit a unit cube. Default `null` to keep model original
     * size
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     *
     * @see loadModelGlb
     */
    constructor(
        modelGlbFileLocation: String,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((modelInstance: ModelInstance) -> Unit)? = null
    ) : this() {
        loadModelGlbAsync(
            modelGlbFileLocation,
            autoAnimate,
            scaleToUnits,
            centerOrigin,
            onError,
            onLoaded
        )
    }

    /**
     * ### Create the Node and load a monolithic binary glTF and add it to the Node
     *
     * @param modelInstance the model instance
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param scaleToUnits Scale the model to fit a unit cube. Default `null` to keep model original
     * size
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     */
    constructor(
        modelInstance: ModelInstance,
        autoAnimate: Boolean = true,
        scaleToUnits: Float? = null,
        centerOrigin: Position? = null
    ) : this() {
        setModelInstance(modelInstance, autoAnimate, scaleToUnits, centerOrigin)
    }

    override fun onArFrame(arFrame: ArFrame) {
        super.onArFrame(arFrame)

        if (!isAnchored) {
            // Try to find an anchor if none has been found
            if (instantAnchor) {
                anchor()
            } else if (
                followHitPosition &&
                // Disable hit placement when a gesture is being made
                currentEditingTransform == null &&
                // Add condition in here if a new feature needs hit result
                arFrame.fps(hitTestFrame) <= maxHitTestPerSecond
            ) {
                hitTestFrame = arFrame
                hitResult = hitTest()
            }
        }
        this.arFrame = arFrame
    }

    override fun onMoveBegin(detector: MoveGestureDetector, e: NodeMotionEvent) {
        super.onMoveBegin(detector, e)

        if (isPositionEditable && currentEditingTransform == null) {
            currentEditingTransform = ::position
            detachAnchor()
        }
    }

    override fun onMove(detector: MoveGestureDetector, e: NodeMotionEvent) {
        super.onMove(detector, e)

        if (isPositionEditable && currentEditingTransform == ::position) {
            hitResult = sceneView?.hitTest(
                xPx = e.motionEvent.x,
                yPx = e.motionEvent.y,
                plane = placementMode.planeEnabled,
                depth = placementMode.depthEnabled,
                instant = placementMode.instantPlacementEnabled
            )
        }
    }

    override fun onMoveEnd(detector: MoveGestureDetector, e: NodeMotionEvent) {
        super.onMoveEnd(detector, e)

        if (isPositionEditable && currentEditingTransform == ::position) {
            anchor = lastTrackingHitResult?.createAnchor()
            currentEditingTransform = null
        }
    }

    /**
     * ### Performs a ray cast to retrieve the ARCore info at this camera point
     *
     * @param frame the [ArFrame] from where we take the [HitResult]
     * By default the latest session frame if any exist
     * @param xPx x view coordinate in pixels
     * By default the [cameraPosition.x][hitPosition] of this Node is used
     * @param yPx y view coordinate in pixels
     * By default the [cameraPosition.y][hitPosition] of this Node is used
     *
     * @return the hitResult or null if no info is retrieved
     *
     * @see ArFrame.hitTest
     */
    fun hitTest(
        plane: Boolean = placementMode.planeEnabled,
        depth: Boolean = placementMode.depthEnabled,
        instant: Boolean = placementMode.instantPlacementEnabled,
    ): HitResult? = sceneView?.hitTest(hitPosition, plane, depth, instant)

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
        // lastTrackingHitResult might become not tracking anymore during time
        val hitResult =
            lastTrackingHitResult?.takeIf { it.isTracking } ?: hitTest()?.takeIf { it.isTracking }
        return hitResult?.createAnchor()
    }

    override fun clone() = copy(ArModelNode())

    fun copy(toNode: ArModelNode = ArModelNode()) = toNode.apply {
        super.copy(toNode)

        placementMode = this@ArModelNode.placementMode
        hitPosition = this@ArModelNode.hitPosition
    }

    companion object {
        val DEFAULT_PLACEMENT_MODE = PlacementMode.BEST_AVAILABLE
        val DEFAULT_PLACEMENT_DISTANCE = 2.0f
        val DEFAULT_HIT_POSITION =
            Position(0.0f, 0.0f, -DEFAULT_PLACEMENT_DISTANCE)
    }
}

/**
 * # How an object is placed on the real world
 *
 * @param instantPlacementDistance Distance in meters at which to create an InstantPlacementPoint.
 * This is only used while the tracking method for the returned point is InstantPlacementPoint.
 * @param instantPlacementFallback Fallback to instantly place nodes at a fixed orientation and an
 * approximate distance when the base placement type is not available yet or at all.
 */

enum class PlacementMode(
    var instantPlacementDistance: Float = ArModelNode.DEFAULT_PLACEMENT_DISTANCE,
    var instantPlacementFallback: Boolean = false
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
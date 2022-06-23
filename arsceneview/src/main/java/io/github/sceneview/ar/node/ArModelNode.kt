package io.github.sceneview.ar.node

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.math.Position
import io.github.sceneview.node.Node
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
     * ### The node camera/screen/view position where the hit will be made to find an AR position
     *
     * The Node will try to find the real world position/orientation of this screen coordinate and
     * and place himself accordingly.
     *
     * The Z value is only used when no surface is actually detected or when [followHitPosition] and
     * [instantAnchor] is set to false or when instant placement is enabled:
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
     * - `true` The node will be anchored in the real world at the first suitable place available.
     * Depending on your need, you can change the [placementMode] to adjust between a quick
     * ([PlacementMode.INSTANT]), more accurate ([PlacementMode.DEPTH]), only on planes/walls
     * ([PlacementMode.PLANE_HORIZONTAL], [PlacementMode.PLANE_VERTICAL],
     * [PlacementMode.PLANE_HORIZONTAL_AND_VERTICAL]) or auto refining accuracy
     * ([PlacementMode.BEST_AVAILABLE]) placement.
     * - `false` The node will follow its AR [pose] and update it constantly to follow the
     * camera [hitPosition] on the real world.
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

    override var editableTransforms = setOf(EditableTransform.ROTATION, EditableTransform.SCALE)

    var arFrame: ArFrame? = null
        private set
    var hitTestFrame: ArFrame? = null
        private set

    var hitResult: HitResult? = null
        private set(value) {
            field = value
            if (followHitPosition &&
                // Keep the last position when no tracking result
                value?.isTracking == true
            ) {
                pose = value.hitPose
            }
            onHitResult(value)
        }

    var onHitResult: ((node: ArNode, hitResult: HitResult?) -> Unit)? = null

    /**
     * ### Construct a new placement ArModelNode
     *
     * @param placementMode See [ArModelNode.placementMode]
     * @param hitPosition See [ArModelNode.hitPosition]
     * @param followHitPosition See [ArModelNode.followHitPosition]
     * @param instantAnchor See [ArModelNode.instantAnchor]
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
    constructor(hitResult: HitResult) : super(hitResult.createAnchor())

    /**
     * ### Create the Node and load a monolithic binary glTF and add it to the Node
     *
     * @param lifecycle Provide your lifecycle in order to load your model instantly and to destroy
     * it (and its resources) when the lifecycle goes to destroy state.
     * Otherwise the model loading is done when the parent [SceneView] is attached because it needs
     * a [kotlinx.coroutines.CoroutineScope] to load and resources will be destroyed when the
     * [SceneView] is.
     * You are responsible of manually destroy this [Node] only if you don't provide lifecycle and
     * the node is never attached to a [SceneView]
     * @param modelFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param autoAnimate Plays the animations automatically if the model has one
     * @param autoScale Scale the model to fit a unit cube
     * @param centerOrigin Center the model origin to this unit cube position
     * - `null` = Keep the original model center point
     * - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
     * - ...
     *
     * @see loadModel
     */
    constructor(
        context: Context,
        lifecycle: Lifecycle? = null,
        modelFileLocation: String,
        autoAnimate: Boolean = true,
        autoScale: Boolean = false,
        centerOrigin: Position? = null,
        onError: ((error: Exception) -> Unit)? = null,
        onLoaded: ((instance: RenderableInstance) -> Unit)? = null
    ) : this() {
        loadModelAsync(
            context,
            lifecycle,
            modelFileLocation,
            autoAnimate,
            autoScale,
            centerOrigin,
            onError,
            onLoaded
        )
    }

    override fun onArFrame(arFrame: ArFrame) {
        super.onArFrame(arFrame)

        if (!isAnchored) {
            // Try to find an anchor if none has been found
            if (instantAnchor) {
                anchor()
            } else if (followHitPosition &&
                // Add condition in here if a new feature needs hit result
                arFrame.fps(hitTestFrame) <= maxHitTestPerSecond
            ) {
                hitTestFrame = arFrame
                hitResult = hitTest(arFrame)
            }
        }
        this.arFrame = arFrame
    }

    /**
     * TODO : Doc
     */
    open fun onHitResult(hitResult: HitResult?) {
        onHitResult?.invoke(this, hitResult)
    }

    /**
     * ### Performs a ray cast to retrieve the ARCore info at this camera point
     *
     * @param frame the [ArFrame] from where we take the [HitResult]
     * By default the latest session frame if any exist
     * @param xPx x view coordinate in pixels
     * By default the [cameraPosition.x][hitPosition] of this Node is used
     * @property yPx y view coordinate in pixels
     * By default the [cameraPosition.y][hitPosition] of this Node is used
     *
     * @return the hitResult or null if no info is retrieved
     *
     * @see ArFrame.hitTest
     */
    fun hitTest(
        frame: ArFrame? = arSession?.currentFrame,
        approximateDistanceMeters: Float = abs(hitPosition.z),
        plane: Boolean = placementMode.planeEnabled,
        depth: Boolean = placementMode.depthEnabled,
        instantPlacement: Boolean = placementMode.instantPlacementEnabled
    ): HitResult? = super.hitTest(
        frame = frame,
        xPx = (arSession?.displayWidth ?: 0) / 2.0f * (1.0f + hitPosition.x),
        yPx = (arSession?.displayHeight ?: 0) / 2.0f * (1.0f - hitPosition.y),
        approximateDistanceMeters = approximateDistanceMeters,
        plane = plane,
        depth = depth,
        instantPlacement = instantPlacement
    )

    override fun createAnchor(): Anchor? {
        return (hitTest()?.takeIf { it.isTracking }
        // Fallback to last hit result
            ?: hitResult?.takeIf { it.isTracking })
            ?.let { createAnchor(it) }
    }

    fun createAnchor(hitResult: HitResult): Anchor? {
        return hitResult.takeIf { it.isTracking }?.createAnchor()
    }

    override fun clone() = copy(ArModelNode())

    fun copy(toNode: ArModelNode = ArModelNode()) = toNode.apply {
        super.copy(toNode)

        hitPosition = this@ArModelNode.hitPosition
    }

    companion object {
        val DEFAULT_HIT_POSITION =
            Position(0.0f, 0.0f, -DEFAULT_PLACEMENT_DISTANCE)
    }
}
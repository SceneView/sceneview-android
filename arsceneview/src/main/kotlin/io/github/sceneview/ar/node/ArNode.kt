package io.github.sceneview.ar.node

import com.google.ar.core.*
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
    protected val session: ArSession? get() = sceneView?.session

    /**
     * TODO : Doc
     */
    open val isTracking get() = pose != null

    /**
     * ### Move smoothly/slowly when there is a pose (AR position and rotation) update
     *
     * Use [smoothMoveSpeed] to change the position/rotation smooth update speed
     */
    var smoothPose = true

    /**
     * TODO : Doc
     */
    var pose: Pose? = null
        set(value) {
            val position = value?.position
            val quaternion = value?.rotation
            if (position == field?.position || quaternion != field?.rotation) {
                field = value
                if (position != null && quaternion != null) {
                    if (smoothPose) {
                        smooth(position = position, rotation = quaternion)
                    } else {
                        transform(position = position, rotation = quaternion)
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
     * TODO : Doc
     */
    var onTrackingChanged: ((node: ArNode, isTracking: Boolean, pose: Pose?) -> Unit)? = null

    /**
     * TODO : Doc
     */
    val onAnchorChanged = mutableListOf<(node: Node, anchor: Anchor?) -> Unit>()

    /**
     * TODO : Doc
     */
    constructor(anchor: Anchor) : this() {
        this.anchor = anchor
    }

    override fun onArFrame(arFrame: ArFrame) {
        // Update the anchor position if any
        if (anchor?.trackingState == TrackingState.TRACKING) {
            pose = anchor?.pose
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
        onAnchorChanged.forEach { it(this, anchor) }
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
    fun anchor(): Boolean {
        anchor?.detach()
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
//        Quaternion.rotateVector(worldRotationQuaternion, direction)

    /**
     * ### Converts a direction from world-space to the local-space of this node.
     *
     * Not impacted by the position or scale of the node.
     *
     * @param direction the direction in world-space to convert
     * @return a new vector that represents the direction in local-space
     */
//    fun worldToLocalDirection(direction: Vector3) =
//        Quaternion.inverseRotateVector(worldRotationQuaternion, direction)

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
}
package io.github.sceneview.sample.arviewnode.utils

import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.toVector3

/**
 * ## Overview
 * * The NodeRotationHelper class is designed to manage the rotation of nodes in an
 * augmented reality (AR) scene. It updates the rotation of anchored nodes based on the
 * position of the camera in the scene, ensuring that the anchored nodes face the camera.
 * This is particularly useful in AR applications where the alignment of virtual
 * objects relative to the user's viewpoint is crucial for a realistic and interactive experience.
 *
 * ## Constructor
 * * maxSkipFrames [Int]: The maximum number of frames to skip before updating the
 *                        rotation of anchored nodes. Default value is [MAX_SKIP_FRAMES]
 *
 * ## Example Usage
 * ```
 * // Create an instance of NodeRotationHelper with default maxSkipFrames
 * val rotationHelper = NodeRotationHelper()
 *
 * // Update the rotation of nodes in an AR scene
 * rotationHelper.updateRotationOfViewNodes(sceneView, nodeList)
 * ```
 *
 * ## Notes
 * * The class is designed to minimize the computational load by skipping frame updates,
 * controlled by the maxSkipFrames property.
 * * Ensure that the AR environment and dependencies (like ARCore or equivalent) are
 * properly set up for this class to function as intended.
 */
class NodeRotationHelper(
    var maxSkipFrames: Int = MAX_SKIP_FRAMES
) {

    /**
     * A counter to keep track of the number of skipped frames.
     * This property is used internally to determine when to update the rotation of the nodes.
     */
    private var skipCounter = 0

    /**
     * Updates the rotation of the anchored nodes in the provided list based on the
     * position of the camera in the scene view.
     *
     * @param sceneView [ARSceneView]: The AR scene view containing the camera and nodes.
     * @param nodeList [MutableList<[AnchorNode]>]: A list of nodes whose rotation needs to be updated.
     */
    fun updateRotationOfViewNodes(
        sceneView: ARSceneView,
        nodeList: MutableList<AnchorNode>
    ) {
        if (skipCounter <= maxSkipFrames) {
            skipCounter++
            return
        }

        skipCounter = 0

        nodeList.forEach {
            if (it.anchor.trackingState == TrackingState.TRACKING) {
                val cameraPosition = sceneView.cameraNode.worldPosition

                val nodePosition = it.worldPosition

                val cameraVec3 = cameraPosition.toVector3()
                val nodeVec3 = nodePosition.toVector3()

                val direction = Vector3.subtract(cameraVec3, nodeVec3)

                val lookRotation = Quaternion.lookRotation(direction, Vector3.up())

                it.worldQuaternion = dev.romainguy.kotlin.math.Quaternion(
                    lookRotation.x,
                    lookRotation.y,
                    lookRotation.z,
                    lookRotation.w
                )
            }
        }
    }

    companion object {
        const val MAX_SKIP_FRAMES = 10
    }
}
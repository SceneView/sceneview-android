package io.github.sceneview.sample.arviewnode.utils

import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.toVector3

object NodeRotationHelper {

    fun updateRotationOfViewNodes(
        sceneView: ARSceneView,
        nodeList: MutableList<AnchorNode>
    ) {
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
                    lookRotation.w)
            }
        }
    }
}
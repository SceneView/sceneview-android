package io.github.sceneview.ar.interaction

import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.toFloat4

internal class TranslationGesture(arNode: ArNode) : GestureStrategy(arNode) {
    private var lastArHitResult: HitResult? = null

    override fun beginGesture(x: Int, y: Int) {
        arNode.detachAnchor()
    }

    override fun continueGesture(x: Int, y: Int) {
        val scene = arNode.getSceneViewInternal() ?: return
        val frame = (scene as ArSceneView).currentFrame ?: return

        val hitResultList = frame.hitTests(x.toFloat(), y.toFloat())
        hitResultList.forEach {
            val trackable = it.trackable
            val pose = it.hitPose
            if (trackable is Plane) {
                val plane = trackable
                if (plane.isPoseInPolygon(pose)
                /**&& allowedPlaneTypes.contains(plane.type)**/
                ) {
                    arNode.smooth(
                        Float3(pose.tx(), pose.ty(), pose.tz()),
                        quaternion = Quaternion(pose.rotationQuaternion.toFloat4())
                    )
                }
                lastArHitResult = it
            }
        }
    }

    override fun endGesture() {
        val hitResult = lastArHitResult ?: return
        if (hitResult.trackable.trackingState == TrackingState.TRACKING) {
            val anchorNode = arNode
            val oldAnchor = anchorNode.anchor
            oldAnchor?.detach()
            val newAnchor = hitResult.createAnchor()
            anchorNode.anchor = newAnchor

// TODO: View if it is usefull
//      Vector3 worldPosition = getTransformableNode().getWorldPosition();
//      Quaternion worldRotation = getTransformableNode().getWorldRotation();
//      Quaternion finalDesiredWorldRotation = worldRotation;
//
//      // Since we change the anchor, we need to update the initialForwardInLocal into the new
//      // coordinate space. Local variable for nullness analysis.
//      Quaternion desiredLocalRotation = this.desiredLocalRotation;
//      if (desiredLocalRotation != null) {
//        getTransformableNode().setQuaternion(desiredLocalRotation);
//        finalDesiredWorldRotation = getTransformableNode().getWorldRotation();
//      }
//
//      anchorNode.setAnchor(newAnchor);
//
//      // Temporarily set the node to the final world rotation so that we can accurately
//      // determine the initialForwardInLocal in the new coordinate space.
//      getTransformableNode().setWorldRotation(finalDesiredWorldRotation);
//      Vector3 initialForwardInWorld = getTransformableNode().getForward();
//      initialForwardInLocal.set(anchorNode.worldToLocalDirection(initialForwardInWorld));
//
//      getTransformableNode().setWorldRotation(worldRotation);
//      getTransformableNode().setWorldPosition(worldPosition);
        }
    }
}
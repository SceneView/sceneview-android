package io.github.sceneview.ar.interaction

import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode

internal class TranslationGesture(private var arModelNode: ArModelNode) :
    GestureStrategy(arModelNode) {
    private var lastArHitResult: HitResult? = null

    private val allowedPlaneTypes: Set<Plane.Type> = when (arNode.placementMode) {
        PlacementMode.PLANE_HORIZONTAL -> setOf(
            Plane.Type.HORIZONTAL_UPWARD_FACING,
            Plane.Type.HORIZONTAL_DOWNWARD_FACING
        )
        PlacementMode.PLANE_VERTICAL -> setOf(Plane.Type.VERTICAL)
        PlacementMode.PLANE_HORIZONTAL_AND_VERTICAL -> Plane.Type.values().toSet()
        else -> emptySet()
    }

    override fun beginGesture(x: Int, y: Int) {
        arNode.detachAnchor()
    }

    override fun continueGesture(x: Int, y: Int) {
        arModelNode.hitTest(xPx = x.toFloat(), yPx = y.toFloat())?.takeIf { it.isTracking }
            ?.let { hitResult ->
                val trackable = hitResult.trackable
                if (trackable is Plane && !allowedPlaneTypes.contains(trackable.type)) return
                arNode.pose = hitResult.hitPose
                lastArHitResult = hitResult
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
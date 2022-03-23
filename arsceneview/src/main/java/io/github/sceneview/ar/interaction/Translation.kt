package io.github.sceneview.ar.interaction

import com.google.ar.core.HitResult
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.depthEnabled
import io.github.sceneview.ar.arcore.instantPlacementEnabled
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.arcore.planeFindingEnabled
import io.github.sceneview.ar.node.ArNode

internal class TranslationGesture(arNode: ArNode) : GestureStrategy(arNode) {
    private var lastArHitResult: HitResult? = null

    override fun beginGesture(x: Int, y: Int) {
        arNode.detachAnchor()
    }

    override fun continueGesture(x: Int, y: Int) {
        val sceneView = arNode.getSceneViewInternal() as? ArSceneView ?: return
        val config = sceneView.arSessionConfig ?: return
        val arFrame = sceneView.currentFrame ?: return
        arFrame.hitTest(
            xPx = x.toFloat(), yPx = y.toFloat(),
            plane = config.planeFindingEnabled,
            depth = config.depthEnabled,
            instantPlacement = config.instantPlacementEnabled
        )?.takeIf { it.isTracking }?.let { hitResult ->
            lastArHitResult = hitResult
            hitResult.hitPose?.let { hitPose ->
                arNode.pose = hitPose
            }
        }
    }

    override fun endGesture() {
        lastArHitResult?.takeIf { it.trackable.trackingState == TrackingState.TRACKING }
            ?.let { hitResult ->
                arNode.anchor = hitResult.createAnchor()
            }
    }
}

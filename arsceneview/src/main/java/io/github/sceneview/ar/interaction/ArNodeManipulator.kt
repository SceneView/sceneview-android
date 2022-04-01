package io.github.sceneview.ar.interaction

import android.util.Log
import com.google.ar.core.HitResult
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.*
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.depthEnabled
import io.github.sceneview.ar.arcore.instantPlacementEnabled
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.arcore.planeFindingEnabled
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.EditableTransform
import io.github.sceneview.node.Node
import io.github.sceneview.scene.SelectionVisualizer

open class ArNodeManipulator(
    protected val sceneView: ArSceneView
) {
    var currentNode: ArNode? = null
    protected open val selectionVisualizer: SelectionVisualizer
        get() = sceneView.selectionVisualizer

    open fun onNodeTouch(node: Node) {
        val oldCurrentNode = currentNode
        currentNode = node as? ArNode
        if (oldCurrentNode == currentNode) return
        currentNode?.let { selectionVisualizer.applySelectionVisual(it) }
        oldCurrentNode?.let { selectionVisualizer.removeSelectionVisual(it) }
    }

    open fun rotate(deltaDegree: Float) {
        val nodeToRotate = currentNode?.takeIf { it.rotationEditable } ?: return
        Log.d("Rotation", "Rotation delta: $deltaDegree")
        val rotationDelta =
            normalize(Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), degrees(deltaDegree)))
        nodeToRotate.modelQuaternion = nodeToRotate.modelQuaternion * rotationDelta
    }

    open fun scale(factor: Float): Boolean {
        val nodeToScale = currentNode?.takeIf { it.scaleEditable } ?: return false
        nodeToScale.scale = clamp(
            nodeToScale.scale + factor, 0.5f, 1.5f
        )
        return true
    }

    protected var lastArHitResult: HitResult? = null

    open fun beginTransform() {
        lastArHitResult = null
        val nodeToTransform = currentNode?.takeIf { it.positionEditable } ?: return
        nodeToTransform.detachAnchor()
    }

    open fun continueTransform(x: Float, y: Float) {
        val nodeToTransform = currentNode?.takeIf { it.positionEditable } ?: return
        val sceneView = nodeToTransform.getSceneViewInternal() as? ArSceneView ?: return
        val config = sceneView.arSessionConfig ?: return
        val arFrame = sceneView.currentFrame ?: return
        arFrame.hitTest(
            xPx = x, yPx = y,
            plane = config.planeFindingEnabled,
            depth = config.depthEnabled,
            instantPlacement = config.instantPlacementEnabled
        )?.takeIf { it.isTracking }?.let { hitResult ->
            lastArHitResult = hitResult
            hitResult.hitPose?.let { hitPose ->
                nodeToTransform.pose = hitPose
            }
        }
    }

    open fun endTransform() {
        val nodeToTransform = currentNode?.takeIf { it.positionEditable } ?: return
        lastArHitResult?.takeIf { it.trackable.trackingState == TrackingState.TRACKING }
            ?.let { hitResult ->
                nodeToTransform.anchor = hitResult.createAnchor()
            }
    }

}

internal val ArNode.positionEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.POSITION)

internal val ArNode.rotationEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.ROTATION)

internal val ArNode.scaleEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.SCALE)

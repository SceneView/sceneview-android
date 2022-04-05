package io.github.sceneview.ar.interaction

import android.util.Log
import com.google.ar.core.HitResult
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.*
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.EditableTransform
import io.github.sceneview.node.Node
import io.github.sceneview.scene.SelectionVisualizer

open class ArNodeManipulator(
    protected val sceneView: ArSceneView
) {
    var currentNode: ArNode? = null
    private var gestureInProgress = false

    protected open val selectionVisualizer: SelectionVisualizer
        get() = sceneView.selectionVisualizer

    open fun onNodeTouch(node: Node) {
        val oldCurrentNode = currentNode
        currentNode = node as? ArNode
        if (oldCurrentNode == currentNode) return
        currentNode?.let { selectionVisualizer.applySelectionVisual(it) }
        oldCurrentNode?.let { selectionVisualizer.removeSelectionVisual(it) }
    }

    open fun beginRotate(): Boolean {
        if (!(currentNode?.rotationEditable == true && !gestureInProgress)) return false
        gestureInProgress = true
        return true
    }

    open fun rotate(deltaDegree: Float): Boolean {
        val nodeToRotate = currentNode?.takeIf { it.rotationEditable } ?: return false
        Log.d(this::class.toString(), "Rotation delta: $deltaDegree")
        val rotationDelta =
            normalize(Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), degrees(deltaDegree)))
        nodeToRotate.modelQuaternion = nodeToRotate.modelQuaternion * rotationDelta
        return true
    }

    open fun endRotate() {
        gestureInProgress = false
    }

    open fun beginScale(): Boolean {
        if (!(currentNode?.scaleEditable == true && !gestureInProgress)) return false
        gestureInProgress = true
        return true
    }

    open fun scale(factor: Float): Boolean {
        Log.d(this::class.toString(), "Scale factor: $factor")
        val nodeToScale = currentNode?.takeIf { it.scaleEditable } ?: return false
        val newScale = clamp(
            nodeToScale.scale * factor, 0.5f, 1.5f
        )
        nodeToScale.scale = newScale
        return true
    }

    open fun endScale() {
        gestureInProgress = false
    }

    protected var lastArHitResult: HitResult? = null

    val positionIsEditable: Boolean = currentNode?.positionEditable ?: false

    open fun beginTransform(): Boolean {
        if (gestureInProgress) return false
        gestureInProgress = true
        lastArHitResult = null
        val nodeToTransform = currentNode?.takeIf { it.positionEditable } ?: return false
        nodeToTransform.detachAnchor()
        return true
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
                Log.d("Transform", "New pose")
                nodeToTransform.smooth(hitPose.position, quaternion = hitPose.quaternion)
            }
        }
    }

    open fun endTransform() {
        val nodeToTransform = currentNode?.takeIf { it.positionEditable } ?: return
        lastArHitResult?.takeIf { it.trackable.trackingState == TrackingState.TRACKING }
            ?.let { hitResult ->
                Log.d("Transform", "New anchor")
                nodeToTransform.anchor = hitResult.createAnchor()
            }
        gestureInProgress = false
    }

}

internal val ArNode.positionEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.POSITION)

internal val ArNode.rotationEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.ROTATION)

internal val ArNode.scaleEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.SCALE)

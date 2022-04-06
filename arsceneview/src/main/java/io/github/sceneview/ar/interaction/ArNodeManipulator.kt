package io.github.sceneview.ar.interaction

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
    private var currentGesture: EditableTransform? = null

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
        if (!(currentNode?.rotationEditable == true && currentGesture == null)) return false
        currentGesture = EditableTransform.ROTATION
        return true
    }

    open fun rotate(deltaDegree: Float): Boolean =
        runIfCurrentGestureIs(EditableTransform.ROTATION) {
            val nodeToRotate =
                currentNode?.takeIf { it.rotationEditable } ?: return@runIfCurrentGestureIs false
            val rotationDelta =
                normalize(Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), degrees(deltaDegree)))
            nodeToRotate.modelQuaternion = nodeToRotate.modelQuaternion * rotationDelta
            true
        }

    open fun endRotate() {
        currentGesture = null
    }

    open fun beginScale(): Boolean {
        if (!(currentNode?.scaleEditable == true && currentGesture == null)) return false
        currentGesture = EditableTransform.SCALE
        return true
    }

    open fun scale(factor: Float) = runIfCurrentGestureIs(EditableTransform.SCALE) {
        val nodeToScale =
            currentNode?.takeIf { it.scaleEditable } ?: return@runIfCurrentGestureIs false
        nodeToScale.scale = clamp(
            nodeToScale.scale * factor, 0.5f, 1.5f
        )
        true
    }

    open fun endScale() {
        currentGesture = null
    }

    protected var lastArHitResult: HitResult? = null

    val positionIsEditable: Boolean
        get() = currentNode?.positionEditable ?: false

    open fun beginTransform(): Boolean {
        if (!(currentGesture == null && currentNode?.positionEditable == true)) return false
        currentGesture = EditableTransform.POSITION
        lastArHitResult = null
        val nodeToTransform = currentNode?.takeIf { it.positionEditable } ?: return false
        nodeToTransform.detachAnchor()
        return true
    }

    open fun continueTransform(x: Float, y: Float) =
        runIfCurrentGestureIs(EditableTransform.POSITION) {
            val nodeToTransform =
                currentNode?.takeIf { it.positionEditable } ?: return@runIfCurrentGestureIs false
            val sceneView = nodeToTransform.getSceneViewInternal() as? ArSceneView
                ?: return@runIfCurrentGestureIs false
            val config = sceneView.arSessionConfig ?: return@runIfCurrentGestureIs false
            val arFrame = sceneView.currentFrame ?: return@runIfCurrentGestureIs false
            arFrame.hitTest(
                xPx = x, yPx = y,
                plane = config.planeFindingEnabled,
                depth = config.depthEnabled,
                instantPlacement = config.instantPlacementEnabled
            )?.takeIf { it.isTracking }?.let { hitResult ->
                lastArHitResult = hitResult
                hitResult.hitPose?.let { hitPose ->
                    nodeToTransform.transform(hitPose.position, quaternion = hitPose.quaternion)
                }
            }
            true
        }

    open fun endTransform() = runIfCurrentGestureIs(EditableTransform.POSITION) {
        val nodeToTransform =
            currentNode?.takeIf { it.positionEditable } ?: return@runIfCurrentGestureIs false
        lastArHitResult?.takeIf { it.trackable.trackingState == TrackingState.TRACKING }
            ?.let { hitResult ->
                nodeToTransform.anchor = hitResult.createAnchor()
            }
        currentGesture = null
        true
    }

    private fun runIfCurrentGestureIs(gesture: EditableTransform, block: () -> Boolean): Boolean {
        if (currentGesture != gesture) return false
        return block.invoke()
    }
}

internal val ArNode.positionEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.POSITION)

internal val ArNode.rotationEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.ROTATION)

internal val ArNode.scaleEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.SCALE)

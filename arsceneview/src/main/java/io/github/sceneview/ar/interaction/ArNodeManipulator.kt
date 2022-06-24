package io.github.sceneview.ar.interaction

import com.google.ar.core.HitResult
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.clamp
import dev.romainguy.kotlin.math.degrees
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.EditableTransform
import io.github.sceneview.interaction.SelectedNodeVisualizer
import io.github.sceneview.node.Node

open class ArNodeManipulator(
    protected val sceneView: ArSceneView
) {
    var minZoomScale = 0.1f
    var maxZoomScale = 10.0f

    private var currentGestureTransform: EditableTransform? = null
    private var lastHitResult: HitResult? = null

    var selectedNode: ArNode? = null
        set(value) {
            if (field != value) {
                field?.let { selectedNodeVisualizer.selectNode(it, false) }
                value?.let { selectedNodeVisualizer.selectNode(it, true) }
                field = value
            }
        }

    open val selectedNodeVisualizer: SelectedNodeVisualizer
        get() = sceneView.selectedNodeVisualizer

    private var savedFollowHitPositionState : Boolean = false

    open fun beginTranslate(): Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == null && it.positionEditable
        }?.also { node ->
            currentGestureTransform = EditableTransform.POSITION
            (node as? ArModelNode)?.let { arModelNode ->
                savedFollowHitPositionState = arModelNode.followHitPosition
                arModelNode.followHitPosition = false
            }
            node.detachAnchor()
        } != null

    open fun continueTranslate(x: Float, y: Float): Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.POSITION && it.positionEditable
        }?.also { node ->
            node.hitTest(xPx = x, yPx = y)?.takeIf { it.isTracking }?.let { hitResult ->
                hitResult.hitPose?.let { hitPose ->
                    lastHitResult = hitResult
                    node.pose = hitPose
                }
            }
        } != null

    open fun endTranslate(): Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.POSITION && it.positionEditable
        }?.also { node ->
            currentGestureTransform = null
            lastHitResult?.takeIf { it.isTracking }?.apply {
                lastHitResult = null
                node.anchor = createAnchor()
            } ?: node.anchor()
            (node as? ArModelNode)?.followHitPosition = savedFollowHitPositionState
        } != null

    // The first delta is always way off as it contains all delta until threshold to
    // recognize rotate gesture is met
    private var skipFirstRotate = false

    open fun beginRotate(): Boolean {
        return (currentGestureTransform == null && selectedNode?.rotationEditable == true).also { beginRotate ->
            if (beginRotate) {
                currentGestureTransform = EditableTransform.ROTATION
                skipFirstRotate = true
            }
        }
    }

    open fun rotate(deltaRadians: Float): Boolean {
        return selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.ROTATION && it.rotationEditable
        }?.also { node ->
            if (skipFirstRotate) {
                skipFirstRotate = false
                return true
            }
            val rotationDelta =
                Quaternion.fromAxisAngle(Float3(y = 1.0f), degrees(-deltaRadians))
            node.modelQuaternion = node.modelQuaternion * rotationDelta
        } != null
    }

    open fun endRotate(): Boolean {
        return (currentGestureTransform == EditableTransform.ROTATION &&
            selectedNode?.rotationEditable == true)
            .also { if (it) currentGestureTransform = null }
    }

    open fun beginScale(): Boolean =
        (currentGestureTransform == null && selectedNode?.scaleEditable == true).also { beginScale ->
            if (beginScale) currentGestureTransform = EditableTransform.SCALE
        }

    open fun scale(factor: Float): Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.SCALE && it.scaleEditable
        }?.also { node ->
            node.modelScale = clamp(node.modelScale * factor, minZoomScale, maxZoomScale)
        } != null

    open fun endScale(): Boolean =
        (currentGestureTransform == EditableTransform.SCALE &&
            selectedNode?.scaleEditable == true)
            .also { if (it) currentGestureTransform = null }
}

internal val ArNode.positionEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.POSITION)

internal val ArNode.rotationEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.ROTATION)

internal val ArNode.scaleEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.SCALE)

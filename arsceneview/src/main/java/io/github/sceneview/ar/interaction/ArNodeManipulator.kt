package io.github.sceneview.ar.interaction

import dev.romainguy.kotlin.math.*
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.*
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

    open fun onNodeTouch(node: Node) {
        selectedNode = node as? ArNode
    }

    open fun beginTransform(): Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == null && it.positionEditable
        }?.let { node ->
            currentGestureTransform = EditableTransform.POSITION
            node.detachAnchor()
        } != null

    open fun continueTransform(x: Float, y: Float) : Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.POSITION && it.positionEditable
        }?.let { node ->
            node.hitTest(xPx = x, yPx = y)?.takeIf { it.isTracking }?.let { hitResult ->
                hitResult.hitPose?.let { hitPose ->
                    node.pose = hitPose
                }
            }
        } != null

    open fun endTransform() : Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.POSITION && it.positionEditable
        }?.let { node ->
            node.anchor()
            currentGestureTransform = null
        } != null

    open fun beginRotate(): Boolean =
        (currentGestureTransform == null && selectedNode?.rotationEditable == true).also {
            currentGestureTransform = EditableTransform.ROTATION
        }

    open fun rotate(deltaRadians: Float) : Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.ROTATION && it.rotationEditable
        }?.let { node ->
            // TODO: Fix a round rotation behavior
            val rotationDelta = Quaternion.fromAxisAngle(Float3(y = 1.0f), degrees(deltaRadians))
            node.modelQuaternion = node.modelQuaternion * rotationDelta
        } != null

    open fun endRotate(): Boolean =
        (currentGestureTransform == EditableTransform.ROTATION &&
            selectedNode?.rotationEditable == true)
        .also { currentGestureTransform = null }

    open fun beginScale(): Boolean =
        (currentGestureTransform == null && selectedNode?.scaleEditable == true).also {
            currentGestureTransform = EditableTransform.SCALE
        }

    open fun scale(factor: Float) : Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.SCALE && it.scaleEditable
        }?.let { node ->
            node.modelScale = clamp(node.modelScale * factor, minZoomScale, maxZoomScale)
        } != null

    open fun endScale(): Boolean =
        (currentGestureTransform == EditableTransform.SCALE &&
                selectedNode?.scaleEditable == true)
            .also { currentGestureTransform = null }
}

internal val ArNode.positionEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.POSITION)

internal val ArNode.rotationEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.ROTATION)

internal val ArNode.scaleEditable: Boolean
    get() = editableTransforms.contains(EditableTransform.SCALE)

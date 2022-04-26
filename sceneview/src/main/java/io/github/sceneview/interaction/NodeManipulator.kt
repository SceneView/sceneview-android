package io.github.sceneview.interaction

import dev.romainguy.kotlin.math.*
import io.github.sceneview.SceneView
import io.github.sceneview.math.toQuaternion
import io.github.sceneview.node.Node
import io.github.sceneview.node.EditableTransform


open class NodeManipulator(
    protected val sceneView: SceneView
) {
    private var minZoomScale = 0.1f
    private var maxZoomScale = 10.0f

    private var currentGestureTransform: EditableTransform? = null

    var selectedNode: Node? = null
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
        selectedNode = node as? Node
    }

    open fun beginTransform(): Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == null && it.positionEditable
        }?.let { node ->
            currentGestureTransform = EditableTransform.POSITION
        } != null

    open fun continueTransform(x: Float, y: Float) : Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.POSITION && it.positionEditable
        }?.let { node ->
            var localRotation = node.rotation.toQuaternion()
            val rotationAmountX = -x * 0.5f
            val rotationDeltaX = Quaternion.fromEuler(Float3(0f, rotationAmountX, 0f))

            localRotation = rotationDeltaX.times(localRotation)
            node.rotation = localRotation.toEulerAngles()
        } != null

    open fun endTransform() : Boolean =
        selectedNode?.takeIf {
            currentGestureTransform == EditableTransform.POSITION && it.positionEditable
        }?.let { node ->
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
            node.quaternion = node.quaternion * rotationDelta
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
            node.scale = clamp(node.scale * factor, minZoomScale, maxZoomScale)
        } != null

    open fun endScale(): Boolean =
        (currentGestureTransform == EditableTransform.SCALE &&
                selectedNode?.scaleEditable == true)
            .also { currentGestureTransform = null }
}

internal val Node.positionEditable: Boolean
    get() = true

internal val Node.rotationEditable: Boolean
    get() = false

internal val Node.scaleEditable: Boolean
    get() = false

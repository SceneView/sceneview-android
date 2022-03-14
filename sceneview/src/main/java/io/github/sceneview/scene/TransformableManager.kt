/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.sceneview.scene

import android.util.DisplayMetrics
import android.view.MotionEvent
import com.google.ar.sceneform.PickHitResult
import com.google.ar.sceneform.ux.BaseGestureRecognizer
import io.github.sceneview.scene.BaseTransformationController
import com.google.ar.sceneform.ux.DragGesture
import com.google.ar.sceneform.ux.DragGestureRecognizer
import com.google.ar.sceneform.ux.GesturePointersUtility
import com.google.ar.sceneform.ux.PinchGestureRecognizer
import com.google.ar.sceneform.ux.TwistGestureRecognizer
import io.github.sceneview.node.Node

/**
 * Handles selection and gestures on ARNode. Replaces functionality provided by
 * BaseTransformableNode and TransformationSystem in original Sceneform.
 */
class TransformableManager(
    displayMetrics: DisplayMetrics,
    val selectionVisualizer: SelectionVisualizer
) : SelectionManager {
    private val gesturePointersUtility = GesturePointersUtility(displayMetrics)
    private val dragGestureRecognizer = DragGestureRecognizer(gesturePointersUtility)
    private val pinchGestureRecognizer = PinchGestureRecognizer(gesturePointersUtility)
    private val twistGestureRecognizer = TwistGestureRecognizer(gesturePointersUtility)

    private val gestureRecognizers: List<BaseGestureRecognizer<*>> =
        listOf(dragGestureRecognizer, pinchGestureRecognizer, twistGestureRecognizer)

    private val activeTransformationControllers =
        mutableListOf<BaseTransformationController<*>>()

    private var selectedNode: Node? = null

    var translationControllerBuilder: TranslationControllerBuilder? = null

    fun onNodeTap(node: Node?) {
        selectedNode?.let { deselectNode() }
        if (node !is Transformable || !node.isFocusable) return
        activeTransformationControllers.forEach { it.cancel() }
        activeTransformationControllers.clear()

        node.editModes.forEach {
            val gestureController = when (it) {
                Transformable.EditMode.SCALE -> ScaleController(node, pinchGestureRecognizer, this)
                Transformable.EditMode.MOVE -> checkNotNull(
                    translationControllerBuilder?.build(
                        node,
                        this,
                        dragGestureRecognizer
                    )
                ) { "translationControllerBuilder needs to be set to use MOVE gesture." }
                else -> {null}
            }
           gestureController?.let { activeTransformationControllers.add(gestureController) }
        }
        selectNode(node)
    }

    private fun selectNode(node: Node): Boolean {
        if ((node as? Transformable)?.isFocusable != true) return false
        if (!deselectNode()) {
            return false
        }
        selectionVisualizer.applySelectionVisual(node)
        selectedNode = node
        return true
    }

    private fun deselectNode(): Boolean {
        if (selectedNode == null) {
            return true
        }

        if (selectedNodeIsTransforming()) {
            return false
        }

        selectedNode?.let {
            selectionVisualizer.removeSelectionVisual(it)
        }
        selectedNode = null

        return true
    }

    private fun selectedNodeIsTransforming() =
        activeTransformationControllers.any { it.isTransforming }

    /** Dispatches touch events to the gesture recognizers contained by this transformation system.  */
    fun onTouch(pickHitResult: PickHitResult?, motionEvent: MotionEvent?) {
        if (selectedNode == null && pickHitResult?.node?.isFocusable == true) {
            pickHitResult.node?.let { onNodeTap(it) }
        } else {
            gestureRecognizers.forEach {
                it.onTouch(pickHitResult, motionEvent)
            }
        }
    }

    override fun isSelected(node: Node): Boolean = selectedNode === node

    override fun select(node: Node): Boolean {
        return (node as? Transformable)?.let { selectNode(node) } ?: false
    }

    interface TranslationControllerBuilder {
        fun build(
            node: Node,
            selectionManager: SelectionManager,
            gestureRecognizer: DragGestureRecognizer
        ): BaseTransformationController<DragGesture>
    }
}

interface Transformable {
    val isFocusable: Boolean
    val editModes: Set<EditMode>

    /**
     * Allowed ways to manipulate the node.
     */
    enum class EditMode {
        ROTATE, SCALE, MOVE
    }
}
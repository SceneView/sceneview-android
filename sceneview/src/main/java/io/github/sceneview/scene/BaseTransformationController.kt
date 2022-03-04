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

import androidx.annotation.CallSuper
import com.google.ar.sceneform.ux.BaseGesture
import com.google.ar.sceneform.ux.BaseGestureRecognizer
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime

/**
 * Manipulates the transform properties (i.e. scale/rotation/translation) of a [ ] by responding to Gestures via a [BaseGestureRecognizer].
 *
 *
 * Example's include, changing the [Node]'s Scale based on a Pinch Gesture.
 */
public abstract class BaseTransformationController<T : BaseGesture<T>?>(
    val transformableNode: Node, gestureRecognizer: BaseGestureRecognizer<T>,
    protected val selectionManager: SelectionManager
) : BaseGestureRecognizer.OnGestureStartedListener<T>, BaseGesture.OnGestureEventListener<T> {
    private val gestureRecognizer: BaseGestureRecognizer<T>
    var activeGesture: T? = null
        private set

    var enabled = false
        set(value) {
            field = value
            updateActiveAndEnabled()
        }

    private var activeAndEnabled = false

    open val isTransforming: Boolean
        get() = activeGesture != null

    init {
        transformableNode.onRenderingChanged.add { node: Node?, isRendering: Boolean ->
            if (isRendering) {
                onActivated(node)
            } else {
                onDeactivated(node)
            }
        }
        transformableNode.onFrame.add { frameTime: FrameTime, node: Node ->
            onFrameUpdated(frameTime, node)
        }
        this.gestureRecognizer = gestureRecognizer
        enabled = true
    }

    fun cancel() {
        disconnectFromRecognizer()
    }

    // ---------------------------------------------------------------------------------------
    // Implementation of interface Node.LifecycleListener
    // ---------------------------------------------------------------------------------------
    @CallSuper
    open fun onActivated(node: Node?) {
        updateActiveAndEnabled()
    }

    open fun onFrameUpdated(frameTime: FrameTime, node: Node) {}

    @CallSuper
    fun onDeactivated(node: Node?) {
        updateActiveAndEnabled()
    }

    // ---------------------------------------------------------------------------------------
    // Implementation of interface BaseGestureRecognizer.OnGestureStartedListener
    // ---------------------------------------------------------------------------------------
    override fun onGestureStarted(gesture: T) {
        if (isTransforming) {
            return
        }
        if (canStartTransformation(gesture)) {
            setActiveGesture(gesture)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Implementation of interface BaseGesture.OnGestureEventListener
    // ---------------------------------------------------------------------------------------
    override fun onUpdated(gesture: T) {
        onContinueTransformation(gesture)
    }

    override fun onFinished(gesture: T) {
        onEndTransformation(gesture)
        setActiveGesture(null)
    }

    protected open fun canStartTransformation(gesture: T): Boolean {
        return selectionManager.isSelected(transformableNode)
    }

    protected abstract fun onContinueTransformation(gesture: T)
    protected abstract fun onEndTransformation(gesture: T)
    private fun setActiveGesture(gesture: T?) {
        activeGesture?.setGestureEventListener(null)
        activeGesture = gesture
        activeGesture?.setGestureEventListener(this)
    }

    private fun updateActiveAndEnabled() {
        val newActiveAndEnabled = transformableNode.isRendered && enabled
        if (newActiveAndEnabled == activeAndEnabled) {
            return
        }
        activeAndEnabled = newActiveAndEnabled
        if (activeAndEnabled) {
            connectToRecognizer()
        } else {
            disconnectFromRecognizer()
            activeGesture?.cancel()
        }
    }

    private fun connectToRecognizer() {
        gestureRecognizer.addOnGestureStartedListener(this)
    }

    private fun disconnectFromRecognizer() {
        gestureRecognizer.removeOnGestureStartedListener(this)
    }
}
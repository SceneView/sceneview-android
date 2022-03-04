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

import com.google.ar.sceneform.math.MathHelper
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.PinchGesture
import com.google.ar.sceneform.ux.PinchGestureRecognizer
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime

/**
 * Manipulates the Scale of a [BaseTransformableNode] using a Pinch [ ]. Applies a tunable elastic bounce-back when scaling the [ ] beyond the min/max scale.
 */
class ScaleController(
    transformableNode: Node, gestureRecognizer: PinchGestureRecognizer,
    selectionManager: SelectionManager
) : BaseTransformationController<PinchGesture>(
    transformableNode, gestureRecognizer, selectionManager
) {
    var minScale = DEFAULT_MIN_SCALE
    var maxScale = DEFAULT_MAX_SCALE
    var sensitivity = DEFAULT_SENSITIVITY
    var elasticity = DEFAULT_ELASTICITY
    private var currentScaleRatio = 0f
    override fun onActivated(node: Node?) {
        super.onActivated(node)
        val scale = transformableNode.scale.toVector3()
        currentScaleRatio = (scale.x - minScale) / scaleDelta
    }

    override fun onFrameUpdated(frameTime: FrameTime, node: Node) {
        if (isTransforming || !enabled) {
            return
        }
        val t = MathHelper.clamp(frameTime.intervalSeconds.toFloat() * LERP_SPEED, 0.0f, 1.0f)
        currentScaleRatio = MathHelper.lerp(currentScaleRatio, clampedScaleRatio, t)
        val finalScaleValue = finalScale
        val finalScale = Vector3(finalScaleValue, finalScaleValue, finalScaleValue)
        transformableNode.scale = finalScale.toFloat3()
    }

    override fun onContinueTransformation(gesture: PinchGesture) {
        currentScaleRatio += gesture.gapDeltaInches() * sensitivity
        val finalScaleValue = finalScale
        val finalScale = Vector3(finalScaleValue, finalScaleValue, finalScaleValue)
        transformableNode.scale = finalScale.toFloat3()
        if (currentScaleRatio < -ELASTIC_RATIO_LIMIT
            || currentScaleRatio > 1.0f + ELASTIC_RATIO_LIMIT
        ) {
            gesture.cancel()
        }
    }

    override fun onEndTransformation(gesture: PinchGesture) {}
    private val scaleDelta: Float
        get() {
            val scaleDelta = maxScale - minScale
            check(scaleDelta > 0.0f) { "maxScale must be greater than minScale." }
            return scaleDelta
        }
    private val clampedScaleRatio: Float
        get() = Math.min(1.0f, Math.max(0.0f, currentScaleRatio))
    private val finalScale: Float
        get() {
            val elasticScaleRatio = clampedScaleRatio + elasticDelta
            return minScale + elasticScaleRatio * scaleDelta
        }
    private val elasticDelta: Float
        get() {
            val overRatio: Float = if (currentScaleRatio > 1.0f) {
                currentScaleRatio - 1.0f
            } else if (currentScaleRatio < 0.0f) {
                currentScaleRatio
            } else {
                return 0.0f
            }
            return (1.0f - 1.0f / (Math.abs(overRatio) * elasticity + 1.0f)) * Math.signum(overRatio)
        }

    companion object {
        const val DEFAULT_MIN_SCALE = 0.75f
        const val DEFAULT_MAX_SCALE = 1.75f
        const val DEFAULT_SENSITIVITY = 0.75f
        const val DEFAULT_ELASTICITY = 0.15f
        private const val ELASTIC_RATIO_LIMIT = 0.8f
        private const val LERP_SPEED = 8.0f
    }
}
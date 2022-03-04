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

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import io.github.sceneview.scene.BaseTransformationController
import com.google.ar.sceneform.ux.TwistGesture
import com.google.ar.sceneform.ux.TwistGestureRecognizer
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.node.Node

/**
 * Manipulates the rotation of a [BaseTransformableNode] using a [ ].
 */
class RotationController(
    transformableNode: Node?, gestureRecognizer: TwistGestureRecognizer?,
    selectionManager: SelectionManager?
) : BaseTransformationController<TwistGesture>(
    transformableNode!!, gestureRecognizer!!, selectionManager!!
) {
    // Rate that the node rotates in degrees per degree of twisting.
    var rotationRateDegrees = 2.5f
    override fun onContinueTransformation(gesture: TwistGesture) {
        val rotationAmount = -gesture.deltaRotationDegrees * rotationRateDegrees
        // TODO :  Move to kotlin-math
        val rotationDelta = Quaternion(Vector3.up(), rotationAmount)
        var localQuaternion = transformableNode.quaternion.toOldQuaternion()
        localQuaternion = Quaternion.multiply(localQuaternion, rotationDelta)
        transformableNode.quaternion = localQuaternion.toNewQuaternion()
    }

    override fun onEndTransformation(gesture: TwistGesture) {}
}

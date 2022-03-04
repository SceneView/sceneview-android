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
package com.google.ar.sceneform.ux

import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.math.MathHelper
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.utilities.Preconditions
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.Node
import io.github.sceneview.scene.BaseTransformationController
import io.github.sceneview.scene.SelectionManager
import io.github.sceneview.utils.FrameTime
import java.util.EnumSet

/**
 * Manipulates the position of a [BaseTransformableNode] using a [ ]. If not selected, the [BaseTransformableNode] will become selected
 * when the [DragGesture] starts.
 */
class TranslationController(
    transformableNode: Node, gestureRecognizer: DragGestureRecognizer,
    selectionManager: SelectionManager
) : BaseTransformationController<DragGesture>(
    transformableNode, gestureRecognizer, selectionManager
) {
    private var lastArHitResult: HitResult? = null
    private var desiredLocalPosition: Vector3? = null
    private var desiredLocalRotation: Quaternion? = null
    private val initialForwardInLocal = Vector3()
    /**
     * Gets a reference to the EnumSet that determines which types of ArCore Planes this
     * TranslationController is allowed to translate on.
     */
    /**
     * Sets which types of ArCore Planes this TranslationController is allowed to translate on.
     */
    var allowedPlaneTypes = EnumSet.allOf(
        Plane.Type::class.java
    )

    override fun onFrameUpdated(frameTime: FrameTime, node: Node) {
        updatePosition(frameTime)
        updateRotation(frameTime)
    }

    // As long as the transformable node is still interpolating towards the final pose, this
    // controller is still transforming.
    override val isTransforming: Boolean
        get() =// As long as the transformable node is still interpolating towards the final pose, this
            // controller is still transforming.
            super.isTransforming || desiredLocalRotation != null || desiredLocalPosition != null

    override fun canStartTransformation(gesture: DragGesture): Boolean {
        val targetNode = gesture.getTargetNode() ?: return false
        val transformableNode = transformableNode
        if (targetNode !== transformableNode && !targetNode.isDescendantOf(transformableNode)) {
            return false
        }
        if (!selectionManager.isSelected(transformableNode) && !selectionManager.select(
                transformableNode
            )
        ) {
            return false
        }
        val nodeTransformMatrix = transformableNode.transformationMatrix
        val nodePosition = Vector3()
        nodeTransformMatrix.decomposeTranslation(nodePosition)
        val nodeScale = Vector3()
        nodeTransformMatrix.decomposeScale(nodeScale)
        val nodeRotation = Quaternion()
        nodeTransformMatrix.decomposeRotation(nodeScale, nodeRotation)
        val nodeBack = Quaternion.rotateVector(nodeRotation, Vector3.back())
        val initialForwardInWorld = Quaternion.rotateVector(nodeRotation, Vector3.forward())
        val parent = transformableNode.parentNode
        if (parent != null) {
            val parentNodeTransformMatrix = parent.transformationMatrix
            val parentNodeScale = Vector3()
            parentNodeTransformMatrix.decomposeScale(parentNodeScale)
            val parentNodeRotation = Quaternion()
            parentNodeTransformMatrix.decomposeRotation(parentNodeScale, parentNodeRotation)
            initialForwardInLocal.set(
                Quaternion.inverseRotateVector(
                    parentNodeRotation,
                    initialForwardInWorld
                )
            )
        } else {
            initialForwardInLocal.set(initialForwardInWorld)
        }
        return true
    }

    override fun onContinueTransformation(gesture: DragGesture) {
        val scene = transformableNode.getSceneViewInternal() ?: return
        val frame = (scene as ArSceneView).currentFrame ?: return
        val arCamera = frame.camera
        if (arCamera.trackingState != TrackingState.TRACKING) {
            return
        }
        val position = gesture.position
        val hitResultList = frame.hitTests(position.x, position.y)
        for (i in hitResultList.indices) {
            val hit = hitResultList[i]
            val trackable = hit.trackable
            val pose = hit.hitPose
            if (trackable is Plane) {
                val plane = trackable
                if (plane.isPoseInPolygon(pose) && allowedPlaneTypes.contains(plane.type)) {
                    desiredLocalPosition = Vector3(pose.tx(), pose.ty(), pose.tz())
                    desiredLocalRotation = Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())
                    val parent = transformableNode.parentNode
                    if (parent != null && desiredLocalPosition != null && desiredLocalRotation != null) {
                        val parentNodeTransformMatrix = parent.transformationMatrix
                        val parentNodeScale = Vector3()
                        parentNodeTransformMatrix.decomposeScale(parentNodeScale)
                        val parentNodeRotation = Quaternion()
                        parentNodeTransformMatrix.decomposeRotation(
                            parentNodeScale,
                            parentNodeRotation
                        )
                        desiredLocalPosition =
                            parent.transformationMatrix.transformPoint(desiredLocalPosition)
                        desiredLocalRotation = Quaternion.multiply(
                            parentNodeRotation.inverted(),
                            Preconditions.checkNotNull(desiredLocalRotation)
                        )
                    }
                    desiredLocalRotation = calculateFinalDesiredLocalRotation(
                        Preconditions.checkNotNull(desiredLocalRotation)
                    )
                    lastArHitResult = hit
                    break
                }
            }
        }
    }

    override fun onEndTransformation(gesture: DragGesture) {
        val hitResult = lastArHitResult ?: return
        if (hitResult.trackable.trackingState == TrackingState.TRACKING) {
            val anchorNode = anchorNodeOrDie
            val oldAnchor = anchorNode.anchor
            oldAnchor?.detach()
            val newAnchor = hitResult.createAnchor()
            anchorNode.anchor = newAnchor

// TODO: View if it is usefull
//      Vector3 worldPosition = getTransformableNode().getWorldPosition();
//      Quaternion worldRotation = getTransformableNode().getWorldRotation();
//      Quaternion finalDesiredWorldRotation = worldRotation;
//
//      // Since we change the anchor, we need to update the initialForwardInLocal into the new
//      // coordinate space. Local variable for nullness analysis.
//      Quaternion desiredLocalRotation = this.desiredLocalRotation;
//      if (desiredLocalRotation != null) {
//        getTransformableNode().setQuaternion(desiredLocalRotation);
//        finalDesiredWorldRotation = getTransformableNode().getWorldRotation();
//      }
//
//      anchorNode.setAnchor(newAnchor);
//
//      // Temporarily set the node to the final world rotation so that we can accurately
//      // determine the initialForwardInLocal in the new coordinate space.
//      getTransformableNode().setWorldRotation(finalDesiredWorldRotation);
//      Vector3 initialForwardInWorld = getTransformableNode().getForward();
//      initialForwardInLocal.set(anchorNode.worldToLocalDirection(initialForwardInWorld));
//
//      getTransformableNode().setWorldRotation(worldRotation);
//      getTransformableNode().setWorldPosition(worldPosition);
        }
        desiredLocalPosition = Vector3.zero()
        desiredLocalRotation = calculateFinalDesiredLocalRotation(Quaternion.identity())
    }

    private val anchorNodeOrDie: ArNode
        get() {
            val parent = transformableNode.parent
            check(parent is ArNode) { "TransformableNode must have an ArNode as a parent." }
            return parent
        }

    private fun updatePosition(frameTime: FrameTime) {
        // Store in local variable for nullness static analysis.
        val desiredLocalPosition = desiredLocalPosition ?: return
        var localPosition = transformableNode.position.toVector3()
        val lerpFactor = MathHelper.clamp(frameTime.intervalSeconds.toFloat() * LERP_SPEED, 0f, 1f)
        localPosition = Vector3.lerp(localPosition, desiredLocalPosition, lerpFactor)
        val lengthDiff = Math.abs(Vector3.subtract(desiredLocalPosition, localPosition).length())
        if (lengthDiff <= POSITION_LENGTH_THRESHOLD) {
            localPosition = desiredLocalPosition
            this.desiredLocalPosition = null
        }
        transformableNode.position = localPosition.toFloat3()
    }

    private fun updateRotation(frameTime: FrameTime) {
        // Store in local variable for nullness static analysis.
        val desiredLocalRotation = desiredLocalRotation ?: return

        // TODO :  Move to kotlin-math
        var localQuaternion = transformableNode.quaternion.toOldQuaternion()
        val lerpFactor = MathHelper.clamp(frameTime.intervalSeconds.toFloat() * LERP_SPEED, 0f, 1f)
        localQuaternion = Quaternion.slerp(localQuaternion, desiredLocalRotation, lerpFactor)
        val dot = Math.abs(dotQuaternion(localQuaternion, desiredLocalRotation))
        if (dot >= ROTATION_DOT_THRESHOLD) {
            localQuaternion = desiredLocalRotation
            this.desiredLocalRotation = null
        }
        transformableNode.quaternion = localQuaternion.toNewQuaternion()
    }

    /**
     * When translating, the up direction of the node must match the up direction of the plane from
     * the hit result. However, we also need to make sure that the original forward direction of the
     * node is respected.
     */
    private fun calculateFinalDesiredLocalRotation(desiredLocalRotation: Quaternion): Quaternion {
        // Get a rotation just to the up direction.
        // Otherwise, the node will spin around as you rotate.
        var desiredLocalRotation = desiredLocalRotation
        val rotatedUp = Quaternion.rotateVector(desiredLocalRotation, Vector3.up())
        desiredLocalRotation = Quaternion.rotationBetweenVectors(Vector3.up(), rotatedUp)

        // Adjust the rotation to make sure the node maintains the same forward direction.
        val forwardInLocal =
            Quaternion.rotationBetweenVectors(Vector3.forward(), initialForwardInLocal)
        desiredLocalRotation = Quaternion.multiply(desiredLocalRotation, forwardInLocal)
        return desiredLocalRotation.normalized()
    }

    companion object {
        private const val LERP_SPEED = 12.0f
        private const val POSITION_LENGTH_THRESHOLD = 0.01f
        private const val ROTATION_DOT_THRESHOLD = 0.99f
        private fun dotQuaternion(lhs: Quaternion, rhs: Quaternion): Float {
            return lhs.x * rhs.x + lhs.y * rhs.y + lhs.z * rhs.z + lhs.w * rhs.w
        }
    }
}
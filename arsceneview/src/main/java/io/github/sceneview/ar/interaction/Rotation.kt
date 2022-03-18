package io.github.sceneview.ar.interaction

import android.util.Log
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.node.ArNode

class RotationGesture(arNode: ArNode) : GestureStrategy(arNode) {
    override fun rotate(rotateDelta: Float) {
        val rotationDelta = Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), rotateDelta * 2.5f)
arNode.modelQuaternion = arNode.modelQuaternion * rotationDelta
    }
}
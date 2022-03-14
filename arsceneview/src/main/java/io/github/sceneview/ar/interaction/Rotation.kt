package io.github.sceneview.ar.interaction

import android.util.Log
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.node.ArNode

class Rotation(arNode: ArNode) : GestureStrategy(arNode) {
    override fun rotate(rotateDelta: Float) {
        val rotationDelta = Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), rotateDelta * 2.5f)
        val localQuaternion = arNode.modelQuaternion * rotationDelta
        arNode.modelQuaternion = localQuaternion
        Log.d("Rotate", "Rotate delta: ${rotateDelta * 2.5f}")
    }
}
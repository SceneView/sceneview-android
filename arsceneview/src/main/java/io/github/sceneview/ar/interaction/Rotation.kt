package io.github.sceneview.ar.interaction

import android.util.Log
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.degrees
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.ar.node.ArNode

class RotationGesture(arNode: ArNode) : GestureStrategy(arNode) {
    override fun rotate(rotateDelta: Float) {
        Log.d("Rotation", "Rotation delta: $rotateDelta")
        val rotationDelta = normalize(Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), degrees(rotateDelta)))
        arNode.modelQuaternion = arNode.modelQuaternion * rotationDelta
    }
}

package io.github.sceneview.ar.interaction

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.clamp
import io.github.sceneview.ar.node.ArNode

class ScaleGesture(arNode: ArNode) : GestureStrategy(arNode) {
    override fun scale(scaleFactor: Float) {
        arNode.scale = clamp(
                arNode.scale + Float3(scaleFactor, scaleFactor, scaleFactor), 0.5f, 1.5f)
    }
}

package io.github.sceneview.collision

import android.animation.TypeEvaluator

/**
 * TypeEvaluator for Vector3. Used to animate positions and other vectors.
 */
class Vector3Evaluator : TypeEvaluator<Vector3> {
    override fun evaluate(fraction: Float, startValue: Vector3, endValue: Vector3): Vector3 {
        return Vector3.lerp(startValue, endValue, fraction)
    }
}

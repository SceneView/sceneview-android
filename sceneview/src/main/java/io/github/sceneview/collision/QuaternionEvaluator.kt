package io.github.sceneview.collision

import android.animation.TypeEvaluator

/** TypeEvaluator for Quaternions. Used to animate rotations. */
class QuaternionEvaluator : TypeEvaluator<Quaternion> {
    override fun evaluate(fraction: Float, startValue: Quaternion, endValue: Quaternion): Quaternion {
        return Quaternion.slerp(startValue, endValue, fraction)
    }
}

package io.github.sceneview.animation

import android.animation.FloatArrayEvaluator
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.components.TransformComponent
import io.github.sceneview.math.*

object TransformAnimator {

    class Float3Evaluator : TypeEvaluator<Float3> {
        val evaluator = FloatArrayEvaluator()

        override fun evaluate(fraction: Float, startValue: Float3, endValue: Float3) =
            evaluator.evaluate(fraction, startValue.toFloatArray(), endValue.toFloatArray())
                .toFloat3()
    }

    class QuaternionEvaluator : TypeEvaluator<Quaternion> {
        val evaluator = FloatArrayEvaluator()

        override fun evaluate(fraction: Float, startValue: Quaternion, endValue: Quaternion) =
            evaluator.evaluate(fraction, startValue.toFloatArray(), endValue.toFloatArray())
                .toQuaternion()
    }

    class TransformEvaluator : TypeEvaluator<Transform> {
        val evaluator = FloatArrayEvaluator()
        override fun evaluate(fraction: Float, startValue: Transform, endValue: Transform) =
            evaluator.evaluate(fraction, startValue.toFloatArray(), endValue.toFloatArray())
                .toTransform()
    }

    fun ofPosition(transformable: TransformComponent, vararg positions: Position) =
        ObjectAnimator.ofObject(transformable, "position", Float3Evaluator(), *positions)

    fun ofQuaternion(transformable: TransformComponent, vararg quaternions: Quaternion) =
        ObjectAnimator.ofObject(transformable, "quaternion", QuaternionEvaluator(), *quaternions)

    fun ofRotation(transformable: TransformComponent, vararg rotations: Rotation) =
        ObjectAnimator.ofObject(transformable, "rotation", Float3Evaluator(), *rotations)

    fun ofScale(transformable: TransformComponent, vararg scales: Scale) =
        ObjectAnimator.ofObject(transformable, "scale", Float3Evaluator(), *scales)

    fun ofTransform(transformable: TransformComponent, vararg transforms: Transform) =
        ObjectAnimator.ofObject(transformable, "transform", TransformEvaluator(), *transforms)
}
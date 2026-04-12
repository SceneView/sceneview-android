package io.github.sceneview.animation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform
import io.github.sceneview.math.quaternion
import io.github.sceneview.node.Node

object NodeAnimator {

    fun ofTransform(node: Node, vararg transforms: Transform) = AnimatorSet().apply {
        playTogether(
            ofPosition(node, *transforms.map { it.position }.toTypedArray()),
            ofQuaternion(node, *transforms.map { it.quaternion }.toTypedArray()),
            ofScale(node, *transforms.map { it.scale }.toTypedArray())
        )
    }

    fun ofPosition(node: Node, vararg positions: Position): ObjectAnimator {
        val target = Position(node.position)
        return ObjectAnimator.ofPropertyValuesHolder(
            target,
            PropertyValuesHolder.ofFloat("x", *positions.map { it.x }.toFloatArray()),
            PropertyValuesHolder.ofFloat("y", *positions.map { it.y }.toFloatArray()),
            PropertyValuesHolder.ofFloat("z", *positions.map { it.z }.toFloatArray())
        ).apply {
            addUpdateListener { node.position = target }
        }
    }

    fun ofQuaternion(node: Node, vararg quaternions: Quaternion): ObjectAnimator {
        val target = Quaternion(node.quaternion)
        return ObjectAnimator.ofPropertyValuesHolder(
            target,
            PropertyValuesHolder.ofFloat("x", *quaternions.map { it.x }.toFloatArray()),
            PropertyValuesHolder.ofFloat("y", *quaternions.map { it.y }.toFloatArray()),
            PropertyValuesHolder.ofFloat("z", *quaternions.map { it.z }.toFloatArray()),
            PropertyValuesHolder.ofFloat("w", *quaternions.map { it.w }.toFloatArray())
        ).apply {
            addUpdateListener { node.quaternion = target }
        }
    }

    fun ofRotation(node: Node, vararg rotations: Rotation): ObjectAnimator {
        val target = Rotation(node.rotation)
        return ObjectAnimator.ofPropertyValuesHolder(
            target,
            PropertyValuesHolder.ofFloat("x", *rotations.map { it.x }.toFloatArray()),
            PropertyValuesHolder.ofFloat("y", *rotations.map { it.y }.toFloatArray()),
            PropertyValuesHolder.ofFloat("z", *rotations.map { it.z }.toFloatArray())
        ).apply {
            addUpdateListener { node.rotation = target }
        }
    }

    fun ofScale(node: Node, vararg scales: Scale): ObjectAnimator {
        val target = Scale(node.scale)
        return ObjectAnimator.ofPropertyValuesHolder(
            target,
            PropertyValuesHolder.ofFloat("x", *scales.map { it.x }.toFloatArray()),
            PropertyValuesHolder.ofFloat("y", *scales.map { it.y }.toFloatArray()),
            PropertyValuesHolder.ofFloat("z", *scales.map { it.z }.toFloatArray())
        ).apply {
            addUpdateListener { node.scale = target }
        }
    }
}

package io.github.sceneview.rendering

import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.inverse
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform
import io.github.sceneview.math.times
import io.github.sceneview.math.toQuaternion
import io.github.sceneview.math.toRotation

/**
 * Cross-platform spatial transform operations for scene graph nodes.
 *
 * Provides local ↔ world space conversion using pure kotlin-math types.
 * Platform-specific Node classes can delegate transform math to this.
 */
object NodeTransform {

    /**
     * Converts a world position to local space relative to the given parent transform.
     */
    fun getLocalPosition(parentWorldTransform: Transform, worldPosition: Position): Position {
        val worldToLocal = inverse(parentWorldTransform)
        return (worldToLocal * worldPosition)
    }

    /**
     * Converts a local position to world space given the parent's world transform.
     */
    fun getWorldPosition(parentWorldTransform: Transform, localPosition: Position): Position {
        return (parentWorldTransform * localPosition)
    }

    /**
     * Converts a world quaternion to local space.
     */
    fun getLocalQuaternion(parentWorldQuaternion: Quaternion, worldQuaternion: Quaternion): Quaternion {
        return inverse(parentWorldQuaternion) * worldQuaternion
    }

    /**
     * Converts a local quaternion to world space.
     */
    fun getWorldQuaternion(parentWorldQuaternion: Quaternion, localQuaternion: Quaternion): Quaternion {
        return parentWorldQuaternion * localQuaternion
    }

    /**
     * Converts a world rotation (Euler angles) to local space.
     */
    fun getLocalRotation(parentWorldQuaternion: Quaternion, worldRotation: Rotation): Rotation {
        return getLocalQuaternion(parentWorldQuaternion, worldRotation.toQuaternion()).toRotation()
    }

    /**
     * Converts a local rotation (Euler angles) to world space.
     */
    fun getWorldRotation(parentWorldQuaternion: Quaternion, localRotation: Rotation): Rotation {
        return getWorldQuaternion(parentWorldQuaternion, localRotation.toQuaternion()).toRotation()
    }

    /**
     * Converts a world transform to local space.
     */
    fun getLocalTransform(parentWorldTransform: Transform, worldTransform: Transform): Transform {
        return inverse(parentWorldTransform) * worldTransform
    }

    /**
     * Converts a local transform to world space.
     */
    fun getWorldTransform(parentWorldTransform: Transform, localTransform: Transform): Transform {
        return parentWorldTransform * localTransform
    }
}

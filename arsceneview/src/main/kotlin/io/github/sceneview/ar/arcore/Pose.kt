package io.github.sceneview.ar.arcore

import com.google.ar.core.Pose
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.utils.Direction
import io.github.sceneview.utils.Position

val Pose.position: Position
    get() = Position(x = tx(), y = ty(), z = tz())

val Pose.rotation: Quaternion
    get() = Quaternion(x = qx(), y = qy(), z = qz(), w = qw())

val Pose.xDirection: Direction
    get() = xAxis!!.let { (x, y, z) ->
        Direction(x, y, z)
    }

val Pose.yDirection: Direction
    get() = yAxis!!.let { (x, y, z) ->
        Direction(x, y, z)
    }

val Pose.zDirection: Direction
    get() = zAxis!!.let { (x, y, z) ->
        Direction(x, y, z)
    }

/**
 * Calculate the normal distance from this to the other, the given other pose should have y axis
 * parallel to plane's normal, for example plane's center pose or hit test pose.
 */
fun Pose.distanceTo(other: Pose): Float {
    val normal = FloatArray(3)
    // Get transformed Y axis of plane's coordinate system.
    getTransformedAxis(1, 1.0f, normal, 0)
    // Compute dot product of plane's normal with vector from camera to plane center.
    return (other.tx() - tx()) * normal[0] + (other.ty() - ty()) * normal[1] + (other.tz() - tz()) * normal[2]
}

// Calculate the normal distance to plane from cameraPose, the given planePose should have y axis
// parallel to plane's normal, for example plane's center pose or hit test pose.
fun Pose.calculateDistanceToPlane(cameraPose: Pose): Float {
    val normal = FloatArray(3)
    val cameraX = cameraPose.tx()
    val cameraY = cameraPose.ty()
    val cameraZ = cameraPose.tz()
    // Get transformed Y axis of plane's coordinate system.
    this.getTransformedAxis(1, 1.0f, normal, 0)
    // Compute dot product of plane's normal with vector from camera to plane center.
    return (cameraX - this.tx()) * normal[0] + (cameraY - this.ty()) * normal[1] + (cameraZ - this.tz()) * normal[2]
}

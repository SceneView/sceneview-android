package io.github.sceneview.ar.arcore

import com.google.ar.core.GeospatialPose
import com.google.ar.core.Pose
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.rotation
import dev.romainguy.kotlin.math.translation
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toPosition
import io.github.sceneview.math.toTransform

val Pose.position: Position
    get() = Position(x = tx(), y = ty(), z = tz())

val Pose.quaternion: Quaternion
    get() = Quaternion(x = qx(), y = qy(), z = qz(), w = qw())

val Pose.transform: Transform
    get() = FloatArray(16).apply { toMatrix(this, 0) }.toTransform()
//    get() = translation(position) * rotation(quaternion)

val Pose.rotation: Rotation
    get() = quaternion.toEulerAngles()

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
 * Calculate the normal distance from this to the other.
 *
 * The given other pose should have y axis parallel to plane's normal, for example plane's center
 * pose or hit test pose.
 */
fun Pose.distanceTo(other: Pose): Float {
    val normal = FloatArray(3)
    // Get transformed Y axis of plane's coordinate system.
    getTransformedAxis(1, 1.0f, normal, 0)
    // Compute dot product of plane's normal with vector from camera to plane center.
    return (other.tx() - tx()) * normal[0] + (other.ty() - ty()) * normal[1] + (other.tz() - tz()) * normal[2]
}

/**
 * Calculate the normal distance to plane from camera Pose
 *
 * The given planePose should have y axis parallel to plane's normal, for example plane's center
 * pose or hit test pose.
 */
fun Pose.distanceToPlane(cameraPose: Pose): Float {
    val normal = FloatArray(3).apply {
        // Get transformed Y axis of plane's coordinate system.
        getTransformedAxis(1, 1.0f, this, 0)
    }.toPosition()
    val position = this.position
    val cameraPosition = cameraPose.position
    // Compute dot product of plane's normal with vector from camera to plane center.
    // TODO:Use Kotlin math distance?
    return dot((cameraPosition - position), normal)
//    return (cameraPosition.x - this.tx()) * normal[0] +
//            (cameraPosition.y - this.ty()) * normal[1] +
//            (cameraPosition.z - this.tz()) * normal[2]
}

val GeospatialPose.transform: Transform
    get() = translation(
        Position(
            latitude.toFloat(),
            longitude.toFloat(),
            altitude.toFloat()
        )
    ) * rotation(eastUpSouthQuaternion.let { Quaternion(it[0], it[1], it[2], it[3]) })
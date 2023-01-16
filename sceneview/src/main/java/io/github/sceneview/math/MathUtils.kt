package io.github.sceneview.math

import com.google.ar.sceneform.math.MathHelper
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.*

const val DEFAULT_EPSILON = 0.001f

typealias Position = Float3
typealias Rotation = Float3
typealias Scale = Float3
typealias Direction = Float3
typealias Size = Float3
typealias Axis = VectorComponent
typealias Transform = Mat4

fun Transform(position: Position, quaternion: Quaternion, scale: Scale) =
    translation(position) * rotation(quaternion) * scale(scale)

fun Transform(position: Position, rotation: Rotation, scale: Scale) =
    translation(position) * rotation(rotation.toQuaternion()) * scale(scale)

fun FloatArray.toFloat3() = this.let { (x, y, z) -> Float3(x, y, z) }
fun FloatArray.toFloat4() = this.let { (x, y, z, w) -> Float4(x, y, z, w) }
fun FloatArray.toPosition() = this.let { (x, y, z) -> Position(x, y, z) }
fun FloatArray.toRotation() = this.let { (x, y, z) -> Rotation(x, y, z) }
fun FloatArray.toScale() = this.let { (x, y, z) -> Scale(x, y, z) }
fun FloatArray.toDirection() = this.let { (x, y, z) -> Direction(x, y, z) }

fun Rotation.toQuaternion(order: RotationsOrder = RotationsOrder.ZYX) =
    Quaternion.fromEuler(this, order)

fun Quaternion.toRotation(order: RotationsOrder = RotationsOrder.ZYX) = eulerAngles(this, order)

//TODO: Remove when everything use Float3
fun Float3.toVector3() = Vector3(x, y, z)

//TODO: Remove when everything use Float3
fun Vector3.toFloat3() = Float3(x, y, z)

//TODO: Remove when everything use Quaternion
fun Quaternion.toOldQuaternion() = com.google.ar.sceneform.math.Quaternion(x, y, z, w)

//TODO: Remove when everything use Quaternion
fun com.google.ar.sceneform.math.Quaternion.toNewQuaternion() = Quaternion(x, y, z, w)

val Mat4.quaternion: Quaternion
    get() = rotation(this).toQuaternion()

fun Mat4.toColumnsFloatArray() = floatArrayOf(
    x.x, x.y, x.z, x.w,
    y.x, y.y, y.z, y.w,
    z.x, z.y, z.z, z.w,
    w.x, w.y, w.z, w.w
)

fun lerp(
    start: Float3,
    end: Float3,
    deltaSeconds: Float,
    epsilon: Float = DEFAULT_EPSILON
): Float3 {
    return if (!equals(start, end, epsilon)) {
        return mix(start, end, deltaSeconds)
    } else end
}

fun lerp(
    start: Quaternion,
    end: Quaternion,
    deltaSeconds: Float,
    epsilon: Float = DEFAULT_EPSILON
): Quaternion {
    return if (!equals(start, end, epsilon)) {
        return dev.romainguy.kotlin.math.lerp(start, end, deltaSeconds)
    } else end
}

fun slerp(
    start: Quaternion,
    end: Quaternion,
    deltaSeconds: Float,
    epsilon: Float = DEFAULT_EPSILON
): Quaternion {
    return if (!equals(start, end, epsilon)) {
        return dev.romainguy.kotlin.math.slerp(start, end, deltaSeconds)
    } else end
}

/**
 * ### Spherical Linear Interpolate a transform
 *
 * @epsilon Smooth Epsilon minimum equality limit
 * This is used to avoid very near positions/rotations/scale smooth modifications.
 * It prevents:
 * - The modifications from appearing too quick if the ranges are too close because of linearly
 * interpolation for upper dot products
 * - The end value to take to much time to be reached
 * - The end to never be reached because of matrices calculations floating points
 */
fun slerp(
    start: Transform,
    end: Transform,
    deltaSeconds: Double,
    speed: Float,
    epsilon: Float = DEFAULT_EPSILON
): Transform {
    return if (!equals(start, end, epsilon)) {
        val lerpFactor = MathHelper.clamp((deltaSeconds * speed).toFloat(), 0.0f, 1.0f)
        Transform(
            position = lerp(start.position, end.position, lerpFactor, epsilon),
            quaternion = slerp(start.quaternion, end.quaternion, lerpFactor, epsilon),
            scale = lerp(start.scale, end.scale, lerpFactor, epsilon)
        )
    } else end
}

/**
 * If rendering in linear space, first convert the values to linear space by rising to the power 2.2
 */
fun FloatArray.toLinearSpace() = map { pow(it, 2.2f) }.toFloatArray()

fun lookAt(eye: Position, target: Position): Mat4 {
    return lookAt(eye, target - eye, Direction(y = 1.0f))
}

fun lookTowards(eye: Position, direction: Direction) =
    lookTowards(eye, direction, Direction(y = 1.0f)).toQuaternion()
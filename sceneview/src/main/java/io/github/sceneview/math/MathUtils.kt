package io.github.sceneview.math

import com.google.android.filament.Box
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.*

typealias Position = Float3
typealias Rotation = Float3
typealias Scale = Float3
typealias Direction = Float3
typealias Size = Float3
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

fun FloatArray.toSize() = this.let { (x, y, z) -> Size(x, y, z) }

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

fun FloatArray.toTransform() = Transform(
    x = Float4(this[0], this[1], this[2], this[3]),
    y = Float4(this[4], this[5], this[6], this[7]),
    z = Float4(this[8], this[9], this[10], this[11]),
    w = Float4(this[12], this[13], this[14], this[15])
)

fun lerp(start: Float3, end: Float3, deltaSeconds: Float) = mix(start, end, deltaSeconds)

fun normalToTangent(normal: Float3): Quaternion {
    var tangent: Float3
    val bitangent: Float3

    // Calculate basis vectors (+x = tangent, +y = bitangent, +z = normal).
    tangent = cross(Direction(y = 1.0f), normal)

    // Uses almostEqualRelativeAndAbs for equality checks that account for float inaccuracy.
    if (dot(tangent, tangent) == 0.0f) {
        bitangent = normalize(cross(normal, Direction(x = 1.0f)))
        tangent = normalize(cross(bitangent, normal))
    } else {
        tangent = normalize(tangent)
        bitangent = normalize(cross(normal, tangent))
    }
    // Rotation of a 4x4 Transformation Matrix is represented by the top-left 3x3 elements.
    return Transform(right = tangent, up = bitangent, forward = normal).toQuaternion()
}

fun Box(center: Position, halfExtent: Size) = Box(center.toFloatArray(), halfExtent.toFloatArray())
var Box.centerPosition: Position
    get() = center.toPosition()
    set(value) {
        setCenter(value.x, value.y, value.z)
    }
var Box.halfExtentSize: Size
    get() = halfExtent.toSize()
    set(value) {
        setHalfExtent(value.x, value.y, value.z)
    }
var Box.size
    get() = halfExtentSize * 2.0f
    set(value) {
        halfExtentSize = value / 2.0f
    }

fun Box.toVector3Box(): com.google.ar.sceneform.collision.Box {
    val halfExtent = halfExtent
    val center = center
    return com.google.ar.sceneform.collision.Box(
        Vector3(halfExtent[0], halfExtent[1], halfExtent[2]).scaled(2.0f),
        Vector3(center[0], center[1], center[2])
    )
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
    speed: Float
): Transform {
    val lerpFactor = clamp((deltaSeconds * speed).toFloat(), 0.0f, 1.0f)
    return Transform(
        position = lerp(start.position, end.position, lerpFactor),
        quaternion = slerp(start.quaternion, end.quaternion, lerpFactor),
        scale = lerp(start.scale, end.scale, lerpFactor)
    )
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
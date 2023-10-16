package io.github.sceneview.math

import com.google.android.filament.Box
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
fun DoubleArray.toFloat4() = this.map { it.toFloat() }.let { (x, y, z, w) -> Float4(x, y, z, w) }
fun FloatArray.toPosition() = this.let { (x, y, z) -> Position(x, y, z) }
fun FloatArray.toRotation() = this.let { (x, y, z) -> Rotation(x, y, z) }
fun FloatArray.toScale() = this.let { (x, y, z) -> Scale(x, y, z) }
fun FloatArray.toDirection() = this.let { (x, y, z) -> Direction(x, y, z) }
fun FloatArray.toQuaternion() = this.let { (x, y, z, w) -> Quaternion(x, y, z, w) }
fun FloatArray.toSize() = this.let { (x, y, z) -> Size(x, y, z) }
fun FloatArray.toTransform() = Transform.of(*this)
/**
 * Converts a column-major [FloatArray] to a [Transform]
 */
fun FloatArray.toTransformTransposed(): Transform {
    require(size >= 16)
    return Transform(
        Float4(this[0], this[1], this[2], this[3]),
        Float4(this[4], this[5], this[6], this[7]),
        Float4(this[8], this[9], this[10], this[11]),
        Float4(this[12], this[13], this[14], this[15])
    )
}
fun DoubleArray.toTransform() = Transform.of(*this.map { it.toFloat() }.toFloatArray())
/**
 * Converts a column-major [DoubleArray] to a [Transform]
 */
fun DoubleArray.toTransformTransposed() = this.map { it.toFloat() }.toFloatArray().toTransformTransposed()
fun Mat4.toDoubleArray() : DoubleArray = toFloatArray().map { it.toDouble() }.toDoubleArray()
val Mat4.quaternion: Quaternion get() = rotation(this).toQuaternion()

operator fun Mat4.times(v: Float3) = (this * Float4(v, 1f)).xyz

fun Rotation.toQuaternion(order: RotationsOrder = RotationsOrder.ZYX) =
    Quaternion.fromEuler(this, order)

fun Quaternion.toRotation(order: RotationsOrder = RotationsOrder.ZYX) = eulerAngles(this, order)

fun min(a: List<Float3>): Float3 {
    var min = Float3()
    a.forEach {
        min = min(min, it)
    }
    return min
}

fun max(a: List<Float3>): Float3 {
    var max = Float3()
    a.forEach {
        max = max(max, it)
    }
    return max
}

fun Mat4.toColumnsFloatArray() = floatArrayOf(
    x.x, x.y, x.z, x.w,
    y.x, y.y, y.z, y.w,
    z.x, z.y, z.z, z.w,
    w.x, w.y, w.z, w.w
)

/**
 * If rendering in linear space, first convert the values to linear space by rising to the power 2.2
 */
fun FloatArray.toLinearSpace() = map { pow(it, 2.2f) }.toFloatArray()

data class LookAt(val eye: Position, val target: Position, val upward: Direction)

fun lookAt(eye: Position, target: Position): Mat4 {
    return lookAt(eye, target - eye, Direction(y = 1.0f))
}

fun lookTowards(eye: Position, direction: Direction) =
    lookTowards(eye, direction, Direction(y = 1.0f)).toQuaternion()

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

package io.github.sceneview.math

import com.google.android.filament.Box
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.Ray
import dev.romainguy.kotlin.math.RotationsOrder
import dev.romainguy.kotlin.math.clamp
import dev.romainguy.kotlin.math.cross
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.eulerAngles
import dev.romainguy.kotlin.math.lookAt
import dev.romainguy.kotlin.math.lookTowards
import dev.romainguy.kotlin.math.mix
import dev.romainguy.kotlin.math.normalize
import dev.romainguy.kotlin.math.pow
import dev.romainguy.kotlin.math.rotation
import dev.romainguy.kotlin.math.scale
import dev.romainguy.kotlin.math.slerp
import dev.romainguy.kotlin.math.translation
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.Vector3
import kotlin.math.absoluteValue

typealias Position = Float3
typealias Rotation = Float3
typealias Scale = Float3
typealias Direction = Float3
typealias Size = Float3
typealias Transform = Mat4
typealias Color = Float4

fun Transform(
    position: Position = Position(),
    quaternion: Quaternion = Quaternion(),
    scale: Scale = Scale(1.0f)
) = translation(position) * rotation(quaternion) * scale(scale)

fun Transform(
    position: Position = Position(),
    rotation: Rotation,
    scale: Scale = Scale(1.0F)
) = translation(position) * rotation(rotation.toQuaternion()) * scale(scale)

fun FloatArray.toFloat3() = this.let { (x, y, z) -> Float3(x, y, z) }
fun FloatArray.toFloat4() = this.let { (x, y, z, w) -> Float4(x, y, z, w) }
fun DoubleArray.toFloat4() = this.map { it.toFloat() }.let { (x, y, z, w) -> Float4(x, y, z, w) }
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
fun Quaternion.toOldQuaternion() =
    io.github.sceneview.collision.Quaternion(x, y, z, w)

//TODO: Remove when everything use Quaternion
fun io.github.sceneview.collision.Quaternion.toNewQuaternion() = Quaternion(x, y, z, w)

fun Mat4.toColumnsDoubleArray(): DoubleArray =
    toColumnsFloatArray().map { it.toDouble() }.toDoubleArray()

val Mat4.quaternion: Quaternion
    get() = rotation(this).toQuaternion()

operator fun Mat4.times(v: Float3) = (this * Float4(v, 1f)).xyz

fun Mat4.toMatrix() = Matrix(toColumnsFloatArray())

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

fun DoubleArray.toTransform() = this.map { it.toFloat() }.toFloatArray().toTransform()

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

fun Box.toVector3Box(): io.github.sceneview.collision.Box =
    io.github.sceneview.collision.Box(
        (halfExtentSize * 2.0f).toVector3(),
        centerPosition.toVector3()
    )

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


// TODO: Integrated in Kotlin-math 1.34.1 remove this when published.
/////////////////////////////////////////////////////////////////////
fun Float.compareTo(v: Float, delta: Float): Float = when {
    equals(v, delta) -> 0.0f
    else -> compareTo(v).toFloat()
}

fun Float.equals(v: Float, delta: Float) = (this - v).absoluteValue < delta

fun Float2.compareTo(v: Float, delta: Float = 0.0f) = Float2(
    x.compareTo(v, delta),
    y.compareTo(v, delta)
)

fun Float2.equals(v: Float, delta: Float = 0.0f) = x.equals(v, delta) && y.equals(v, delta)

fun Float2.compareTo(v: Float2, delta: Float = 0.0f) = Float2(
    x.compareTo(v.x, delta),
    y.compareTo(v.y, delta)
)

fun Float2.equals(v: Float2, delta: Float = 0.0f) = x.equals(v.x, delta) && y.equals(v.y, delta)

fun Float3.compareTo(v: Float, delta: Float = 0.0f) = Float3(
    xy.compareTo(v, delta),
    z.compareTo(v, delta)
)

fun Float3.equals(v: Float, delta: Float = 0.0f) = xy.equals(v, delta) && z.equals(v, delta)

fun Float3.compareTo(v: Float3, delta: Float = 0.0f) = Float3(
    xy.compareTo(v.xy, delta),
    z.compareTo(v.z, delta)
)

fun Float3.equals(v: Float3, delta: Float = 0.0f) =
    xy.equals(v.xy, delta) && z.equals(v.z, delta)

fun Float4.compareTo(v: Float, delta: Float = 0.0f) = Float4(
    xyz.compareTo(v, delta),
    w.compareTo(v, delta)
)

fun Float4.equals(v: Float, delta: Float = 0.0f) = xyz.equals(v, delta) && w.equals(v, delta)

fun Float4.compareTo(v: Float4, delta: Float = 0.0f) = Float4(
    xyz.compareTo(v.xyz, delta),
    w.compareTo(v.w, delta)
)

fun Float4.equals(v: Float4, delta: Float = 0.0f) =
    xyz.equals(v.xyz, delta) && w.equals(v.w, delta)

fun Mat4.compareTo(m: Mat4, delta: Float = 0.0f) = Mat4(
    x.compareTo(m.x, delta),
    y.compareTo(m.y, delta),
    z.compareTo(m.z, delta),
    w.compareTo(m.w, delta)
)

fun Mat4.equals(m: Mat4, delta: Float = 0.0f) =
    x.equals(m.x, delta) && y.equals(m.y, delta) && z.equals(m.z, delta) && w.equals(m.w, delta)

fun colorOf(r: Float = 0.0f, g: Float = 0.0f, b: Float = 0.0f, a: Float = 1.0f) = Color(r, g, b, a)
fun colorOf(rgb: Float = 0.0f, a: Float = 1.0f) = colorOf(r = rgb, g = rgb, b = rgb, a = a)
fun colorOf(color: androidx.compose.ui.graphics.Color) = colorOf(
    r = color.red,
    g = color.green,
    b = color.blue,
    a = color.alpha
)

fun colorOf(color: Int) = colorOf(
    r = android.graphics.Color.red(color) / 255.0f,
    g = android.graphics.Color.green(color) / 255.0f,
    b = android.graphics.Color.blue(color) / 255.0f,
    a = android.graphics.Color.alpha(color) / 255.0f
)

fun FloatArray.toColor() = Color(this[0], this[1], this[2], this.getOrNull(3) ?: 1.0f)

/**
 * If rendering in linear space, first convert the gray scaled values to linear space by rising to
 * the power 2.2
 */
fun Color.toLinearSpace() = transform { com.google.android.filament.utils.pow(it, 2.2f) }

fun Ray.toCollisionRay() =
    io.github.sceneview.collision.Ray(origin.toVector3(), direction.toVector3())
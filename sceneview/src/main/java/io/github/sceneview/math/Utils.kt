package io.github.sceneview.math

import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3.cross
import dev.romainguy.kotlin.math.*


typealias Position = Float3
typealias Rotation = Float3
typealias Scale = Float3
typealias Direction = Float3
typealias Size = Float3
typealias Transform = Mat4

fun FloatArray.toFloat3() = this.let { (x, y, z) -> Float3(x, y, z) }
fun FloatArray.toFloat4() = this.let { (x, y, z, w) -> Float4(x, y, z, w) }
fun FloatArray.toPosition() = this.let { (x, y, z) -> Position(x, y, z) }
fun FloatArray.toRotation() = this.let { (x, y, z) -> Rotation(x, y, z) }
fun FloatArray.toScale() = this.let { (x, y, z) -> Scale(x, y, z) }
fun FloatArray.toDirection() = this.let { (x, y, z) -> Direction(x, y, z) }

fun Rotation.toQuaternion(order: RotationsOrder = RotationsOrder.ZYX) =
    Quaternion.fromEuler(this, order)

fun Quaternion.toRotation(order: RotationsOrder = RotationsOrder.ZYX) = eulerAngles(this, order)

fun FloatArray.toTransform(): Transform = toMat4()

//TODO: Remove when everything use Float3
fun Float3.toVector3() = Vector3(x, y, z)

//TODO: Remove when everything use Float3
fun Vector3.toFloat3() = Float3(x, y, z)

//TODO: Remove when everything use Quaternion
fun Quaternion.toOldQuaternion() = com.google.ar.sceneform.math.Quaternion(x, y, z, w)

//TODO: Remove when everything use Quaternion
fun com.google.ar.sceneform.math.Quaternion.toNewQuaternion() = Quaternion(x, y, z, w)

fun Mat4.toColumnsFloatArray() = floatArrayOf(
    x.x, x.y, x.z, x.w,
    y.x, y.y, y.z, y.w,
    z.x, z.y, z.z, z.w,
    w.x, w.y, w.z, w.w
)

fun FloatArray.toMat4() = Mat4(
    x= Float4(this[0], this[1], this[2], this[3]),
    y= Float4(this[4], this[5], this[6], this[7]),
    z= Float4(this[8], this[9], this[10], this[11]),
    w= Float4(this[12], this[13], this[14], this[15]))

fun Matrix.toMat4() = data.toMat4()

fun lerp(a: Float3, b: Float3, t: Float) = mix(a, b, t)

//fun quaternion(eye: Float3, target: Float3, up: Float3 = Float3(y = 1.0f)): Quaternion {
//    return lookAt(eye, target, up).toQuaternion()
//}

//val Mat4.quaternion :Quaternion get() = rotation(this).toQuaternion()
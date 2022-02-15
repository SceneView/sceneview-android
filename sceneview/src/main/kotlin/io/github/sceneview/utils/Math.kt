package io.github.sceneview.utils

import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.*

typealias Position = Float3
typealias Rotation = Float3
typealias Scale = Float3
typealias Direction = Float3
typealias Color = Float4
typealias Transform = Mat4

fun FloatArray.toFloat3() = this.let { (x, y, z) -> Float3(x, y, z) }
fun FloatArray.toFloat4() = this.let { (x, y, z, w) -> Float4(x, y, z, w) }
fun FloatArray.toPosition() = this.let { (x, y, z) -> Position(x, y, z) }
fun FloatArray.toRotation() = this.let { (x, y, z) -> Rotation(x, y, z) }
fun FloatArray.toScale() = this.let { (x, y, z) -> Scale(x, y, z) }
fun FloatArray.toDirection() = this.let { (x, y, z) -> Direction(x, y, z) }
fun FloatArray.toColor() = Color(this[0], this[1], this[2], this.getOrNull(3) ?: 1.0f)
fun colorOf(r: Float = 0.0f, g: Float = 0.0f, b: Float = 0.0f, a: Float = 1.0f) = Color(r, g, b, a)
fun colorOf(array: List<Float> = listOf(0.0f, 0.0f, 0.0f)) = Color(array[0], array[1], array[2])

//TODO: Remove when everything use Float3
fun Float3.toVector3() = Vector3(x, y, z)

//TODO: Remove when everything use Float3
fun Vector3.toFloat3() = Float3(x, y, z)

//TODO: Remove when everything use Quaternion
fun Quaternion.toOldQuaternion() = com.google.ar.sceneform.math.Quaternion(x, y, z, w)

//TODO: Remove when everything use Quaternion
fun com.google.ar.sceneform.math.Quaternion.toNewQuaternion() = Quaternion(x, y, z, w)

fun lerp(a: Float3, b: Float3, t:Float) = mix(a, b, t)
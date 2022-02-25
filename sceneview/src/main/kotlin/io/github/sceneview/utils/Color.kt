package io.github.sceneview.utils

import com.google.android.filament.utils.pow
import dev.romainguy.kotlin.math.Float4

typealias Color = Float4

fun FloatArray.toColor() = Color(this[0], this[1], this[2], this.getOrNull(3) ?: 1.0f)
fun colorOf(r: Float = 0.0f, g: Float = 0.0f, b: Float = 0.0f, a: Float = 1.0f) = Color(r, g, b, a)
fun colorOf(color: Int) = colorOf(
    android.graphics.Color.red(color) / 255.0f,
    android.graphics.Color.green(color) / 255.0f,
    android.graphics.Color.blue(color) / 255.0f,
    android.graphics.Color.alpha(color) / 255.0f
)
fun colorOf(array: List<Float> = listOf(0.0f, 0.0f, 0.0f)) = Color(array[0], array[1], array[2])

/**
 * @see FloatArray.toLinearSpace
 */
fun Color.toLinearSpace() = colorOf(this.toFloatArray().toLinearSpace())

/**
 * If rendering in linear space, first convert the gray scaled values to linear space by rising to
 * the power 2.2
 */
fun FloatArray.toLinearSpace() = map { pow(it, 2.2f) }
package io.github.sceneview.utils

import android.graphics.drawable.ColorDrawable
import com.google.android.filament.utils.pow
import dev.romainguy.kotlin.math.Float4

typealias Color = Float4

fun colorOf(r: Float = 0.0f, g: Float = 0.0f, b: Float = 0.0f, a: Float = 1.0f) = Color(r, g, b, a)
fun colorOf(rgb: Float = 0.0f, a: Float = 1.0f) = colorOf(r = rgb, g = rgb, b = rgb, a = a)
fun colorOf(color: Int) = colorOf(
    r = android.graphics.Color.red(color) / 255.0f,
    g = android.graphics.Color.green(color) / 255.0f,
    b = android.graphics.Color.blue(color) / 255.0f,
    a = android.graphics.Color.alpha(color) / 255.0f
)

fun Color(color: Int) = colorOf(
    r = android.graphics.Color.red(color) / 255.0f,
    g = android.graphics.Color.green(color) / 255.0f,
    b = android.graphics.Color.blue(color) / 255.0f,
    a = android.graphics.Color.alpha(color) / 255.0f
)

fun Color(drawable: ColorDrawable) = colorOf(
    r = android.graphics.Color.red(drawable.color) / 255.0f,
    g = android.graphics.Color.green(drawable.color) / 255.0f,
    b = android.graphics.Color.blue(drawable.color) / 255.0f,
    a = drawable.alpha / 255.0f
)

fun FloatArray.toColor() = Color(this[0], this[1], this[2], this.getOrNull(3) ?: 1.0f)

/**
 * If rendering in linear space, first convert the gray scaled values to linear space by rising to
 * the power 2.2
 */
fun Color.toLinearSpace() = transform { pow(it, 2.2f) }
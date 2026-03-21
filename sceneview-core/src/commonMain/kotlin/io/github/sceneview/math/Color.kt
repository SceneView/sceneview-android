package io.github.sceneview.math

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * sRGB to linear conversion using the precise piecewise sRGB transfer function.
 */
private fun srgbToLinear(v: Float): Float =
    if (v <= 0.04045f) v / 12.92f
    else ((v + 0.055f) / 1.055f).pow(2.4f)

/**
 * Linear to sRGB conversion using the precise piecewise sRGB transfer function.
 */
private fun linearToSrgb(v: Float): Float =
    if (v <= 0.0031308f) v * 12.92f
    else 1.055f * v.pow(1.0f / 2.4f) - 0.055f

/**
 * Converts this color from sRGB space to linear space.
 * Alpha is left unchanged.
 */
fun Color.toLinearSpace(): Color = Color(
    srgbToLinear(x),
    srgbToLinear(y),
    srgbToLinear(z),
    w
)

/**
 * Converts this color from linear space to sRGB space.
 * Alpha is left unchanged.
 */
fun Color.toSrgbSpace(): Color = Color(
    linearToSrgb(x),
    linearToSrgb(y),
    linearToSrgb(z),
    w
)

/**
 * Returns the perceived luminance of this color using the BT.709 coefficients.
 * Assumes the color is in linear space.
 */
fun Color.luminance(): Float = 0.2126f * x + 0.7152f * y + 0.0722f * z

/**
 * Component-wise linear interpolation between two colors in linear space.
 * Both colors are converted to linear space before interpolation, and the result
 * is converted back to sRGB space.
 */
fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val s = start.toLinearSpace()
    val e = end.toLinearSpace()
    return Color(
        s.x + (e.x - s.x) * fraction,
        s.y + (e.y - s.y) * fraction,
        s.z + (e.z - s.z) * fraction,
        s.w + (e.w - s.w) * fraction
    ).toSrgbSpace()
}

/**
 * Returns a copy of this color with the given alpha value.
 */
fun Color.withAlpha(alpha: Float): Color = Color(x, y, z, alpha)

/**
 * Converts HSV color values to an RGB [Color].
 *
 * @param h Hue in degrees (0..360)
 * @param s Saturation (0..1)
 * @param v Value / brightness (0..1)
 * @return An sRGB [Color] with alpha = 1.0
 */
fun hsvToRgb(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val hPrime = (h % 360f) / 60f
    val x = c * (1f - abs(hPrime % 2f - 1f))
    val m = v - c

    val (r1, g1, b1) = when {
        hPrime < 1f -> Float3(c, x, 0f)
        hPrime < 2f -> Float3(x, c, 0f)
        hPrime < 3f -> Float3(0f, c, x)
        hPrime < 4f -> Float3(0f, x, c)
        hPrime < 5f -> Float3(x, 0f, c)
        else -> Float3(c, 0f, x)
    }

    return Color(r1 + m, g1 + m, b1 + m, 1.0f)
}

/**
 * Converts this RGB [Color] to HSV.
 *
 * @return A [Float3] where x = hue (0..360), y = saturation (0..1), z = value (0..1).
 */
fun Color.toHsv(): Float3 {
    val r = x
    val g = y
    val b = z

    val cMax = max(r, max(g, b))
    val cMin = min(r, min(g, b))
    val delta = cMax - cMin

    val hue = when {
        delta == 0f -> 0f
        cMax == r -> 60f * (((g - b) / delta) % 6f)
        cMax == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }

    val saturation = if (cMax == 0f) 0f else delta / cMax
    val value = cMax

    return Float3(
        if (hue < 0f) hue + 360f else hue,
        saturation,
        value
    )
}

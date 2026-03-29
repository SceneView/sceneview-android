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

/**
 * Converts HSL color values to an RGB [Color].
 *
 * @param h Hue in degrees (0..360).
 * @param s Saturation (0..1).
 * @param l Lightness (0..1).
 * @return An sRGB [Color] with alpha = 1.0.
 */
fun hslToRgb(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val hPrime = (h % 360f) / 60f
    val x = c * (1f - abs(hPrime % 2f - 1f))
    val m = l - c / 2f

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
 * Converts this RGB [Color] to HSL.
 *
 * @return A [Float3] where x = hue (0..360), y = saturation (0..1), z = lightness (0..1).
 */
fun Color.toHsl(): Float3 {
    val r = x
    val g = y
    val b = z

    val cMax = max(r, max(g, b))
    val cMin = min(r, min(g, b))
    val delta = cMax - cMin
    val l = (cMax + cMin) / 2f

    val hue = when {
        delta == 0f -> 0f
        cMax == r -> 60f * (((g - b) / delta) % 6f)
        cMax == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }

    val saturation = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))

    return Float3(
        if (hue < 0f) hue + 360f else hue,
        saturation.coerceIn(0f, 1f),
        l
    )
}

/**
 * Evaluates a color gradient at the given [t] position.
 *
 * @param stops List of (position, color) pairs sorted by position in [0..1].
 *              Must contain at least 2 stops.
 * @param t Parameter in [0..1] to evaluate.
 * @return Interpolated color at the given position (uses linear-space interpolation).
 */
fun colorGradient(stops: List<Pair<Float, Color>>, t: Float): Color {
    require(stops.size >= 2) { "Gradient requires at least 2 color stops" }

    val clamped = t.coerceIn(0f, 1f)

    // Before first stop
    if (clamped <= stops.first().first) return stops.first().second
    // After last stop
    if (clamped >= stops.last().first) return stops.last().second

    // Find the two surrounding stops
    for (i in 0 until stops.size - 1) {
        val (pos0, color0) = stops[i]
        val (pos1, color1) = stops[i + 1]
        if (clamped in pos0..pos1) {
            val localT = if (pos1 == pos0) 0f else (clamped - pos0) / (pos1 - pos0)
            return lerpColor(color0, color1, localT)
        }
    }

    return stops.last().second
}

/**
 * Creates a color from 0-255 integer RGB values.
 *
 * @param r Red (0..255).
 * @param g Green (0..255).
 * @param b Blue (0..255).
 * @param a Alpha (0..255). Default 255.
 * @return An sRGB [Color] with values in [0..1].
 */
fun colorFromRgb(r: Int, g: Int, b: Int, a: Int = 255): Color = Color(
    r / 255f, g / 255f, b / 255f, a / 255f
)

/**
 * Creates a color from a hex string (e.g., "#FF5500" or "FF5500" or "#FF550080").
 *
 * Supports 6-character (RGB) and 8-character (RGBA) hex strings.
 *
 * @param hex Hex color string, optionally prefixed with '#'.
 * @return An sRGB [Color].
 */
fun colorFromHex(hex: String): Color {
    val clean = hex.removePrefix("#")
    require(clean.length == 6 || clean.length == 8) {
        "Hex color must be 6 or 8 characters (got ${clean.length})"
    }
    val r = clean.substring(0, 2).toInt(16)
    val g = clean.substring(2, 4).toInt(16)
    val b = clean.substring(4, 6).toInt(16)
    val a = if (clean.length == 8) clean.substring(6, 8).toInt(16) else 255
    return colorFromRgb(r, g, b, a)
}

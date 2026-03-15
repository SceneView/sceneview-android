package com.google.ar.sceneform.rendering

import androidx.annotation.ColorInt
import com.google.android.filament.Colors

/**
 * An RGBA color. Each component is a value with a range from 0 to 1. Can be created from an Android
 * ColorInt.
 *
 * @deprecated
 */
@Deprecated("Deprecated")
class Color {
    var r: Float = 0f
    var g: Float = 0f
    var b: Float = 0f
    var a: Float = 0f

    /** Construct a Color and default it to white (1, 1, 1, 1). */
    constructor() {
        setWhite()
    }

    /** Construct a Color with the values of another color. */
    constructor(color: Color) {
        set(color)
    }

    /** Construct a color with the RGB values passed in and an alpha of 1. */
    constructor(r: Float, g: Float, b: Float) {
        set(r, g, b)
    }

    /** Construct a color with the RGBA values passed in. */
    constructor(r: Float, g: Float, b: Float, a: Float) {
        set(r, g, b, a)
    }

    /**
     * Construct a color with an integer in the sRGB color space packed as an ARGB value. Used for
     * constructing from an Android ColorInt.
     */
    constructor(@ColorInt argb: Int) {
        set(argb)
    }

    /** Set to the values of another color. */
    fun set(color: Color) {
        set(color.r, color.g, color.b, color.a)
    }

    /** Set to the RGB values passed in and an alpha of 1. */
    fun set(r: Float, g: Float, b: Float) {
        set(r, g, b, 1.0f)
    }

    /** Set to the RGBA values passed in. */
    fun set(r: Float, g: Float, b: Float, a: Float) {
        this.r = r.coerceIn(0.0f, 1.0f)
        this.g = g.coerceIn(0.0f, 1.0f)
        this.b = b.coerceIn(0.0f, 1.0f)
        this.a = a.coerceIn(0.0f, 1.0f)
    }

    /**
     * Set to RGBA values from an integer in the sRGB color space packed as an ARGB value. Used for
     * setting from an Android ColorInt.
     */
    fun set(@ColorInt argb: Int) {
        // sRGB color
        val red = android.graphics.Color.red(argb)
        val green = android.graphics.Color.green(argb)
        val blue = android.graphics.Color.blue(argb)
        val alpha = android.graphics.Color.alpha(argb)

        // Convert from sRGB to linear and from int to float.
        val linearColor = Colors.toLinear(
            Colors.RgbType.SRGB,
            red.toFloat() * INT_COLOR_SCALE,
            green.toFloat() * INT_COLOR_SCALE,
            blue.toFloat() * INT_COLOR_SCALE
        )

        r = linearColor[0]
        g = linearColor[1]
        b = linearColor[2]
        a = alpha.toFloat() * INT_COLOR_SCALE
    }

    /** Sets the color to white. RGBA is (1, 1, 1, 1). */
    private fun setWhite() {
        set(1.0f, 1.0f, 1.0f)
    }

    /** Returns a new color with Sceneform's tonemapping inversed. */
    fun inverseTonemap(): Color {
        val color = Color(r, g, b, a)
        color.r = inverseTonemap(r)
        color.g = inverseTonemap(g)
        color.b = inverseTonemap(b)
        return color
    }

    companion object {
        private const val INT_COLOR_SCALE = 1.0f / 255.0f

        private fun inverseTonemap(v: Float): Float {
            return (v * -0.155f) / (v - 1.019f)
        }
    }
}

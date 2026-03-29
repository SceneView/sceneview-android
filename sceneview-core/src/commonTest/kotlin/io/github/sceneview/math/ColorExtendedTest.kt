package io.github.sceneview.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorExtendedTest {

    // --- HSL ---

    @Test
    fun hslToRgbRed() {
        val color = hslToRgb(0f, 1f, 0.5f)
        assertEquals(1f, color.x, 0.01f) // R
        assertEquals(0f, color.y, 0.01f) // G
        assertEquals(0f, color.z, 0.01f) // B
    }

    @Test
    fun hslToRgbGreen() {
        val color = hslToRgb(120f, 1f, 0.5f)
        assertEquals(0f, color.x, 0.01f)
        assertEquals(1f, color.y, 0.01f)
        assertEquals(0f, color.z, 0.01f)
    }

    @Test
    fun hslToRgbWhite() {
        val color = hslToRgb(0f, 0f, 1f)
        assertEquals(1f, color.x, 0.01f)
        assertEquals(1f, color.y, 0.01f)
        assertEquals(1f, color.z, 0.01f)
    }

    @Test
    fun hslToRgbBlack() {
        val color = hslToRgb(0f, 0f, 0f)
        assertEquals(0f, color.x, 0.01f)
        assertEquals(0f, color.y, 0.01f)
        assertEquals(0f, color.z, 0.01f)
    }

    @Test
    fun rgbToHslRoundtrip() {
        val original = Color(0.8f, 0.3f, 0.5f, 1f)
        val hsl = original.toHsl()
        val back = hslToRgb(hsl.x, hsl.y, hsl.z)
        assertEquals(original.x, back.x, 0.02f)
        assertEquals(original.y, back.y, 0.02f)
        assertEquals(original.z, back.z, 0.02f)
    }

    // --- Gradient ---

    @Test
    fun gradientAtStopReturnsExactColor() {
        val red = Color(1f, 0f, 0f, 1f)
        val blue = Color(0f, 0f, 1f, 1f)
        val stops = listOf(0f to red, 1f to blue)
        val result = colorGradient(stops, 0f)
        assertEquals(1f, result.x, 0.01f)
        assertEquals(0f, result.z, 0.01f)
    }

    @Test
    fun gradientAtEndReturnsEndColor() {
        val red = Color(1f, 0f, 0f, 1f)
        val blue = Color(0f, 0f, 1f, 1f)
        val stops = listOf(0f to red, 1f to blue)
        val result = colorGradient(stops, 1f)
        assertEquals(0f, result.x, 0.02f)
    }

    @Test
    fun gradientMidpointIsBlended() {
        val black = Color(0f, 0f, 0f, 1f)
        val white = Color(1f, 1f, 1f, 1f)
        val stops = listOf(0f to black, 1f to white)
        val mid = colorGradient(stops, 0.5f)
        // Should be approximately gray
        assertTrue(mid.x > 0.2f && mid.x < 0.8f, "Mid gradient should be near gray")
    }

    @Test
    fun gradientClampsOutOfRange() {
        val red = Color(1f, 0f, 0f, 1f)
        val blue = Color(0f, 0f, 1f, 1f)
        val stops = listOf(0f to red, 1f to blue)
        val before = colorGradient(stops, -0.5f)
        val after = colorGradient(stops, 1.5f)
        assertEquals(1f, before.x, 0.01f) // Should be red
        assertEquals(0f, after.x, 0.02f) // Should be blue
    }

    @Test
    fun gradientMultipleStops() {
        val red = Color(1f, 0f, 0f, 1f)
        val green = Color(0f, 1f, 0f, 1f)
        val blue = Color(0f, 0f, 1f, 1f)
        val stops = listOf(0f to red, 0.5f to green, 1f to blue)
        val result = colorGradient(stops, 0.5f)
        // At 0.5 should be exactly green
        assertEquals(0f, result.x, 0.02f)
    }

    // --- colorFromRgb ---

    @Test
    fun colorFromRgbConverts() {
        val c = colorFromRgb(255, 128, 0)
        assertEquals(1f, c.x, 0.01f)
        assertTrue(c.y > 0.49f && c.y < 0.51f)
        assertEquals(0f, c.z, 0.01f)
    }

    // --- colorFromHex ---

    @Test
    fun colorFromHexRed() {
        val c = colorFromHex("#FF0000")
        assertEquals(1f, c.x, 0.01f)
        assertEquals(0f, c.y, 0.01f)
        assertEquals(0f, c.z, 0.01f)
    }

    @Test
    fun colorFromHexWithAlpha() {
        val c = colorFromHex("FF000080")
        assertEquals(1f, c.x, 0.01f)
        assertTrue(c.w > 0.49f && c.w < 0.52f) // ~128/255
    }

    @Test
    fun colorFromHexWithoutHash() {
        val c = colorFromHex("00FF00")
        assertEquals(0f, c.x, 0.01f)
        assertEquals(1f, c.y, 0.01f)
    }

    @Test
    fun colorFromHexInvalidLength() {
        var threw = false
        try { colorFromHex("FFF") } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}

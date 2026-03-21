package io.github.sceneview.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ColorTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.01f) {
        assertTrue(
            abs(expected - actual) < epsilon,
            "Expected $expected but got $actual (delta=${abs(expected - actual)})"
        )
    }

    // --- sRGB <-> linear round-trip ---

    @Test
    fun srgbToLinearRoundTrip() {
        val original = colorOf(0.2f, 0.5f, 0.8f, 0.9f)
        val roundTripped = original.toLinearSpace().toSrgbSpace()
        assertClose(original.x, roundTripped.x)
        assertClose(original.y, roundTripped.y)
        assertClose(original.z, roundTripped.z)
        assertClose(original.w, roundTripped.w)
    }

    @Test
    fun linearToSrgbRoundTrip() {
        val original = colorOf(0.1f, 0.4f, 0.7f, 1.0f)
        val roundTripped = original.toSrgbSpace().toLinearSpace()
        assertClose(original.x, roundTripped.x)
        assertClose(original.y, roundTripped.y)
        assertClose(original.z, roundTripped.z)
        assertClose(original.w, roundTripped.w)
    }

    @Test
    fun toLinearSpacePreservesAlpha() {
        val c = colorOf(0.5f, 0.5f, 0.5f, 0.3f)
        assertClose(0.3f, c.toLinearSpace().w)
    }

    // --- Luminance ---

    @Test
    fun luminanceOfWhiteIsOne() {
        val white = colorOf(1.0f, 1.0f, 1.0f, 1.0f)
        assertClose(1.0f, white.luminance())
    }

    @Test
    fun luminanceOfBlackIsZero() {
        val black = colorOf(0.0f, 0.0f, 0.0f, 1.0f)
        assertClose(0.0f, black.luminance())
    }

    @Test
    fun luminanceOfPureRedChannel() {
        val red = colorOf(1.0f, 0.0f, 0.0f, 1.0f)
        assertClose(0.2126f, red.luminance())
    }

    // --- HSV -> RGB for known values ---

    @Test
    fun hsvRedIsCorrect() {
        val red = hsvToRgb(0f, 1f, 1f)
        assertClose(1f, red.x)
        assertClose(0f, red.y)
        assertClose(0f, red.z)
        assertClose(1f, red.w)
    }

    @Test
    fun hsvGreenIsCorrect() {
        val green = hsvToRgb(120f, 1f, 1f)
        assertClose(0f, green.x)
        assertClose(1f, green.y)
        assertClose(0f, green.z)
    }

    @Test
    fun hsvBlueIsCorrect() {
        val blue = hsvToRgb(240f, 1f, 1f)
        assertClose(0f, blue.x)
        assertClose(0f, blue.y)
        assertClose(1f, blue.z)
    }

    @Test
    fun hsvWhiteIsCorrect() {
        val white = hsvToRgb(0f, 0f, 1f)
        assertClose(1f, white.x)
        assertClose(1f, white.y)
        assertClose(1f, white.z)
    }

    // --- RGB -> HSV ---

    @Test
    fun rgbToHsvRed() {
        val hsv = colorOf(1f, 0f, 0f).toHsv()
        assertClose(0f, hsv.x)
        assertClose(1f, hsv.y)
        assertClose(1f, hsv.z)
    }

    @Test
    fun rgbToHsvRoundTrip() {
        val original = hsvToRgb(200f, 0.7f, 0.8f)
        val hsv = original.toHsv()
        val reconstructed = hsvToRgb(hsv.x, hsv.y, hsv.z)
        assertClose(original.x, reconstructed.x)
        assertClose(original.y, reconstructed.y)
        assertClose(original.z, reconstructed.z)
    }

    // --- withAlpha ---

    @Test
    fun withAlphaChangesOnlyAlpha() {
        val c = colorOf(0.2f, 0.4f, 0.6f, 1.0f)
        val result = c.withAlpha(0.5f)
        assertClose(0.2f, result.x)
        assertClose(0.4f, result.y)
        assertClose(0.6f, result.z)
        assertClose(0.5f, result.w)
    }

    @Test
    fun withAlphaZero() {
        val c = colorOf(1f, 1f, 1f, 1f)
        assertClose(0f, c.withAlpha(0f).w)
    }

    // --- lerpColor ---

    @Test
    fun lerpColorAtZeroReturnsStart() {
        val start = colorOf(0.2f, 0.4f, 0.6f, 1.0f)
        val end = colorOf(0.8f, 0.6f, 0.4f, 0.5f)
        val result = lerpColor(start, end, 0f)
        assertClose(start.x, result.x)
        assertClose(start.y, result.y)
        assertClose(start.z, result.z)
        assertClose(start.w, result.w)
    }

    @Test
    fun lerpColorAtOneReturnsEnd() {
        val start = colorOf(0.2f, 0.4f, 0.6f, 1.0f)
        val end = colorOf(0.8f, 0.6f, 0.4f, 0.5f)
        val result = lerpColor(start, end, 1f)
        assertClose(end.x, result.x)
        assertClose(end.y, result.y)
        assertClose(end.z, result.z)
        assertClose(end.w, result.w)
    }

    @Test
    fun lerpColorAtHalfIsBetween() {
        val start = colorOf(0f, 0f, 0f, 1.0f)
        val end = colorOf(1f, 1f, 1f, 1.0f)
        val result = lerpColor(start, end, 0.5f)
        // Result should be between start and end for each channel
        assertTrue(result.x > 0f && result.x < 1f, "R channel should be between 0 and 1")
        assertTrue(result.y > 0f && result.y < 1f, "G channel should be between 0 and 1")
        assertTrue(result.z > 0f && result.z < 1f, "B channel should be between 0 and 1")
        assertClose(1.0f, result.w) // alpha should stay at 1
    }
}

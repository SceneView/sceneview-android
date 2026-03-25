package io.github.sceneview.collision

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MathHelperTest {

    @Test
    fun almostEqualForIdenticalValues() {
        assertTrue(MathHelper.almostEqualRelativeAndAbs(1.0f, 1.0f))
    }

    @Test
    fun almostEqualForZero() {
        assertTrue(MathHelper.almostEqualRelativeAndAbs(0.0f, 0.0f))
    }

    @Test
    fun almostEqualForVerySmallDifference() {
        assertTrue(MathHelper.almostEqualRelativeAndAbs(1.0f, 1.0f + 1e-11f))
    }

    @Test
    fun notAlmostEqualForLargeDifference() {
        assertFalse(MathHelper.almostEqualRelativeAndAbs(1.0f, 2.0f))
    }

    @Test
    fun clampWithinRange() {
        assertEquals(5f, MathHelper.clamp(5f, 0f, 10f))
    }

    @Test
    fun clampBelowMin() {
        assertEquals(0f, MathHelper.clamp(-5f, 0f, 10f))
    }

    @Test
    fun clampAboveMax() {
        assertEquals(10f, MathHelper.clamp(15f, 0f, 10f))
    }

    @Test
    fun lerpAtZero() {
        assertEquals(0f, MathHelper.lerp(0f, 10f, 0f))
    }

    @Test
    fun lerpAtOne() {
        assertEquals(10f, MathHelper.lerp(0f, 10f, 1f))
    }

    @Test
    fun lerpAtHalf() {
        assertEquals(5f, MathHelper.lerp(0f, 10f, 0.5f))
    }

    @Test
    fun lerpWithNegativeValues() {
        assertEquals(-5f, MathHelper.lerp(-10f, 0f, 0.5f))
    }
}

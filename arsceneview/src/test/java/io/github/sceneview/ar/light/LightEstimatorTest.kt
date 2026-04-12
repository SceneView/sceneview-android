package io.github.sceneview.ar.light

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for [LightEstimator]-related logic that does not require
 * Android, Filament, or ARCore.
 *
 * Tests cover:
 * 1. [LightEstimator.Estimation] data-class contract (equality, copy, nullability).
 * 2. [LightEstimator.SPHERICAL_HARMONICS_IRRADIANCE_FACTORS] math — the constant
 *    encodes a carefully-ordered set of pre-scaled SH coefficients that are
 *    critical for correct light estimation. Any accidental reordering or value
 *    change would silently produce wrong lighting in all AR apps.
 */
class LightEstimatorTest {

    // ── Estimation data class ────────────────────────────────────────────────

    @Test
    fun `Estimation default constructor has all fields null`() {
        val est = LightEstimator.Estimation()
        assertNull(est.mainLightColor)
        assertNull(est.mainLightIntensity)
        assertNull(est.mainLightDirection)
        assertNull(est.reflections)
        assertNull(est.irradiance)
    }

    @Test
    fun `Estimation equality holds for equal instances`() {
        val a = LightEstimator.Estimation(mainLightIntensity = 1.5f)
        val b = LightEstimator.Estimation(mainLightIntensity = 1.5f)
        assertEquals(a, b)
    }

    @Test
    fun `Estimation equality fails for different intensities`() {
        val a = LightEstimator.Estimation(mainLightIntensity = 1.5f)
        val b = LightEstimator.Estimation(mainLightIntensity = 2.0f)
        assertFalse(a == b)
    }

    @Test
    fun `Estimation copy preserves non-copied fields`() {
        val original = LightEstimator.Estimation(mainLightIntensity = 3.0f)
        val copy = original.copy(mainLightIntensity = 5.0f)
        assertEquals(5.0f, copy.mainLightIntensity)
        // Other fields should still be null (from default original)
        assertNull(copy.mainLightColor)
        assertNull(copy.mainLightDirection)
    }

    @Test
    fun `Estimation irradiance array survives data-class round-trip`() {
        val coeffs = FloatArray(9) { it.toFloat() }
        val est = LightEstimator.Estimation(irradiance = coeffs)
        assertNotNull(est.irradiance)
        assertEquals(9, est.irradiance!!.size)
        for (i in 0..8) {
            assertEquals(i.toFloat(), est.irradiance!![i], 0.0001f)
        }
    }

    // ── SPHERICAL_HARMONICS_IRRADIANCE_FACTORS ───────────────────────────────

    /**
     * The constant must have exactly 9 entries — one factor per SH band-coefficient
     * (L0, L1x, L1y, L1z, L2_0, L2_1, L2_2, L2_3, L2_4).
     */
    @Test
    fun `SPHERICAL_HARMONICS_IRRADIANCE_FACTORS has 9 entries`() {
        assertEquals(9, LightEstimator.SPHERICAL_HARMONICS_IRRADIANCE_FACTORS.size)
    }

    /**
     * Spot-check the first three values (the L0 and L1 band factors).
     * These are derived from the SH normalization + BRDF + pi factor conversion.
     * Values verified against the Filament SceneformMaintained reference:
     * https://github.com/ThomasGorisse/SceneformMaintained/pull/156
     */
    @Test
    fun `SPHERICAL_HARMONICS_IRRADIANCE_FACTORS first three values are correct`() {
        val f = LightEstimator.SPHERICAL_HARMONICS_IRRADIANCE_FACTORS
        assertEquals(0.282095f, f[0], 0.000001f)   // L0 band
        assertEquals(-0.325735f, f[1], 0.000001f)  // L1 x-axis
        assertEquals(0.325735f, f[2], 0.000001f)   // L1 y-axis
    }

    /**
     * The source array has indices 6 and 7 SWAPPED before being stored.
     * This is required because ARCore and Filament use different SH coefficient
     * ordering. Verify the swap happened correctly.
     *
     * Source before swap:  [..., 0.078848, -0.273137, 0.136569]  (indices 6,7,8)
     * After swap at 6↔7:   [..., -0.273137, 0.078848, 0.136569]
     */
    @Test
    fun `SPHERICAL_HARMONICS_IRRADIANCE_FACTORS has swapped indices 6 and 7`() {
        val f = LightEstimator.SPHERICAL_HARMONICS_IRRADIANCE_FACTORS
        // After the swap: index 6 should be the original index-7 value (-0.273137)
        assertEquals(-0.273137f, f[6], 0.000001f)
        // After the swap: index 7 should be the original index-6 value (0.078848)
        assertEquals(0.078848f, f[7], 0.000001f)
        // Index 8 is unchanged
        assertEquals(0.136569f, f[8], 0.000001f)
    }

    /**
     * All factors must be finite (no NaN or Infinity that would corrupt
     * the Filament irradiance upload).
     */
    @Test
    fun `SPHERICAL_HARMONICS_IRRADIANCE_FACTORS are all finite`() {
        for ((i, v) in LightEstimator.SPHERICAL_HARMONICS_IRRADIANCE_FACTORS.withIndex()) {
            assertTrue("factor[$i] = $v is not finite", v.isFinite())
        }
    }

    /**
     * The magnitude of each factor is reasonable (within [-1, 1]).
     * Larger values would over-amplify the SH and blow out the lighting.
     */
    @Test
    fun `SPHERICAL_HARMONICS_IRRADIANCE_FACTORS magnitudes are within (-1, 1)`() {
        for ((i, v) in LightEstimator.SPHERICAL_HARMONICS_IRRADIANCE_FACTORS.withIndex()) {
            assertTrue(
                "factor[$i] = $v is out of expected range (-1, 1)",
                v > -1f && v < 1f
            )
        }
    }

    /**
     * Ambient intensity normalization: factor[3] (L1 z-axis) must be negative,
     * matching the Filament convention where the Y-up axis has negative contribution.
     */
    @Test
    fun `SPHERICAL_HARMONICS_IRRADIANCE_FACTORS index-3 is negative`() {
        val f = LightEstimator.SPHERICAL_HARMONICS_IRRADIANCE_FACTORS
        assertTrue("factor[3] should be negative, got ${f[3]}", f[3] < 0f)
    }
}

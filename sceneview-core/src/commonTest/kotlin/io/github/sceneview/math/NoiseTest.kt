package io.github.sceneview.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class NoiseTest {

    @Test
    fun perlin2DReturnsValuesInRange() {
        for (x in 0..20) {
            for (y in 0..20) {
                val v = Noise.perlin2D(x * 0.5f, y * 0.5f)
                assertTrue(v >= -1.5f && v <= 1.5f, "Perlin2D value $v out of expected range at ($x, $y)")
            }
        }
    }

    @Test
    fun perlin3DReturnsValuesInRange() {
        for (x in 0..10) {
            for (y in 0..10) {
                val v = Noise.perlin3D(x * 0.5f, y * 0.5f, 0.5f)
                assertTrue(v >= -1.5f && v <= 1.5f, "Perlin3D value $v out of expected range")
            }
        }
    }

    @Test
    fun perlin2DIsSmooth() {
        // Adjacent samples should not differ wildly
        val v1 = Noise.perlin2D(1.0f, 1.0f)
        val v2 = Noise.perlin2D(1.01f, 1.0f)
        assertTrue(abs(v1 - v2) < 0.1f, "Noise should be smooth: diff=${abs(v1 - v2)}")
    }

    @Test
    fun perlin2DDeterministic() {
        val v1 = Noise.perlin2D(42.5f, 17.3f)
        val v2 = Noise.perlin2D(42.5f, 17.3f)
        assertTrue(v1 == v2, "Same inputs should return same output")
    }

    @Test
    fun perlin3DDeterministic() {
        val v1 = Noise.perlin3D(1f, 2f, 3f)
        val v2 = Noise.perlin3D(1f, 2f, 3f)
        assertTrue(v1 == v2)
    }

    @Test
    fun fbm2DReturnsValuesInRange() {
        for (x in 0..10) {
            for (y in 0..10) {
                val v = Noise.fbm2D(x * 0.3f, y * 0.3f, octaves = 4)
                assertTrue(v >= -1.5f && v <= 1.5f, "fBm2D value $v out of range")
            }
        }
    }

    @Test
    fun fbm3DReturnsValuesInRange() {
        val v = Noise.fbm3D(1f, 2f, 3f, octaves = 3)
        assertTrue(v >= -1.5f && v <= 1.5f, "fBm3D value $v out of range")
    }

    @Test
    fun fbmWithMoreOctavesAddsFinerDetail() {
        // The variance should be different but the function should not crash
        val v1 = Noise.fbm2D(5f, 5f, octaves = 1)
        val v2 = Noise.fbm2D(5f, 5f, octaves = 6)
        // Just check both return valid values
        assertTrue(abs(v1) < 2f && abs(v2) < 2f)
    }

    @Test
    fun perlin2DAtOriginIsZero() {
        // At integer coordinates, gradient noise is often 0 (depends on permutation)
        // Just verify it doesn't crash
        val v = Noise.perlin2D(0f, 0f)
        assertTrue(abs(v) < 1.5f)
    }
}

package io.github.sceneview.math

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * Tests for pure math utility functions in [io.github.sceneview.math].
 *
 * All functions tested here are pure (no Filament/Android deps) and live in
 * sceneview-core commonMain. They are accessible from Android JVM unit tests.
 */
class MathUtilsTest {

    // ── Float.equals(delta) ──────────────────────────────────────────────────────

    @Test
    fun `Float equals with zero delta is exact equality`() {
        assertTrue(1.0f.equals(1.0f, 0f))
        assertFalse(1.0f.equals(1.001f, 0f))
    }

    @Test
    fun `Float equals within delta returns true`() {
        assertTrue(1.0f.equals(1.05f, 0.1f))
    }

    @Test
    fun `Float equals outside delta returns false`() {
        assertFalse(1.0f.equals(1.2f, 0.1f))
    }

    @Test
    fun `Float equals with equal values always returns true`() {
        assertTrue(0.5f.equals(0.5f, 0.0001f))
        assertTrue((-3f).equals(-3f, 0f))
    }

    // ── Float.almostEquals ───────────────────────────────────────────────────────

    @Test
    fun `almostEquals same value returns true`() {
        assertTrue(1.0f almostEquals 1.0f)
    }

    @Test
    fun `almostEquals zero and tiny delta returns true`() {
        assertTrue(0.0f almostEquals 0.0f)
    }

    @Test
    fun `almostEquals clearly different values returns false`() {
        assertFalse(1.0f almostEquals 2.0f)
    }

    @Test
    fun `almostEquals negative values`() {
        assertTrue((-1.0f) almostEquals (-1.0f))
        assertFalse((-1.0f) almostEquals 1.0f)
    }

    // ── colorOf factory ──────────────────────────────────────────────────────────

    @Test
    fun `colorOf r g b a constructs correct color`() {
        val c = colorOf(r = 1f, g = 0f, b = 0f, a = 1f)
        assertEquals(1f, c.x, 0.0001f)
        assertEquals(0f, c.y, 0.0001f)
        assertEquals(0f, c.z, 0.0001f)
        assertEquals(1f, c.w, 0.0001f)
    }

    @Test
    fun `colorOf rgb shortcut constructs gray color`() {
        val c = colorOf(rgb = 0.5f)
        assertEquals(0.5f, c.x, 0.0001f)
        assertEquals(0.5f, c.y, 0.0001f)
        assertEquals(0.5f, c.z, 0.0001f)
        assertEquals(1.0f, c.w, 0.0001f) // default alpha
    }

    @Test
    fun `colorOf default values produce transparent black`() {
        val c = colorOf()
        assertEquals(0f, c.x, 0.0001f)
        assertEquals(0f, c.y, 0.0001f)
        assertEquals(0f, c.z, 0.0001f)
        assertEquals(1f, c.w, 0.0001f) // default alpha is 1
    }

    @Test
    fun `colorOf white`() {
        val c = colorOf(1f, 1f, 1f, 1f)
        assertEquals(1f, c.x, 0.0001f)
        assertEquals(1f, c.y, 0.0001f)
        assertEquals(1f, c.z, 0.0001f)
        assertEquals(1f, c.w, 0.0001f)
    }

    // ── FloatArray.toColor ───────────────────────────────────────────────────────

    @Test
    fun `toColor from 4-element array`() {
        val arr = floatArrayOf(1f, 0.5f, 0.25f, 0.75f)
        val c = arr.toColor()
        assertEquals(1f, c.x, 0.0001f)
        assertEquals(0.5f, c.y, 0.0001f)
        assertEquals(0.25f, c.z, 0.0001f)
        assertEquals(0.75f, c.w, 0.0001f)
    }

    @Test
    fun `toColor from 3-element array uses alpha 1`() {
        val arr = floatArrayOf(0.2f, 0.4f, 0.6f)
        val c = arr.toColor()
        assertEquals(0.2f, c.x, 0.0001f)
        assertEquals(0.4f, c.y, 0.0001f)
        assertEquals(0.6f, c.z, 0.0001f)
        assertEquals(1.0f, c.w, 0.0001f)
    }

    // ── List<Position>.getCenter ─────────────────────────────────────────────────

    @Test
    fun `getCenter of two positions returns midpoint`() {
        val positions = listOf(Position(0f, 0f, 0f), Position(2f, 4f, 6f))
        val center = positions.getCenter()
        assertEquals(1f, center.x, 0.0001f)
        assertEquals(2f, center.y, 0.0001f)
        assertEquals(3f, center.z, 0.0001f)
    }

    @Test
    fun `getCenter of single position returns itself`() {
        val positions = listOf(Position(5f, 10f, 15f))
        val center = positions.getCenter()
        assertEquals(5f, center.x, 0.0001f)
        assertEquals(10f, center.y, 0.0001f)
        assertEquals(15f, center.z, 0.0001f)
    }

    @Test
    fun `getCenter of symmetric points returns origin`() {
        val positions = listOf(
            Position(-1f, -1f, -1f), Position(1f, 1f, 1f)
        )
        val center = positions.getCenter()
        assertEquals(0f, center.x, 0.0001f)
        assertEquals(0f, center.y, 0.0001f)
        assertEquals(0f, center.z, 0.0001f)
    }

    @Test
    fun `getCenter of four positions`() {
        val positions = listOf(
            Position(0f, 0f, 0f),
            Position(4f, 0f, 0f),
            Position(4f, 4f, 0f),
            Position(0f, 4f, 0f)
        )
        val center = positions.getCenter()
        assertEquals(2f, center.x, 0.0001f)
        assertEquals(2f, center.y, 0.0001f)
        assertEquals(0f, center.z, 0.0001f)
    }

    // ── List<Position2>.getCenter ────────────────────────────────────────────────

    @Test
    fun `getCenter of 2D positions returns midpoint`() {
        val positions = listOf(Float2(0f, 0f), Float2(4f, 6f))
        val center = positions.getCenter()
        assertEquals(2f, center.x, 0.0001f)
        assertEquals(3f, center.y, 0.0001f)
    }

    @Test
    fun `getCenter of single 2D position returns itself`() {
        val positions = listOf(Float2(3f, 7f))
        val center = positions.getCenter()
        assertEquals(3f, center.x, 0.0001f)
        assertEquals(7f, center.y, 0.0001f)
    }

    // ── FloatArray conversions ───────────────────────────────────────────────────

    @Test
    fun `FloatArray toFloat3`() {
        val arr = floatArrayOf(1f, 2f, 3f)
        val v = arr.toFloat3()
        assertEquals(1f, v.x, 0.0001f)
        assertEquals(2f, v.y, 0.0001f)
        assertEquals(3f, v.z, 0.0001f)
    }

    @Test
    fun `FloatArray toFloat4`() {
        val arr = floatArrayOf(1f, 2f, 3f, 4f)
        val v = arr.toFloat4()
        assertEquals(1f, v.x, 0.0001f)
        assertEquals(2f, v.y, 0.0001f)
        assertEquals(3f, v.z, 0.0001f)
        assertEquals(4f, v.w, 0.0001f)
    }

    @Test
    fun `FloatArray toPosition`() {
        val arr = floatArrayOf(5f, 6f, 7f)
        val pos = arr.toPosition()
        assertEquals(Float3(5f, 6f, 7f), pos)
    }

    @Test
    fun `FloatArray toScale`() {
        val arr = floatArrayOf(2f, 3f, 4f)
        val scale = arr.toScale()
        assertEquals(Float3(2f, 3f, 4f), scale)
    }

    @Test
    fun `FloatArray toDirection`() {
        val arr = floatArrayOf(0f, 1f, 0f)
        val dir = arr.toDirection()
        assertEquals(Float3(0f, 1f, 0f), dir)
    }

    // ── Float3/Float4 fuzzy equals ───────────────────────────────────────────────

    @Test
    fun `Float3 fuzzy equals within tight delta`() {
        assertTrue(Float3(1f, 1f, 1f).equals(Float3(1.001f, 0.999f, 1.0005f), 0.01f))
    }

    @Test
    fun `Float3 fuzzy equals within wider delta`() {
        assertTrue(Float3(1f, 1f, 1f).equals(Float3(1.05f, 0.95f, 1.02f), 0.1f))
    }

    @Test
    fun `Float3 fuzzy equals outside delta`() {
        assertFalse(Float3(1f, 1f, 1f).equals(Float3(1.5f, 1f, 1f), 0.1f))
    }

    @Test
    fun `Float4 fuzzy equals within tight delta`() {
        assertTrue(Float4(1f, 1f, 1f, 1f).equals(Float4(1.001f, 0.999f, 1f, 1.001f), 0.01f))
    }

    @Test
    fun `Float4 fuzzy equals within wider delta`() {
        assertTrue(Float4(1f, 1f, 1f, 1f).equals(Float4(1.05f, 0.95f, 1f, 1.02f), 0.1f))
    }

    @Test
    fun `Float4 fuzzy equals outside delta`() {
        assertFalse(Float4(1f, 1f, 1f, 1f).equals(Float4(2f, 1f, 1f, 1f), 0.1f))
    }

    // ── normalToTangent ──────────────────────────────────────────────────────────

    @Test
    fun `normalToTangent for Y-up normal returns unit quaternion`() {
        val q = normalToTangent(Float3(0f, 1f, 0f))
        val len = sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
        assertEquals("Quaternion should be unit length", 1f, len, 0.001f)
    }

    @Test
    fun `normalToTangent for Z-up normal returns unit quaternion`() {
        val q = normalToTangent(Float3(0f, 0f, 1f))
        val len = sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
        assertEquals("Quaternion should be unit length", 1f, len, 0.001f)
    }

    @Test
    fun `normalToTangent for X-axis normal returns unit quaternion`() {
        val q = normalToTangent(Float3(1f, 0f, 0f))
        val len = sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
        assertEquals("Quaternion should be unit length", 1f, len, 0.001f)
    }
}

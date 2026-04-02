package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for geometry utility functions and data classes that have no Filament dependency.
 *
 * - [Geometry.Vertex] is a data class with no native deps
 * - [getOffsets] is a pure List<List<Int>> extension
 * - [hasNormals], [hasUvCoordinates], [hasColors] are list extensions
 */
class GeometryUtilsTest {

    // ── Vertex data class ───────────────────────────────────────────────────────

    @Test
    fun `Vertex default constructor has zero position and null optionals`() {
        val v = Geometry.Vertex()

        assertEquals(Float3(0f, 0f, 0f), v.position)
        assertNull(v.normal)
        assertNull(v.uvCoordinate)
        assertNull(v.color)
    }

    @Test
    fun `Vertex with all fields set`() {
        val pos = Float3(1f, 2f, 3f)
        val normal = Float3(0f, 1f, 0f)
        val uv = Float2(0.5f, 0.5f)
        val color = dev.romainguy.kotlin.math.Float4(1f, 0f, 0f, 1f)

        val v = Geometry.Vertex(
            position = pos,
            normal = normal,
            uvCoordinate = uv,
            color = color
        )

        assertEquals(pos, v.position)
        assertEquals(normal, v.normal)
        assertEquals(uv, v.uvCoordinate)
        assertEquals(color, v.color)
    }

    @Test
    fun `Vertex equals and copy work as data class`() {
        val a = Geometry.Vertex(position = Float3(1f, 2f, 3f))
        val b = Geometry.Vertex(position = Float3(1f, 2f, 3f))
        assertEquals(a, b)

        val c = a.copy(position = Float3(4f, 5f, 6f))
        assertEquals(Float3(4f, 5f, 6f), c.position)
    }

    // ── List extension: hasNormals / hasUvCoordinates / hasColors ────────────────

    @Test
    fun `hasNormals returns false for empty list`() {
        assertFalse(emptyList<Geometry.Vertex>().hasNormals)
    }

    @Test
    fun `hasNormals returns false when no vertex has normal`() {
        val vertices = listOf(Geometry.Vertex(), Geometry.Vertex())
        assertFalse(vertices.hasNormals)
    }

    @Test
    fun `hasNormals returns true when at least one vertex has normal`() {
        val vertices = listOf(
            Geometry.Vertex(),
            Geometry.Vertex(normal = Float3(0f, 1f, 0f))
        )
        assertTrue(vertices.hasNormals)
    }

    @Test
    fun `hasUvCoordinates returns false when no vertex has uv`() {
        val vertices = listOf(Geometry.Vertex())
        assertFalse(vertices.hasUvCoordinates)
    }

    @Test
    fun `hasUvCoordinates returns true when at least one vertex has uv`() {
        val vertices = listOf(
            Geometry.Vertex(uvCoordinate = Float2(0f, 0f))
        )
        assertTrue(vertices.hasUvCoordinates)
    }

    @Test
    fun `hasColors returns false when no vertex has color`() {
        assertFalse(listOf(Geometry.Vertex()).hasColors)
    }

    @Test
    fun `hasColors returns true when at least one vertex has color`() {
        val vertices = listOf(
            Geometry.Vertex(color = dev.romainguy.kotlin.math.Float4(1f, 0f, 0f, 1f))
        )
        assertTrue(vertices.hasColors)
    }

    // ── getOffsets ──────────────────────────────────────────────────────────────

    @Test
    fun `getOffsets with empty list returns empty`() {
        val offsets = emptyList<List<Int>>().getOffsets()
        assertTrue(offsets.isEmpty())
    }

    @Test
    fun `getOffsets with single primitive`() {
        val indices = listOf(listOf(0, 1, 2, 3, 4, 5))
        val offsets = indices.getOffsets()

        assertEquals(1, offsets.size)
        assertEquals(0 until 6, offsets[0])
    }

    @Test
    fun `getOffsets with multiple primitives`() {
        val indices = listOf(
            listOf(0, 1, 2),       // 3 indices
            listOf(3, 4, 5, 6, 7), // 5 indices
            listOf(8, 9)           // 2 indices
        )
        val offsets = indices.getOffsets()

        assertEquals(3, offsets.size)
        assertEquals(0 until 3, offsets[0])
        assertEquals(3 until 8, offsets[1])
        assertEquals(8 until 10, offsets[2])
    }

    @Test
    fun `getOffsets preserves total index count`() {
        val indices = listOf(
            listOf(0, 1, 2, 3, 4, 5),
            listOf(6, 7, 8, 9, 10, 11)
        )
        val offsets = indices.getOffsets()
        val totalCount = offsets.sumOf { it.count() }
        assertEquals(indices.sumOf { it.size }, totalCount)
    }
}

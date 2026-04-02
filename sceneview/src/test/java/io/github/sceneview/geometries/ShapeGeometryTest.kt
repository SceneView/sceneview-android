package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [Shape] companion object geometry generation.
 *
 * [Shape.getVertices], [Shape.getPolygonIndices], and [Shape.getDelaunayIndices] are pure
 * functions with no Filament dependency, so they are fully testable as JVM unit tests.
 *
 * [Shape.Builder.build] and [Shape.update] require a Filament [Engine] and are NOT tested here.
 */
class ShapeGeometryTest {

    // ── DEFAULT_NORMAL ───────────────────────────────────────────────────────────

    @Test
    fun `default normal points forward along Z`() {
        assertEquals(Float3(0f, 0f, 1f), Shape.DEFAULT_NORMAL)
    }

    // ── getVertices ──────────────────────────────────────────────────────────────

    @Test
    fun `getVertices returns empty list for empty positions`() {
        val vertices = Shape.getVertices(emptyList())
        assertTrue(vertices.isEmpty())
    }

    @Test
    fun `getVertices count matches positions count`() {
        val positions = listOf(Float2(0f, 0f), Float2(1f, 0f), Float2(0.5f, 1f))
        val vertices = Shape.getVertices(positions)
        assertEquals(3, vertices.size)
    }

    @Test
    fun `getVertices positions map to 3D with z=0 by default`() {
        val positions = listOf(Float2(1f, 2f), Float2(3f, 4f))
        val vertices = Shape.getVertices(positions)
        assertEquals(1f, vertices[0].position.x, 0.0001f)
        assertEquals(2f, vertices[0].position.y, 0.0001f)
        assertEquals(0f, vertices[0].position.z, 0.0001f)
        assertEquals(3f, vertices[1].position.x, 0.0001f)
        assertEquals(4f, vertices[1].position.y, 0.0001f)
    }

    @Test
    fun `getVertices uses provided normal`() {
        val normal = Float3(0f, 1f, 0f)
        val positions = listOf(Float2(0f, 0f), Float2(1f, 0f))
        val vertices = Shape.getVertices(positions, normal = normal)
        for (v in vertices) {
            assertEquals(normal, v.normal)
        }
    }

    @Test
    fun `getVertices uses default normal when not specified`() {
        val positions = listOf(Float2(0f, 0f), Float2(1f, 0f))
        val vertices = Shape.getVertices(positions)
        for (v in vertices) {
            assertEquals(Shape.DEFAULT_NORMAL, v.normal)
        }
    }

    @Test
    fun `getVertices UV coordinates computed from positions when no scale`() {
        val positions = listOf(Float2(1f, 2f))
        val scale = Float2(1f, 1f)
        val vertices = Shape.getVertices(positions, uvScale = scale)
        // UV = position * uvScale(1,1) = (1,2)
        assertEquals(1f, vertices[0].uvCoordinate!!.x, 0.0001f)
        assertEquals(2f, vertices[0].uvCoordinate!!.y, 0.0001f)
    }

    @Test
    fun `getVertices UV coordinates scale with uvScale`() {
        val positions = listOf(Float2(1f, 2f))
        val scale = Float2(2f, 3f)
        val vertices = Shape.getVertices(positions, uvScale = scale)
        assertEquals(2f, vertices[0].uvCoordinate!!.x, 0.0001f)
        assertEquals(6f, vertices[0].uvCoordinate!!.y, 0.0001f)
    }

    @Test
    fun `getVertices color is null when not specified`() {
        val positions = listOf(Float2(0f, 0f), Float2(1f, 0f))
        val vertices = Shape.getVertices(positions)
        for (v in vertices) {
            assertNull("Color should be null by default", v.color)
        }
    }

    @Test
    fun `getVertices color is applied when specified`() {
        val color = Float4(1f, 0f, 0f, 1f)
        val positions = listOf(Float2(0f, 0f), Float2(1f, 0f))
        val vertices = Shape.getVertices(positions, color = color)
        for (v in vertices) {
            assertNotNull("Color should be set", v.color)
            assertEquals(color, v.color)
        }
    }

    @Test
    fun `getVertices with many positions`() {
        val count = 50
        val positions = (0 until count).map { Float2(it.toFloat(), it.toFloat()) }
        val vertices = Shape.getVertices(positions)
        assertEquals(count, vertices.size)
    }

    // ── getPolygonIndices ────────────────────────────────────────────────────────

    @Test
    fun `getPolygonIndices empty positions returns empty list`() {
        val indices = Shape.getPolygonIndices(emptyList())
        assertTrue(indices.isEmpty())
    }

    @Test
    fun `getPolygonIndices triangle returns non-empty indices`() {
        val triangle = listOf(Float2(0f, 0f), Float2(1f, 0f), Float2(0.5f, 1f))
        val indices = Shape.getPolygonIndices(triangle)
        assertTrue("Triangle should produce indices", indices.isNotEmpty())
        assertTrue("Should have at least 3 indices", indices[0].size >= 3)
    }

    @Test
    fun `getPolygonIndices triangle has exactly 3 indices (1 triangle)`() {
        val triangle = listOf(Float2(0f, 0f), Float2(1f, 0f), Float2(0.5f, 1f))
        val indices = Shape.getPolygonIndices(triangle)
        assertEquals(3, indices[0].size)
    }

    @Test
    fun `getPolygonIndices quad has 6 indices (2 triangles)`() {
        val quad = listOf(
            Float2(0f, 0f), Float2(1f, 0f), Float2(1f, 1f), Float2(0f, 1f)
        )
        val indices = Shape.getPolygonIndices(quad)
        assertEquals(6, indices[0].size)
    }

    @Test
    fun `getPolygonIndices all index values within vertex range`() {
        val pentagon = listOf(
            Float2(0f, 1f), Float2(0.95f, 0.31f), Float2(0.59f, -0.81f),
            Float2(-0.59f, -0.81f), Float2(-0.95f, 0.31f)
        )
        val indices = Shape.getPolygonIndices(pentagon)
        for (primitive in indices) {
            for (idx in primitive) {
                assertTrue("Index $idx out of range [0, 5)", idx in 0 until pentagon.size)
            }
        }
    }

    @Test
    fun `getPolygonIndices returns single primitive`() {
        val triangle = listOf(Float2(0f, 0f), Float2(1f, 0f), Float2(0.5f, 1f))
        val indices = Shape.getPolygonIndices(triangle)
        assertEquals(1, indices.size)
    }

    // ── getDelaunayIndices ───────────────────────────────────────────────────────

    @Test
    fun `getDelaunayIndices empty positions returns empty list`() {
        val indices = Shape.getDelaunayIndices(emptyList())
        assertTrue(indices.isEmpty())
    }

    @Test
    fun `getDelaunayIndices triangle returns non-empty indices`() {
        val triangle = listOf(Float2(0f, 0f), Float2(1f, 0f), Float2(0.5f, 1f))
        val indices = Shape.getDelaunayIndices(triangle)
        assertTrue("Delaunay triangle should produce indices", indices.isNotEmpty())
    }

    @Test
    fun `getDelaunayIndices triangle produces multiples of 3`() {
        val triangle = listOf(Float2(0f, 0f), Float2(1f, 0f), Float2(0.5f, 1f))
        val indices = Shape.getDelaunayIndices(triangle)
        assertEquals(0, indices[0].size % 3)
    }

    @Test
    fun `getDelaunayIndices quad produces multiples of 3`() {
        val quad = listOf(
            Float2(0f, 0f), Float2(1f, 0f), Float2(1f, 1f), Float2(0f, 1f)
        )
        val indices = Shape.getDelaunayIndices(quad)
        assertEquals(0, indices[0].size % 3)
    }

    @Test
    fun `getDelaunayIndices returns single primitive`() {
        val triangle = listOf(Float2(0f, 0f), Float2(1f, 0f), Float2(0.5f, 1f))
        val indices = Shape.getDelaunayIndices(triangle)
        assertEquals(1, indices.size)
    }

    @Test
    fun `getDelaunayIndices all index values within point count`() {
        val points = listOf(
            Float2(0f, 0f), Float2(2f, 0f), Float2(2f, 2f), Float2(0f, 2f), Float2(1f, 1f)
        )
        val indices = Shape.getDelaunayIndices(points)
        for (primitive in indices) {
            for (idx in primitive) {
                assertTrue("Index $idx out of range [0, ${points.size})", idx in 0 until points.size)
            }
        }
    }
}

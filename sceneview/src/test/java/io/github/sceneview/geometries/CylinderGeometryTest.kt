package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [Cylinder] companion object geometry generation.
 *
 * [Cylinder.getVertices] and [Cylinder.getIndices] are pure functions using only kotlin-math,
 * so they are fully testable without Filament.
 */
class CylinderGeometryTest {

    @Test
    fun `default radius is 1`() {
        assertEquals(1.0f, Cylinder.DEFAULT_RADIUS, 0f)
    }

    @Test
    fun `default height is 2`() {
        assertEquals(2.0f, Cylinder.DEFAULT_HEIGHT, 0f)
    }

    @Test
    fun `default center is origin`() {
        assertEquals(Float3(0f, 0f, 0f), Cylinder.DEFAULT_CENTER)
    }

    @Test
    fun `default side count is 24`() {
        assertEquals(24, Cylinder.DEFAULT_SIDE_COUNT)
    }

    @Test
    fun `getVertices produces non-empty list`() {
        val vertices = Cylinder.getVertices(1f, 2f, Float3(0f), 8)
        assertTrue("Should have vertices", vertices.isNotEmpty())
    }

    @Test
    fun `getIndices returns sideCount primitives`() {
        val sideCount = 8
        val indices = Cylinder.getIndices(sideCount)
        assertEquals(sideCount, indices.size)
    }

    @Test
    fun `each primitive has 12 indices (2 side tris + 2 cap tris = 4 tris x 3 verts)`() {
        val sideCount = 6
        val indices = Cylinder.getIndices(sideCount)
        for (primitive in indices) {
            assertEquals(12, primitive.size)
        }
    }

    @Test
    fun `all index values are non-negative`() {
        val sideCount = 12
        val indices = Cylinder.getIndices(sideCount)
        for (primitive in indices) {
            for (idx in primitive) {
                assertTrue("Index $idx is negative", idx >= 0)
            }
        }
    }

    @Test
    fun `all index values are within vertex range`() {
        val sideCount = 8
        val vertices = Cylinder.getVertices(1f, 2f, Float3(0f), sideCount)
        val vertexCount = vertices.size
        val indices = Cylinder.getIndices(sideCount)

        for (primitive in indices) {
            for (idx in primitive) {
                assertTrue(
                    "Index $idx out of range [0, $vertexCount)",
                    idx in 0 until vertexCount
                )
            }
        }
    }

    @Test
    fun `vertices y range matches half-height for default params`() {
        val height = 2f
        val vertices = Cylinder.getVertices(1f, height, Float3(0f), 12)

        val minY = vertices.minOf { it.position.y }
        val maxY = vertices.maxOf { it.position.y }

        assertEquals(-height / 2f, minY, 0.001f)
        assertEquals(height / 2f, maxY, 0.001f)
    }

    @Test
    fun `vertices shift when center is non-zero`() {
        val center = Float3(10f, 20f, 30f)
        val vertices = Cylinder.getVertices(1f, 2f, center, 6)

        val minY = vertices.minOf { it.position.y }
        val maxY = vertices.maxOf { it.position.y }

        assertEquals(center.y - 1f, minY, 0.001f)
        assertEquals(center.y + 1f, maxY, 0.001f)
    }

    @Test
    fun `all vertices have normals`() {
        val vertices = Cylinder.getVertices(1f, 2f, Float3(0f), 6)
        for (v in vertices) {
            assertTrue("Vertex missing normal", v.normal != null)
        }
    }

    @Test
    fun `all vertices have UV coordinates`() {
        val vertices = Cylinder.getVertices(1f, 2f, Float3(0f), 6)
        for (v in vertices) {
            assertTrue("Vertex missing UV", v.uvCoordinate != null)
        }
    }
}

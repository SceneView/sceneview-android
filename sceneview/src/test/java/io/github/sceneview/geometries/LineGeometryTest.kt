package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [Line] companion object geometry generation.
 *
 * [Line.getVertices] and [Line.getIndices] are pure functions with no Filament dependency,
 * so they are fully testable as JVM unit tests.
 *
 * [Line.Builder.build] and [Line.update] require a Filament [Engine] and are NOT tested here.
 */
class LineGeometryTest {

    @Test
    fun `default start is origin`() {
        assertEquals(Float3(0f, 0f, 0f), Line.DEFAULT_START)
    }

    @Test
    fun `default end is (1, 0, 0)`() {
        assertEquals(Float3(1f, 0f, 0f), Line.DEFAULT_END)
    }

    @Test
    fun `getVertices returns exactly 2 vertices`() {
        val vertices = Line.getVertices(Line.DEFAULT_START, Line.DEFAULT_END)
        assertEquals(2, vertices.size)
    }

    @Test
    fun `getVertices first vertex matches start`() {
        val start = Float3(1f, 2f, 3f)
        val end = Float3(4f, 5f, 6f)
        val vertices = Line.getVertices(start, end)
        assertEquals(start, vertices[0].position)
    }

    @Test
    fun `getVertices second vertex matches end`() {
        val start = Float3(1f, 2f, 3f)
        val end = Float3(4f, 5f, 6f)
        val vertices = Line.getVertices(start, end)
        assertEquals(end, vertices[1].position)
    }

    @Test
    fun `getVertices with identical start and end produces degenerate line`() {
        val point = Float3(5f, 5f, 5f)
        val vertices = Line.getVertices(point, point)
        assertEquals(2, vertices.size)
        assertEquals(point, vertices[0].position)
        assertEquals(point, vertices[1].position)
    }

    @Test
    fun `getIndices returns a single primitive`() {
        val indices = Line.getIndices()
        assertEquals(1, indices.size)
    }

    @Test
    fun `getIndices primitive has exactly 2 entries (one segment)`() {
        val indices = Line.getIndices()
        assertEquals(2, indices[0].size)
    }

    @Test
    fun `getIndices references vertex 0 and vertex 1`() {
        val indices = Line.getIndices()
        assertEquals(0, indices[0][0])
        assertEquals(1, indices[0][1])
    }

    @Test
    fun `getVertices along Y axis preserves positions`() {
        val start = Float3(0f, -1f, 0f)
        val end = Float3(0f, 1f, 0f)
        val vertices = Line.getVertices(start, end)
        assertEquals(start.y, vertices[0].position.y, 0.0001f)
        assertEquals(end.y, vertices[1].position.y, 0.0001f)
    }

    @Test
    fun `getVertices along Z axis preserves positions`() {
        val start = Float3(0f, 0f, -5f)
        val end = Float3(0f, 0f, 5f)
        val vertices = Line.getVertices(start, end)
        assertEquals(-5f, vertices[0].position.z, 0.0001f)
        assertEquals(5f, vertices[1].position.z, 0.0001f)
    }

    @Test
    fun `vertices have null normals (line has no surface normal)`() {
        val vertices = Line.getVertices(Line.DEFAULT_START, Line.DEFAULT_END)
        for (v in vertices) {
            assertEquals("Line vertex should have no normal", null, v.normal)
        }
    }

    @Test
    fun `vertices have null UV coordinates`() {
        val vertices = Line.getVertices(Line.DEFAULT_START, Line.DEFAULT_END)
        for (v in vertices) {
            assertEquals("Line vertex should have no UV", null, v.uvCoordinate)
        }
    }

    @Test
    fun `all index values are non-negative`() {
        val indices = Line.getIndices()
        for (primitive in indices) {
            for (idx in primitive) {
                assertTrue("Index $idx is negative", idx >= 0)
            }
        }
    }

    @Test
    fun `all index values are within vertex range`() {
        val vertexCount = Line.getVertices(Line.DEFAULT_START, Line.DEFAULT_END).size
        val indices = Line.getIndices()
        for (primitive in indices) {
            for (idx in primitive) {
                assertTrue("Index $idx out of range [0, $vertexCount)", idx in 0 until vertexCount)
            }
        }
    }

    @Test
    fun `large coordinate values are handled correctly`() {
        val start = Float3(1000f, -1000f, 500f)
        val end = Float3(-1000f, 1000f, -500f)
        val vertices = Line.getVertices(start, end)
        assertEquals(start, vertices[0].position)
        assertEquals(end, vertices[1].position)
    }
}

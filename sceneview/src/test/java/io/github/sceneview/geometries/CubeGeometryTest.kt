package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [Cube] companion object geometry generation.
 *
 * [Cube.getVertices] and [Cube.INDICES] are pure functions that only use kotlin-math types,
 * so they are fully testable without Filament.
 *
 * [Cube.Builder.build] and [Cube.update] require a Filament [Engine] and are NOT tested here.
 */
class CubeGeometryTest {

    @Test
    fun `default size is 1x1x1`() {
        assertEquals(Float3(1f, 1f, 1f), Cube.DEFAULT_SIZE)
    }

    @Test
    fun `default center is origin`() {
        assertEquals(Float3(0f, 0f, 0f), Cube.DEFAULT_CENTER)
    }

    @Test
    fun `getVertices returns 24 vertices for a cube (4 per face x 6 faces)`() {
        val vertices = Cube.getVertices(Cube.DEFAULT_SIZE, Cube.DEFAULT_CENTER)
        assertEquals(24, vertices.size)
    }

    @Test
    fun `INDICES has 6 primitives (one per face)`() {
        assertEquals(6, Cube.INDICES.size)
    }

    @Test
    fun `each face has 6 indices (2 triangles x 3 vertices)`() {
        for (face in Cube.INDICES) {
            assertEquals(6, face.size)
        }
    }

    @Test
    fun `total index count is 36`() {
        val total = Cube.INDICES.sumOf { it.size }
        assertEquals(36, total)
    }

    @Test
    fun `vertices are within half-extent bounds for unit cube`() {
        val vertices = Cube.getVertices(Float3(1f), Float3(0f))
        val halfExtent = 0.5f + 0.0001f // small epsilon

        for (v in vertices) {
            assertTrue(
                "Position ${v.position} out of bounds",
                v.position.x in -halfExtent..halfExtent &&
                        v.position.y in -halfExtent..halfExtent &&
                        v.position.z in -halfExtent..halfExtent
            )
        }
    }

    @Test
    fun `vertices shift when center is non-zero`() {
        val center = Float3(10f, 20f, 30f)
        val vertices = Cube.getVertices(Float3(1f), center)

        // All vertices should be within 0.5 of the center
        for (v in vertices) {
            assertTrue(
                "Vertex ${v.position} not near center $center",
                v.position.x in 9.49f..10.51f &&
                        v.position.y in 19.49f..20.51f &&
                        v.position.z in 29.49f..30.51f
            )
        }
    }

    @Test
    fun `all vertices have normals`() {
        val vertices = Cube.getVertices(Cube.DEFAULT_SIZE, Cube.DEFAULT_CENTER)
        for (v in vertices) {
            assertTrue("Vertex missing normal", v.normal != null)
        }
    }

    @Test
    fun `all vertices have UV coordinates`() {
        val vertices = Cube.getVertices(Cube.DEFAULT_SIZE, Cube.DEFAULT_CENTER)
        for (v in vertices) {
            assertTrue("Vertex missing UV", v.uvCoordinate != null)
        }
    }

    @Test
    fun `UV coordinates are in 0-1 range`() {
        val vertices = Cube.getVertices(Cube.DEFAULT_SIZE, Cube.DEFAULT_CENTER)
        for (v in vertices) {
            val uv = v.uvCoordinate!!
            assertTrue("UV x=${uv.x} out of range", uv.x in 0f..1f)
            assertTrue("UV y=${uv.y} out of range", uv.y in 0f..1f)
        }
    }

    @Test
    fun `custom size produces larger extents`() {
        val size = Float3(4f, 6f, 8f)
        val vertices = Cube.getVertices(size, Float3(0f))

        val maxX = vertices.maxOf { it.position.x }
        val maxY = vertices.maxOf { it.position.y }
        val maxZ = vertices.maxOf { it.position.z }

        assertEquals(2f, maxX, 0.001f)
        assertEquals(3f, maxY, 0.001f)
        assertEquals(4f, maxZ, 0.001f)
    }

    @Test
    fun `all index values are within vertex range`() {
        val vertexCount = 24
        for (face in Cube.INDICES) {
            for (idx in face) {
                assertTrue("Index $idx >= $vertexCount", idx < vertexCount)
                assertTrue("Index $idx < 0", idx >= 0)
            }
        }
    }
}

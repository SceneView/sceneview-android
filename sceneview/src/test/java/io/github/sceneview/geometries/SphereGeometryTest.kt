package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * Tests for [Sphere] companion object geometry generation.
 *
 * [Sphere.getVertices] and [Sphere.getIndices] are pure functions using only kotlin-math,
 * so they are fully testable without Filament.
 */
class SphereGeometryTest {

    @Test
    fun `default radius is 1`() {
        assertEquals(1.0f, Sphere.DEFAULT_RADIUS, 0f)
    }

    @Test
    fun `default center is origin`() {
        assertEquals(Float3(0f, 0f, 0f), Sphere.DEFAULT_CENTER)
    }

    @Test
    fun `default stacks and slices are 24`() {
        assertEquals(24, Sphere.DEFAULT_STACKS)
        assertEquals(24, Sphere.DEFAULT_SLICES)
    }

    @Test
    fun `getVertices count is (stacks+1) x (slices+1)`() {
        val stacks = 10
        val slices = 8
        val vertices = Sphere.getVertices(1f, Float3(0f), stacks, slices)
        assertEquals((stacks + 1) * (slices + 1), vertices.size)
    }

    @Test
    fun `vertices are approximately on the sphere surface for unit radius`() {
        val vertices = Sphere.getVertices(1f, Float3(0f), 12, 12)

        for (v in vertices) {
            val dist = sqrt(
                v.position.x * v.position.x +
                        v.position.y * v.position.y +
                        v.position.z * v.position.z
            )
            assertEquals("Vertex ${v.position} not on unit sphere", 1f, dist, 0.01f)
        }
    }

    @Test
    fun `vertices shift when center is non-zero`() {
        val center = Float3(5f, 5f, 5f)
        val vertices = Sphere.getVertices(1f, center, 4, 4)

        // All vertices should be within radius+epsilon of the center
        for (v in vertices) {
            val dx = v.position.x - center.x
            val dy = v.position.y - center.y
            val dz = v.position.z - center.z
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            assertEquals("Vertex ${v.position} not at radius 1 from center", 1f, dist, 0.01f)
        }
    }

    @Test
    fun `vertices scale with radius`() {
        val radius = 3f
        val vertices = Sphere.getVertices(radius, Float3(0f), 8, 8)

        for (v in vertices) {
            val dist = sqrt(
                v.position.x * v.position.x +
                        v.position.y * v.position.y +
                        v.position.z * v.position.z
            )
            assertEquals("Vertex ${v.position} not on sphere of radius $radius", radius, dist, 0.05f)
        }
    }

    @Test
    fun `getIndices returns stacks primitives`() {
        val stacks = 10
        val slices = 8
        val indices = Sphere.getIndices(stacks, slices)
        assertEquals(stacks, indices.size)
    }

    @Test
    fun `all index values are within vertex range`() {
        val stacks = 8
        val slices = 6
        val vertexCount = (stacks + 1) * (slices + 1)
        val indices = Sphere.getIndices(stacks, slices)

        for (primitive in indices) {
            for (idx in primitive) {
                assertTrue("Index $idx out of range [0, $vertexCount)", idx in 0 until vertexCount)
            }
        }
    }

    @Test
    fun `all vertices have normals`() {
        val vertices = Sphere.getVertices(1f, Float3(0f), 6, 6)
        for (v in vertices) {
            assertTrue("Vertex missing normal", v.normal != null)
        }
    }

    @Test
    fun `all vertices have UV coordinates`() {
        val vertices = Sphere.getVertices(1f, Float3(0f), 6, 6)
        for (v in vertices) {
            assertTrue("Vertex missing UV", v.uvCoordinate != null)
        }
    }

    @Test
    fun `UV coordinates are in 0-1 range`() {
        val vertices = Sphere.getVertices(1f, Float3(0f), 12, 12)
        for (v in vertices) {
            val uv = v.uvCoordinate!!
            assertTrue("UV x=${uv.x} out of range", uv.x in -0.001f..1.001f)
            assertTrue("UV y=${uv.y} out of range", uv.y in -0.001f..1.001f)
        }
    }

    @Test
    fun `minimum stacks and slices produce valid geometry`() {
        val vertices = Sphere.getVertices(1f, Float3(0f), 2, 3)
        val indices = Sphere.getIndices(2, 3)

        assertTrue("Should have vertices", vertices.isNotEmpty())
        assertTrue("Should have indices", indices.isNotEmpty())
        assertEquals(2, indices.size)
    }
}

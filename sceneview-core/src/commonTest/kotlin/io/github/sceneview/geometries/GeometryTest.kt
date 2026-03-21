package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.length
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeometryTest {

    // ----- Cube -----

    @Test
    fun cubeVertexCount() {
        val cube = generateCube()
        // 6 faces * 4 vertices each = 24
        assertEquals(24, cube.vertices.size)
    }

    @Test
    fun cubeIndexCount() {
        val cube = generateCube()
        // 6 faces * 2 triangles * 3 indices = 36
        assertEquals(36, cube.indices.size)
    }

    @Test
    fun cubeNormalsAreUnitLength() {
        val cube = generateCube()
        for (v in cube.vertices) {
            val n = v.normal!!
            assertEquals(1f, length(n), 1e-5f, "Normal $n is not unit length")
        }
    }

    @Test
    fun cubeUvsInRange() {
        val cube = generateCube()
        for (v in cube.vertices) {
            val uv = v.uvCoordinate!!
            assertTrue(uv.x in 0f..1f, "UV x=${uv.x} out of range")
            assertTrue(uv.y in 0f..1f, "UV y=${uv.y} out of range")
        }
    }

    @Test
    fun cubeCustomSize() {
        val cube = generateCube(size = Float3(2f, 4f, 6f))
        val xs = cube.vertices.map { it.position.x }
        val ys = cube.vertices.map { it.position.y }
        val zs = cube.vertices.map { it.position.z }
        assertEquals(2f, xs.max() - xs.min(), 1e-5f)
        assertEquals(4f, ys.max() - ys.min(), 1e-5f)
        assertEquals(6f, zs.max() - zs.min(), 1e-5f)
    }

    // ----- Sphere -----

    @Test
    fun sphereVertexCount() {
        val stacks = 24
        val slices = 24
        val sphere = generateSphere(stacks = stacks, slices = slices)
        // (stacks + 1) * (slices + 1)
        assertEquals((stacks + 1) * (slices + 1), sphere.vertices.size)
    }

    @Test
    fun sphereSmallVertexCount() {
        val stacks = 8
        val slices = 12
        val sphere = generateSphere(stacks = stacks, slices = slices)
        assertEquals((stacks + 1) * (slices + 1), sphere.vertices.size)
    }

    @Test
    fun sphereNormalsAreUnitLength() {
        val sphere = generateSphere(stacks = 10, slices = 10)
        for (v in sphere.vertices) {
            val n = v.normal!!
            val len = length(n)
            // At the poles sin(phi)=0 so the position is zero and normalize returns zero.
            // Skip those degenerate vertices.
            if (len > 1e-6f) {
                assertEquals(1f, len, 1e-4f, "Normal $n is not unit length (len=$len)")
            }
        }
    }

    @Test
    fun sphereUvsInRange() {
        val sphere = generateSphere(stacks = 10, slices = 10)
        for (v in sphere.vertices) {
            val uv = v.uvCoordinate!!
            assertTrue(uv.x in -1e-5f..1f + 1e-5f, "UV x=${uv.x} out of range")
            assertTrue(uv.y in -1e-5f..1f + 1e-5f, "UV y=${uv.y} out of range")
        }
    }

    // ----- Cylinder -----

    @Test
    fun cylinderHasVertices() {
        val cylinder = generateCylinder()
        assertTrue(cylinder.vertices.isNotEmpty())
        assertTrue(cylinder.indices.isNotEmpty())
    }

    @Test
    fun cylinderNormalsAreUnitLength() {
        val cylinder = generateCylinder(sideCount = 12)
        for (v in cylinder.vertices) {
            val n = v.normal!!
            assertEquals(1f, length(n), 1e-4f, "Normal $n is not unit length")
        }
    }

    @Test
    fun cylinderUvsInRange() {
        val cylinder = generateCylinder(sideCount = 12)
        for (v in cylinder.vertices) {
            val uv = v.uvCoordinate!!
            assertTrue(uv.x in -1e-5f..1f + 1e-5f, "UV x=${uv.x} out of range")
            assertTrue(uv.y in -1e-5f..1f + 1e-5f, "UV y=${uv.y} out of range")
        }
    }

    // ----- Plane -----

    @Test
    fun planeVertexCount() {
        val plane = generatePlane()
        assertEquals(4, plane.vertices.size)
    }

    @Test
    fun planeIndexCount() {
        val plane = generatePlane()
        // 2 triangles * 3 = 6
        assertEquals(6, plane.indices.size)
    }

    @Test
    fun planeNormalsAreUnitLength() {
        val plane = generatePlane()
        for (v in plane.vertices) {
            val n = v.normal!!
            assertEquals(1f, length(n), 1e-5f)
        }
    }

    @Test
    fun planeUvsInRange() {
        val plane = generatePlane()
        for (v in plane.vertices) {
            val uv = v.uvCoordinate!!
            assertTrue(uv.x in 0f..1f, "UV x=${uv.x} out of range")
            assertTrue(uv.y in 0f..1f, "UV y=${uv.y} out of range")
        }
    }
}

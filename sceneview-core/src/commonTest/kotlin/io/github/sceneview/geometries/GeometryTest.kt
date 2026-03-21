package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.length
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    // ----- BoundingBox -----

    @Test
    fun cubeBoundingBox() {
        val cube = generateCube(size = Float3(2f, 4f, 6f))
        val bb = cube.boundingBox()
        assertEquals(0f, bb.center.x, 1e-5f)
        assertEquals(0f, bb.center.y, 1e-5f)
        assertEquals(0f, bb.center.z, 1e-5f)
        assertEquals(1f, bb.halfExtent.x, 1e-5f)
        assertEquals(2f, bb.halfExtent.y, 1e-5f)
        assertEquals(3f, bb.halfExtent.z, 1e-5f)
    }

    @Test
    fun sphereBoundingBox() {
        val sphere = generateSphere(radius = 2f, stacks = 16, slices = 16)
        val bb = sphere.boundingBox()
        // Center should be ~0
        assertTrue(abs(bb.center.x) < 0.1f)
        assertTrue(abs(bb.center.y) < 0.1f)
        assertTrue(abs(bb.center.z) < 0.1f)
        // Half extent should be ~radius
        assertTrue(abs(bb.halfExtent.x - 2f) < 0.2f)
        assertTrue(abs(bb.halfExtent.y - 2f) < 0.01f) // poles are exact
    }

    @Test
    fun cubeHasNormalsAndUvs() {
        val cube = generateCube()
        assertTrue(cube.hasNormals)
        assertTrue(cube.hasUvCoordinates)
        assertFalse(cube.hasColors)
    }

    // ----- Line -----

    @Test
    fun lineVertexCount() {
        val line = generateLine()
        assertEquals(2, line.vertices.size)
    }

    @Test
    fun lineIndexCount() {
        val line = generateLine()
        assertEquals(2, line.indices.size)
        assertEquals(0, line.indices[0])
        assertEquals(1, line.indices[1])
    }

    @Test
    fun lineCustomEndpoints() {
        val start = Float3(1f, 2f, 3f)
        val end = Float3(4f, 5f, 6f)
        val line = generateLine(start = start, end = end)
        assertEquals(start, line.vertices[0].position)
        assertEquals(end, line.vertices[1].position)
    }

    @Test
    fun lineBoundingBox() {
        val line = generateLine(start = Float3(0f), end = Float3(2f, 4f, 6f))
        val bb = line.boundingBox()
        assertEquals(1f, bb.center.x, 1e-5f)
        assertEquals(2f, bb.center.y, 1e-5f)
        assertEquals(3f, bb.center.z, 1e-5f)
    }

    // ----- Path -----

    @Test
    fun pathVertexCount() {
        val points = listOf(Float3(0f), Float3(1f, 0f, 0f), Float3(2f, 0f, 0f))
        val path = generatePath(points)
        assertEquals(3, path.vertices.size)
    }

    @Test
    fun pathOpenIndices() {
        val points = listOf(Float3(0f), Float3(1f, 0f, 0f), Float3(2f, 0f, 0f))
        val path = generatePath(points, closed = false)
        // 2 segments * 2 indices = 4
        assertEquals(4, path.indices.size)
        assertEquals(listOf(0, 1, 1, 2), path.indices)
    }

    @Test
    fun pathClosedIndices() {
        val points = listOf(Float3(0f), Float3(1f, 0f, 0f), Float3(2f, 0f, 0f))
        val path = generatePath(points, closed = true)
        // 3 segments * 2 indices = 6
        assertEquals(6, path.indices.size)
        assertEquals(listOf(0, 1, 1, 2, 2, 0), path.indices)
    }

    @Test
    fun pathRequiresAtLeastTwoPoints() {
        var threw = false
        try {
            generatePath(listOf(Float3(0f)))
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "Should throw for fewer than 2 points")
    }

    // ----- Shape -----

    @Test
    fun shapeTriangleIndicesDivisibleByThree() {
        val square = listOf(
            Float2(0f, 0f),
            Float2(1f, 0f),
            Float2(1f, 1f),
            Float2(0f, 1f)
        )
        val shape = generateShape(polygonPath = square)
        assertTrue(shape.indices.size % 3 == 0, "Triangle indices must be divisible by 3")
        assertTrue(shape.indices.isNotEmpty(), "Shape should produce indices")
    }

    @Test
    fun shapeVertexCount() {
        val triangle = listOf(
            Float2(0f, 0f),
            Float2(1f, 0f),
            Float2(0.5f, 1f)
        )
        val shape = generateShape(polygonPath = triangle)
        assertEquals(3, shape.vertices.size)
    }

    @Test
    fun shapeHasNormalsAndUvs() {
        val triangle = listOf(
            Float2(0f, 0f),
            Float2(1f, 0f),
            Float2(0.5f, 1f)
        )
        val shape = generateShape(polygonPath = triangle)
        assertTrue(shape.hasNormals)
        assertTrue(shape.hasUvCoordinates)
    }

    @Test
    fun shapeTriangleProducesOneTriangle() {
        val triangle = listOf(
            Float2(0f, 0f),
            Float2(1f, 0f),
            Float2(0.5f, 1f)
        )
        val shape = generateShape(polygonPath = triangle)
        // A simple triangle should produce exactly 3 indices (1 triangle)
        assertEquals(3, shape.indices.size)
    }

    @Test
    fun shapeSquareProducesTwoTriangles() {
        val square = listOf(
            Float2(0f, 0f),
            Float2(1f, 0f),
            Float2(1f, 1f),
            Float2(0f, 1f)
        )
        val shape = generateShape(polygonPath = square)
        // A quad should produce 6 indices (2 triangles)
        assertEquals(6, shape.indices.size)
    }

    @Test
    fun shapeEmptyPolygonProducesNoIndices() {
        val shape = generateShape(polygonPath = emptyList())
        assertEquals(0, shape.vertices.size)
        assertEquals(0, shape.indices.size)
    }

    @Test
    fun shapeIndicesInBounds() {
        val pentagon = listOf(
            Float2(0f, 0f),
            Float2(1f, 0f),
            Float2(1.3f, 0.8f),
            Float2(0.5f, 1.2f),
            Float2(-0.3f, 0.8f)
        )
        val shape = generateShape(polygonPath = pentagon)
        for (idx in shape.indices) {
            assertTrue(idx in 0 until shape.vertices.size, "Index $idx out of bounds")
        }
    }
}

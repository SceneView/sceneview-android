package io.github.sceneview.triangulation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EarcutTest {

    @Test
    fun triangulateTriangle() {
        // Simple triangle: 3 vertices → 1 triangle → 3 indices
        val vertices = doubleArrayOf(
            0.0, 0.0,
            1.0, 0.0,
            0.0, 1.0
        )
        val indices = Earcut.triangulate(vertices)
        assertEquals(3, indices.size, "A triangle should produce exactly 3 indices")
        // All vertex indices should be in range [0, 2]
        assertTrue(indices.all { it in 0..2 }, "All indices should reference valid vertices")
        // All three vertices should be referenced
        assertEquals(3, indices.toSet().size, "All 3 vertices should appear in the triangle")
    }

    @Test
    fun triangulateSquare() {
        // Square: 4 vertices → 2 triangles → 6 indices
        val vertices = doubleArrayOf(
            0.0, 0.0,
            1.0, 0.0,
            1.0, 1.0,
            0.0, 1.0
        )
        val indices = Earcut.triangulate(vertices)
        assertEquals(6, indices.size, "A square should produce 6 indices (2 triangles)")
        assertTrue(indices.all { it in 0..3 }, "All indices should reference valid vertices")
    }

    @Test
    fun triangulatePolygonWithHole() {
        // Outer square: vertices 0-3
        // Inner (hole) square: vertices 4-7
        val vertices = doubleArrayOf(
            0.0, 0.0,   // 0
            10.0, 0.0,  // 1
            10.0, 10.0, // 2
            0.0, 10.0,  // 3
            2.0, 2.0,   // 4 (hole start)
            8.0, 2.0,   // 5
            8.0, 8.0,   // 6
            2.0, 8.0    // 7
        )
        val holeIndices = intArrayOf(4) // hole starts at vertex index 4

        val indices = Earcut.triangulate(vertices, holeIndices)
        // Must produce triangles (multiple of 3 indices)
        assertEquals(0, indices.size % 3, "Index count must be a multiple of 3")
        // With a hole, we need at least 8 triangles to fill the ring
        assertTrue(indices.size >= 24, "Polygon with hole should produce at least 8 triangles")
        // All indices should reference valid vertices
        assertTrue(indices.all { it in 0..7 }, "All indices should reference valid vertices")

        // Verify area coverage using the deviation function
        val deviation = Earcut.deviation(vertices, holeIndices, 2, indices)
        assertTrue(deviation < 1e-10, "Triangulation should cover the polygon area accurately, deviation=$deviation")
    }
}

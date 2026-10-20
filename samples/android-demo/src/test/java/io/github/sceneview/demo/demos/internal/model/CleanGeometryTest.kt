package io.github.sceneview.demo.demos.internal.model

import io.github.sceneview.demo.demos.internal.model.CleanScenes.flatnessRms
import io.github.sceneview.demo.demos.internal.model.CleanScenes.openEdges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.min

/**
 * The clean-up on synthetic scans fused by the real TSDF and meshed by the real marching cubes:
 * a noisy box, a room with a doubled wall and a table, a room with floating debris. Each test
 * measures what the brief asks for — pieces, flatness (RMS distance to the true plane) and
 * watertightness — before and after, and prints the figures.
 */
class CleanGeometryTest {
    @Test
    fun `a noisy box comes out as one closed piece with flat faces`() {
        val box = Box(0f, 0f, 0f, 1.2f, 0.8f, 1.0f)
        val tsdf = CleanScenes.fuse(Box(-0.2f, -0.2f, -0.2f, 1.4f, 1.0f, 1.2f), rippleM = 0.006f, noiseM = 0.012f) { x, y, z ->
            box.sdf(x, y, z)
        }
        val raw = CleanScenes.mesh(tsdf)
        val rmsBefore = flatnessRms(raw, box.planes())
        val openBefore = openEdges(raw)
        val (clean, stats) = CleanGeometry.clean(raw)
        val rmsAfter = flatnessRms(clean, box.planes())
        val flipped = CleanScenes.flippedShare(clean, box.planes())
        println(
            "noisy box: flipped area ${pct(flipped)}, pieces ${stats.piecesIn} -> ${stats.piecesOut}, flatness RMS " +
                "${mm(rmsBefore)} -> ${mm(rmsAfter)} mm, open edges $openBefore -> ${openEdges(clean)}, " +
                "planes ${stats.planes}, flat ${pct(stats.flatShare)}, doubled ${stats.doubledTriangles}, holes ${stats.holesFilled}"
        )
        assertEquals(1, stats.piecesOut)
        assertEquals(0, openEdges(clean))
        assertEquals(6, stats.planes)
        assertTrue("flipped $flipped", flipped < 0.001f)
        assertTrue("flatness $rmsAfter vs $rmsBefore", rmsAfter < rmsBefore / 4f)
        assertTrue("flatness $rmsAfter", rmsAfter < 0.001f)
    }

    @Test
    fun `a wall meshed twice keeps only its front sheet, a table top keeps both faces`() {
        val room = Box(0f, 0f, 0f, 3.0f, 2.4f, 2.6f)
        val pocket = Box(-0.09f, 0.6f, 0.6f, -0.04f, 1.8f, 2.0f) // air a drifted pass left behind the x = 0 wall
        val table = Box(1.0f, 0.71f, 0.8f, 2.0f, 0.75f, 1.6f)
        val tsdf = CleanScenes.fuse(Box(-0.3f, -0.2f, -0.2f, 3.2f, 2.6f, 2.8f), rippleM = 0.004f, noiseM = 0.006f) { x, y, z ->
            min(maxOf(-room.sdf(x, y, z), -pocket.sdf(x, y, z)), table.sdf(x, y, z))
        }
        val raw = CleanScenes.mesh(tsdf)
        val behindBefore = trianglesWhere(raw) { x, _, _ -> x < -0.02f }
        val tableBefore = trianglesWhere(raw) { x, y, z -> table.sdf(x, y, z) < 0.03f }
        val (clean, stats) = CleanGeometry.clean(raw)
        val behindAfter = trianglesWhere(clean) { x, _, _ -> x < -0.02f }
        val tableUp = facing(clean, table, 1f)
        val tableDown = facing(clean, table, -1f)
        println(
            "doubled wall: triangles behind the wall $behindBefore -> $behindAfter " +
                "(${stats.doubledTriangles} doubled removed), table triangles $tableBefore -> " +
                "${trianglesWhere(clean) { x, y, z -> table.sdf(x, y, z) < 0.03f }}, pieces ${stats.piecesIn} -> ${stats.piecesOut}"
        )
        assertTrue(behindBefore > 500)
        assertEquals(0, behindAfter)
        assertTrue("table top kept: $tableUp", tableUp > 200)
        assertTrue("table underside kept: $tableDown", tableDown > 200)
        assertEquals(2, stats.piecesOut) // the room and the floating table
    }

    @Test
    fun `floating debris goes, a hanging lamp and furniture stay, walls come out flat`() {
        val room = Box(0f, 0f, 0f, 3.2f, 2.5f, 2.8f)
        val cabinet = Box(-0.2f, -0.2f, -0.2f, 0.6f, 0.9f, 1.2f) // in a corner, on the floor (built into the walls: no zero-thickness seam)
        val lamp = Box(1.4f, 1.9f, 1.2f, 1.75f, 2.2f, 1.55f) // floating, but a real thing
        val debris = listOf(
            // Big enough to outlive marching cubes' own 500-triangle filter, small next to the room.
            Box(2.0f, 1.0f, 2.0f, 2.17f, 1.17f, 2.17f),
            Box(0.9f, 1.5f, 0.4f, 1.15f, 1.6f, 0.6f),
            Box(2.5f, 0.5f, 0.7f, 2.67f, 0.67f, 0.87f),
            Box(1.5f, 0.9f, 2.2f, 1.75f, 1.0f, 2.4f),
        )
        val tsdf = CleanScenes.fuse(Box(-0.2f, -0.2f, -0.2f, 3.4f, 2.7f, 3.0f), rippleM = 0.006f, noiseM = 0.008f) { x, y, z ->
            var d = min(-room.sdf(x, y, z), cabinet.sdf(x, y, z))
            d = min(d, lamp.sdf(x, y, z))
            for (bit in debris) d = min(d, bit.sdf(x, y, z))
            d
        }
        val raw = CleanScenes.mesh(tsdf)
        val walls = Box(0f, 0f, 0f, 3.2f, 2.5f, 2.8f).planes()
        val rmsBefore = flatnessRms(raw, walls)
        val (clean, stats) = CleanGeometry.clean(raw)
        val rmsAfter = flatnessRms(clean, walls)
        val inward = walls.map { Plane(-it.nx, -it.ny, -it.nz, -it.offset) }
        val flippedBefore = CleanScenes.flippedShare(raw, inward)
        val flipped = CleanScenes.flippedShare(clean, inward)
        val lampAfter = trianglesWhere(clean) { x, y, z -> lamp.sdf(x, y, z) < 0.03f }
        println(
            "floating debris: pieces ${stats.piecesIn} -> ${stats.piecesOut} (${stats.piecesDropped} dropped), " +
                "wall flatness RMS ${mm(rmsBefore)} -> ${mm(rmsAfter)} mm, planes ${stats.planes}, " +
                "flat ${pct(stats.flatShare)}, lamp triangles $lampAfter, flipped area ${pct(flippedBefore)} -> ${pct(flipped)}"
        )
        assertTrue(stats.piecesIn >= 6)
        assertEquals(2, stats.piecesOut) // the room with its cabinet, and the lamp
        assertTrue(lampAfter > 100)
        assertTrue("flipped $flipped", flipped < 0.001f)
        for (bit in debris) assertEquals(0, trianglesWhere(clean) { x, y, z -> bit.sdf(x, y, z) < 0.03f })
        assertTrue("flatness $rmsAfter vs $rmsBefore", rmsAfter < rmsBefore / 4f)
        assertTrue("flatness $rmsAfter", rmsAfter < 0.001f)
    }

    @Test
    fun `small holes close, a doorway stays open`() {
        // A floor patch, 2 m square, with a 6 cm pin-hole and a 60 cm opening.
        val step = 0.02f
        val cells = 100
        val positions = FloatArray((cells + 1) * (cells + 1) * 3)
        for (j in 0..cells) for (i in 0..cells) {
            val v = j * (cells + 1) + i
            positions[v * 3] = i * step
            positions[v * 3 + 2] = j * step
        }
        val indices = ArrayList<Int>()
        for (j in 0 until cells) for (i in 0 until cells) {
            val x = (i + 0.5f) * step
            val z = (j + 0.5f) * step
            if (abs(x - 0.5f) < 0.03f && abs(z - 0.5f) < 0.03f) continue
            if (abs(x - 1.4f) < 0.3f && abs(z - 1.4f) < 0.3f) continue
            val a = j * (cells + 1) + i
            val b = a + 1
            val c = a + cells + 1
            val d = c + 1
            indices += listOf(a, c, b, b, c, d) // facing +Y
        }
        val mesh = WorkMesh(positions, IntArray(positions.size / 3) { CleanScenes.GREY }, indices.toIntArray())
        val before = openEdges(mesh)
        val (filled, holes) = MeshCleanup.fillSmallHoles(mesh)
        println("holes: closed $holes, open edges $before -> ${openEdges(filled)}")
        assertEquals(1, holes)
        // Left open: the patch's outline (400 edges) and the opening's (120).
        assertEquals(400 + 120, openEdges(filled))
        val normals = vertexNormals(filled.positions, filled.indices)
        for (v in 0 until filled.vertexCount) assertEquals(1f, normals[v * 3 + 1], 1e-4f)
    }

    private fun trianglesWhere(mesh: WorkMesh, test: (Float, Float, Float) -> Boolean): Int {
        val faces = FaceGeometry(mesh.positions, mesh.indices)
        return (0 until mesh.triangleCount).count {
            test(faces.centroids[it * 3], faces.centroids[it * 3 + 1], faces.centroids[it * 3 + 2])
        }
    }

    private fun facing(mesh: WorkMesh, box: Box, up: Float): Int {
        val faces = FaceGeometry(mesh.positions, mesh.indices)
        return (0 until mesh.triangleCount).count {
            box.sdf(faces.centroids[it * 3], faces.centroids[it * 3 + 1], faces.centroids[it * 3 + 2]) < 0.03f &&
                faces.normals[it * 3 + 1] * up > 0.9f
        }
    }

    private fun mm(m: Float) = "%.2f".format(m * 1000f)

    private fun pct(share: Float) = "%.2f%%".format(share * 100f)
}

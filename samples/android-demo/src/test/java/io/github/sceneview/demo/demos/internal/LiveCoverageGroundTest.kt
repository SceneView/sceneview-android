package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The live 3D map's floor: what is covered, and the rest of the room's floor still to scan. */
class LiveCoverageGroundTest {
    private val cell = 0.25f

    /** A 2 × 2 m square floor from the origin, counter-clockwise. */
    private val square = listOf(0f to 0f, 2f to 0f, 2f to 2f, 0f to 2f)

    @Test
    fun nothingCoveredNorFoundIsNoGround() {
        assertNull(LiveCoverageGround.of(LongArray(0), cell, emptyList(), 0f))
    }

    @Test
    fun theRoomNotCoveredIsAllMissing() {
        val ground = requireNotNull(LiveCoverageGround.of(LongArray(0), cell, square, -1.2f))
        assertEquals(8, ground.columns)
        assertEquals(8, ground.rows)
        assertEquals(64, ground.missing)
        assertEquals(0, ground.seen)
        assertEquals(-1.2f, ground.floorY, 0f)
    }

    @Test
    fun coveredCellsAreSeenAndTheRestOfTheRoomMissing() {
        // The first row of the room covered, eight cells along X.
        val covered = LongArray(8) { CoverageGrid.key(it, 0) }
        val ground = requireNotNull(LiveCoverageGround.of(covered, cell, square, 0f))
        assertEquals(8, ground.seen)
        assertEquals(56, ground.missing)
        assertEquals(ScanGround.SEEN, ground.at(3, 0))
        assertEquals(ScanGround.MISSING, ground.at(3, 1))
        assertEquals(0f, ground.originX, 1e-6f)
        assertEquals(0f, ground.originZ, 1e-6f)
    }

    @Test
    fun aCellCoveredOutsideTheRoomIsSeenAndWidensTheGround() {
        val covered = longArrayOf(CoverageGrid.key(-2, 0))
        val ground = requireNotNull(LiveCoverageGround.of(covered, cell, square, 0f))
        assertEquals(10, ground.columns)
        assertEquals(ScanGround.SEEN, ground.at(0, 0))
        // The cell between it and the room is neither covered nor in the room.
        assertEquals(ScanGround.OUTSIDE, ground.at(1, 0))
    }

    @Test
    fun aRunawayExtentIsCut() {
        val huge = listOf(0f to 0f, 500f to 0f, 500f to 1f, 0f to 1f)
        val ground = requireNotNull(LiveCoverageGround.of(LongArray(0), cell, huge, 0f))
        assertEquals(LiveCoverageGround.MAX_CELLS_PER_SIDE, ground.columns)
    }

    @Test
    fun containsFollowsTheOutline() {
        val triangle = listOf(0f to 0f, 2f to 0f, 0f to 2f)
        assertTrue(LiveCoverageGround.contains(triangle, 0.5f, 0.5f))
        assertFalse(LiveCoverageGround.contains(triangle, 1.5f, 1.5f))
    }
}

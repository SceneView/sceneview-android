package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The nearest wall and the nearest opening to a pose, on synthetic rooms whose answers are known
 * in closed form. Everything is in the scan's frame: metres, +Y up, the floor at y = 0 unless a
 * test moves it.
 */
class RoomNearestTest {

    /** A phone held at chest height, metres above the floor. */
    private val eye = 1.4f

    /**
     * A [width] × [depth] room standing on the origin, its walls round it in order (south z = 0,
     * east x = width, north z = depth, west x = 0), each normal pointing in.
     */
    private fun room(
        width: Float,
        depth: Float,
        confidence: List<Float?> = List(4) { null },
    ): List<ShellWall> = listOf(
        ShellWall(0f, 0f, width, 0f, 0f, 1f, confidence[0]),
        ShellWall(width, 0f, width, depth, -1f, 0f, confidence[1]),
        ShellWall(width, depth, 0f, depth, 0f, -1f, confidence[2]),
        ShellWall(0f, depth, 0f, 0f, 1f, 0f, confidence[3]),
    )

    private fun <T : Any> need(value: T?, what: String): T =
        value ?: throw AssertionError("expected a nearest $what")

    private fun assertVec(expected: Vec3, actual: Vec3) {
        assertEquals("x", expected.x, actual.x, EPS)
        assertEquals("y", expected.y, actual.y, EPS)
        assertEquals("z", expected.z, actual.z, EPS)
    }

    @Test
    fun a93CentimetreDoorComesBackWithItsWidthHeightAndConfidence() {
        val walls = room(4f, 5f)
        // In the south wall, 1.00 m to 1.93 m from its start: centred on x = 1.465.
        val door = ShellOpening.onWall(walls[0], 1f, 0.93f, 0f, 2.04f, 0.8f)

        val found = need(RoomShell(walls, listOf(door)).nearest(DebugPose(1.465f, eye, 1.5f)).opening, "opening")

        assertSame(door, found.opening)
        assertEquals(0.93f, found.width, EPS)
        assertEquals(2.04f, found.height, EPS)
        assertEquals(0.8f, found.confidence)
        assertEquals(1.5f, found.distance, EPS)
        assertVec(Vec3(1.465f, eye, 0f), found.point)
    }

    @Test
    fun anObliqueWallIsMeasuredSquareToItsOwnPlane() {
        val yaw = (PI / 6).toFloat()
        val ux = cos(yaw)
        val uz = sin(yaw)
        val nx = -uz
        val nz = ux
        // 4 m long at 30° from +X; the room on its left, given only roughly.
        val wall = ShellWall(1f, 2f, 1f + 4f * ux, 2f + 4f * uz, -0.3f, 1f)
        val shell = RoomShell(listOf(wall))

        // 1.5 m along it and 0.8 m out into the room.
        val near = need(shell.nearest(1f + 1.5f * ux + 0.8f * nx, eye, 2f + 1.5f * uz + 0.8f * nz).wall, "wall")
        assertSame(wall, near.wall)
        assertEquals(0.8f, near.distance, EPS)
        assertVec(Vec3(nx, 0f, nz), near.normal)
        assertVec(Vec3(1f + 1.5f * ux, eye, 2f + 1.5f * uz), near.point)
        assertFalse(near.fromBehind)
        // The plane normal · p = offset holds at both ends of the wall.
        assertEquals(nx * wall.ax + nz * wall.az, near.offset, EPS)
        assertEquals(nx * wall.bx + nz * wall.bz, near.offset, EPS)

        // The same wall drawn the other way round faces the same room.
        val reversed = ShellWall(wall.bx, wall.bz, wall.ax, wall.az, -0.3f, 1f)
        val back = need(RoomShell(listOf(reversed)).nearest(1f + 1.5f * ux + 0.8f * nx, eye, 2f + 1.5f * uz + 0.8f * nz).wall, "wall")
        assertVec(Vec3(nx, 0f, nz), back.normal)
        assertEquals(0.8f, back.distance, EPS)

        // The wall stops at its end: 0.3 m past it and 0.4 m out is 0.5 m from its far corner.
        val beyond = need(shell.nearest(wall.bx + 0.3f * ux + 0.4f * nx, eye, wall.bz + 0.3f * uz + 0.4f * nz).wall, "wall")
        assertEquals(0.5f, beyond.distance, EPS)
        assertVec(Vec3(wall.bx, eye, wall.bz), beyond.point)
    }

    @Test
    fun anEmptySurveyHasNoNearestWallNorOpening() {
        val pose = DebugPose(1f, eye, 1f)
        assertEquals(RoomNearest(null, null), RoomShell(emptyList(), emptyList()).nearest(pose))

        // Walls without openings, or openings without walls: only the missing half is null.
        val walls = room(4f, 4f)
        val door = ShellOpening.onWall(walls[0], 1f, 0.9f, 0f, 2f)
        val wallsOnly = RoomShell(walls).nearest(pose)
        assertNotNull(wallsOnly.wall)
        assertNull(wallsOnly.opening)
        val openingsOnly = RoomShell(emptyList(), listOf(door)).nearest(pose)
        assertNull(openingsOnly.wall)
        assertNotNull(openingsOnly.opening)

        // Under a centimetre, a wall or an opening is noise.
        val speck = ShellWall(1f, 1f, 1.005f, 1f, 0f, 1f)
        val sliver = ShellOpening(1f, 1f, 1.004f, 1f, 0f, 2f)
        assertEquals(RoomNearest(null, null), RoomShell(listOf(speck), listOf(sliver)).nearest(pose))

        // A pose that is not a place finds nothing.
        assertEquals(RoomNearest(null, null), RoomShell(walls, listOf(door)).nearest(Float.NaN, eye, 1f))
    }

    @Test
    fun onAnOpeningsAxisItsDistanceIsTheStraightLineToIt() {
        val walls = room(4f, 5f)
        // In the south wall from x = 1.50 to 2.43.
        val door = ShellOpening.onWall(walls[0], 1.5f, 0.93f, 0f, 2.04f)
        val shell = RoomShell(walls, listOf(door))
        val axis = 1.965f

        val ahead = need(shell.nearest(axis, eye, 1.2f).opening, "opening")
        assertEquals(1.2f, ahead.distance, EPS)
        assertVec(Vec3(axis, eye, 0f), ahead.point)

        // In the doorway the opening is right here, and so is its wall: an opening does not cut it.
        val inside = shell.nearest(axis, eye, 0f)
        assertEquals(0f, need(inside.opening, "opening").distance, EPS)
        val wall = need(inside.wall, "wall")
        assertSame(walls[0], wall.wall)
        assertEquals(0f, wall.distance, EPS)
        assertFalse(wall.fromBehind)

        // Half a metre through it: the door is 0.5 m back, its wall seen from behind.
        val through = shell.nearest(axis, eye, -0.5f)
        assertEquals(0.5f, need(through.opening, "opening").distance, EPS)
        val behind = need(through.wall, "wall")
        assertSame(walls[0], behind.wall)
        assertTrue(behind.fromBehind)
    }

    @Test
    fun wallsAsNearAsEachOtherAreSettledByConfidenceThenByOrder() {
        val centre = DebugPose(2f, eye, 2f)
        fun winner(vararg confidence: Float?): Int {
            val walls = room(4f, 4f, confidence.toList())
            val found = need(RoomShell(walls).nearest(centre).wall, "wall")
            assertEquals(2f, found.distance, EPS)
            return walls.indexOfFirst { it === found.wall }
        }

        assertEquals(1, winner(0.5f, 0.9f, 0.7f, null))
        assertEquals(1, winner(null, 0.1f, null, null))
        assertEquals(0, winner(null, null, null, null))
        assertEquals(0, winner(0.6f, 0.6f, 0.6f, 0.6f))

        // 5 mm nearer is nearer, whatever the confidence.
        val walls = room(4f, 4f, listOf(0.9f, 0.9f, 0.1f, 0.9f))
        assertSame(walls[2], need(RoomShell(walls).nearest(2f, eye, 2.005f).wall, "wall").wall)
        // Within a millimetre it is a tie again: north at 1.9992 m, east and west at 2 m.
        assertSame(walls[1], need(RoomShell(walls).nearest(2f, eye, 2.0008f).wall, "wall").wall)
    }

    @Test
    fun openingsAsNearAsEachOtherAreSettledTheSameWay() {
        val walls = room(4f, 4f)
        val south = ShellOpening.onWall(walls[0], 1.5f, 1f, 0f, 2f, 0.4f)
        val north = ShellOpening.onWall(walls[2], 1.5f, 1f, 0f, 2f, 0.7f)
        val centre = DebugPose(2f, eye, 2f)
        fun winner(vararg openings: ShellOpening): ShellOpening {
            val found = need(RoomShell(walls, openings.toList()).nearest(centre).opening, "opening")
            assertEquals(2f, found.distance, EPS)
            return found.opening
        }

        assertSame(north, winner(south, north))
        val unknown = south.copy(confidence = null)
        val known = north.copy(confidence = 0.05f)
        assertSame(known, winner(unknown, known))
        val first = south.copy(confidence = null)
        val second = north.copy(confidence = null)
        assertSame(first, winner(first, second))
    }

    @Test
    fun aWallSeenFromBehindStillCountsAndSaysSo() {
        val walls = room(4f, 4f)

        // Outside the room, 0.4 m beyond its west wall.
        val outside = need(RoomShell(walls).nearest(-0.4f, eye, 1f).wall, "wall")
        assertSame(walls[3], outside.wall)
        assertEquals(0.4f, outside.distance, EPS)
        assertTrue(outside.fromBehind)
        // The normal still points into the room, away from the pose.
        assertVec(Vec3(1f, 0f, 0f), outside.normal)

        // As near a wall faced as one seen from behind, the faced one wins over a higher confidence.
        val faced = ShellWall(0f, 0f, 4f, 0f, 0f, 1f, 0.2f)
        val backOn = ShellWall(0f, 1f, 4f, 1f, 0f, 1f, 0.9f)
        val near = need(RoomShell(listOf(backOn, faced)).nearest(2f, eye, 0.5f).wall, "wall")
        assertSame(faced, near.wall)
        assertEquals(0.5f, near.distance, EPS)
        assertFalse(near.fromBehind)
    }

    @Test
    fun theNearestOpeningNeedNotBeInTheNearestWall() {
        val walls = room(4f, 5f)
        // North wall, which runs from x = 4 back to 0: from x = 1.0 to 0.1.
        val door = ShellOpening.onWall(walls[2], 3f, 0.9f, 0f, 2.04f, 0.6f)
        // East wall, from z = 2.0 to 3.2.
        val window = ShellOpening.onWall(walls[1], 2f, 1.2f, 0.9f, 2.1f, 0.9f)

        val found = RoomShell(walls, listOf(window, door)).nearest(0.5f, eye, 4f)

        val wall = need(found.wall, "wall")
        assertSame(walls[3], wall.wall)
        assertEquals(0.5f, wall.distance, EPS)
        val opening = need(found.opening, "opening")
        assertSame(door, opening.opening)
        assertEquals(1f, opening.distance, EPS)
        assertEquals(0.6f, opening.confidence)
    }

    @Test
    fun aHighWindowCountsTheClimbToIt() {
        val walls = room(4f, 4f)
        // South wall, x = 2.6 to 3.6, its sill 1.8 m up.
        val window = ShellOpening.onWall(walls[0], 2.6f, 1f, 1.8f, 2.4f)
        // East wall, z = 0.1 to 1.0, down to the floor.
        val door = ShellOpening.onWall(walls[1], 0.1f, 0.9f, 0f, 2.04f)
        val pose = DebugPose(3.1f, 1f, 0.6f)

        // 0.6 m away on the plan, 0.8 m below its sill: 1.0 m away.
        val alone = need(RoomShell(walls, listOf(window)).nearest(pose).opening, "opening")
        assertEquals(1f, alone.distance, EPS)
        assertVec(Vec3(3.1f, 1.8f, 0f), alone.point)

        // So the door 0.9 m away is the nearer opening, though it is further on the plan.
        val found = need(RoomShell(walls, listOf(window, door)).nearest(pose).opening, "opening")
        assertSame(door, found.opening)
        assertEquals(0.9f, found.distance, EPS)
    }

    @Test
    fun aTurnedRoomAwayFromTheOriginGivesTheSameAnswers() {
        val turn = 0.65f
        val c = cos(turn)
        val s = sin(turn)
        val floor = -1.3f
        fun x(px: Float, pz: Float) = 12f + px * c - pz * s
        fun z(px: Float, pz: Float) = -7f + px * s + pz * c
        fun place(w: ShellWall) = ShellWall(
            x(w.ax, w.az), z(w.ax, w.az), x(w.bx, w.bz), z(w.bx, w.bz),
            w.nx * c - w.nz * s, w.nx * s + w.nz * c, w.confidence,
        )

        val walls = room(4f, 5f)
        val door = ShellOpening.onWall(walls[0], 1f, 0.93f, 0f, 2.04f, 0.8f)
        val here = RoomShell(walls, listOf(door)).nearest(DebugPose(0.7f, eye, 1.6f))

        val turned = walls.map(::place)
        val turnedDoor = ShellOpening.onWall(turned[0], 1f, 0.93f, floor, floor + 2.04f, 0.8f)
        // Where the phone looks does not matter, only where it is.
        val pose = DebugPose(x(0.7f, 1.6f), floor + eye, z(0.7f, 1.6f), qy = sin(turn / 2), qw = cos(turn / 2))
        val there = RoomShell(turned, listOf(turnedDoor)).nearest(pose)

        val wallHere = need(here.wall, "wall")
        val wallThere = need(there.wall, "wall")
        assertSame(walls[3], wallHere.wall)
        assertSame(turned[3], wallThere.wall)
        assertEquals(0.7f, wallThere.distance, EPS)
        assertVec(Vec3(c, 0f, s), wallThere.normal)

        val doorHere = need(here.opening, "opening")
        val doorThere = need(there.opening, "opening")
        assertEquals(doorHere.distance, doorThere.distance, EPS)
        assertEquals(0.93f, doorThere.width, EPS)
        assertEquals(2.04f, doorThere.height, EPS)
        assertVec(Vec3(x(1f, 0f), floor + eye, z(1f, 0f)), doorThere.point)
    }

    @Test
    fun openingsSitWhereTheirWallOrTheirBoxSays() {
        val yaw = 0.4f
        val c = cos(yaw)
        val s = sin(yaw)

        val wall = ShellWall(1f, -2f, 1f + 3f * c, -2f + 3f * s, -s, c)
        val inWall = ShellOpening.onWall(wall, 1f, 0.93f, 0f, 2.04f, 0.5f)
        assertEquals(1f + c, inWall.ax, EPS)
        assertEquals(-2f + s, inWall.az, EPS)
        assertEquals(1f + 1.93f * c, inWall.bx, EPS)
        assertEquals(-2f + 1.93f * s, inWall.bz, EPS)
        assertEquals(0.93f, inWall.width, EPS)
        assertEquals(2.04f, inWall.height, EPS)
        assertEquals(0.5f, inWall.confidence)

        // A box standing in a wall's plane: as wide as it is long, from its base up its height.
        val box = ScanBox(centerX = 2f, centerZ = 3f, baseY = 0.9f, yaw = yaw, length = 1.2f, depth = 0.12f, height = 1.1f)
        val window = ShellOpening.of(box, 0.7f)
        val corners = box.corners()
        assertEquals((corners[0].x + corners[3].x) / 2, window.ax, EPS)
        assertEquals((corners[0].z + corners[3].z) / 2, window.az, EPS)
        assertEquals((corners[1].x + corners[2].x) / 2, window.bx, EPS)
        assertEquals((corners[1].z + corners[2].z) / 2, window.bz, EPS)
        assertEquals(0.9f, window.bottom, EPS)
        assertEquals(2f, window.top, EPS)
        assertEquals(1.2f, window.width, EPS)
        assertEquals(1.1f, window.height, EPS)
        assertEquals(0.7f, window.confidence)

        // 0.5 m in front of its middle, at its sill's height.
        val found = need(RoomShell(listOf(wall), listOf(window)).nearest(2f - 0.5f * s, 0.9f, 3f + 0.5f * c).opening, "opening")
        assertEquals(0.5f, found.distance, EPS)
    }

    private companion object {
        const val EPS = 1e-4f
    }
}

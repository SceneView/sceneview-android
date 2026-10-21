package io.github.sceneview.demo.demos.internal

import io.github.sceneview.math.Position
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * The square corner of Room mode, through the reticle's snapper: a corner is squared up to 6°
 * off when a tracked wall confirms the right angle, and up to 3° off otherwise — a room that is
 * not square stays as it is traced.
 */
class MeasureSquareCornerTest {
    private fun p(x: Float, z: Float) = Position(x, 0f, z)

    // The last wall runs along x from the origin to the corner (4, 0); the next one is aimed at
    // [reach] metres from the corner, [degrees] off square.
    private val from = p(0f, 0f)
    private val corner = p(4f, 0f)

    private fun aim(degrees: Float, reach: Float = 1.5f): Position {
        val r = Math.toRadians(degrees.toDouble())
        return p(corner.x + reach * sin(r).toFloat(), corner.z + reach * cos(r).toFloat())
    }

    /** A tracked wall whose foot runs [start]..[end] metres from the corner, [degrees] off square. */
    private fun wallFromCorner(degrees: Float, start: Float = 2.5f, end: Float = 3.5f): MeasurePlane =
        wall(aim(degrees, start), aim(degrees, end))

    /** A tracked wall standing on the floor line [a]–[b], 0.1 to 2 m up. */
    private fun wall(a: Position, b: Position) = MeasurePlane(
        MeasurePlaneKind.Vertical,
        listOf(
            Position(a.x, 0.1f, a.z),
            Position(b.x, 0.1f, b.z),
            Position(b.x, 2f, b.z),
            Position(a.x, 2f, a.z),
        ),
    )

    private fun snap(raw: Position, planes: List<MeasurePlane> = emptyList()): MeasureSnap =
        measureSnapAim(MeasureMode.Room, planes, 0f, listOf(from, corner), listOf(from, corner), raw, TOLERANCE)

    private fun assertSquared(raw: Position, planes: List<MeasurePlane> = emptyList()) {
        val snap = snap(raw, planes)
        assertEquals(MeasureSnapKind.Square, snap.kind)
        assertEquals(corner.x, snap.position.x, 1e-4f)
        // The reach along the squared wall is kept.
        assertEquals(raw.z, snap.position.z, 1e-4f)
    }

    private fun assertAsAimed(raw: Position, planes: List<MeasurePlane> = emptyList()) {
        val snap = snap(raw, planes)
        assertEquals(MeasureSnapKind.None, snap.kind)
        assertEquals(raw, snap.position)
    }

    @Test
    fun `without a tracked wall a corner is squared within three degrees only`() {
        assertSquared(aim(2.5f))
        assertSquared(aim(-2.5f))
        assertAsAimed(aim(4.5f))
        assertAsAimed(aim(-5.5f))
    }

    @Test
    fun `a tracked wall square to the last one squares a corner within six degrees`() {
        val square = listOf(wallFromCorner(0f))
        assertSquared(aim(5.5f), square)
        assertSquared(aim(-4.5f), square)
        // Six degrees stays the limit.
        assertAsAimed(aim(6.5f), square)
        // A tracked wall a little off square, as ARCore sees one, still confirms it.
        assertSquared(aim(5.5f), listOf(wallFromCorner(1.5f)))
    }

    @Test
    fun `a tracked wall that is not square to the last one keeps three degrees`() {
        val skewed = listOf(wallFromCorner(5f))
        assertAsAimed(aim(4.5f), skewed)
        assertSquared(aim(2.5f), skewed)
    }

    @Test
    fun `a tracked wall away from the corner or too short confirms nothing`() {
        // Square to the last wall, but across the room.
        assertAsAimed(aim(4.5f), listOf(wall(p(0f, 0.5f), p(0f, 2.5f))))
        // Square and on the corner's line, but a 10 cm patch says nothing of its direction.
        assertAsAimed(aim(4.5f), listOf(wallFromCorner(0f, start = 2.5f, end = 2.6f)))
    }

    private companion object {
        const val TOLERANCE = 0.05f
    }
}

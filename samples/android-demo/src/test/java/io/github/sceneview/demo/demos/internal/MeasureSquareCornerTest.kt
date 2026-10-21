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
 *
 * A tracked wall is a vertical plane standing on the floor (its bottom within 30 cm of it), at
 * least 1.5 m tall, whose foot runs at least 1 m within 3° of square to the last wall and comes
 * within 20 cm of the corner. An open door, a cupboard's side, a wall cupboard or a wall further
 * along the same line stand square by a corner too, and confirm nothing.
 */
class MeasureSquareCornerTest {
    private fun p(x: Float, z: Float) = Position(x, 0f, z)

    // The last wall runs along x from the origin to the corner (4, 0); the next one is aimed at
    // [reach] metres from the corner, [degrees] off square — past the end of every tracked foot
    // below, so the aim never locks onto one.
    private val from = p(0f, 0f)
    private val corner = p(4f, 0f)

    private fun aim(degrees: Float, reach: Float = 2f): Position {
        val r = Math.toRadians(degrees.toDouble())
        return p(corner.x + reach * sin(r).toFloat(), corner.z + reach * cos(r).toFloat())
    }

    /** A tracked wall whose foot runs [start]..[end] metres from the corner, [degrees] off square. */
    private fun wallFromCorner(degrees: Float, start: Float = 0.1f, end: Float = 1.3f): MeasurePlane =
        wall(aim(degrees, start), aim(degrees, end))

    /** A vertical plane on the floor line [a]–[b], [bottom] to [top] metres up: a wall by default. */
    private fun wall(a: Position, b: Position, bottom: Float = 0.1f, top: Float = 2f) = MeasurePlane(
        MeasurePlaneKind.Vertical,
        listOf(
            Position(a.x, bottom, a.z),
            Position(b.x, bottom, b.z),
            Position(b.x, top, b.z),
            Position(a.x, top, a.z),
        ),
    )

    /** The foot [start]..[end] metres along the square from the corner, [across] towards the room's inside. */
    private fun square(start: Float, end: Float, across: Float = 0f, bottom: Float = 0.1f, top: Float = 2f) =
        wall(p(corner.x - across, start), p(corner.x - across, end), bottom, top)

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
        // Square and from the corner, but a 10 cm patch says nothing of its direction.
        assertAsAimed(aim(4.5f), listOf(wallFromCorner(0f, start = 0.1f, end = 0.2f)))
    }

    // ── What stands square by a corner and is no wall ──────────────────────────────────────

    @Test
    fun `an open door by the corner confirms nothing`() {
        // Hinged 15 cm from the corner and opened square: 0.8 m along the floor, 2 m high.
        val door = square(start = 0.1f, end = 0.9f, bottom = 0f, top = 2f)
        assertAsAimed(aim(5f), listOf(door))
        assertAsAimed(aim(-5f), listOf(door))
    }

    @Test
    fun `a wall cupboard by the corner confirms nothing`() {
        // 0.6 m high, hung 1.4 m up, square to the last wall from the corner.
        assertAsAimed(aim(5f), listOf(square(start = 0.1f, end = 1.3f, bottom = 1.4f, top = 2f)))
    }

    @Test
    fun `a wall further along the corner's line confirms nothing`() {
        // Square, on the line through the corner, but its foot starts 2.5 m away.
        assertAsAimed(aim(5f), listOf(square(start = 2.5f, end = 3.7f)))
    }

    @Test
    fun `a horizontal plane confirms nothing`() {
        // A strip of floor along the square from the corner, as long as a wall's foot.
        val floor = MeasurePlane(
            MeasurePlaneKind.Upward,
            listOf(p(3.98f, 0.1f), p(4.02f, 0.1f), p(4.02f, 1.3f), p(3.98f, 1.3f)),
        )
        assertAsAimed(aim(5f), listOf(floor))
    }

    // ── Each threshold, just inside and just outside ────────────────────────────────────────

    @Test
    fun `the wall's foot comes within 20 cm of the corner`() {
        // A gap at the corner, along the square.
        assertSquared(aim(5.5f), listOf(square(start = 0.15f, end = 1.35f)))
        assertAsAimed(aim(5.5f), listOf(square(start = 0.25f, end = 1.45f)))
        // Beside the corner, towards the room's inside.
        assertSquared(aim(5.5f), listOf(square(start = 0f, end = 1.2f, across = 0.15f)))
        assertAsAimed(aim(5.5f), listOf(square(start = 0f, end = 1.2f, across = 0.25f)))
    }

    @Test
    fun `the wall's foot runs at least 1 m`() {
        assertSquared(aim(5.5f), listOf(square(start = 0.05f, end = 1.1f)))
        assertAsAimed(aim(5.5f), listOf(square(start = 0.05f, end = 1f)))
    }

    @Test
    fun `the wall runs within 3 degrees of square`() {
        assertSquared(aim(5.5f), listOf(wallFromCorner(2.5f)))
        assertAsAimed(aim(5.5f), listOf(wallFromCorner(3.5f)))
    }

    @Test
    fun `the wall stands on the floor`() {
        assertSquared(aim(5.5f), listOf(square(start = 0.1f, end = 1.3f, bottom = 0.25f, top = 2f)))
        assertAsAimed(aim(5.5f), listOf(square(start = 0.1f, end = 1.3f, bottom = 0.35f, top = 2f)))
    }

    @Test
    fun `the wall rises at least a metre and a half`() {
        assertSquared(aim(5.5f), listOf(square(start = 0.1f, end = 1.3f, bottom = 0.05f, top = 1.6f)))
        assertAsAimed(aim(5.5f), listOf(square(start = 0.1f, end = 1.3f, bottom = 0.05f, top = 1.5f)))
    }

    private companion object {
        const val TOLERANCE = 0.05f
    }
}

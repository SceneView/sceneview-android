package io.github.sceneview.demo.demos.internal

import io.github.sceneview.math.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a tap on Plan does: it opens the plan of the last room traced and raised, is greyed out
 * with no such room, and says why when that room has no plan to draw — never a silent tap.
 */
class MeasurePlanTapTest {

    /** A room traced along [corners] (x, z on the floor), closed, its walls [height] metres high. */
    private fun room(corners: List<Pair<Float, Float>>, height: Float? = 2.5f) = MeasureFigure(
        mode = MeasureMode.Room,
        points = corners.map { (x, z) -> Position(x, -1.2f, z) },
        closed = true,
        height = height,
    )

    private fun tap(vararg figures: MeasureFigure<Position>) =
        measurePlanTap(figures.toList(), figures.map { measureFigureOutline(it.mode, it.points) })

    private val rectangle = listOf(0f to 0f, 4f to 0f, 4f to 3f, 0f to 3f)

    /** Three corners in a line: the room closed and raised, but with no floor between its walls. */
    private val line = listOf(0f to 0f, 2f to 0f, 4f to 0f)

    @Test
    fun `with no room traced and raised, Plan has nothing to open`() {
        assertNull(tap())
        assertNull(tap(room(rectangle, height = null)))
        assertNull(tap(MeasureFigure(mode = MeasureMode.Wall, points = room(rectangle).points.take(2), height = 2.5f)))
        assertEquals(-1, measurePlanRoom(listOf(room(rectangle, height = null))))
    }

    @Test
    fun `a raised room opens its plan`() {
        val opened = tap(room(rectangle))
        assertTrue(opened.toString(), opened is MeasurePlanTap.Open)
        opened as MeasurePlanTap.Open
        assertEquals(0, opened.room)
        assertEquals(12f, opened.survey.area!!.value, 1e-3f)
    }

    @Test
    fun `a raised room with no floor says it has no plan`() {
        assertEquals(MeasurePlanTap.Refused(MeasureRefusal.NoPlan), tap(room(line)))
    }

    @Test
    fun `Plan draws the last room raised`() {
        val opened = tap(room(line), room(rectangle))
        assertTrue(opened.toString(), opened is MeasurePlanTap.Open && opened.room == 1)
        assertEquals(1, measurePlanRoom(listOf(room(line), room(rectangle), room(rectangle, height = null))))
        // The last one has no floor: Plan says so rather than draw an older room.
        assertEquals(MeasurePlanTap.Refused(MeasureRefusal.NoPlan), tap(room(rectangle), room(line)))
    }
}

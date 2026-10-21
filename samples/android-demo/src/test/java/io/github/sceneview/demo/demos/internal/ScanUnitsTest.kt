package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * 3D Scan names a room in the floor plan's units (#4599): the replay's floor labels and room line
 * follow [SurveyFormat.units] like the plan they open, feet and inches in US English, metres in
 * every other language, other English ones included.
 */
class ScanUnitsTest {

    private val room = RoomMeasure(floatArrayOf(0f, 0f, 3.73f, 0f, 3.73f, 3.76f, 0f, 3.76f), 3.73f, 3.76f, 0f)

    @Test
    fun `replay labels read feet and inches in US English`() {
        val (width, depth) = room.scanLabels(Locale.US)
        assertEquals(formatMeasureLength(3.73f, MeasureUnitSystem.Imperial, Locale.US), width)
        assertEquals(formatMeasureLength(3.76f, MeasureUnitSystem.Imperial, Locale.US), depth)
        assertEquals("12 ft 3 in" to "12 ft 4 in", width to depth)
    }

    @Test
    fun `replay summary reads feet in US English`() {
        val summary = room.scanSummary(Locale.US)
        assertEquals(room.summary(MeasureUnitSystem.Imperial, Locale.US), summary)
        assertEquals(false, summary.contains(" m"))
    }

    @Test
    fun `replay labels read metres in British English`() {
        assertEquals("3.73 m" to "3.76 m", room.scanLabels(Locale.UK))
    }

    @Test
    fun `replay summary reads metres in British English`() {
        assertEquals("3.73 m × 3.76 m · 14.0 m²", room.scanSummary(Locale.UK))
    }

    @Test
    fun `replay labels read metres in French with the locale's decimal separator`() {
        assertEquals("3,73 m" to "3,76 m", room.scanLabels(Locale.FRANCE))
    }

    @Test
    fun `replay summary reads metres in French`() {
        assertEquals(room.summary(MeasureUnitSystem.Metric, Locale.FRANCE), room.scanSummary(Locale.FRANCE))
        assertEquals(true, room.scanSummary(Locale.FRANCE).startsWith("3,73 m"))
    }
}

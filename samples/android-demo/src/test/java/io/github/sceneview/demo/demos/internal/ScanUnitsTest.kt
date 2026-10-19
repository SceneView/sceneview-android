package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * 3D Scan names a room in the floor plan's units (#4599): the replay's dimensions and the plan it
 * opens never read one in feet and the other in metres, even where the locale measures in feet.
 */
class ScanUnitsTest {

    private val room = RoomMeasure(floatArrayOf(0f, 0f, 3.73f, 0f, 3.73f, 3.76f, 0f, 3.76f), 3.73f, 3.76f, 0f)

    @Test
    fun `the plan's units are metric`() {
        assertEquals(MeasureUnitSystem.Metric, SurveyFormat.UNITS)
    }

    @Test
    fun `replay labels read metres under a US locale`() {
        assertEquals("3.73 m" to "3.76 m", room.scanLabels(Locale.US))
    }

    @Test
    fun `replay summary reads metres under a US locale`() {
        assertEquals("3.73 m × 3.76 m · 14.0 m²", room.scanSummary(Locale.US))
    }

    @Test
    fun `labels keep the locale's decimal separator`() {
        assertEquals("3,73 m" to "3,76 m", room.scanLabels(Locale.FRANCE))
    }
}

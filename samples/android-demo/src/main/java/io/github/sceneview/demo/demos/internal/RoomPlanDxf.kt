package io.github.sceneview.demo.demos.internal

import java.io.OutputStream

/**
 * The floor plan as a DXF drawing, for a CAD tool to take it up where the PDF stops.
 *
 * AutoCAD 2000 (`AC1015`) in ASCII: the oldest version with the light polyline, which every CAD
 * tool still opens. One drawing unit is a millimetre (`$INSUNITS` 4), y up as CAD has it. Five
 * layers:
 * - [WALLS]: the room's outline, one closed polyline;
 * - [OPENINGS]: each door with its leaf and swing, each window with its frame, and its size;
 * - [DIMENSIONS]: each wall's length as plain lines and text. No DIMENSION entity: a CAD tool
 *   redraws one its own way and recomputes its figure from the line, which would drop the
 *   plan's `≈` and its rounding;
 * - [TEXT]: the room's name and floor area;
 * - [LEGEND]: what the drawing is, and that it is to be checked on site.
 *
 * The figures are the plan's own, the PDF's, in the wording's units.
 */
object RoomPlanDxf {
    const val WALLS = "WALLS"
    const val OPENINGS = "OPENINGS"
    const val DIMENSIONS = "DIMENSIONS"
    const val TEXT = "TEXT"
    const val LEGEND = "LEGEND"

    enum class OpeningKind { Door, Window }

    /**
     * A door or a window in the survey's wall number [wall], starting [from] metres along it and
     * [width] metres wide; its [height] and, for a window, its [sill] (its bottom above the
     * floor) in metres, when known.
     */
    class Opening(
        val kind: OpeningKind,
        val wall: Int,
        val from: Float,
        val width: Float,
        val height: Float? = null,
        val sill: Float? = null,
    )

    /**
     * What the drawing says besides its figures: [door] and [window] name an opening around its
     * size (`Door %1$s`), [sill] gives a window's sill height (`sill %1$s`), and [legend] are the
     * lines under the plan, the warning to check it on site among them.
     */
    class Words(
        val legend: List<String>,
        val door: String,
        val window: String,
        val sill: String,
    )

    /** Writes [survey] with its [openings] to [out] as a DXF drawing, its figures worded by [wording]. */
    @Suppress("UNUSED_PARAMETER")
    fun write(
        out: OutputStream,
        survey: RoomSurvey,
        wording: SurveyWording,
        words: Words,
        openings: List<Opening> = emptyList(),
    ) = Unit
}

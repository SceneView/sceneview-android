package io.github.sceneview.demo.demos.internal

import kotlin.math.max

/*
 * A room survey in words (#4540): the rows of its table, on the screen and on the PDF, and the
 * few plain lines that are shared as text. What was not measured is said, never filled in.
 */

/** The survey's words, read from the app's resources: this file holds none of its own. */
class SurveyWording(
    /** `%1$s` the scan's title, `%2$s` its date. */
    val heading: String,
    val area: String,
    val dimensions: String,
    val perimeter: String,
    val height: String,
    /** The one dimension a scan with a single pair of facing walls gives. */
    val span: String,
    val walls: String,
    /** `%1$s` the wall's letter. */
    val wall: String,
    val notSeen: String,
    val notMeasured: String,
    val estimated: String,
    val estimateNote: String,
    val notice: String,
    val madeWith: String,
    /** `%1$s` the bar's length in metres. */
    val scaleBar: String,
    /** What to scan for each thing a survey can lack. */
    val gaps: Map<SurveyGap, String>,
)

/** One line of the survey's table. [muted] ones say a figure is missing. */
class SurveyRow(val label: String, val value: String, val muted: Boolean = false)

object SurveyReport {
    private const val SEPARATOR = " · "
    private const val METRES = " m"
    private const val SQUARE_METRES = " m²"
    private const val PLUS_MINUS = " ± "

    /** `≈ 20.0 m² ± 0.5`. */
    fun area(figure: SurveyFigure): String =
        (if (figure.estimate) SurveyFormat.ESTIMATE else "") + SurveyFormat.area(figure.value) + SQUARE_METRES +
            PLUS_MINUS + SurveyFormat.areaSpread(figure.plusMinus)

    /** `≈ 5.00 m ± 0.05`. */
    fun length(figure: SurveyFigure): String =
        (if (figure.estimate) SurveyFormat.ESTIMATE else "") + SurveyFormat.length(figure.value) + METRES +
            PLUS_MINUS + SurveyFormat.lengthSpread(figure.plusMinus)

    /** What the scan lacks for an area, as the thing to go and scan; `null` when it lacks nothing. */
    fun areaHint(survey: RoomSurvey, wording: SurveyWording): String? =
        survey.gaps.firstOrNull { it != SurveyGap.Ceiling }?.let { wording.gaps[it] }

    /** The survey's figures: area, length × width, perimeter and ceiling height — or what is missing. */
    fun rows(survey: RoomSurvey, wording: SurveyWording): List<SurveyRow> {
        val rows = ArrayList<SurveyRow>()
        val area = survey.area
        val length = survey.length
        val width = survey.width
        rows += when (area) {
            null -> SurveyRow(wording.area, wording.notMeasured, muted = true)
            else -> SurveyRow(wording.area, area(area))
        }
        if (length != null && width != null) {
            val mark = if (length.estimate || width.estimate) SurveyFormat.ESTIMATE else ""
            val spread = SurveyFormat.lengthSpread(max(length.plusMinus, width.plusMinus))
            rows += SurveyRow(
                wording.dimensions,
                mark + SurveyFormat.length(length.value) + " × " + SurveyFormat.length(width.value) + METRES +
                    PLUS_MINUS + spread,
            )
        } else {
            (length ?: width)?.let { rows += SurveyRow(wording.span, length(it)) }
        }
        survey.perimeter?.let { rows += SurveyRow(wording.perimeter, length(it)) }
        val height = survey.ceilingHeight
        rows += when (height) {
            null -> SurveyRow(wording.height, wording.notSeen, muted = true)
            else -> SurveyRow(wording.height, length(height))
        }
        return rows
    }

    /** One row per wall: its letter, its length, and whether it was seen. */
    fun wallRows(survey: RoomSurvey, wording: SurveyWording): List<SurveyRow> = survey.walls.map { wall ->
        val label = wording.wall.format(wall.tag)
        when {
            !wall.seen -> SurveyRow(label, SurveyFormat.wall(wall) + METRES + SEPARATOR + wording.notSeen, muted = true)
            wall.length.estimate ->
                SurveyRow(label, SurveyFormat.wall(wall) + METRES + SEPARATOR + wording.estimated, muted = true)
            else -> SurveyRow(label, length(wall.length))
        }
    }

    /** The plan's own words: a label per wall, the area in the middle, the scale bar's length. */
    fun planTexts(survey: RoomSurvey, wording: SurveyWording): PlanTexts = PlanTexts(
        walls = survey.walls.map { SurveyFormat.wall(it) },
        area = survey.area?.let {
            (if (it.estimate) SurveyFormat.ESTIMATE else "") + SurveyFormat.area(it.value) + SQUARE_METRES
        },
        areaNote = survey.area?.let { PLUS_MINUS.trim() + " " + SurveyFormat.areaSpread(it.plusMinus) + SQUARE_METRES },
        scale = { metres -> wording.scaleBar.format(if (metres < 1f) "0.5" else metres.toInt().toString()) },
    )

    /**
     * The survey as a few plain lines, ready to paste: the figures, the walls, what is an
     * estimate, and the notice that this is no certified measurement.
     */
    fun text(survey: RoomSurvey, wording: SurveyWording, title: String, date: String): String {
        val lines = ArrayList<String>()
        lines += wording.heading.format(title, date)
        val rows = rows(survey, wording)
        val height = rows.last()
        lines += rows.dropLast(1).joinToString(SEPARATOR) { row ->
            val hint = areaHint(survey, wording).takeIf { row.label == wording.area && survey.area == null }
            row.label + ": " + row.value + (hint?.let { " ($it)" } ?: "")
        }
        lines += height.label + ": " + height.value
        if (survey.walls.isNotEmpty()) {
            lines += wording.walls + ": " + survey.walls.joinToString(", ") { wall ->
                wall.tag + " " + SurveyFormat.wall(wall) + METRES + if (wall.seen) "" else " (" + wording.notSeen + ")"
            }
        }
        if (survey.hasEstimates) lines += wording.estimateNote
        lines += wording.notice + " " + wording.madeWith
        return lines.joinToString("\n")
    }
}

package io.github.sceneview.demo.demos.internal

import kotlin.math.max

/*
 * A room survey in words (#4540): the rows of its table, on the screen and on the PDF, and the
 * few plain lines that are shared as text. What was not measured is said, never filled in.
 *
 * The three outputs read the same rows and the same notes from here, so a figure carries the
 * same marks wherever it is read: "≈" in front of an estimate, which has no "±"; "±" behind a
 * measured figure, with the note that says it is the scan's scatter and not an accuracy.
 */

/**
 * What the export sheet offers for a room's floor plan (#4714): the plan once the survey has one,
 * and otherwise why not — never a PDF that says nothing was measured.
 */
enum class PlanOffer {
    /** The survey is still being read: the row waits, disabled. */
    Measuring,

    /** The survey drew a plan: the row opens it. */
    Ready,

    /** The scan has no depth to measure: no plan, whatever is scanned again on this phone. */
    NoDepth,

    /** The scan has depth but too few walls for a plan. */
    NoWalls,
    ;

    companion object {
        /** The offer for [survey], `null` while it is being read. */
        fun of(survey: RoomSurvey?): PlanOffer = when {
            survey == null -> Measuring
            survey.hasPlan -> Ready
            SurveyGap.NoDepth in survey.gaps -> NoDepth
            else -> NoWalls
        }
    }
}

/** The survey's words, read from the app's resources: this file holds none of its own. */
class SurveyWording(
    /** `%1$s` the scan's title, `%2$s` its date. */
    val heading: String,
    val area: String,
    val dimensions: String,
    val perimeter: String,
    val height: String,
    val volume: String,
    /** The rooms' table: one row per room, then the plan's total. */
    val rooms: String,
    /** `%1$d` the room's number on the plan. */
    val room: String,
    val total: String,
    /** The one dimension a scan with a single pair of facing walls gives. */
    val span: String,
    val walls: String,
    /** `%1$s` the wall's letter. */
    val wall: String,
    val notSeen: String,
    val notMeasured: String,
    val estimated: String,
    /** What "≈" and a dashed wall mean. */
    val estimateNote: String,
    /** What "±" is: the scatter of the scan, not an accuracy. */
    val spreadNote: String,
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
    private const val CUBIC_METRES = " m³"
    private const val ROOM_SEPARATOR = "; "
    private const val PLUS_MINUS = " ± "

    /** `20.0 m² ± 0.3` when measured, `≈ 20.0 m²` when an estimate. */
    fun area(figure: SurveyFigure): String = when {
        figure.estimate -> SurveyFormat.ESTIMATE + SurveyFormat.area(figure.value) + SQUARE_METRES
        else -> SurveyFormat.area(figure.value) + SQUARE_METRES + PLUS_MINUS + SurveyFormat.areaSpread(figure.plusMinus)
    }

    /**
     * `37.4 m³ ± 1.2` when measured, `≈ 37.4 m³` when an estimate: to a tenth of a cubic metre,
     * the area's own step — a finer one would claim more than the floor it stands on.
     */
    fun volume(figure: SurveyFigure): String = when {
        figure.estimate -> SurveyFormat.ESTIMATE + SurveyFormat.area(figure.value) + CUBIC_METRES
        else -> SurveyFormat.area(figure.value) + CUBIC_METRES + PLUS_MINUS + SurveyFormat.areaSpread(figure.plusMinus)
    }

    /** `5.00 m ± 0.05` when measured, `≈ 5.00 m` when an estimate. */
    fun length(figure: SurveyFigure): String = when {
        figure.estimate -> SurveyFormat.ESTIMATE + SurveyFormat.length(figure.value) + METRES
        else -> SurveyFormat.length(figure.value) + METRES + PLUS_MINUS + SurveyFormat.lengthSpread(figure.plusMinus)
    }

    /** The scan's name and, when the scan has one, its date. */
    fun heading(wording: SurveyWording, title: String, date: String?): String =
        if (date == null) title else wording.heading.format(title, date)

    /** What the scan lacks for an area, as the thing to go and scan; `null` when it lacks nothing. */
    fun areaHint(survey: RoomSurvey, wording: SurveyWording): String? =
        survey.gaps.firstOrNull { it != SurveyGap.Ceiling }?.let { wording.gaps[it] }

    /** What to go and scan, one line per thing the survey lacks. */
    fun hints(survey: RoomSurvey, wording: SurveyWording): List<String> = survey.gaps.mapNotNull { wording.gaps[it] }

    /** What the marks in use mean: "≈" when a figure is an estimate, "±" when one is measured. */
    fun notes(survey: RoomSurvey, wording: SurveyWording): List<String> = listOfNotNull(
        wording.estimateNote.takeIf { survey.hasEstimates },
        wording.spreadNote.takeIf { survey.hasSpread },
    )

    /** The survey's figures: area, length × width, perimeter, ceiling height and volume — or what is missing. */
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
            val both = SurveyFormat.length(length.value) + " × " + SurveyFormat.length(width.value) + METRES
            val value = when {
                length.estimate || width.estimate -> SurveyFormat.ESTIMATE + both
                else -> both + PLUS_MINUS + SurveyFormat.lengthSpread(max(length.plusMinus, width.plusMinus))
            }
            rows += SurveyRow(wording.dimensions, value)
        } else {
            (length ?: width)?.let { rows += SurveyRow(wording.span, length(it)) }
        }
        survey.perimeter?.let { rows += SurveyRow(wording.perimeter, length(it)) }
        val height = survey.ceilingHeight
        rows += when (height) {
            null -> SurveyRow(wording.height, wording.notSeen, muted = true)
            else -> SurveyRow(wording.height, length(height))
        }
        rows += when (val volume = PlanAreas.volume(survey.area, height)) {
            null -> SurveyRow(wording.volume, wording.notMeasured, muted = true)
            else -> SurveyRow(wording.volume, volume(volume))
        }
        return rows
    }

    /** The plan's rooms: a scan is one room, numbered as the plan's first. */
    fun areas(survey: RoomSurvey, wording: SurveyWording): PlanAreas =
        PlanAreas.of(listOf(PlanAreas.room(wording.room.format(1), survey)))

    /**
     * One row per room — its floor area, then its volume — and the plan's total last. A room
     * without a figure says so, and the total then gives none: it would leave that room out.
     */
    fun roomRows(areas: PlanAreas, wording: SurveyWording): List<SurveyRow> =
        areas.rooms.map { roomRow(it.name, it.area, it.volume, wording) } +
            roomRow(wording.total, areas.area, areas.volume, wording)

    private fun roomRow(label: String, area: SurveyFigure?, volume: SurveyFigure?, wording: SurveyWording) =
        SurveyRow(
            label = label,
            value = (area?.let(::area) ?: wording.notMeasured) + SEPARATOR +
                (volume?.let(::volume) ?: wording.notMeasured),
            muted = area == null,
        )

    /** One row per wall: its letter, its length, and whether it was seen. */
    fun wallRows(survey: RoomSurvey, wording: SurveyWording): List<SurveyRow> = survey.walls.map { wall ->
        val label = wording.wall.format(wall.tag)
        when {
            !wall.sized ->
                SurveyRow(label, wording.notMeasured + (if (wall.seen) "" else SEPARATOR + wording.notSeen), muted = true)
            !wall.seen -> SurveyRow(label, SurveyFormat.wall(wall) + METRES + SEPARATOR + wording.notSeen, muted = true)
            wall.length.estimate ->
                SurveyRow(label, SurveyFormat.wall(wall) + METRES + SEPARATOR + wording.estimated, muted = true)
            else -> SurveyRow(label, length(wall.length))
        }
    }

    /**
     * [rows] cut into pages: [first] of them on the page the table starts on, [following] on
     * each page after it. No row is dropped, however many walls the room has.
     */
    fun pages(rows: List<SurveyRow>, first: Int, following: Int): List<List<SurveyRow>> {
        if (rows.size <= first) return listOf(rows)
        return listOf(rows.take(first)) + rows.drop(first).chunked(following)
    }

    /** The plan's own words: a label per wall, the area in the middle, the scale bar's length. */
    fun planTexts(survey: RoomSurvey, wording: SurveyWording): PlanTexts = PlanTexts(
        walls = survey.walls.map { SurveyFormat.wall(it) },
        area = survey.area?.let {
            (if (it.estimate) SurveyFormat.ESTIMATE else "") + SurveyFormat.area(it.value) + SQUARE_METRES
        },
        // An estimate has no scatter to give.
        areaNote = survey.area?.takeIf { !it.estimate }?.let {
            PLUS_MINUS.trim() + " " + SurveyFormat.areaSpread(it.plusMinus) + SQUARE_METRES
        },
        scale = { metres -> wording.scaleBar.format(if (metres < 1f) "0.5" else metres.toInt().toString()) },
    )

    /**
     * The survey as a few plain lines, ready to paste: the figures, the walls, what to scan for
     * what is missing, what the marks mean, and the notice that this is no certified measurement.
     * The figures and the walls are the table's own rows: the same marks as on the screen and the PDF.
     */
    fun text(survey: RoomSurvey, wording: SurveyWording, title: String, date: String?): String {
        val lines = ArrayList<String>()
        lines += heading(wording, title, date)
        val rows = rows(survey, wording)
        // The floor's figures on one line, the height and the volume it gives on the next.
        val room = rows.takeLast(2)
        lines += rows.dropLast(2).joinToString(SEPARATOR) { it.label + ": " + it.value }
        lines += room.joinToString(SEPARATOR) { it.label + ": " + it.value }
        if (survey.hasPlan) {
            lines += wording.rooms + ": " +
                roomRows(areas(survey, wording), wording).joinToString(ROOM_SEPARATOR) { it.label + " " + it.value }
        }
        if (survey.walls.isNotEmpty()) {
            val walls = wallRows(survey, wording)
            lines += wording.walls + ": " +
                survey.walls.indices.joinToString(", ") { survey.walls[it].tag + " " + walls[it].value }
        }
        hints(survey, wording).takeIf { it.isNotEmpty() }?.let { lines += it.joinToString(" ") }
        lines += notes(survey, wording)
        lines += wording.notice + " " + wording.madeWith
        return lines.joinToString("\n")
    }
}

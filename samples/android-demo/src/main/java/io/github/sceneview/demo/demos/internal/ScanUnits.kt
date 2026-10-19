package io.github.sceneview.demo.demos.internal

import java.util.Locale

/*
 * 3D Scan names a room in the floor plan's units (#4599): the replay's dimensions, the room line
 * under its timeline and the plan the scan opens all read [SurveyFormat.UNITS], so one screen
 * never says feet and the next metres.
 */

/** [RoomMeasure.summary] in the floor plan's units: what 3D Scan names the room. */
fun RoomMeasure.scanSummary(locale: Locale): String = summary(SurveyFormat.UNITS, locale)

/** The width and depth labels 3D Scan draws on the floor, in the floor plan's units. */
fun RoomMeasure.scanLabels(locale: Locale): Pair<String, String> =
    formatMeasureLength(width, SurveyFormat.UNITS, locale) to formatMeasureLength(depth, SurveyFormat.UNITS, locale)

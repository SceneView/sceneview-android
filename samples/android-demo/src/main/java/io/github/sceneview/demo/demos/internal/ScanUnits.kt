package io.github.sceneview.demo.demos.internal

import java.util.Locale

/*
 * 3D Scan names a room in the floor plan's units (#4599): the replay's floor labels, the room line
 * under its timeline and the plan the scan opens all read [SurveyFormat.units] of the same locale,
 * so one screen never says feet and the next metres.
 */

/** [RoomMeasure.summary] in the floor plan's units under [locale]: what 3D Scan names the room. */
fun RoomMeasure.scanSummary(locale: Locale): String = summary(SurveyFormat.units(locale), locale)

/** The width and depth labels 3D Scan draws on the floor, in the floor plan's units under [locale]. */
fun RoomMeasure.scanLabels(locale: Locale): Pair<String, String> {
    val units = SurveyFormat.units(locale)
    return formatMeasureLength(width, units, locale) to formatMeasureLength(depth, units, locale)
}

/**
 * The floor a scan covered, as its result names it: `6.8 m²`, rounded as the live map rounds it
 * ([ScanAreaCopy.area]).
 */
@Suppress("UnusedParameter")
fun scanFloorArea(squareMetres: Float, locale: Locale): String = ScanAreaCopy.area(squareMetres)

/** How far the phone went, as the replay's settings name it: `14.2 m`, `42 cm`. */
@Suppress("UnusedParameter")
fun scanPathLength(metres: Float, locale: Locale): String = ArDebugFormat.distance(metres)

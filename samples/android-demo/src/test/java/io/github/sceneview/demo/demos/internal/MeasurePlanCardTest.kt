package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * The card under a traced room's plan is written in the plan's units: one page, one unit system.
 * The plan takes feet and inches in US English only ([SurveyFormat.units]); the card follows it,
 * whatever the country alone would suggest.
 */
class MeasurePlanCardTest {

    // A 0.9 × 2.1 m door on wall B and a 1.2 × 1 m window on wall C; 31.91 m² of wall left.
    private val openings = listOf(
        MeasurePlanOpening("B", MeasureOpening(from = 0.5f, to = 1.4f, bottom = 0f, top = 2.1f, wall = 1)),
        MeasurePlanOpening("C", MeasureOpening(from = 1f, to = 2.2f, bottom = 0.9f, top = 1.9f, wall = 2)),
    )

    private fun card(locale: Locale) = measurePlanCard(NET_WALL_AREA, openings, locale)

    @Test
    fun `Spanish in the US reads the card in metres, as the plan`() {
        val card = card(Locale.forLanguageTag("es-US"))
        assertEquals("31.9 m²", card.netWallArea)
        assertEquals(
            listOf(MeasurePlanCardOpening("B", "90 cm", "2.10 m"), MeasurePlanCardOpening("C", "1.20 m", "1.00 m")),
            card.openings,
        )
    }

    @Test
    fun `US English reads the card in feet and inches, as the plan`() {
        val card = card(Locale.US)
        assertEquals("343 ft²", card.netWallArea)
        assertEquals(
            listOf(MeasurePlanCardOpening("B", "2 ft 11 in", "6 ft 11 in"), MeasurePlanCardOpening("C", "3 ft 11 in", "3 ft 3 in")),
            card.openings,
        )
    }

    @Test
    fun `French reads the card in metres with its own decimal mark`() {
        val card = card(Locale.FRANCE)
        assertEquals("31,9 m²", card.netWallArea)
        assertEquals(MeasurePlanCardOpening("B", "90 cm", "2,10 m"), card.openings.first())
    }

    @Test
    fun `the card and the plan never disagree on units`() {
        val locales = listOf("en-US", "es-US", "en-LR", "my-MM", "en-GB", "fr-FR", "de-DE", "en-CA").map(Locale::forLanguageTag)
        for (locale in locales) {
            val feet = card(locale).netWallArea.endsWith("ft²")
            assertEquals(locale.toLanguageTag(), SurveyFormat.units(locale) == MeasureUnitSystem.Imperial, feet)
        }
    }

    private companion object {
        const val NET_WALL_AREA = 31.91f
    }
}

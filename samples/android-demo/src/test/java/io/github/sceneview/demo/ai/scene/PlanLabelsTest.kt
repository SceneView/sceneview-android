package io.github.sceneview.demo.ai.scene

import io.github.sceneview.demo.demos.PlanLabel
import io.github.sceneview.demo.demos.PlanLabelPlacement
import io.github.sceneview.demo.demos.PlanPoint
import io.github.sceneview.demo.demos.PlanRect
import io.github.sceneview.demo.demos.placePlanLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanLabelsTest {
    private val viewport = PlanRect(0f, 0f, 320f, 224f)
    private val gap = 4f

    private fun label(rect: PlanRect, width: Float = 64f, height: Float = 18f) = PlanLabel(
        rect.corners, width, height, markerWidth = 12f, markerHeight = 18f,
    )

    private fun assertClear(placements: List<PlanLabelPlacement>, bounds: PlanRect = viewport) {
        placements.forEachIndexed { index, placement ->
            val rect = checkNotNull(placement.bounds)
            assertTrue(bounds.contains(rect))
            placements.drop(index + 1).forEach { other ->
                assertFalse(rect.overlaps(checkNotNull(other.bounds), gap))
            }
        }
    }

    @Test
    fun `a fitting label stays centered without changing its size`() {
        val label = label(PlanRect(80f, 70f, 220f, 140f))
        val result = placePlanLabels(listOf(label), viewport, emptyList(), gap).single()
        assertEquals(PlanRect.around(label.center, 64f, 18f), result.bounds)
        assertFalse(result.leader)
        assertNull(result.number)
    }

    @Test
    fun `plant label leaves a small footprint and gets a leader`() {
        val plant = label(PlanRect(150f, 100f, 162f, 112f), width = 40f)
        val result = placePlanLabels(listOf(plant), viewport, emptyList(), gap).single()
        assertTrue(result.leader)
        assertFalse(checkNotNull(result.bounds).overlaps(PlanRect.enclosing(plant.footprint), gap))
        assertEquals(40f, result.bounds.right - result.bounds.left, 0f)
        assertNull(result.number)
    }

    @Test
    fun `overlapping desk and chair labels occupy separate screen rectangles`() {
        val labels = listOf(
            label(PlanRect(110f, 80f, 220f, 130f)),
            label(PlanRect(155f, 95f, 180f, 122f), width = 88f),
        )
        val results = placePlanLabels(labels, viewport, emptyList(), gap)
        assertClear(results)
        assertTrue(results.any { it.leader })
    }

    @Test
    fun `rotated footprint uses polygon containment rather than its bounding box`() {
        val diamond = label(PlanRect(70f, 60f, 170f, 160f), width = 90f, height = 30f).copy(
            footprint = listOf(PlanPoint(120f, 60f), PlanPoint(170f, 110f),
                PlanPoint(120f, 160f), PlanPoint(70f, 110f)),
        )
        val result = placePlanLabels(listOf(diamond), viewport, emptyList(), gap).single()
        assertTrue(result.leader)
        assertNull(result.number)
        assertEquals(90f, result.bounds!!.right - result.bounds.left, 0f)
        assertEquals(30f, result.bounds.bottom - result.bounds.top, 0f)
    }

    @Test
    fun `wall dimensions and viewport edges stay clear of displaced text`() {
        val labels = listOf(label(PlanRect(0f, 0f, 10f, 10f)))
        val dimensions = listOf(PlanRect(0f, 0f, 160f, 40f))
        val results = placePlanLabels(labels, viewport, dimensions, gap)
        assertClear(results)
        assertFalse(results.single().bounds!!.overlaps(dimensions.single(), gap))
    }

    @Test
    fun `crowded plan falls back to stable numbered markers without overlapping`() {
        val bounds = PlanRect(0f, 0f, 120f, 80f)
        val labels = listOf(
            label(bounds, width = 110f, height = 70f),
            label(bounds, width = 110f, height = 70f),
            label(bounds, width = 110f, height = 70f),
        )
        val results = placePlanLabels(labels, bounds, emptyList(), gap)
        assertEquals(listOf(1, 2, 3), results.map { it.number })
        assertClear(results, bounds)
        assertEquals(results, placePlanLabels(labels, bounds, emptyList(), gap))
    }

    @Test
    fun `large font falls back without reducing text size`() {
        val label = label(PlanRect(130f, 80f, 170f, 120f), width = 400f, height = 36f)
            .copy(markerWidth = 24f, markerHeight = 36f)
        val result = placePlanLabels(listOf(label), viewport, emptyList(), gap).single()
        assertEquals(1, result.number)
        assertNotNull(result.bounds)
        assertEquals(36f, result.bounds!!.bottom - result.bounds.top, 0f)
    }

    @Test
    fun `empty and unusably small viewports retain safe legend output`() {
        assertTrue(placePlanLabels(emptyList(), viewport, emptyList(), gap).isEmpty())
        val result = placePlanLabels(
            listOf(label(PlanRect(0f, 0f, 2f, 2f))), PlanRect(0f, 0f, 2f, 2f), emptyList(), gap,
        ).single()
        assertNull(result.bounds)
        assertEquals(1, result.number)
    }
}

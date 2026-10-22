package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A room's live scan: the 3D map on top, the camera under it, down to the controls. */
class ScanStageSplitTest {

    @Test
    fun theMapTakesTheLargerPart() {
        val split = present(ScanStageSplit.of(2000f, hudBottom = 400f, controlsTop = 1600f, mapFirst = true))
        // 1,200 px between card and controls: 720 to the map, 480 to the camera.
        assertEquals(1120f, split.mapBottom, 0.5f)
        assertEquals(split.mapBottom, split.cameraTop, 0f)
        assertEquals(1600f, split.cameraBottom, 0f)
        assertEquals(400f, split.clearTop, 0f)
    }

    @Test
    fun swappedTheCameraTakesIt() {
        val map = present(ScanStageSplit.of(2000f, 400f, 1600f, mapFirst = true))
        val camera = present(ScanStageSplit.of(2000f, 400f, 1600f, mapFirst = false))
        assertTrue(camera.cameraBottom - camera.cameraTop > map.cameraBottom - map.cameraTop)
        assertEquals(760f, camera.mapBottom, 0.5f)
    }

    @Test
    fun unmeasuredChromeFallsBackToItsUsualPlace() {
        val split = present(ScanStageSplit.of(1000f, Float.NaN, Float.NaN, mapFirst = true))
        assertEquals(200f, split.clearTop, 0f)
        assertEquals(780f, split.cameraBottom, 0f)
    }

    @Test
    fun aShortStageKeepsTheCard() {
        // Landscape: the card and the controls leave 150 px of a 1,000 px stage.
        assertNull(ScanStageSplit.of(1000f, 400f, 550f, mapFirst = true))
        assertNull(ScanStageSplit.of(0f, 0f, 0f, mapFirst = true))
    }

    private fun <T> present(value: T?): T {
        org.junit.Assert.assertNotNull(value)
        return value!!
    }
}

package io.github.sceneview.ar

import com.google.ar.core.TrackingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for ARCore [TrackingState] enum contract.
 *
 * These tests verify that the tracking state values SceneView depends on
 * actually exist in the ARCore SDK. This acts as a compile-time and runtime
 * guard against breaking changes in ARCore updates.
 */
class TrackingStateTest {

    @Test
    fun `TrackingState contains all expected values`() {
        val values = TrackingState.values()
        val names = values.map { it.name }.toSet()
        assertTrue("TRACKING missing", "TRACKING" in names)
        assertTrue("PAUSED missing", "PAUSED" in names)
        assertTrue("STOPPED missing", "STOPPED" in names)
    }

    @Test
    fun `TrackingState has exactly 3 values`() {
        assertEquals(3, TrackingState.values().size)
    }

    @Test
    fun `valueOf returns correct enum`() {
        assertEquals(TrackingState.TRACKING, TrackingState.valueOf("TRACKING"))
        assertEquals(TrackingState.PAUSED, TrackingState.valueOf("PAUSED"))
        assertEquals(TrackingState.STOPPED, TrackingState.valueOf("STOPPED"))
    }
}

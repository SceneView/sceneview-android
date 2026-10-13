package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Back on the Record screen: it asks only when leaving would drop a scan that holds something,
 * and the scan's clock stops at Stop. Both were read on a phone — a scan dropped by one swipe,
 * a line counting from 2:57 to 3:06 under "Building your scan…".
 */
class ScanBackTest {

    @Test
    fun `back asks during a scan that holds points`() {
        assertEquals(ScanBackAction.Ask, ScanBack.on(ScanBackState.Recording, points = 1))
        assertEquals(ScanBackAction.Ask, ScanBack.on(ScanBackState.Recording, points = 250_000))
    }

    @Test
    fun `back leaves at once from a scan that holds nothing yet`() {
        assertEquals(ScanBackAction.Leave, ScanBack.on(ScanBackState.Recording, points = 0))
    }

    @Test
    fun `back leaves at once before the scan starts`() {
        assertEquals(ScanBackAction.Leave, ScanBack.on(ScanBackState.Waiting, points = 0))
        // A count left over from a previous scan is not a scan in progress.
        assertEquals(ScanBackAction.Leave, ScanBack.on(ScanBackState.Waiting, points = 4_000))
    }

    @Test
    fun `back does not ask while the scan is built`() {
        assertEquals(ScanBackAction.Leave, ScanBack.on(ScanBackState.Building, points = 0))
        assertEquals(ScanBackAction.Leave, ScanBack.on(ScanBackState.Building, points = 4_000))
    }

    @Test
    fun `every state and count has an answer and only one of them asks`() {
        val asking = ScanBackState.entries.flatMap { state ->
            listOf(0, 1, 4_000).filter { ScanBack.on(state, it) == ScanBackAction.Ask }.map { state to it }
        }
        assertEquals(listOf(ScanBackState.Recording to 1, ScanBackState.Recording to 4_000), asking)
    }

    @Test
    fun `the clock follows now while the scan runs`() {
        assertEquals(177_000L, ScanClock.readAt(nowNanos = 177_000L, stoppedNanos = null))
    }

    @Test
    fun `the clock holds the time of Stop while the scan is built`() {
        val stopped = 176_000L
        assertEquals(stopped, ScanClock.readAt(nowNanos = 177_000L, stoppedNanos = stopped))
        assertEquals(stopped, ScanClock.readAt(nowNanos = 186_000L, stoppedNanos = stopped))
    }

    @Test
    fun `a Stop stamped after the last tick does not run the clock ahead`() {
        assertEquals(176_000L, ScanClock.readAt(nowNanos = 176_000L, stoppedNanos = 176_400L))
    }
}

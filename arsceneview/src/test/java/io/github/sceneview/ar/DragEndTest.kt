package io.github.sceneview.ar

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What a finished one-finger drag does to an automatic placement (#4731): a placed room drifted
 * away from where it was set down, a few centimetres per pinch, because the first finger's
 * travel before the second landed was committed as a move.
 */
class DragEndTest {

    @Test
    fun `a drag the finger lifts from is committed`() {
        assertEquals(DragEnd.COMMIT, DragEnd.of(began = true, pointerCount = 1))
    }

    @Test
    fun `a drag a second finger interrupts goes back on its anchor`() {
        assertEquals(DragEnd.REVERT, DragEnd.of(began = true, pointerCount = 2))
        assertEquals(DragEnd.REVERT, DragEnd.of(began = true, pointerCount = 3))
    }

    @Test
    fun `a drag that never began neither moves the room nor closes an adjustment`() {
        // The detector ends a drag still being detected when a second finger lands: that end
        // must not close the adjustment the pinch is opening.
        assertEquals(DragEnd.NONE, DragEnd.of(began = false, pointerCount = 2))
        assertEquals(DragEnd.NONE, DragEnd.of(began = false, pointerCount = 1))
    }
}

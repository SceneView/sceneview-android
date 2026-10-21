package io.github.sceneview.demo.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.github.sceneview.demo.demos.internal.ScanCopy
import io.github.sceneview.demo.demos.internal.ScanTurn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The turn a room's scan points to ("Turn left — no wall seen there yet") holds a few seconds
 * before it changes: facing a wall, the next gap's side flips with every small sway of the phone,
 * and a hint that flips back and forth says nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScanTurnHoldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a turn shown a second ago is not replaced by the other side`() {
        var turn by mutableStateOf<ScanTurn?>(ScanTurn.Left)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { Box(Modifier.fillMaxSize()) { ScanTurnArrow(turn) } }
        composeRule.mainClock.advanceTimeBy(SHOWN_MS)
        composeRule.runOnUiThread { turn = ScanTurn.Right; Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(SHOWN_MS)
        composeRule.onNodeWithContentDescription(ScanCopy.turnHint(ScanTurn.Left)).assertExists()
        composeRule.onNodeWithContentDescription(ScanCopy.turnHint(ScanTurn.Right)).assertDoesNotExist()
    }

    @Test
    fun `the other side shows once the turn has held its time`() {
        var turn by mutableStateOf<ScanTurn?>(ScanTurn.Left)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { Box(Modifier.fillMaxSize()) { ScanTurnArrow(turn) } }
        composeRule.mainClock.advanceTimeBy(SHOWN_MS)
        composeRule.runOnUiThread { turn = ScanTurn.Right; Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(SCAN_HINT_HOLD_MS)
        composeRule.onNodeWithContentDescription(ScanCopy.turnHint(ScanTurn.Right)).assertExists()
    }

    @Test
    fun `a side that comes and goes within the hold is never shown`() {
        var turn by mutableStateOf<ScanTurn?>(ScanTurn.Left)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { Box(Modifier.fillMaxSize()) { ScanTurnArrow(turn) } }
        composeRule.mainClock.advanceTimeBy(SHOWN_MS)
        composeRule.runOnUiThread { turn = ScanTurn.Right; Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(SHOWN_MS)
        composeRule.runOnUiThread { turn = ScanTurn.Left; Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(SCAN_HINT_HOLD_MS)
        composeRule.onNodeWithContentDescription(ScanCopy.turnHint(ScanTurn.Left)).assertExists()
        composeRule.onNodeWithContentDescription(ScanCopy.turnHint(ScanTurn.Right)).assertDoesNotExist()
    }

    private companion object {
        /** Well under the hold: the time a sway of the phone takes. */
        const val SHOWN_MS = 1_000L
    }
}

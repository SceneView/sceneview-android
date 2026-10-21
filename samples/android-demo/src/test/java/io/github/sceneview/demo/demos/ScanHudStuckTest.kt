package io.github.sceneview.demo.demos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.sceneview.demo.demos.internal.ScanActivity
import io.github.sceneview.demo.demos.internal.ScanCopy
import io.github.sceneview.demo.demos.internal.ScanPhase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The capture's top line says "Stuck" only when the scan adds nothing: a count of points that
 * keeps rising is the scan working, whatever the coverage of the floor says.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScanHudStuckTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var seconds by mutableStateOf(START_S)
    private var points by mutableStateOf(START_POINTS)

    private fun showHud() = composeRule.setContent {
        ScanHud(phase = ScanPhase.Scanning, seconds = seconds, points = points, activity = ScanActivity.Stuck)
    }

    @Test
    fun `a scan whose points keep rising does not say it is stuck`() {
        showHud()
        composeRule.runOnIdle {
            seconds = START_S + 1f
            points = START_POINTS + 200
        }
        composeRule.onNodeWithText(ScanCopy.STUCK, substring = true).assertDoesNotExist()
    }

    @Test
    fun `a scan whose points stay flat still says it is stuck`() {
        showHud()
        composeRule.runOnIdle { seconds = START_S + 1f }
        composeRule.onNodeWithText(ScanCopy.STUCK, substring = true).assertExists()
    }

    private companion object {
        const val START_S = 10f
        const val START_POINTS = 1_000
    }
}

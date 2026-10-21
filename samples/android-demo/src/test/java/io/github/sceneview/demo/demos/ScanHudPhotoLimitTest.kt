package io.github.sceneview.demo.demos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.sceneview.demo.demos.internal.ScanPhase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The photo limit changes nothing of a scan's geometry: once the coaching line has said so
 * ("points and path keep recording"), the HUD keeps no lasting reminder of it that reads as a
 * fault for the rest of the scan.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScanHudPhotoLimitTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a scan past its photo limit keeps no lasting reminder of it`() {
        composeRule.setContent {
            ScanHud(phase = ScanPhase.Scanning, seconds = SECONDS, points = POINTS)
        }
        composeRule.onNodeWithText(LIMIT_WORDS, substring = true, ignoreCase = true).assertDoesNotExist()
    }

    private companion object {
        const val SECONDS = 90f
        const val POINTS = 400_000
        const val LIMIT_WORDS = "photo limit"
    }
}

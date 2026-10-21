package io.github.sceneview.demo.demos

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.sceneview.demo.theme.SceneViewDemoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The model screen's status line is heard as it changes — "Creating your model…", then
 * "Finishing your model…" over a big scan's raw surface — not only seen. Its progress bar is not:
 * a bar that spoke on every step would drown the line.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RerunModelStatusCardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `the status line is announced politely, its progress is not`() {
        rule.setContent {
            SceneViewDemoTheme(darkTheme = false) {
                ModelStatusCard(text = FINISHING, progress = { HALF })
            }
        }
        val line = rule.onNodeWithText(FINISHING).fetchSemanticsNode()
        assertEquals(LiveRegionMode.Polite, line.config.getOrNull(SemanticsProperties.LiveRegion))
        val bar = rule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(HALF, 0f..1f))).fetchSemanticsNode()
        assertNull(bar.config.getOrNull(SemanticsProperties.LiveRegion))
    }

    private companion object {
        const val FINISHING = "Finishing your model…"
        const val HALF = 0.5f
    }
}

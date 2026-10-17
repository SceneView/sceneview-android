package io.github.sceneview.demo.ui.viewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import io.github.sceneview.demo.theme.SceneViewTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerBackdropTest {

    @Test
    fun `inverse PBR Neutral round-trips across the range`() {
        // Scene-linear inputs from deep shadow to three times the compression knee: every colour
        // the forward curve can output, the inverse brings back.
        val samples = listOf(0f, 0.001f, 0.0033f, 0.01f, 0.03f, 0.05f, 0.2f, 0.5f, 0.75f, 1f, 1.6f, 2.5f)
        samples.forEach { a ->
            samples.forEach { b ->
                val scene = floatArrayOf(a, b, (a + b) / 2f)
                val back = ViewerBackdrop.inversePbrNeutral(ViewerBackdrop.pbrNeutral(scene))
                for (i in 0..2) assertEquals("($a, $b) channel $i", scene[i], back[i], 1e-3f * maxOf(1f, scene[i]))
            }
        }
    }

    @Test
    fun `PBR Neutral is the identity in the mid range`() {
        // The property that keeps base colours as authored: no toe offset above 0.08, no
        // compression below 0.76 — the shift is the constant 0.04 only.
        val mid = floatArrayOf(0.3f, 0.5f, 0.7f)
        val out = ViewerBackdrop.pbrNeutral(mid)
        for (i in 0..2) assertEquals(mid[i] - 0.04f, out[i], 1e-6f)
    }

    @Test
    fun `the backdrop reaches the screen as the stage token, light and dark`() {
        listOf(SceneViewTokens.Stage.background, Color(0xFF07090D), Color(0xFFF5F6F8)).forEach { color ->
            val token = color.convert(ColorSpaces.LinearSrgb)
            val drawn = ViewerBackdrop.linearBeforeToneMap(color)
            val shown = ViewerBackdrop.pbrNeutral(drawn)
            assertEquals(token.red, shown[0], 2e-4f)
            assertEquals(token.green, shown[1], 2e-4f)
            assertEquals(token.blue, shown[2], 2e-4f)
            // The toe pulls a channel down: what is drawn must be brighter than the token.
            assertTrue(drawn[2] > token.blue)
        }
    }
}

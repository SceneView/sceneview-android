package io.github.sceneview.demo.ui.viewer

import androidx.compose.ui.graphics.colorspace.ColorSpaces
import io.github.sceneview.demo.theme.SceneViewTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerBackdropTest {

    @Test
    fun `inverse filmic round-trips across the range`() {
        listOf(0.001f, 0.0033f, 0.01f, 0.05f, 0.2f, 0.5f, 0.8f).forEach { y ->
            assertEquals(y, ViewerBackdrop.filmic(ViewerBackdrop.inverseFilmic(y)), 1e-5f)
        }
    }

    @Test
    fun `the backdrop reaches the screen as the stage token`() {
        val token = SceneViewTokens.Stage.background.convert(ColorSpaces.LinearSrgb)
        val drawn = ViewerBackdrop.linearBeforeFilmic(SceneViewTokens.Stage.background)
        val shown = drawn.map(ViewerBackdrop::filmic)
        assertEquals(token.red, shown[0], 1e-5f)
        assertEquals(token.green, shown[1], 1e-5f)
        assertEquals(token.blue, shown[2], 1e-5f)
        // The toe crushes a colour this dark: what is drawn must be brighter than the token.
        assertTrue(drawn[2] > token.blue * 2f)
    }

    @Test
    fun `PBR Neutral keeps a mid grey minus its toe offset`() {
        val shown = ViewerBackdrop.pbrNeutral(floatArrayOf(0.18f, 0.18f, 0.18f))
        shown.forEach { assertEquals(0.14f, it, 1e-6f) }
    }

    @Test
    fun `inverse PBR Neutral round-trips in the toe, the linear part and the shoulder`() {
        listOf(
            floatArrayOf(0.004f, 0.01f, 0.02f),
            floatArrayOf(0.03f, 0.05f, 0.07f),
            floatArrayOf(0.2f, 0.3f, 0.5f),
            floatArrayOf(0.6f, 0.62f, 0.7f),
            floatArrayOf(0.9f, 1.2f, 2f),
            floatArrayOf(3f, 3f, 3f),
        ).forEach { scene ->
            val back = ViewerBackdrop.inversePbrNeutral(ViewerBackdrop.pbrNeutral(scene))
            scene.indices.forEach { assertEquals(scene[it], back[it], scene[it] * 1e-3f + 1e-5f) }
        }
    }

    @Test
    fun `every showroom token reaches the screen as itself`() {
        listOf(SceneViewTokens.Showroom.Light, SceneViewTokens.Showroom.Dark).forEach { palette ->
            listOf(palette.top, palette.horizon, palette.floor, palette.pool).forEach { color ->
                val token = color.convert(ColorSpaces.LinearSrgb)
                val shown = ViewerBackdrop.pbrNeutral(ViewerBackdrop.linearBeforeNeutral(color))
                assertEquals(token.red, shown[0], 1e-4f)
                assertEquals(token.green, shown[1], 1e-4f)
                assertEquals(token.blue, shown[2], 1e-4f)
            }
        }
    }
}

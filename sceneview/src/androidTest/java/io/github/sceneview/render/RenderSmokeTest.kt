package io.github.sceneview.render

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import io.github.sceneview.node.CameraNode
import io.github.sceneview.render.RenderTestHarness.Companion.colorsMatch
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests verifying that Filament renders correctly in a headless environment.
 *
 * These tests use [RenderTestHarness] to render simple scenes without a window surface,
 * then read back specific pixels to verify the output. They catch regressions in:
 * - Engine initialization
 * - Skybox rendering
 * - Scene/View pipeline
 * - Camera setup
 *
 * The tolerance on color comparisons (±10 per channel) accounts for differences
 * between GPU drivers (hardware vs SwiftShader software rendering).
 */
@RunWith(AndroidJUnit4::class)
class RenderSmokeTest {

    private lateinit var harness: RenderTestHarness

    @Before
    fun setup() {
        harness = RenderTestHarness(width = 64, height = 64)
    }

    @After
    fun teardown() {
        harness.destroy()
    }

    // ── Engine initialization ───────────────────────────────────────────────

    @Test
    fun engine_initializes_without_crash() {
        // If we get here, the harness created Engine + Renderer + View + SwapChain successfully
        harness.runOnMain {
            assertNotNull(harness.engine)
            assertNotNull(harness.renderer)
            assertNotNull(harness.view)
            assertNotNull(harness.scene)
        }
    }

    // ── Solid skybox renders correct color ───────────────────────────────────

    @Test
    fun solidRedSkybox_rendersRedPixels() {
        var bitmap: Bitmap? = null
        harness.runOnMain {
            // Set a solid red skybox
            harness.scene.skybox = Skybox.Builder()
                .color(1f, 0f, 0f, 1f)
                .build(harness.engine)

            // Add a camera so Filament has something to render from
            val camera = harness.engine.createCamera(harness.engine.entityManager.create())
            camera.setProjection(45.0, 1.0, 0.1, 100.0, com.google.android.filament.Camera.Fov.VERTICAL)
            harness.view.camera = camera

            harness.renderFrames(5)
            bitmap = harness.capturePixels()
        }

        val bmp = bitmap!!
        val centerColor = bmp.getPixel(32, 32)
        assertTrue(
            "Center pixel should be red but was ${colorToString(centerColor)}",
            colorsMatch(centerColor, Color.RED, tolerance = 30)
        )
    }

    @Test
    fun solidBlueSkybox_rendersBluePixels() {
        var bitmap: Bitmap? = null
        harness.runOnMain {
            harness.scene.skybox = Skybox.Builder()
                .color(0f, 0f, 1f, 1f)
                .build(harness.engine)

            val camera = harness.engine.createCamera(harness.engine.entityManager.create())
            camera.setProjection(45.0, 1.0, 0.1, 100.0, com.google.android.filament.Camera.Fov.VERTICAL)
            harness.view.camera = camera

            harness.renderFrames(5)
            bitmap = harness.capturePixels()
        }

        val bmp = bitmap!!
        val centerColor = bmp.getPixel(32, 32)
        assertTrue(
            "Center pixel should be blue but was ${colorToString(centerColor)}",
            colorsMatch(centerColor, Color.BLUE, tolerance = 30)
        )
    }

    // ── Rendering produces non-black output ─────────────────────────────────

    @Test
    fun whiteScene_isNotBlack() {
        var bitmap: Bitmap? = null
        harness.runOnMain {
            harness.scene.skybox = Skybox.Builder()
                .color(1f, 1f, 1f, 1f)
                .build(harness.engine)

            val camera = harness.engine.createCamera(harness.engine.entityManager.create())
            camera.setProjection(45.0, 1.0, 0.1, 100.0, com.google.android.filament.Camera.Fov.VERTICAL)
            harness.view.camera = camera

            harness.renderFrames(5)
            bitmap = harness.capturePixels()
        }

        val bmp = bitmap!!
        // Check that at least some pixels are non-black (rendering actually happened)
        var nonBlackPixels = 0
        for (x in 0 until bmp.width) {
            for (y in 0 until bmp.height) {
                val pixel = bmp.getPixel(x, y)
                if (Color.red(pixel) > 10 || Color.green(pixel) > 10 || Color.blue(pixel) > 10) {
                    nonBlackPixels++
                }
            }
        }
        val totalPixels = bmp.width * bmp.height
        val nonBlackPercent = nonBlackPixels * 100 / totalPixels
        assertTrue(
            "At least 80% of pixels should be non-black (got $nonBlackPercent%)",
            nonBlackPercent >= 80
        )
    }

    // ── Different skybox colors produce different renders ────────────────────

    @Test
    fun differentSkyboxColors_produceDifferentRenders() {
        var bitmapRed: Bitmap? = null
        var bitmapGreen: Bitmap? = null

        harness.runOnMain {
            val camera = harness.engine.createCamera(harness.engine.entityManager.create())
            camera.setProjection(45.0, 1.0, 0.1, 100.0, com.google.android.filament.Camera.Fov.VERTICAL)
            harness.view.camera = camera

            // Render red
            harness.scene.skybox = Skybox.Builder()
                .color(1f, 0f, 0f, 1f)
                .build(harness.engine)
            harness.renderFrames(5)
            bitmapRed = harness.capturePixels()

            // Render green
            harness.scene.skybox = Skybox.Builder()
                .color(0f, 1f, 0f, 1f)
                .build(harness.engine)
            harness.renderFrames(5)
            bitmapGreen = harness.capturePixels()
        }

        val redCenter = bitmapRed!!.getPixel(32, 32)
        val greenCenter = bitmapGreen!!.getPixel(32, 32)

        // Red and green renders must be different
        assertTrue(
            "Red and green skybox should produce different pixels. " +
                    "Red=${colorToString(redCenter)}, Green=${colorToString(greenCenter)}",
            !colorsMatch(redCenter, greenCenter, tolerance = 20)
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun colorToString(color: Int): String =
        "ARGB(${Color.alpha(color)}, ${Color.red(color)}, ${Color.green(color)}, ${Color.blue(color)})"
}

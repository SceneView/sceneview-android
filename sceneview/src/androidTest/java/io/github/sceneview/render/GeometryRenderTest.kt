package io.github.sceneview.render

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.filament.Camera
import com.google.android.filament.Skybox
import io.github.sceneview.geometries.Cube
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.colorOf
import io.github.sceneview.node.CubeNode
import io.github.sceneview.render.RenderTestHarness.Companion.colorsMatch
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Render tests for geometry nodes (CubeNode, SphereNode, etc.).
 *
 * These tests verify that procedural geometry actually appears in the rendered output.
 * A [MaterialLoader] is used to create coloured materials, and the rendered pixels
 * are compared against expected values.
 *
 * Requires an Android device or emulator with GPU support (SwiftShader OK).
 */
@RunWith(AndroidJUnit4::class)
class GeometryRenderTest {

    private lateinit var harness: RenderTestHarness
    private lateinit var materialLoader: MaterialLoader

    @Before
    fun setup() {
        harness = RenderTestHarness(width = 128, height = 128)
        harness.runOnMain {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            materialLoader = MaterialLoader(harness.engine, context)
        }
    }

    @After
    fun teardown() {
        harness.runOnMain {
            materialLoader.destroy()
        }
        harness.destroy()
    }

    // ── CubeNode renders visible pixels ─────────────────────────────────────

    @Test
    fun cubeNode_rendersNonBlackPixels() {
        var bitmap: Bitmap? = null
        harness.runOnMain {
            // White background so we can detect the cube
            harness.scene.skybox = Skybox.Builder()
                .color(1f, 1f, 1f, 1f)
                .build(harness.engine)

            // Camera looking at origin
            val camera = harness.engine.createCamera(harness.engine.entityManager.create())
            camera.setProjection(45.0, 1.0, 0.1, 100.0, Camera.Fov.VERTICAL)
            camera.lookAt(
                0.0, 0.0, 5.0,   // eye position
                0.0, 0.0, 0.0,   // target
                0.0, 1.0, 0.0    // up
            )
            harness.view.camera = camera

            // Red cube at origin
            val redMaterial = materialLoader.createColorInstance(
                color = colorOf(1f, 0f, 0f, 1f)
            )
            val cubeNode = CubeNode(
                engine = harness.engine,
                size = Cube.DEFAULT_SIZE,
                center = Position(0f, 0f, 0f),
                materialInstances = listOf(redMaterial)
            )
            harness.scene.addEntity(cubeNode.entity)

            harness.renderFrames(5)
            bitmap = harness.capturePixels()

            // Cleanup
            harness.scene.removeEntity(cubeNode.entity)
            cubeNode.destroy()
        }

        val bmp = bitmap!!
        // The cube should occlude some of the white background at the center
        // Count pixels that are NOT white (i.e. the cube is rendering something)
        var nonWhitePixels = 0
        val cx = bmp.width / 2
        val cy = bmp.height / 2
        val region = 20 // check a 40x40 region around center
        for (x in (cx - region) until (cx + region)) {
            for (y in (cy - region) until (cy + region)) {
                val pixel = bmp.getPixel(x, y)
                if (!colorsMatch(pixel, Color.WHITE, tolerance = 30)) {
                    nonWhitePixels++
                }
            }
        }

        assertTrue(
            "Cube should render visible pixels at the center. " +
                    "Found $nonWhitePixels non-white pixels in center region",
            nonWhitePixels > 50 // at least some pixels should be from the cube
        )
    }

    // ── Different material colors produce different renders ──────────────────

    @Test
    fun cubeNode_differentColors_produceDifferentRenders() {
        var bitmapRed: Bitmap? = null
        var bitmapBlue: Bitmap? = null

        harness.runOnMain {
            harness.scene.skybox = Skybox.Builder()
                .color(0f, 0f, 0f, 1f)  // black background
                .build(harness.engine)

            val camera = harness.engine.createCamera(harness.engine.entityManager.create())
            camera.setProjection(45.0, 1.0, 0.1, 100.0, Camera.Fov.VERTICAL)
            camera.lookAt(0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
            harness.view.camera = camera

            // Red cube
            val redMaterial = materialLoader.createColorInstance(colorOf(1f, 0f, 0f, 1f))
            val redCube = CubeNode(
                engine = harness.engine,
                size = Cube.DEFAULT_SIZE,
                center = Position(0f, 0f, 0f),
                materialInstances = listOf(redMaterial)
            )
            harness.scene.addEntity(redCube.entity)
            harness.renderFrames(5)
            bitmapRed = harness.capturePixels()
            harness.scene.removeEntity(redCube.entity)
            redCube.destroy()

            // Blue cube (same position)
            val blueMaterial = materialLoader.createColorInstance(colorOf(0f, 0f, 1f, 1f))
            val blueCube = CubeNode(
                engine = harness.engine,
                size = Cube.DEFAULT_SIZE,
                center = Position(0f, 0f, 0f),
                materialInstances = listOf(blueMaterial)
            )
            harness.scene.addEntity(blueCube.entity)
            harness.renderFrames(5)
            bitmapBlue = harness.capturePixels()
            harness.scene.removeEntity(blueCube.entity)
            blueCube.destroy()
        }

        val cx = bitmapRed!!.width / 2
        val cy = bitmapRed!!.height / 2
        val redCenter = bitmapRed!!.getPixel(cx, cy)
        val blueCenter = bitmapBlue!!.getPixel(cx, cy)

        assertTrue(
            "Red and blue cubes should produce different center pixels. " +
                    "Red=${colorStr(redCenter)}, Blue=${colorStr(blueCenter)}",
            !colorsMatch(redCenter, blueCenter, tolerance = 20)
        )
    }

    private fun colorStr(c: Int) =
        "ARGB(${Color.alpha(c)},${Color.red(c)},${Color.green(c)},${Color.blue(c)})"
}

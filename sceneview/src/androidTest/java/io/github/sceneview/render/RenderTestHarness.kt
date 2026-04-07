package io.github.sceneview.render

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.SwapChainFlags
import com.google.android.filament.Texture
import com.google.android.filament.View
import com.google.android.filament.Viewport
import io.github.sceneview.createEglContext
import io.github.sceneview.createEngine
import io.github.sceneview.createRenderer
import io.github.sceneview.createView
import io.github.sceneview.safeDestroy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Test harness for headless Filament rendering with pixel readback.
 *
 * Creates a Filament Engine, View, Scene, Renderer and an offscreen [SwapChain]
 * backed by a pbuffer (no window surface required). After rendering one or more
 * frames, call [capturePixels] to read back the framebuffer contents as a [Bitmap].
 *
 * **All Filament calls must run on the main thread.** Use [runOnMain] for convenience.
 *
 * Usage:
 * ```kotlin
 * val harness = RenderTestHarness(width = 64, height = 64)
 * harness.runOnMain {
 *     harness.scene.skybox = Skybox.Builder().color(1f, 0f, 0f, 1f).build(harness.engine)
 *     harness.renderFrames(3)
 *     val bitmap = harness.capturePixels()
 *     val centerColor = bitmap.getPixel(32, 32)
 *     // assert centerColor is red
 * }
 * harness.destroy()
 * ```
 */
class RenderTestHarness(
    val width: Int = 64,
    val height: Int = 64
) {
    lateinit var engine: Engine
        private set
    lateinit var renderer: Renderer
        private set
    lateinit var scene: Scene
        private set
    lateinit var view: View
        private set
    lateinit var swapChain: SwapChain
        private set

    private var destroyed = false

    init {
        runOnMain { setup() }
    }

    private fun setup() {
        val eglContext = createEglContext()
        engine = createEngine(eglContext)
        renderer = createRenderer(engine)
        scene = engine.createScene()
        view = createView(engine).apply {
            viewport = Viewport(0, 0, width, height)
            this.scene = this@RenderTestHarness.scene
        }
        // Headless swap chain (no window surface)
        swapChain = engine.createSwapChain(width, height, SwapChainFlags.CONFIG_DEFAULT)
    }

    /**
     * Renders [count] frames to allow Filament's pipeline to settle.
     *
     * Filament is a triple-buffered pipeline — rendering 3+ frames ensures
     * that geometry, materials and lights are fully resolved in the output.
     */
    fun renderFrames(count: Int = 3) {
        val frameTimeNanos = System.nanoTime()
        repeat(count) { i ->
            if (renderer.beginFrame(swapChain, frameTimeNanos + i * 16_666_667L)) {
                renderer.render(view)
                renderer.endFrame()
            }
        }
    }

    /**
     * Reads back the current framebuffer as an ARGB [Bitmap].
     *
     * Must be called after [renderFrames]. Blocks the calling thread until
     * the pixel data is available (typically < 100ms).
     *
     * @return a [Bitmap] of size [width]×[height] containing the rendered pixels.
     */
    fun capturePixels(): Bitmap {
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.nativeOrder())
        val latch = CountDownLatch(1)

        renderer.readPixels(
            0, 0, width, height,
            Texture.PixelBufferDescriptor(
                buffer,
                Texture.Format.RGBA,
                Texture.Type.UBYTE,
                1, 0, 0, 0,
                null, Runnable { latch.countDown() }
            )
        )

        // Render one more frame to flush the readPixels command
        renderFrames(1)

        check(latch.await(5, TimeUnit.SECONDS)) {
            "readPixels callback did not fire within 5 seconds"
        }

        // Convert RGBA byte buffer to Bitmap (flip vertically — OpenGL origin is bottom-left)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        val row = ByteArray(width * 4)
        for (y in height - 1 downTo 0) {
            buffer.get(row)
            for (x in 0 until width) {
                val offset = x * 4
                val r = row[offset].toInt() and 0xFF
                val g = row[offset + 1].toInt() and 0xFF
                val b = row[offset + 2].toInt() and 0xFF
                val a = row[offset + 3].toInt() and 0xFF
                bitmap.setPixel(x, y, Color.argb(a, r, g, b))
            }
        }
        return bitmap
    }

    /**
     * Destroys all Filament resources. Safe to call multiple times.
     */
    fun destroy() {
        if (destroyed) return
        destroyed = true
        runOnMain {
            engine.destroySwapChain(swapChain)
            engine.destroyView(view)
            engine.destroyScene(scene)
            engine.destroyRenderer(renderer)
            engine.safeDestroy()
        }
    }

    /**
     * Runs [block] on the main (instrumentation) thread and waits for completion.
     */
    fun runOnMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    companion object {
        /**
         * Checks whether two colors are within [tolerance] per channel (0–255 scale).
         *
         * @param actual   the rendered pixel color.
         * @param expected the expected color.
         * @param tolerance max allowed absolute difference per R/G/B/A channel.
         * @return true if all channels are within tolerance.
         */
        fun colorsMatch(actual: Int, expected: Int, tolerance: Int = 10): Boolean {
            return kotlin.math.abs(Color.red(actual) - Color.red(expected)) <= tolerance &&
                    kotlin.math.abs(Color.green(actual) - Color.green(expected)) <= tolerance &&
                    kotlin.math.abs(Color.blue(actual) - Color.blue(expected)) <= tolerance
        }
    }
}

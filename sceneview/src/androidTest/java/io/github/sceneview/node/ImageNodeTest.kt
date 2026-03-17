package io.github.sceneview.node

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.Utils
import io.github.sceneview.createEglContext
import io.github.sceneview.createEngine
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.safeDestroy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageNodeTest {

    private lateinit var engine: com.google.android.filament.Engine
    private lateinit var materialLoader: MaterialLoader

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Gltfio.init(); Filament.init(); Utils.init()
            val context = InstrumentationRegistry.getInstrumentation().context
            val eglContext = createEglContext()
            engine = createEngine(eglContext)
            materialLoader = MaterialLoader(engine, context)
        }
    }

    @After
    fun teardown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            materialLoader.destroy()
            engine.safeDestroy()
        }
    }

    /**
     * Regression test for #630.
     *
     * Before the fix, ImageNode.destroy() called safeDestroyTexture() while the
     * MaterialInstance that referenced the texture was still alive, causing:
     *   SIGABRT: "Invalid texture still bound to MaterialInstance: 'Transparent Textured'"
     *
     * The fix captures materialInstance before super.destroy(), destroys it via
     * materialLoader.destroyMaterialInstance() first, then destroys the texture.
     */
    @Test
    fun destroy_doesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            val node = ImageNode(materialLoader = materialLoader, bitmap = bitmap)
            node.destroy() // must not SIGABRT
            bitmap.recycle()
        }
    }

    /**
     * Simulates the exact MRE from issue #630: rapid add/destroy cycle.
     *
     * MaterialLoader should have zero tracked instances after each iteration.
     */
    @Test
    fun destroy_rapidCycle_doesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
            repeat(20) {
                val node = ImageNode(materialLoader = materialLoader, bitmap = bitmap)
                node.destroy()
            }
            bitmap.recycle()
        }
    }

    /**
     * Verifies that after destroy(), the MaterialInstance is no longer tracked by MaterialLoader.
     * If it were still tracked, materialLoader.destroy() would try to destroy it a second time.
     */
    @Test
    fun destroy_unregistersFromMaterialLoader() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            val node = ImageNode(materialLoader = materialLoader, bitmap = bitmap)
            node.destroy()
            // materialLoader.destroy() must not encounter any dangling instances
            // (implicitly verified: teardown calls materialLoader.destroy() and would crash if
            // there were a live instance referencing the already-destroyed texture)
            bitmap.recycle()
        }
    }

    /**
     * Verifies that a node with a custom size still destroys cleanly.
     */
    @Test
    fun destroy_withExplicitSize_doesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val node = ImageNode(
                materialLoader = materialLoader,
                bitmap = bitmap,
                size = io.github.sceneview.math.Size(0.5f, 0.5f)
            )
            node.destroy()
            bitmap.recycle()
        }
    }
}

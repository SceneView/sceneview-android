package io.github.sceneview.loaders

import android.os.Looper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.model.Model
import io.github.sceneview.node.FilamentWithoutJni
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.LooperMode
import java.io.File
import java.nio.Buffer
import java.util.Collections

/**
 * What a load does with the external resources of a `.gltf` (its `.bin` buffers, its textures),
 * and what `clear()` / `destroy()` may cancel.
 *
 * A real [ModelLoader] over a Filament whose natives return zero (Robolectric instruments
 * `com.google.android.filament`), except where the shadows below say otherwise: every parse
 * "succeeds" with asset handle [ASSET], which names one external resource, [RESOURCE].
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
    instrumentedPackages = ["com.google.android.filament"],
    shadows = [
        ModelLoaderResourceFailureTest.ParsingAssetLoader::class,
        ModelLoaderResourceFailureTest.OneResourceAsset::class,
    ],
)
class ModelLoaderResourceFailureTest {

    private lateinit var loader: ModelLoader

    @Before
    fun setUp() {
        destroyedAssets.clear()
        loader = ModelLoader(FilamentWithoutJni.engine(), RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        destroyedAssets.clear()
    }

    @Test
    fun `a resource next to a glTF at the root of assets resolves to its bare name`() {
        assertEquals("texture.png", ModelLoader.getFolderPath("chair.gltf", "texture.png"))
        assertEquals("models/texture.png", ModelLoader.getFolderPath("models/chair.gltf", "texture.png"))
        assertEquals(
            "file:///data/models/scene.bin",
            ModelLoader.getFolderPath("file:///data/models/scene.gltf", "scene.bin"),
        )
    }

    @Test
    fun `a missing external resource loads the model without it`() {
        // RESOURCE resolves next to the model file, where it does not exist.
        val result = await { loader.loadModelAsync(modelFileUri(), onResult = it) }

        assertTrue("onResult must be called", result.called)
        assertNotNull("A missing resource must not fail the whole load", result.value)
        assertEquals("The loaded model must stay alive", emptyList<Long>(), destroyedAssets.toList())
    }

    @Test
    fun `a load failing after the model was created destroys that model`() {
        val result = await {
            loader.loadModelAsync(
                modelFileUri(),
                resourceResolver = { error("No location for $it") },
                onResult = it,
            )
        }

        assertTrue("onResult must be called", result.called)
        assertNull(result.value)
        assertEquals(
            "The model created before the failure must not stay registered (a leak per retry)",
            listOf(ASSET),
            destroyedAssets.toList(),
        )
    }

    @Test
    fun `clear and destroy cancel the loader's loads, not the scope it was given`() {
        val callerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val unrelated = callerScope.launch { awaitCancellation() }
        try {
            val scoped = ModelLoader(FilamentWithoutJni.engine(), RuntimeEnvironment.getApplication(), callerScope)

            scoped.clear()
            assertTrue("clear() must not cancel the caller's other coroutines", unrelated.isActive)

            scoped.destroy()
            assertTrue("destroy() must not cancel the caller's other coroutines", unrelated.isActive)
        } finally {
            callerScope.cancel()
        }
    }

    /** Every parse succeeds, with handle [ASSET]; destroys are recorded. */
    @Implements(AssetLoader::class)
    @Suppress("UnusedParameter", "FunctionOnlyReturningConstant") // Robolectric shadows mirror native signatures
    class ParsingAssetLoader {
        companion object {
            @JvmStatic
            @Implementation
            fun nCreateAssetLoader(nativeEngine: Long, provider: Any?, nativeEntities: Long): Long = 1L

            @JvmStatic
            @Implementation
            fun nCreateAsset(nativeLoader: Long, buffer: Buffer, remaining: Int): Long = ASSET

            @JvmStatic
            @Implementation
            fun nDestroyAsset(nativeLoader: Long, nativeAsset: Long) {
                destroyedAssets += nativeAsset
            }
        }
    }

    /** Every asset names one external resource, [RESOURCE]. */
    @Implements(FilamentAsset::class)
    @Suppress("UnusedParameter", "FunctionOnlyReturningConstant") // Robolectric shadows mirror native signatures
    class OneResourceAsset {
        companion object {
            @JvmStatic
            @Implementation
            fun nGetResourceUriCount(nativeAsset: Long): Int = 1

            @JvmStatic
            @Implementation
            fun nGetResourceUris(nativeAsset: Long, result: Array<String?>) {
                result[0] = RESOURCE
            }
        }
    }

    private class Result(var called: Boolean = false, var value: Model? = null)

    /** Starts a load and pumps the main looper (where `onResult` is delivered) for up to 5 s. */
    private fun await(start: ((Model?) -> Unit) -> Unit): Result {
        val result = Result()
        start { value ->
            result.called = true
            result.value = value
        }
        val deadline = System.currentTimeMillis() + 5_000
        while (!result.called && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        return result
    }

    /** A model file that exists, alone in its folder: its resource [RESOURCE] does not. */
    private fun modelFileUri(): String {
        val folder = File.createTempFile("model", "").apply {
            delete()
            mkdirs()
            deleteOnExit()
        }
        val file = File(folder, "model.gltf").apply {
            writeBytes(ByteArray(64))
            deleteOnExit()
        }
        return "file://${file.absolutePath}"
    }

    private companion object {
        const val ASSET = 7L
        const val RESOURCE = "missing.bin"
        val destroyedAssets: MutableList<Long> = Collections.synchronizedList(mutableListOf<Long>())
    }
}

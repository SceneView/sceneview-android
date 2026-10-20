package io.github.sceneview.loaders

import android.os.Looper
import android.util.Log
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
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
import org.robolectric.shadows.ShadowLog
import java.io.File
import java.nio.Buffer
import java.util.Collections

/**
 * What a load does with the external resources of a `.gltf` (its `.bin` buffers, its textures),
 * and what `clear()` / `destroy()` may cancel.
 *
 * A real [ModelLoader] over a Filament whose natives return zero (Robolectric instruments
 * `com.google.android.filament`), except where the shadows below say otherwise: every parse
 * "succeeds" with asset handle [ASSET], which names the external resources [resourceUris], and
 * gltfio's `asyncBeginLoad` answers [beginAccepted].
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
    instrumentedPackages = ["com.google.android.filament"],
    shadows = [
        ModelLoaderResourceFailureTest.ParsingAssetLoader::class,
        ModelLoaderResourceFailureTest.ListedResourcesAsset::class,
        ModelLoaderResourceFailureTest.AnsweringResourceLoader::class,
    ],
)
class ModelLoaderResourceFailureTest {

    private lateinit var loader: ModelLoader

    @Before
    fun setUp() {
        destroyedAssets.clear()
        resourceUris = arrayOf(TEXTURE)
        beginAccepted = true
        ShadowLog.clear()
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
    fun `a missing texture loads the model without it, and says so`() {
        // TEXTURE resolves next to the model file, where it does not exist.
        val result = await { loader.loadModelAsync(modelFileUri(), onResult = it) }

        assertTrue("onResult must be called", result.called)
        assertNotNull("A missing texture must not fail the whole load", result.value)
        assertEquals("The loaded model must stay alive", emptyList<Long>(), destroyedAssets.toList())
        assertWarned(TEXTURE, "model.gltf", "loading the model without it")
    }

    @Test
    fun `a missing buffer fails the load and destroys the model`() {
        resourceUris = arrayOf(BUFFER, TEXTURE)

        val result = await { loader.loadModelAsync(modelFileUri(), onResult = it) }

        assertTrue("onResult must be called", result.called)
        assertNull("A model without its geometry buffer must not be handed out", result.value)
        assertEquals("The model created before the miss must be destroyed", listOf(ASSET), destroyedAssets.toList())
        assertWarned(BUFFER, "model.gltf", "the model was destroyed")
    }

    @Test
    fun `a present buffer loads, even when a texture is missing`() {
        resourceUris = arrayOf(BUFFER, TEXTURE)

        val result = await { loader.loadModelAsync(modelFileUri(withBuffer = true), onResult = it) }

        assertTrue("onResult must be called", result.called)
        assertNotNull(result.value)
        assertEquals(emptyList<Long>(), destroyedAssets.toList())
    }

    @Test
    fun `a begin gltfio refuses fails the load and destroys the model`() {
        resourceUris = emptyArray()
        beginAccepted = false

        val result = await { loader.loadModelAsync(modelFileUri(), onResult = it) }

        assertTrue("onResult must be called", result.called)
        assertNull(result.value)
        assertEquals(listOf(ASSET), destroyedAssets.toList())
        assertWarned("refused", "model.gltf")
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

    /** Every asset names the external resources [resourceUris]. */
    @Implements(FilamentAsset::class)
    @Suppress("UnusedParameter") // Robolectric shadows mirror native signatures
    class ListedResourcesAsset {
        companion object {
            @JvmStatic
            @Implementation
            fun nGetResourceUriCount(nativeAsset: Long): Int = resourceUris.size

            @JvmStatic
            @Implementation
            fun nGetResourceUris(nativeAsset: Long, result: Array<String?>) {
                resourceUris.copyInto(result)
            }
        }
    }

    /** gltfio's `asyncBeginLoad` answers [beginAccepted]. */
    @Implements(ResourceLoader::class)
    @Suppress("UnusedParameter") // Robolectric shadows mirror native signatures
    class AnsweringResourceLoader {
        companion object {
            @JvmStatic
            @Implementation
            fun nAsyncBeginLoad(nativeLoader: Long, nativeAsset: Long): Boolean = beginAccepted
        }
    }

    /** Some `Log.w` under the `ModelLoader` tag mentions every one of [fragments]. */
    private fun assertWarned(vararg fragments: String) {
        val warnings = ShadowLog.getLogsForTag("ModelLoader").filter { it.type == Log.WARN }
        assertTrue(
            "Expected a ModelLoader warning naming ${fragments.toList()}, got ${warnings.map { it.msg }}",
            warnings.any { log -> fragments.all { it in log.msg } },
        )
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

    /**
     * A `model.gltf` that declares [BUFFER] as its buffer and [TEXTURE] as its image. Alone in its
     * folder, so neither resource exists next to it — except [BUFFER] when [withBuffer].
     */
    private fun modelFileUri(withBuffer: Boolean = false): String {
        val folder = File.createTempFile("model", "").apply {
            delete()
            mkdirs()
            deleteOnExit()
        }
        val file = File(folder, "model.gltf").apply {
            writeText(
                """{"asset":{"version":"2.0"},"buffers":[{"uri":"$BUFFER","byteLength":4}],""" +
                    """"images":[{"uri":"$TEXTURE"}]}""",
            )
            deleteOnExit()
        }
        if (withBuffer) File(folder, BUFFER).apply { writeBytes(ByteArray(4)) }.deleteOnExit()
        return "file://${file.absolutePath}"
    }

    private companion object {
        const val ASSET = 7L
        const val BUFFER = "scene.bin"
        const val TEXTURE = "missing.png"

        @Volatile
        var resourceUris: Array<String> = arrayOf(TEXTURE)

        @Volatile
        var beginAccepted = true

        val destroyedAssets: MutableList<Long> = Collections.synchronizedList(mutableListOf<Long>())
    }
}

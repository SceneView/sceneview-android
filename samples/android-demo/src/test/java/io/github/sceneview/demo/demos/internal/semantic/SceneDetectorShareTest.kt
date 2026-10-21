package io.github.sceneview.demo.demos.internal.semantic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * The detector is shared by every replay of the process: the first one that needs it fetches it,
 * the others wait for it and show its progress meanwhile. A download cut short — no network, the
 * replay left — resumes where it stopped; a file that fails its check is not fetched again; one
 * that ONNX Runtime refuses is fetched once more, never in a loop. On a metered network nothing is
 * fetched until the user asks, as for the HD pack.
 *
 * Every download goes to a local fake server, into the app's own storage.
 */
@RunWith(RobolectricTestRunner::class)
class SceneDetectorShareTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dir = File(context.filesDir, "scene-detector")

    private val payload = ByteArray(300_000) { (it * 31 % 251).toByte() }
    private val sha = sha256(payload)

    /** The real entry, shrunk to a payload the fake server can serve. */
    private val asset = SceneModel.ASSET.copy(file = sha, sha256 = sha, bytes = payload.size.toLong())

    private val part get() = File(dir, "$sha.part")

    @Before
    fun emptyStorage() {
        dir.deleteRecursively()
    }

    @Test
    fun `offline, a later replay resumes the download where it stopped`() = runBlocking {
        MockWebServer().use { server ->
            // The connection drops halfway through the body.
            server.enqueue(ok(payload).newBuilder().onResponseBody(SocketEffect.CloseSocket()).build())
            server.start()
            val detectors = share(server)
            val states = ArrayList<SceneModelState>()

            assertNull(detectors.share { states += it })

            assertEquals(SceneModelState.Offline, states.last())
            val kept = part.length().toInt()
            assertTrue("kept $kept B of ${payload.size}", kept in 1 until payload.size)
            assertNull(server.takeRequest(5, TimeUnit.SECONDS)!!.headers["Range"])

            server.enqueue(rest(kept))
            val detector = detectors.share()

            assertEquals("bytes=$kept-", server.takeRequest(5, TimeUnit.SECONDS)!!.headers["Range"])
            assertEquals(FakeDetector.name, detector!!.name)
            assertEquals(listOf(sha), dir.list()!!.toList())
        }
    }

    @Test
    fun `a file that fails its check is not fetched again`() = runBlocking {
        MockWebServer().use { server ->
            val wrong = payload.copyOf().also { it[0] = (it[0] + 1).toByte() }
            server.enqueue(ok(wrong))
            server.enqueue(ok(wrong))
            server.start()
            val detectors = share(server)
            val states = ArrayList<SceneModelState>()

            assertNull(detectors.share { states += it })
            assertEquals(SceneModelState.Unusable, states.last())

            val again = ArrayList<SceneModelState>()
            assertNull(detectors.share { again += it })

            assertEquals(listOf<SceneModelState>(SceneModelState.Unusable), again)
            assertEquals(1, server.requestCount)
            assertFalse("a failed file was kept", part.exists())
        }
    }

    @Test
    fun `leaving the replay mid-download keeps what arrived, and the next replay resumes it`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(slow(payload))
            server.start()
            val detectors = share(server)

            val replay = launch(Dispatchers.Default) { detectors.share() }
            until("a part of the file") { part.length() > 0 }
            replay.cancelAndJoin()

            val kept = part.length().toInt()
            assertTrue("kept $kept B of ${payload.size}", kept in 1 until payload.size)
            server.takeRequest(5, TimeUnit.SECONDS)

            server.enqueue(rest(kept))
            assertNotNull(detectors.share())
            assertEquals("bytes=$kept-", server.takeRequest(5, TimeUnit.SECONDS)!!.headers["Range"])
        }
    }

    @Test
    fun `a second replay opened during the download shows its progress`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(slow(payload))
            server.start()
            val detectors = share(server)

            val first = async(Dispatchers.Default) { detectors.share() }
            until("the first replay's download") { part.length() > 0 }
            val states = Collections.synchronizedList(ArrayList<SceneModelState>())
            val second = async(Dispatchers.Default) { detectors.share { states += it } }
            until("the second replay's progress") { states.any { it is SceneModelState.Downloading } }

            assertFalse("the progress came after the download", first.isCompleted)
            assertNotNull(first.await())
            assertNotNull(second.await())
            assertEquals(SceneModelState.Ready, states.last())
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun `a model ONNX Runtime refuses is fetched once more, then given up`() = runBlocking {
        MockWebServer().use { server ->
            repeat(3) { server.enqueue(ok(payload)) }
            server.start()
            var opened = 0
            val detectors = share(server, opens = { opened++; null })
            val states = ArrayList<SceneModelState>()

            assertNull(detectors.share { states += it })

            assertEquals(2, opened)
            assertEquals(2, server.requestCount)
            assertEquals(SceneModelState.Unusable, states.last())

            assertNull(detectors.share())
            assertEquals(2, opened)
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun `a model refused once loads from its second download`() = runBlocking {
        MockWebServer().use { server ->
            repeat(2) { server.enqueue(ok(payload)) }
            server.start()
            var opened = 0
            val detectors = share(server, opens = { if (opened++ == 0) null else FakeDetector })

            assertNotNull(detectors.share())
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun `on a metered network nothing is fetched before the tap`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(ok(payload))
            server.start()
            val detectors = share(server, metered = true)
            val states = Collections.synchronizedList(ArrayList<SceneModelState>())

            val replay = async(Dispatchers.Default) { detectors.share { states += it } }
            until("the download card") { SceneModelState.Metered in states }
            delay(200)

            assertEquals(0, server.requestCount)
            assertFalse(replay.isCompleted)

            detectors.allowMetered()

            assertNotNull(replay.await())
            assertEquals(1, server.requestCount)
            assertEquals(SceneModelState.Ready, states.last())

            // Once on the device it is used on any network, the next launch of the app included.
            val nextLaunch = ArrayList<SceneModelState>()
            assertNotNull(share(server, metered = true).share { nextLaunch += it })
            assertEquals(listOf<SceneModelState>(SceneModelState.Ready), nextLaunch)
            assertEquals(1, server.requestCount)
        }
    }

    private fun share(
        server: MockWebServer,
        metered: Boolean = false,
        opens: (File) -> SceneDetector? = { FakeDetector },
    ) = SceneDetectorShare(
        storeOf = { SceneModelStore(dir, OkHttpClient(), server.url("/").toString().trimEnd('/'), asset) },
        metered = { metered },
        open = { file -> opens(file) },
    )

    private fun ok(body: ByteArray) = MockResponse.Builder().code(200).body(Buffer().write(body)).build()

    /** The whole file, trickling: long enough to leave, or to open a second replay, mid-download. */
    private fun slow(body: ByteArray) = ok(body).newBuilder().throttleBody(8_192, 50, TimeUnit.MILLISECONDS).build()

    /** What is left of the file from [from] on, as a server that honours the resume answers. */
    private fun rest(from: Int) =
        MockResponse.Builder().code(206).body(Buffer().write(payload, from, payload.size - from)).build()

    private suspend fun until(what: String, condition: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!condition()) {
            assertTrue("still waiting for $what", System.nanoTime() < end)
            delay(5)
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private object FakeDetector : SceneDetector {
        override val name = "fake"
        override fun detect(image: SceneImage) = emptyList<Detection2D>()
    }
}

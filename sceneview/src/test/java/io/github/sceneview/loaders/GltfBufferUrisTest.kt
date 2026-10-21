package io.github.sceneview.loaders

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Which of a model's external resources are buffers, the ones a load cannot go on without. */
@RunWith(RobolectricTestRunner::class) // org.json is an Android API
class GltfBufferUrisTest {

    private val json =
        """{"buffers":[{"uri":"scene.bin"},{"uri":"data:application/octet-stream;base64,AAAA"}],""" +
            """"images":[{"uri":"albedo.png"},{"uri":"normal.bin.png"}]}"""

    @Test
    fun `a glTF's buffers are its declared buffer uris, not its images`() {
        val uris = listOf("scene.bin", "albedo.png", "normal.bin.png")

        assertEquals(setOf("scene.bin"), GltfBufferUris.of(ByteBuffer.wrap(json.toByteArray()), uris))
    }

    @Test
    fun `a GLB's buffers come from its JSON chunk`() {
        assertEquals(setOf("scene.bin"), GltfBufferUris.of(glb(json), listOf("scene.bin", "albedo.png")))
    }

    @Test
    fun `data uris are never buffers to fetch`() {
        val dataUri = "data:application/octet-stream;base64,AAAA"

        assertEquals(emptySet<String>(), GltfBufferUris.of(ByteBuffer.wrap(json.toByteArray()), listOf(dataUri)))
    }

    @Test
    fun `an unreadable payload falls back to the bin suffix`() {
        val garbage = ByteBuffer.wrap(byteArrayOf(1, 2, 3))

        assertEquals(setOf("a.BIN"), GltfBufferUris.of(garbage, listOf("a.BIN", "b.png")))
    }

    private fun glb(json: String): ByteBuffer {
        val padded = json.toByteArray().let { it + ByteArray((4 - it.size % 4) % 4) { ' '.code.toByte() } }
        return ByteBuffer.allocate(GLB_HEADER + CHUNK_HEADER + padded.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(0x46546C67)
            putInt(2)
            putInt(capacity())
            putInt(padded.size)
            putInt(0x4E4F534A)
            put(padded)
            flip()
        }
    }

    private companion object {
        const val GLB_HEADER = 12
        const val CHUNK_HEADER = 8
    }
}

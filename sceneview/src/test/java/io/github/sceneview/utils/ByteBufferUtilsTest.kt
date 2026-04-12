package io.github.sceneview.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Tests for [ByteBuffer.clone] and [FloatBuffer.clone] extension functions.
 *
 * Both extensions are pure JVM NIO operations with no Android or Filament dependencies.
 */
class ByteBufferUtilsTest {

    // ── ByteBuffer.clone ─────────────────────────────────────────────────────────

    @Test
    fun `ByteBuffer clone produces a different object`() {
        val original = ByteBuffer.allocate(4).apply { put(byteArrayOf(1, 2, 3, 4)); rewind() }
        val cloned = original.clone()
        assertNotSame(original, cloned)
    }

    @Test
    fun `ByteBuffer clone contains the same bytes`() {
        val data = byteArrayOf(10, 20, 30, 40)
        val original = ByteBuffer.wrap(data)
        val cloned = original.clone()

        val clonedBytes = ByteArray(cloned.remaining())
        cloned.get(clonedBytes)
        assertArrayEquals(data, clonedBytes)
    }

    @Test
    fun `ByteBuffer clone is readable from the beginning`() {
        val data = byteArrayOf(5, 6, 7, 8)
        val original = ByteBuffer.wrap(data)
        // Advance position in original
        original.get()
        original.get()

        val cloned = original.clone()
        // Clone should still be readable from position 0
        assertEquals(4, cloned.remaining())
        assertEquals(5.toByte(), cloned.get())
    }

    @Test
    fun `ByteBuffer clone does not share backing array`() {
        val data = byteArrayOf(1, 2, 3, 4)
        val original = ByteBuffer.allocate(4).apply { put(data); rewind() }
        val cloned = original.clone()

        // Mutate original — clone must not change
        original.put(0, 99.toByte())
        assertEquals(1.toByte(), cloned.get(0))
    }

    @Test
    fun `ByteBuffer clone of empty buffer`() {
        val original = ByteBuffer.allocate(0)
        val cloned = original.clone()
        assertEquals(0, cloned.remaining())
    }

    @Test
    fun `ByteBuffer clone uses native byte order`() {
        val original = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        val cloned = original.clone()
        assertEquals(ByteOrder.nativeOrder(), cloned.order())
    }

    @Test
    fun `ByteBuffer clone capacity matches original`() {
        val original = ByteBuffer.allocate(16).apply { put(ByteArray(16)); rewind() }
        val cloned = original.clone()
        assertEquals(original.capacity(), cloned.capacity())
    }

    // ── FloatBuffer.clone ────────────────────────────────────────────────────────

    @Test
    fun `FloatBuffer clone produces a different object`() {
        val original = FloatBuffer.wrap(floatArrayOf(1f, 2f, 3f))
        val cloned = original.clone()
        assertNotSame(original, cloned)
    }

    @Test
    fun `FloatBuffer clone contains the same floats`() {
        val data = floatArrayOf(1.0f, 2.5f, 3.14f, -1.0f)
        val original = FloatBuffer.wrap(data)
        val cloned = original.clone()

        val clonedFloats = FloatArray(cloned.remaining())
        cloned.get(clonedFloats)
        assertArrayEquals(data, clonedFloats, 0.0f)
    }

    @Test
    fun `FloatBuffer clone is readable from the beginning`() {
        val data = floatArrayOf(10f, 20f, 30f, 40f)
        val original = FloatBuffer.wrap(data)
        // Advance position
        original.get()
        original.get()

        val cloned = original.clone()
        assertEquals(4, cloned.remaining())
        assertEquals(10f, cloned.get(), 0.0f)
    }

    @Test
    fun `FloatBuffer clone does not share backing array`() {
        val fb = FloatBuffer.allocate(4).apply {
            put(floatArrayOf(1f, 2f, 3f, 4f))
            rewind()
        }
        val cloned = fb.clone()

        // Mutate original — clone must not change
        fb.put(0, 99f)
        assertEquals(1f, cloned.get(0), 0.0f)
    }

    @Test
    fun `FloatBuffer clone of empty buffer`() {
        val original = FloatBuffer.allocate(0)
        val cloned = original.clone()
        assertEquals(0, cloned.remaining())
    }

    @Test
    fun `FloatBuffer clone capacity matches original`() {
        val data = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        val original = FloatBuffer.wrap(data)
        val cloned = original.clone()
        assertEquals(original.capacity(), cloned.capacity())
    }

    @Test
    fun `FloatBuffer clone single element`() {
        val original = FloatBuffer.wrap(floatArrayOf(42f))
        val cloned = original.clone()
        assertEquals(42f, cloned.get(), 0.0f)
    }

    // ── InputStream.toByteArray ──────────────────────────────────────────────────

    @Test
    fun `InputStream toByteArray reads all bytes`() {
        val data = byteArrayOf(11, 22, 33, 44, 55)
        val stream = data.inputStream()
        val result = stream.toByteArray()
        assertArrayEquals(data, result)
    }

    @Test
    fun `InputStream toByteArray closes stream after reading`() {
        val data = byteArrayOf(1, 2, 3)
        val stream = data.inputStream()
        stream.toByteArray()
        // After toByteArray the stream is closed; reading again returns -1
        assertEquals(-1, stream.read())
    }

    @Test
    fun `InputStream toByteArray handles empty stream`() {
        val stream = ByteArray(0).inputStream()
        val result = stream.toByteArray()
        assertEquals(0, result.size)
    }
}

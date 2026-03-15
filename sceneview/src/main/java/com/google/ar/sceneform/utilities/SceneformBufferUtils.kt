package com.google.ar.sceneform.utilities

import android.content.res.AssetManager
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.CompletionException

/**
 * A simple class to read InputStreams Once the data is read it can be accessed as a ByteBuffer.
 *
 * @hide
 */
object SceneformBufferUtils {
    private val TAG = SceneformBufferUtils::class.java.simpleName
    private const val DEFAULT_BLOCK_SIZE = 8192

    @JvmStatic
    fun readFile(assets: AssetManager, path: String): ByteBuffer? {
        // TODO: this method/class may be replaceable by SourceBytes
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open(path)
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to read file $path - ${ex.message}")
            return null
        }

        val buffer = readStream(inputStream)

        if (inputStream != null) {
            try {
                inputStream.close()
            } catch (ex: IOException) {
                Log.e(TAG, "Failed to close the input stream from file $path - ${ex.message}")
            }
        }

        return buffer
    }

    @JvmStatic
    fun readStream(inputStream: InputStream?): ByteBuffer? {
        // TODO: this method/class may be replaceable by SourceBytes
        if (inputStream == null) {
            return null
        }

        return try {
            // Try to read the data from the inputStream
            val bytes = inputStreamToByteArray(inputStream)
            ByteBuffer.wrap(bytes)
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to read stream - ${ex.message}")
            null
        }
    }

    @Throws(IOException::class)
    private fun copy(`in`: InputStream, out: OutputStream): Int {
        // TODO: this method/class may be replaceable by SourceBytes
        val buffer = ByteArray(DEFAULT_BLOCK_SIZE)
        var size = 0
        var n: Int
        while (`in`.read(buffer).also { n = it } > 0) {
            size += n
            out.write(buffer, 0, n)
        }
        out.flush()
        return size
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyByteBufferToArray(`in`: ByteBuffer): ByteArray {
        // TODO: this method/class may be replaceable by SourceBytes
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BLOCK_SIZE)
        val end = `in`.limit()
        var bytesRead: Int
        while (`in`.position() < end) {
            val lastPosition = `in`.position()

            `in`.get(buffer, 0, Math.min(DEFAULT_BLOCK_SIZE, end - lastPosition))
            bytesRead = `in`.position() - lastPosition

            out.write(buffer, 0, bytesRead)
        }
        out.flush()
        return out.toByteArray()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyByteBuffer(`in`: ByteBuffer): ByteBuffer {
        return ByteBuffer.wrap(copyByteBufferToArray(`in`))
    }

    @JvmStatic
    fun inputStreamToByteBuffer(inputStreamCreator: Callable<InputStream>): ByteBuffer {
        val result: ByteBuffer?
        try {
            inputStreamCreator.call().use { inputStream ->
                result = SceneformBufferUtils.readStream(inputStream)
            }
        } catch (e: Exception) {
            throw CompletionException(e)
        }
        if (result == null) {
            throw AssertionError("Failed reading data from stream")
        }
        return result
    }

    @JvmStatic
    @Throws(Exception::class)
    fun inputStreamCallableToByteArray(inputStreamCreator: Callable<InputStream>): ByteArray {
        inputStreamCreator.call().use { input ->
            return inputStreamToByteArray(input)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun inputStreamToByteArray(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        copy(input, output)
        return output.toByteArray()
    }
}

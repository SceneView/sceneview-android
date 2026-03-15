package com.google.ar.sceneform.rendering

import android.content.Context
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels

/** Helper class for loading resources in filament. */
internal object ResourceHelper {
    @JvmStatic
    fun readResource(context: Context?, resourceId: Int): ByteBuffer? {
        var buffer: ByteBuffer? = null
        if (context != null) {
            var length = 0
            try {
                val inputStream = context.resources.openRawResource(resourceId)
                // to get the length for use in 'allocateDirect'
                inputStream.mark(-1) // no read limit
                while (inputStream.read() != -1) {
                    length++
                }
                // reset stream to beginning
                inputStream.reset()

                buffer = ByteBuffer.allocateDirect(length)
                val source = Channels.newChannel(inputStream)
                try {
                    source.read(buffer)
                } finally {
                    source.close()
                }
                buffer.rewind()
            } catch (exception: IOException) {
                exception.printStackTrace()
                buffer = null
            }
        }
        return buffer
    }
}

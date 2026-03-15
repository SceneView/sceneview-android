package com.google.ar.sceneform.utilities

import java.nio.ByteBuffer

class BufferHelper {
    companion object {
        /**
         * ```
         * Create a new independent ByteBuffer from a
         * source ByteBuffer.
         * ```
         *
         * @param original [ByteBuffer]
         * @return [ByteBuffer]
         */
        @JvmStatic
        fun cloneByteBuffer(original: ByteBuffer): ByteBuffer {
            val clone = ByteBuffer.allocate(original.capacity())
            original.rewind() //copy from the beginning
            clone.put(original)
            original.rewind()
            clone.flip()
            return clone
        }
    }
}

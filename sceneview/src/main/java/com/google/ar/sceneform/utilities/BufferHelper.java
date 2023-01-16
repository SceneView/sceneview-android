package com.google.ar.sceneform.utilities;

import java.nio.ByteBuffer;

public class BufferHelper {
    /**
     * <pre>
     *     Create a new independent ByteBuffer from a
     *     source ByteBuffer.
     * </pre>
     *
     * @param original {@link ByteBuffer}
     * @return {@link ByteBuffer}
     */
    public static ByteBuffer cloneByteBuffer(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();//copy from the beginning
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }
}

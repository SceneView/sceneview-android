package com.google.ar.sceneform.rendering;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/** Helper class for loading resources in filament. */
class ResourceHelper {
  static ByteBuffer readResource(Context context, int resourceId) {
    ByteBuffer buffer = null;
    if (context != null) {
      int length = 0;
      try {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        // to get the length for use in 'allocateDirect'
        inputStream.mark(-1); // no read limit
        while (inputStream.read() != -1) {
          length++;
        }
        // reset stream to beginning
        inputStream.reset();

        buffer = ByteBuffer.allocateDirect(length);
        final ReadableByteChannel source = Channels.newChannel(inputStream);
        try {
          source.read(buffer);
        } finally {
          source.close();
        }
        buffer.rewind();
      } catch (IOException exception) {
        exception.printStackTrace();
        buffer = null;
      }
    }

    return buffer;
  }
}

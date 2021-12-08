package com.google.ar.sceneform.utilities;

import android.content.res.AssetManager;
import androidx.annotation.Nullable;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;

/**
 * A simple class to read InputStreams Once the data is read it can be accessed as a ByteBuffer.
 *
 * @hide
 */
public final class SceneformBufferUtils {
  private static final String TAG = SceneformBufferUtils.class.getSimpleName();
  private static final int DEFAULT_BLOCK_SIZE = 8192;

  private SceneformBufferUtils() {}

  @Nullable
  public static ByteBuffer readFile(AssetManager assets, String path) {
    // TODO: this method/class may be replaceable by SourceBytes
    InputStream inputStream = null;
    try {
      inputStream = assets.open(path);
    } catch (IOException ex) {
      Log.e(TAG, "Failed to read file " + path + " - " + ex.getMessage());
      return null;
    }

    ByteBuffer buffer = readStream(inputStream);

    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException ex) {
        Log.e(TAG, "Failed to close the input stream from file " + path + " - " + ex.getMessage());
      }
    }

    return buffer;
  }

  @Nullable
  public static ByteBuffer readStream(@Nullable InputStream inputStream) {
    // TODO: this method/class may be replaceable by SourceBytes
    ByteBuffer buffer = null;
    if (inputStream == null) {
      return buffer;
    }

    try {
      // Try to read the data from the inputStream
      byte[] bytes = inputStreamToByteArray(inputStream);
      buffer = ByteBuffer.wrap(bytes);
    } catch (IOException ex) {
      Log.e(TAG, "Failed to read stream - " + ex.getMessage());
    }

    return buffer;
  }

  private static int copy(InputStream in, OutputStream out) throws IOException {
    // TODO: this method/class may be replaceable by SourceBytes
    byte[] buffer = new byte[DEFAULT_BLOCK_SIZE];
    int size = 0;
    int n;
    while ((n = in.read(buffer)) > 0) {
      size += n;
      out.write(buffer, 0, n);
    }
    out.flush();
    return size;
  }

  public static byte[] copyByteBufferToArray(ByteBuffer in) throws IOException {
    // TODO: this method/class may be replaceable by SourceBytes
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[DEFAULT_BLOCK_SIZE];
    int end = in.limit();
    int bytesRead;
    while (in.position() < end) {
      int lastPosition = in.position();

      in.get(buffer, 0, Math.min(DEFAULT_BLOCK_SIZE, end - lastPosition));
      bytesRead = in.position() - lastPosition;

      out.write(buffer, 0, bytesRead);
    }
    out.flush();
    return out.toByteArray();
  }

  public static ByteBuffer copyByteBuffer(ByteBuffer in) throws IOException {
    return ByteBuffer.wrap(copyByteBufferToArray(in));
  }

  public static ByteBuffer inputStreamToByteBuffer(Callable<InputStream> inputStreamCreator) {
    ByteBuffer result;
    try (InputStream inputStream = inputStreamCreator.call()) {
      result = SceneformBufferUtils.readStream(inputStream);
    } catch (Exception e) {
      throw new CompletionException(e);
    }
    if (result == null) {
      throw new AssertionError("Failed reading data from stream");
    }
    return result;
  }

  public static byte[] inputStreamCallableToByteArray(Callable<InputStream> inputStreamCreator)
      throws Exception {
    try (InputStream input = inputStreamCreator.call()) {
      return inputStreamToByteArray(input);
    } finally {
      // Propagate exceptions up.
    }
  }

  public static byte[] inputStreamToByteArray(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    copy(input, output);
    return output.toByteArray();
  }
}

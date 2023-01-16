package com.google.ar.sceneform.rendering;

import android.util.Log;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Prints error messages if needed. */
@SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"}) // CompletableFuture
class FutureHelper {
  private  FutureHelper() {}


  /**
   * Outputs a log message if input completes exceptionally.
   *
   * <p>Does not remove the exception from input. If some later handler is able to do more with the
   * exception it is still possible.
   *
   * @param tag tag for the log message.
   * @param input A completable future that may have failed.
   * @param errorMsg Message to print along with the exception.
   * @return input so that the function may be chained.
   */
  static <T> CompletableFuture<T> logOnException(
      final String tag, final CompletableFuture<T> input, final String errorMsg) {
    input.exceptionally(
        throwable -> {
          Log.e(tag, errorMsg, throwable);
          throw new CompletionException(throwable);
        });
    return input;
  }
}

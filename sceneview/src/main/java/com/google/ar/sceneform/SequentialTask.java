package com.google.ar.sceneform;

import android.annotation.TargetApi;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Executes multiple {@link Runnable}s sequentially by appending them to a {@link
 * CompletableFuture}.
 *
 * <p>This should only be modified on the main thread.
 */
@TargetApi(24)
@SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
class SequentialTask {
  @Nullable private CompletableFuture<Void> future;

  /**
   * Appends a new Runnable to the current future, or creates a new one.
   *
   * @return The current future.
   */
  @MainThread
  public CompletableFuture<Void> appendRunnable(Runnable action, Executor executor) {
    if (future != null && !future.isDone()) {
      future = future.thenRunAsync(action, executor);
    } else {
      future = CompletableFuture.runAsync(action, executor);
    }
    return future;
  }

  /** True if the future is null or done. */
  @MainThread
  public boolean isDone() {
    if (future == null) {
      return true;
    }
    if (future.isDone()) {
      future = null;
      return true;
    }
    return false;
  }
}

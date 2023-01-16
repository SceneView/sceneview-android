package com.google.ar.sceneform.rendering;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;

/**
 * Provides access to default {@link Executor}s to be used
 *
 * @hide
 */
public class ThreadPools {
  private static Executor mainExecutor;
  private static Executor threadPoolExecutor;

  private ThreadPools() {}

  /** {@link Executor} for anything that that touches {@link Renderer} state */
  public static Executor getMainExecutor() {
    if (mainExecutor == null) {
      mainExecutor =
          new Executor() {
            private final Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void execute(Runnable runnable) {
              handler.post(runnable);
            }
          };
    }
    return mainExecutor;
  }

  /** @param executor provides access to the main thread. */
  public static void setMainExecutor(Executor executor) {
    mainExecutor = executor;
  }

  /** Default background {@link Executor} for async operations including file reading. */
  public static Executor getThreadPoolExecutor() {
    if (threadPoolExecutor == null) {
      return AsyncTask.THREAD_POOL_EXECUTOR;
    }
    return threadPoolExecutor;
  }

  /**
   * Sets the default background {@link Executor}.
   *
   * <p>Tasks may be long running. This should not include the main thread
   */
  public static void setThreadPoolExecutor(Executor executor) {
    threadPoolExecutor = executor;
  }
}

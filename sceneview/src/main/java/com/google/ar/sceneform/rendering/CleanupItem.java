package com.google.ar.sceneform.rendering;

/**
 * Runs a {@link Runnable} when a registered object is destroyed.
 *
 * <p>For each object of type {@code T} registered, a {@link CleanupItem} will be created. The
 * registered object's lifecycle will be tracked and when it is disposed the given {@link Runnable}
 * will be run.
 */
public class CleanupItem<T> extends java.lang.ref.PhantomReference<T> {
  private final Runnable cleanupCallback;

  /**
   * @param trackedObject The object to be tracked until garbage collection
   * @param referenceQueue The getFilamentEngine reference tracking mechanism
   * @param cleanupCallback {@link Runnable} to be called once {@code trackedObject} is disposed.
   */
  CleanupItem(
      T trackedObject, java.lang.ref.ReferenceQueue<T> referenceQueue, Runnable cleanupCallback) {
    super(trackedObject, referenceQueue);
    this.cleanupCallback = cleanupCallback;
  }

  /** Executes the {@link Runnable}. */
  void run() {
    cleanupCallback.run();
  }
}

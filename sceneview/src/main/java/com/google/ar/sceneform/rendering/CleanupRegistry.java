package com.google.ar.sceneform.rendering;

import com.google.ar.sceneform.resources.ResourceHolder;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Maintains a {@link ReferenceQueue} and executes a {@link Runnable} after each object in the queue
 * is garbage collected.
 */
public class CleanupRegistry<T> implements ResourceHolder {

  private final HashSet<CleanupItem<T>> cleanupItemHashSet;
  private final ReferenceQueue<T> referenceQueue;

  public CleanupRegistry() {
    this(new HashSet<>(), new ReferenceQueue<>());
  }

  public CleanupRegistry(
      HashSet<CleanupItem<T>> cleanupItemHashSet, ReferenceQueue<T> referenceQueue) {
    this.cleanupItemHashSet = cleanupItemHashSet;
    this.referenceQueue = referenceQueue;
  }

  /**
   * Adds {@code trackedOBject} to the {@link ReferenceQueue}.
   *
   * @param trackedObject The target to be tracked.
   * @param cleanupCallback Will be called after {@code trackedOBject} is disposed.
   */
  public void register(T trackedObject, Runnable cleanupCallback) {
    cleanupItemHashSet.add(new CleanupItem<T>(trackedObject, referenceQueue, cleanupCallback));
  }

  /**
   * Polls the {@link ReferenceQueue} for garbage collected objects and runs the associated {@link
   * Runnable}
   *
   * @return count of resources remaining.
   */
  @Override
  @SuppressWarnings("unchecked") // safe cast from Reference to a CleanupItem
  public long reclaimReleasedResources() {
    CleanupItem<T> ref = (CleanupItem<T>) referenceQueue.poll();
    while (ref != null) {
      if (cleanupItemHashSet.contains(ref)) {
        ref.run();
        cleanupItemHashSet.remove(ref);
      }
      ref = (CleanupItem<T>) referenceQueue.poll();
    }
    return cleanupItemHashSet.size();
  }

  /** Ignores reference count and releases any associated resources */
  @Override
  public void destroyAllResources() {
    Iterator<CleanupItem<T>> iterator = cleanupItemHashSet.iterator();
    while (iterator.hasNext()) {
      CleanupItem<T> ref = iterator.next();
      iterator.remove();
      ref.run();
    }
  }
}

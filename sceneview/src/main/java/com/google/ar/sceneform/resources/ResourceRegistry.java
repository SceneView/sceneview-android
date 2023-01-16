package com.google.ar.sceneform.resources;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import com.google.ar.sceneform.utilities.Preconditions;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ResourceRegistry keeps track of resources that have been loaded and are in the process of being
 * loaded. The registry maintains only weak references and doesn't prevent resources from being
 * collected.
 *
 * @hide
 */
// TODO: Automatically prune dead WeakReferences from ResourceRegistry when the
// ResourceRegistry becomes large.
public class ResourceRegistry<T> implements ResourceHolder {
  private static final String TAG = ResourceRegistry.class.getSimpleName();

  private final Object lock = new Object();

  @GuardedBy("lock")
  private final Map<Object, WeakReference<T>> registry = new HashMap<>();

  @GuardedBy("lock")
  private final Map<Object, CompletableFuture<T>> futureRegistry = new HashMap<>();

  /**
   * Returns a future to a resource previously registered with the same id. If resource has not yet
   * been registered or was garbage collected, returns null. The future may be to a resource that
   * has already finished loading, in which case {@link CompletableFuture#isDone()} will be true.
   */
  @Nullable
  public CompletableFuture<T> get(Object id) {
    Preconditions.checkNotNull(id, "Parameter 'id' was null.");

    synchronized (lock) {
      // If the resource has already finished loading, return a completed future to that resource.
      WeakReference<T> reference = registry.get(id);
      if (reference != null) {
        T resource = reference.get();
        if (resource != null) {
          return CompletableFuture.completedFuture(resource);
        } else {
          registry.remove(id);
        }
      }

      // If the resource is in the process of loading, return the future directly.
      // If the id is not registered, this will be null.
      return futureRegistry.get(id);
    }
  }

  /**
   * Registers a future to a resource by an id. If registering a resource that has already finished
   * loading, use {@link CompletableFuture#completedFuture(Object)}.
   */
  public void register(Object id, CompletableFuture<T> futureResource) {
    Preconditions.checkNotNull(id, "Parameter 'id' was null.");
    Preconditions.checkNotNull(futureResource, "Parameter 'futureResource' was null.");

    // If the future is already completed, add it to the registry for resources that are loaded and
    // return early.
    if (futureResource.isDone()) {
      if (futureResource.isCompletedExceptionally()) {
        return;
      }

      // Suppress warning for passing null into getNow. getNow isn't annotated, but it allows null.
      // Also, there is a precondition check here anyways.
      @SuppressWarnings("nullness")
      T resource = Preconditions.checkNotNull(futureResource.getNow(null));

      synchronized (lock) {
        registry.put(id, new WeakReference<>(resource));

        // If the id was previously registered in the futureRegistry, make sure it is removed.
        futureRegistry.remove(id);
      }

      return;
    }

    synchronized (lock) {
      futureRegistry.put(id, futureResource);

      // If the id was previously registered in the completed registry, make sure it is removed.
      registry.remove(id);
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "unused"})
    CompletableFuture<Void> registerFuture =
        futureResource.handle(
            (result, throwable) -> {
              synchronized (this) {
                // Check to make sure that the future in the registry is this future.
                // Otherwise, this id has already been overwritten with another resource.
                synchronized (lock) {
                  CompletableFuture<T> futureReference = futureRegistry.get(id);
                  if (futureReference == futureResource) {
                    futureRegistry.remove(id);
                    if (throwable == null) {
                      // Only add a reference if there was no exception.
                      registry.put(id, new WeakReference<>(result));
                    }
                  }
                }
              }
              return null;
            });
  }

  /**
   * Removes all cache entries. Cancels any in progress futures. cancel does not interrupt work in
   * progress. It only prevents the final stage from starting.
   */
  @Override
  public void destroyAllResources() {
    synchronized (lock) {
      Iterator<Map.Entry<Object, CompletableFuture<T>>> iterator =
          futureRegistry.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<Object, CompletableFuture<T>> entry = iterator.next();
        iterator.remove();
        CompletableFuture<T> futureResource = entry.getValue();
        if (!futureResource.isDone()) {
          futureResource.cancel(true);
        }
      }

      registry.clear();
    }
  }

  @Override
  public long reclaimReleasedResources() {
    // Resources held in registry are also held by other ResourceHolders.  Return zero for this one
    // and do
    // counting in the other holders.
    return 0;
  }
}

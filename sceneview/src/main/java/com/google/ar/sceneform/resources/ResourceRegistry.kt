package com.google.ar.sceneform.resources

import androidx.annotation.GuardedBy
import io.github.sceneview.collision.Preconditions
import java.lang.ref.WeakReference
import java.util.concurrent.CompletableFuture

/**
 * ResourceRegistry keeps track of resources that have been loaded and are in the process of being
 * loaded. The registry maintains only weak references and doesn't prevent resources from being
 * collected.
 *
 * @hide
 */
// TODO: Automatically prune dead WeakReferences from ResourceRegistry when the
// ResourceRegistry becomes large.
class ResourceRegistry<T> : ResourceHolder {
    companion object {
        private val TAG = ResourceRegistry::class.java.simpleName
    }

    private val lock = Any()

    @GuardedBy("lock")
    private val registry = HashMap<Any, WeakReference<T>>()

    @GuardedBy("lock")
    private val futureRegistry = HashMap<Any, CompletableFuture<T>>()

    /**
     * Returns a future to a resource previously registered with the same id. If resource has not yet
     * been registered or was garbage collected, returns null. The future may be to a resource that
     * has already finished loading, in which case [CompletableFuture.isDone] will be true.
     */
    fun get(id: Any): CompletableFuture<T>? {
        Preconditions.checkNotNull(id, "Parameter 'id' was null.")

        synchronized(lock) {
            // If the resource has already finished loading, return a completed future to that resource.
            val reference = registry[id]
            if (reference != null) {
                val resource = reference.get()
                if (resource != null) {
                    return CompletableFuture.completedFuture(resource)
                } else {
                    registry.remove(id)
                }
            }

            // If the resource is in the process of loading, return the future directly.
            // If the id is not registered, this will be null.
            return futureRegistry[id]
        }
    }

    /**
     * Registers a future to a resource by an id. If registering a resource that has already finished
     * loading, use [CompletableFuture.completedFuture].
     */
    fun register(id: Any, futureResource: CompletableFuture<T>) {
        Preconditions.checkNotNull(id, "Parameter 'id' was null.")
        Preconditions.checkNotNull(futureResource, "Parameter 'futureResource' was null.")

        // If the future is already completed, add it to the registry for resources that are loaded and
        // return early.
        if (futureResource.isDone) {
            if (futureResource.isCompletedExceptionally) {
                return
            }

            // Suppress warning for passing null into getNow. getNow isn't annotated, but it allows null.
            // Also, there is a precondition check here anyways.
            @Suppress("nullness")
            val resource = Preconditions.checkNotNull(futureResource.getNow(null))

            synchronized(lock) {
                registry[id] = WeakReference(resource)

                // If the id was previously registered in the futureRegistry, make sure it is removed.
                futureRegistry.remove(id)
            }

            return
        }

        synchronized(lock) {
            futureRegistry[id] = futureResource

            // If the id was previously registered in the completed registry, make sure it is removed.
            registry.remove(id)
        }

        @Suppress("FutureReturnValueIgnored", "unused")
        val registerFuture: CompletableFuture<Void> =
            futureResource.handle { result, throwable ->
                synchronized(this) {
                    // Check to make sure that the future in the registry is this future.
                    // Otherwise, this id has already been overwritten with another resource.
                    synchronized(lock) {
                        val futureReference = futureRegistry[id]
                        if (futureReference === futureResource) {
                            futureRegistry.remove(id)
                            if (throwable == null) {
                                // Only add a reference if there was no exception.
                                registry[id] = WeakReference(result)
                            }
                        }
                    }
                }
                null
            }
    }

    /**
     * Removes all cache entries. Cancels any in progress futures. cancel does not interrupt work in
     * progress. It only prevents the final stage from starting.
     */
    override fun destroyAllResources() {
        synchronized(lock) {
            val iterator = futureRegistry.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                iterator.remove()
                val futureResource = entry.value
                if (!futureResource.isDone) {
                    futureResource.cancel(true)
                }
            }

            registry.clear()
        }
    }

    override fun reclaimReleasedResources(): Long {
        // Resources held in registry are also held by other ResourceHolders. Return zero for this one
        // and do counting in the other holders.
        return 0
    }
}

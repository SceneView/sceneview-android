package com.google.ar.sceneform

import android.annotation.TargetApi
import androidx.annotation.MainThread
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * Executes multiple [Runnable]s sequentially by appending them to a [CompletableFuture].
 *
 * This should only be modified on the main thread.
 */
@TargetApi(24)
@Suppress("AndroidApiChecker", "FutureReturnValueIgnored")
internal class SequentialTask {
    private var future: CompletableFuture<Void>? = null

    /**
     * Appends a new Runnable to the current future, or creates a new one.
     *
     * @return The current future.
     */
    @MainThread
    fun appendRunnable(action: Runnable, executor: Executor): CompletableFuture<Void> {
        val f = future
        future = if (f != null && !f.isDone) {
            f.thenRunAsync(action, executor)
        } else {
            CompletableFuture.runAsync(action, executor)
        }
        return future!!
    }

    /** True if the future is null or done. */
    @MainThread
    fun isDone(): Boolean {
        val f = future ?: return true
        if (f.isDone) {
            future = null
            return true
        }
        return false
    }
}

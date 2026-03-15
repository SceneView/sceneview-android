package com.google.ar.sceneform.rendering

import android.util.Log
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/** Prints error messages if needed. */
@Suppress("AndroidApiChecker")
internal object FutureHelper {

    /**
     * Outputs a log message if input completes exceptionally.
     *
     * Does not remove the exception from input. If some later handler is able to do more with the
     * exception it is still possible.
     *
     * @param tag tag for the log message.
     * @param input A completable future that may have failed.
     * @param errorMsg Message to print along with the exception.
     * @return input so that the function may be chained.
     */
    @JvmStatic
    fun <T> logOnException(
        tag: String,
        input: CompletableFuture<T>,
        errorMsg: String
    ): CompletableFuture<T> {
        input.exceptionally { throwable ->
            Log.e(tag, errorMsg, throwable)
            throw CompletionException(throwable)
        }
        return input
    }
}

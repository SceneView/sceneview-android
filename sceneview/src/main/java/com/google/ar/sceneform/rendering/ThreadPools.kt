package com.google.ar.sceneform.rendering

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * Provides access to default [Executor]s to be used
 *
 * @hide
 */
object ThreadPools {
    private var mainExecutor: Executor? = null
    private var threadPoolExecutor: Executor? = null

    /** [Executor] for anything that that touches Renderer state */
    @JvmStatic
    fun getMainExecutor(): Executor {
        if (mainExecutor == null) {
            mainExecutor = object : Executor {
                private val handler = Handler(Looper.getMainLooper())

                override fun execute(runnable: Runnable) {
                    handler.post(runnable)
                }
            }
        }
        return mainExecutor!!
    }

    /** @param executor provides access to the main thread. */
    @JvmStatic
    fun setMainExecutor(executor: Executor) {
        mainExecutor = executor
    }

    /** Default background [Executor] for async operations including file reading. */
    @JvmStatic
    fun getThreadPoolExecutor(): Executor {
        return threadPoolExecutor ?: AsyncTask.THREAD_POOL_EXECUTOR
    }

    /**
     * Sets the default background [Executor].
     *
     * Tasks may be long running. This should not include the main thread
     */
    @JvmStatic
    fun setThreadPoolExecutor(executor: Executor) {
        threadPoolExecutor = executor
    }
}

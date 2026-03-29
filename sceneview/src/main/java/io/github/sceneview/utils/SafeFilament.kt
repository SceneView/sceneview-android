package io.github.sceneview.utils

import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.RenderableManager
import io.github.sceneview.Entity
import io.github.sceneview.ExperimentalSceneViewApi

private const val TAG = "SceneView"

/**
 * Wraps a Filament JNI call with error handling that catches native crashes and provides a
 * meaningful error message instead of a bare SIGABRT.
 *
 * Filament operations **must** be called on the main thread. If a call is made from a background
 * thread, the wrapped version logs a clear error message and returns null (or the fallback value)
 * instead of crashing.
 *
 * @param operation Human-readable description of the operation (for error messages).
 * @param block     The Filament JNI call to execute.
 * @return The result of [block], or null if it threw.
 */
@ExperimentalSceneViewApi
inline fun <T> safeFilament(operation: String, block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        Log.e(TAG, buildString {
            append("Filament operation failed: $operation\n")
            append("  Exception: ${e::class.simpleName}: ${e.message}\n")
            append("  Thread: ${Thread.currentThread().name}\n")
            if (Thread.currentThread().name != "main") {
                append("  WARNING: Filament JNI calls must run on the main thread!\n")
                append("  Use rememberModelInstance() in composables, or\n")
                append("  modelLoader.loadModelInstanceAsync() for imperative code.")
            }
        }, e)
        null
    }
}

/**
 * Safely gets the RenderableManager instance for an entity, returning 0 (invalid) if the
 * entity has no renderable component instead of crashing.
 *
 * @param entity The Filament entity to query.
 * @return The renderable instance, or 0 if the entity has no renderable component.
 */
@ExperimentalSceneViewApi
fun RenderableManager.safeGetInstance(entity: Entity): Int {
    return try {
        val instance = getInstance(entity)
        if (instance == 0) {
            Log.w(TAG, "Entity $entity has no renderable component")
        }
        instance
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get renderable instance for entity $entity", e)
        0
    }
}

/**
 * Safely gets the LightManager instance for an entity, returning 0 (invalid) if the entity
 * has no light component instead of crashing.
 *
 * @param entity The Filament entity to query.
 * @return The light instance, or 0 if the entity has no light component.
 */
@ExperimentalSceneViewApi
fun LightManager.safeGetInstance(entity: Entity): Int {
    return try {
        val instance = getInstance(entity)
        if (instance == 0) {
            Log.w(TAG, "Entity $entity has no light component")
        }
        instance
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get light instance for entity $entity", e)
        0
    }
}

/**
 * Validates that the current thread is the main thread.
 *
 * Call at the start of any public API that requires main-thread execution to provide a clear
 * error message instead of a Filament JNI crash.
 *
 * @param operation Description of the operation for the error message.
 * @throws IllegalStateException if not on the main thread.
 */
@ExperimentalSceneViewApi
fun requireMainThread(operation: String) {
    if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
        throw IllegalStateException(
            "SceneView: '$operation' must be called on the main thread. " +
                    "Current thread: ${Thread.currentThread().name}. " +
                    "Use rememberModelInstance() in composables, or " +
                    "withContext(Dispatchers.Main) { } for coroutines."
        )
    }
}

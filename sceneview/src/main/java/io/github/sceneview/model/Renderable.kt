package io.github.sceneview.model

import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.filament.utils.HDRLoader
import com.google.ar.sceneform.rendering.Renderable
import io.github.sceneview.environment.loadEnvironment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import com.google.android.filament.utils.KTX1Loader as KTXLoader

/**
 *
 * ### Load a Renderable in a coroutine scope without blocking a thread
 *
 * This suspending function is cancellable.
 * If the Job of the current coroutine is cancelled or completed while this suspending function
 * is waiting, this function stops waiting for the completion stage and immediately resumes with
 * CancellationException.
 * This method is intended to be used with one-shot futures, so on coroutine cancellation the
 * CompletableFuture that corresponds to this CompletionStage
 * (see CompletionStage.toCompletableFuture) is cancelled.
 * If cancelling the given stage is undesired, stage.asDeferred().await() should be used instead.
 *
 * @return the created directional light
 *
 * @see [KTXLoader.loadEnvironment]
 * @see [HDRLoader.loadEnvironment]
 */
suspend fun <T : Renderable, B : Renderable.Builder<T, B>> Renderable.Builder<T, B>.build(
    coroutineScope: LifecycleCoroutineScope
) {
    coroutineScope.launchWhenCreated {
        await()
    }
}

/**
 *
 * ### Awaits for loading a Renderable with the parameters of the builder without blocking a thread
 *
 * This suspending function is cancellable.
 * If the Job of the current coroutine is cancelled or completed while this suspending function
 * is waiting, this function stops waiting for the completion stage and immediately resumes with
 * CancellationException.
 * This method is intended to be used with one-shot futures, so on coroutine cancellation the
 * CompletableFuture that corresponds to this CompletionStage
 * (see CompletionStage.toCompletableFuture) is cancelled.
 * If cancelling the given stage is undesired, stage.asDeferred().await() should be used instead.
 *
 * @return the created directional light
 *
 * @see [KTXLoader.loadEnvironment]
 * @see [HDRLoader.loadEnvironment]
 */
suspend fun <T : Renderable, B : Renderable.Builder<T, B>> Renderable.Builder<T, B>.await() =
    build().await()

/**
 * ### Deferred renderable loading is a non-blocking cancellable future.
 *
 * It is a [Job] with a result.
 *
 * @see [Deferred]
 */
fun <T : Renderable, B : Renderable.Builder<T, B>> Renderable.Builder<T, B>.asDeferred() =
    build().asDeferred()
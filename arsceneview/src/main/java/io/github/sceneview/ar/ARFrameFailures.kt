package io.github.sceneview.ar

/**
 * Tells an AR frame that failed once from a frame loop that keeps failing (#4510).
 *
 * `ARSceneView`'s render loop catches whatever one frame throws — `Session.update()`, the camera
 * stream, light estimation, a node update, a host callback — so a single bad frame never takes the
 * view down. Until this class the loop also *only logged* it: when every frame threw, the camera
 * image stayed live, `onSessionUpdated` stopped being called, and the host was left on its
 * "starting" state for ever with nothing but a logcat line to say why.
 *
 * The streak is the rule that decides when the host hears about it:
 * - an isolated exception is **not** reported — the next frame that goes through clears it;
 * - a loop that has thrown on every frame for [persistentAfterNanos] is reported, **once per
 *   exception type**, for as long as this instance lives;
 * - two failures further apart than [maxGapNanos] with no frame in between (the session was
 *   paused, the app went to the background) are two separate incidents, not one long streak.
 *
 * Pure Kotlin, driven by the frame clock the caller passes in — see `ARFrameFailureStreakTest`.
 *
 * Not thread-safe: the render loop is its only caller.
 */
internal class ARFrameFailureStreak(
    private val persistentAfterNanos: Long = PERSISTENT_AFTER_NANOS,
    private val maxGapNanos: Long = MAX_GAP_NANOS,
) {
    private var inStreak = false
    private var streakStartNanos = 0L
    private var lastFailureNanos = 0L
    private val reportedTypes = HashSet<Class<out Exception>>()

    /** A frame went all the way through: whatever was failing is no longer failing. */
    fun frameSucceeded() {
        inStreak = false
    }

    /**
     * A frame threw [exception] at [frameTimeNanos].
     *
     * @return `true` when the host must be told now — the loop has been failing without a single
     *   good frame for [persistentAfterNanos] and this exception type was not reported yet.
     */
    fun frameFailed(exception: Exception, frameTimeNanos: Long): Boolean {
        if (!inStreak || frameTimeNanos - lastFailureNanos > maxGapNanos) {
            inStreak = true
            streakStartNanos = frameTimeNanos
        }
        lastFailureNanos = frameTimeNanos
        if (frameTimeNanos - streakStartNanos < persistentAfterNanos) return false
        return reportedTypes.add(exception.javaClass)
    }

    companion object {
        /** How long the loop must fail on every frame before the host is told: 1.5 s. */
        const val PERSISTENT_AFTER_NANOS: Long = 1_500_000_000L

        /** Longest silence between two failures that still counts as the same streak: 0.5 s. */
        const val MAX_GAP_NANOS: Long = 500_000_000L
    }
}

/**
 * Runs one host callback of the AR frame loop and contains what it throws (#4510).
 *
 * A host's `onSessionUpdated` or `onTrackingFailureChanged` that throws used to be caught by the
 * same handler as `Session.update()` and logged as "ARCore session update failed" — which pointed
 * at ARCore for a bug in the app, and let a throwing `onTrackingFailureChanged` skip
 * `onSessionUpdated` for that frame. Each callback now runs in its own guard: the throw is handed
 * to [onThrow] under the callback's own [name], and the next callback still runs.
 *
 * @return the exception [block] threw, or `null` when it returned normally.
 */
internal inline fun guardHostCallback(
    name: String,
    onThrow: (name: String, exception: Exception) -> Unit,
    block: () -> Unit,
): Exception? = try {
    block()
    null
} catch (exception: Exception) {
    onThrow(name, exception)
    exception
}

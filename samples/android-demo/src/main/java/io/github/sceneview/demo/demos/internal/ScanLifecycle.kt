package io.github.sceneview.demo.demos.internal

/*
 * The 3D Scan capture lifecycle (#4627), pure so a JVM test holds it: Pause and Resume, Finish
 * opening a review that "Keep scanning" leaves for the same capture, and only "Save scan"
 * ending it. Nothing here touches ARCore.
 */

/** Where a capture stands. Only [Scanning] integrates new frames. */
enum class ScanPhase {
    /** Frames are integrated: path, points, photos, depth. */
    Scanning,

    /** Frozen by the user; Resume returns to [Scanning]. */
    Paused,

    /** Finish was tapped: frozen, nothing built yet. Keep scanning returns to [Scanning]. */
    Reviewing,

    /** Save scan was tapped: the capture is being built and written. No way back. */
    Saving,

    /** The write failed: the built scan is still held, Try again writes it again. */
    SaveFailed,
    ;

    /** Whether this phase takes new frames into the capture. */
    val integrates: Boolean get() = this == Scanning

    /** Whether the capture can still go back to [Scanning]: nothing has been built yet. */
    val canKeepScanning: Boolean get() = this == Scanning || this == Paused || this == Reviewing

    /** Pause ⇄ Resume. Any other phase is left as it is. */
    fun togglePause(): ScanPhase = when (this) {
        Scanning -> Paused
        Paused -> Scanning
        else -> this
    }

    /** Finish: a reversible review, from a capture that is still open. */
    fun finish(): ScanPhase = if (canKeepScanning) Reviewing else this

    /** Keep scanning: back to the same capture, from anywhere it has not been built yet. */
    fun keepScanning(): ScanPhase = if (canKeepScanning) Scanning else this
}

/**
 * The capture's clock, on a monotonic nanosecond clock: it stops while the capture is not
 * integrating (paused, in review) and resumes where it stopped.
 */
class ScanClock(private val startedNanos: Long) {
    private var stoppedTotal = 0L
    private var stoppedAt: Long? = null

    val isStopped: Boolean get() = stoppedAt != null

    fun stop(nowNanos: Long) {
        if (stoppedAt == null) stoppedAt = nowNanos
    }

    fun resume(nowNanos: Long) {
        val at = stoppedAt ?: return
        stoppedTotal += (nowNanos - at).coerceAtLeast(0L)
        stoppedAt = null
    }

    /** Seconds captured at [nowNanos], not counting the time spent stopped. */
    fun elapsedSeconds(nowNanos: Long): Float {
        val end = stoppedAt ?: nowNanos
        return ((end - startedNanos - stoppedTotal).coerceAtLeast(0L) / NANOS_PER_SECOND).toFloat()
    }

    private companion object {
        const val NANOS_PER_SECOND = 1e9
    }
}

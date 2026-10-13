package io.github.sceneview.ar

import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState

/**
 * Turns the camera's per-frame tracking state into one log line per **transition**, repeated
 * every [REPEAT_NANOS] for as long as the camera is not tracking (#4510).
 *
 * A session that delivers camera images but never reaches `TRACKING` used to leave nothing in
 * logcat: `onTrackingFailureChanged` only reports a non-`NONE` reason, so a camera sitting in
 * `PAUSED` with reason `NONE` was silent. One line per change — `NONE` included, never one per
 * frame — is enough to tell that case from a throwing frame loop.
 *
 * The repeat is what keeps the line readable later: ARCore writes some sixty lines a second from
 * the app's own process, so a line logged once when the session started has left a default
 * 256 KiB log buffer within a couple of minutes. A repeated line carries `for=<n>s`, the time the
 * state has lasted, which tells "never tracked" from "tracked, then lost it". A tracking camera
 * repeats nothing.
 */
internal class ARTrackingStateLog {
    private var lastState: TrackingState? = null
    private var lastReason: TrackingFailureReason? = null
    private var sinceNanos = 0L
    private var loggedAtNanos = 0L

    /**
     * @param timestampNanos the frame's timestamp, `Frame.getTimestamp()`.
     * @return the line to log — when [state] or [reason] differs from the previous frame's (the
     *   first frame always differs), or when a state other than `TRACKING` has lasted another
     *   [REPEAT_NANOS] — or `null` when there is nothing to say.
     */
    fun transition(
        state: TrackingState,
        reason: TrackingFailureReason,
        timestampNanos: Long,
    ): String? {
        val changed = state != lastState || reason != lastReason
        // A timestamp that goes back belongs to another session or recording: start over from it
        // rather than report a negative or a stale duration.
        if (changed || timestampNanos < loggedAtNanos) {
            lastState = state
            lastReason = reason
            sinceNanos = timestampNanos
            loggedAtNanos = timestampNanos
        }
        return when {
            changed -> "$PREFIX state=$state reason=$reason"
            state == TrackingState.TRACKING -> null
            timestampNanos - loggedAtNanos < REPEAT_NANOS -> null
            else -> {
                loggedAtNanos = timestampNanos
                val seconds = (timestampNanos - sinceNanos) / NANOS_PER_SECOND
                "$PREFIX state=$state reason=$reason for=${seconds}s"
            }
        }
    }

    companion object {
        /** Stable prefix: grep a bug report's logcat for it. */
        const val PREFIX: String = "AR tracking:"

        /** How often a camera that is not tracking says so again. */
        const val REPEAT_NANOS: Long = 10_000_000_000L

        private const val NANOS_PER_SECOND = 1_000_000_000L
    }
}

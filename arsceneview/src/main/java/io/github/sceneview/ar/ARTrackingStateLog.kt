package io.github.sceneview.ar

import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState

/**
 * Turns the camera's per-frame tracking state into one log line per **transition** (#4510).
 *
 * A session that delivers camera images but never reaches `TRACKING` used to leave nothing in
 * logcat: `onTrackingFailureChanged` only reports a non-`NONE` reason, so a camera sitting in
 * `PAUSED` with reason `NONE` was silent. One line per change — `NONE` included, never one per
 * frame — is enough for a bug report's logcat to tell that case from a throwing frame loop.
 */
internal class ARTrackingStateLog {
    private var lastState: TrackingState? = null
    private var lastReason: TrackingFailureReason? = null

    /**
     * @return the line to log when [state] or [reason] differs from the previous frame's (the first
     *   frame always differs), or `null` when nothing changed.
     */
    fun transition(state: TrackingState, reason: TrackingFailureReason): String? {
        if (state == lastState && reason == lastReason) return null
        lastState = state
        lastReason = reason
        return "$PREFIX state=$state reason=$reason"
    }

    companion object {
        /** Stable prefix: grep a bug report's logcat for it. */
        const val PREFIX: String = "AR tracking:"
    }
}

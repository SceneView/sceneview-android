package io.github.sceneview.animation

/**
 * Playback mode for a [PropertyAnimation].
 */
enum class PlaybackMode {
    /** Play once from start to end, then stop. */
    ONCE,
    /** Restart from the beginning each time the animation completes. */
    LOOP,
    /** Alternate direction each time the animation completes (start→end→start→…). */
    PING_PONG
}

/**
 * Snapshot of a running property animation.
 *
 * @param startValue  Value at fraction 0.
 * @param endValue    Value at fraction 1.
 * @param durationSeconds Total animation duration in seconds.
 * @param elapsedSeconds  Time elapsed since the animation started.
 * @param easing      Easing function mapping linear fraction to curved fraction.
 * @param playbackMode How the animation behaves when it reaches the end.
 * @param isRunning   Whether the animation is currently advancing.
 * @param isReversed  `true` when the current pass is playing in reverse (ping-pong).
 */
data class AnimationState(
    val startValue: Float,
    val endValue: Float,
    val durationSeconds: Float,
    val elapsedSeconds: Float = 0f,
    val easing: (Float) -> Float = Easing.Linear,
    val playbackMode: PlaybackMode = PlaybackMode.ONCE,
    val isRunning: Boolean = true,
    val isReversed: Boolean = false
) {
    /** Linear fraction in [0..1] based on elapsed / duration. */
    val linearFraction: Float
        get() = if (durationSeconds <= 0f) 1f
        else (elapsedSeconds / durationSeconds).coerceIn(0f, 1f)

    /** Eased fraction after applying the [easing] function. */
    val fraction: Float
        get() {
            val f = easing(linearFraction)
            return if (isReversed) 1f - f else f
        }

    /** Current interpolated value. */
    val value: Float
        get() = lerp(startValue, endValue, fraction)

    /** `true` when a non-looping animation has reached the end. */
    val isFinished: Boolean
        get() = !isRunning ||
                (playbackMode == PlaybackMode.ONCE && elapsedSeconds >= durationSeconds)
}

/**
 * Advance [state] by [deltaSeconds] and return the updated [AnimationState].
 *
 * Pure function — no mutation, no side effects.
 */
fun animate(state: AnimationState, deltaSeconds: Float): AnimationState {
    if (!state.isRunning) return state

    val newElapsed = state.elapsedSeconds + deltaSeconds

    return when (state.playbackMode) {
        PlaybackMode.ONCE -> {
            if (newElapsed >= state.durationSeconds) {
                state.copy(elapsedSeconds = state.durationSeconds, isRunning = false)
            } else {
                state.copy(elapsedSeconds = newElapsed)
            }
        }

        PlaybackMode.LOOP -> {
            state.copy(elapsedSeconds = newElapsed % state.durationSeconds)
        }

        PlaybackMode.PING_PONG -> {
            if (newElapsed >= state.durationSeconds) {
                // Flip direction, reset elapsed
                state.copy(
                    elapsedSeconds = newElapsed % state.durationSeconds,
                    isReversed = !state.isReversed
                )
            } else {
                state.copy(elapsedSeconds = newElapsed)
            }
        }
    }
}

package io.github.sceneview.animation

/**
 * A keyframe in an animation timeline.
 *
 * @param time Time in seconds at which this keyframe occurs.
 * @param value The value at this keyframe.
 * @param easing Easing function for interpolation from this keyframe to the next.
 *               Default is linear.
 */
data class Keyframe(
    val time: Float,
    val value: Float,
    val easing: (Float) -> Float = Easing.Linear
)

/**
 * A keyframe animation track that interpolates between keyframes over time.
 *
 * Keyframes are automatically sorted by time. Evaluation at any time returns
 * the interpolated value between the surrounding keyframes.
 *
 * @param keyframes List of keyframes. Must contain at least 1 keyframe.
 */
class KeyframeTrack(keyframes: List<Keyframe>) {
    val keyframes: List<Keyframe> = keyframes.sortedBy { it.time }
    val duration: Float get() = if (keyframes.isEmpty()) 0f else keyframes.last().time

    init {
        require(keyframes.isNotEmpty()) { "KeyframeTrack requires at least 1 keyframe" }
    }

    /**
     * Evaluate the animation at the given time.
     *
     * @param time Time in seconds.
     * @return Interpolated value at the given time.
     */
    fun evaluate(time: Float): Float {
        if (keyframes.size == 1) return keyframes[0].value
        if (time <= keyframes.first().time) return keyframes.first().value
        if (time >= keyframes.last().time) return keyframes.last().value

        // Find the two surrounding keyframes
        for (i in 0 until keyframes.size - 1) {
            val kf0 = keyframes[i]
            val kf1 = keyframes[i + 1]
            if (time in kf0.time..kf1.time) {
                val localT = (time - kf0.time) / (kf1.time - kf0.time)
                val easedT = kf0.easing(localT)
                return lerp(kf0.value, kf1.value, easedT)
            }
        }

        return keyframes.last().value
    }
}

/**
 * State for a running keyframe animation.
 *
 * @param track The keyframe track to play.
 * @param elapsedSeconds Time elapsed since start.
 * @param speed Playback speed multiplier.
 * @param loop Whether to loop the animation.
 * @param isRunning Whether the animation is currently playing.
 */
data class KeyframeAnimationState(
    val track: KeyframeTrack,
    val elapsedSeconds: Float = 0f,
    val speed: Float = 1f,
    val loop: Boolean = false,
    val isRunning: Boolean = true
) {
    /** Current value of the animation. */
    val value: Float get() = track.evaluate(elapsedSeconds)

    /** Whether the animation has finished (non-looping only). */
    val isFinished: Boolean get() = !loop && elapsedSeconds >= track.duration
}

/**
 * Advance a keyframe animation by [deltaSeconds].
 *
 * Pure function — no mutation.
 */
fun advanceKeyframeAnimation(
    state: KeyframeAnimationState,
    deltaSeconds: Float
): KeyframeAnimationState {
    if (!state.isRunning || state.isFinished) return state

    val newElapsed = state.elapsedSeconds + deltaSeconds * state.speed

    return if (state.loop) {
        val duration = state.track.duration
        if (duration > 0f) {
            state.copy(elapsedSeconds = newElapsed % duration)
        } else {
            state.copy(elapsedSeconds = 0f)
        }
    } else {
        if (newElapsed >= state.track.duration) {
            state.copy(elapsedSeconds = state.track.duration, isRunning = false)
        } else {
            state.copy(elapsedSeconds = newElapsed)
        }
    }
}

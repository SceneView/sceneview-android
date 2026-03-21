package io.github.sceneview.animation

import kotlin.math.abs

/**
 * Platform-independent animation playback state and time computation.
 *
 * Pure data + pure functions — no renderer dependency.
 */

/**
 * State of a single playing animation.
 *
 * @param startTimeNanos The time the animation started (nanoseconds, monotonic clock).
 * @param speed Playback speed multiplier. Negative = reverse. Zero = paused.
 * @param loop Whether the animation loops indefinitely.
 */
data class PlaybackState(
    val startTimeNanos: Long,
    val speed: Float = 1f,
    val loop: Boolean = true
)

/**
 * Result of computing the current animation time for a single animation.
 *
 * @param animationTime The time in seconds to apply to the animation (clamped to duration).
 * @param finished True if the animation has completed (non-looping only).
 */
data class AnimationFrame(
    val animationTime: Float,
    val finished: Boolean
)

/**
 * Compute the current animation time for a playing animation.
 *
 * Pure function — no side effects.
 *
 * @param currentTimeNanos Current frame time in nanoseconds.
 * @param startTimeNanos Time the animation started in nanoseconds.
 * @param speed Playback speed (negative = reverse, zero = paused).
 * @param duration Total animation duration in seconds.
 * @param loop Whether the animation loops.
 * @return The animation time to apply and whether the animation finished.
 */
fun computeAnimationFrame(
    currentTimeNanos: Long,
    startTimeNanos: Long,
    speed: Float,
    duration: Float,
    loop: Boolean
): AnimationFrame {
    if (speed == 0f) return AnimationFrame(0f, false)

    val elapsedNanos = currentTimeNanos - startTimeNanos
    val elapsedSeconds = elapsedNanos / 1_000_000_000.0
    val adjustedTime = (elapsedSeconds * abs(speed)).toFloat()

    val animationTime = if (speed > 0) {
        adjustedTime
    } else {
        duration - adjustedTime
    }

    val finished = !loop && adjustedTime >= duration
    return AnimationFrame(animationTime, finished)
}

/**
 * Compute the scale factor needed to fit an object with given half-extents
 * into a unit cube of the specified size.
 *
 * @param halfExtentX Half-extent along X axis.
 * @param halfExtentY Half-extent along Y axis.
 * @param halfExtentZ Half-extent along Z axis.
 * @param units Target size in world units (default 1.0).
 * @return Uniform scale factor.
 */
fun scaleToFitUnits(
    halfExtentX: Float,
    halfExtentY: Float,
    halfExtentZ: Float,
    units: Float = 1f
): Float {
    val maxHalfExtent = maxOf(halfExtentX, halfExtentY, halfExtentZ)
    return if (maxHalfExtent > 0f) units / (maxHalfExtent * 2f) else 1f
}

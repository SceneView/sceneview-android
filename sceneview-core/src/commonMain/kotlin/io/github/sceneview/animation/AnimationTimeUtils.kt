package io.github.sceneview.animation

/**
 * Pure utility functions for converting between animation time representations:
 * frame numbers, elapsed seconds, and fractional (0..1) positions.
 *
 * These are platform-independent and can be used on any target.
 */

/**
 * Convert a frame number to elapsed time in seconds.
 *
 * @param frame Frame number on the timeline.
 * @param frameRate Frames per second of the animation.
 * @return Elapsed time in seconds.
 */
fun frameToTime(frame: Int, frameRate: Int): Float {
    return frame.toFloat() / frameRate.toFloat()
}

/**
 * Convert elapsed time in seconds to a frame number.
 *
 * @param time Elapsed time in seconds.
 * @param frameRate Frames per second of the animation.
 * @return The frame number at the specified time.
 */
fun timeToFrame(time: Float, frameRate: Int): Int {
    return (time * frameRate).toInt()
}

/**
 * Convert a fractional position (0..1) to elapsed time in seconds.
 *
 * @param fraction The fractional value of interest (0 to 1).
 * @param duration Total duration in seconds.
 * @return Elapsed time at the specified fraction.
 */
fun fractionToTime(fraction: Float, duration: Float): Float {
    return fraction * duration
}

/**
 * Convert elapsed time in seconds to a fractional position (0..1).
 *
 * @param time Elapsed time in seconds.
 * @param duration Total duration in seconds.
 * @return The fractional value (0 to 1) at the specified time.
 */
fun timeToFraction(time: Float, duration: Float): Float {
    return if (duration > 0f) time / duration else 0f
}

/**
 * Convert seconds to milliseconds.
 *
 * @param seconds Time in seconds.
 * @return Time in milliseconds.
 */
fun secondsToMillis(seconds: Float): Long {
    return (seconds * 1000f).toLong()
}

/**
 * Convert milliseconds to seconds.
 *
 * @param millis Time in milliseconds.
 * @return Time in seconds.
 */
fun millisToSeconds(millis: Long): Float {
    return millis / 1000f
}

/**
 * Compute the total frame count for a given duration and frame rate.
 *
 * @param durationSeconds Total duration in seconds.
 * @param frameRate Frames per second.
 * @return Total number of frames.
 */
fun frameCount(durationSeconds: Float, frameRate: Int): Int {
    return timeToFrame(durationSeconds, frameRate)
}

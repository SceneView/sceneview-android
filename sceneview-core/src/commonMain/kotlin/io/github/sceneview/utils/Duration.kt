package io.github.sceneview.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

/**
 * Computes the duration between this nanosecond timestamp and [other].
 *
 * @param other Previous timestamp in nanoseconds, or null (treated as 0).
 * @return The elapsed [Duration].
 */
fun Long.interval(other: Long?): Duration =
    (this - (other ?: 0)).nanoseconds

/**
 * Computes the interval in seconds between this nanosecond timestamp and [other].
 *
 * @param other Previous timestamp in nanoseconds, or null (treated as 0).
 */
fun Long.intervalSeconds(other: Long?): Double = interval(other).toDouble(
    DurationUnit.SECONDS
)

/**
 * Computes the frames-per-second rate from the interval between this and [other].
 *
 * @param other Previous frame timestamp in nanoseconds, or null.
 */
fun Long.fps(other: Long?): Double = 1.0 / intervalSeconds(other)

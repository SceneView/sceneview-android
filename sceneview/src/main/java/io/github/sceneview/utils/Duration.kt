package io.github.sceneview.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

/**
 * The duration between Long nanoseconds and the last one
 */
fun Long.interval(other: Long?): Duration =
    (this - (other ?: 0)).nanoseconds

fun Long.intervalSeconds(other: Long?): Double = interval(other).toDouble(
    DurationUnit.SECONDS
)

fun Long.fps(other: Long?): Double = 1.0 / intervalSeconds(other)
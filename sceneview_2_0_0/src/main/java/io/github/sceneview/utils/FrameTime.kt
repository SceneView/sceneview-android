package io.github.sceneview.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

/**
 * Provides time information for the current frame
 *
 * @param nanoseconds The time when this frame started
 * @param lastNanoseconds The time when the previous frame started
 */
data class FrameTime(val nanoseconds: Long, val lastNanoseconds: Long? = null) {

    /**
     * The duration between this frame and the last frame
     */
    val interval: Duration by lazy { interval(lastNanoseconds) }

    val intervalSeconds: Double by lazy { intervalSeconds(lastNanoseconds) }

    val fps: Double by lazy { fps(lastNanoseconds) }

    /**
     * The duration between this frame and the last frame
     */
    fun interval(lastNanoseconds: Long?): Duration =
        (nanoseconds - (lastNanoseconds ?: 0)).nanoseconds

    fun intervalSeconds(lastNanoseconds: Long?): Double = interval(lastNanoseconds).toDouble(
        DurationUnit.SECONDS
    )

    fun intervalSeconds(frameTime: FrameTime?): Double = intervalSeconds(frameTime?.lastNanoseconds)

    fun fps(lastNanoseconds: Long?): Double = 1.0 / intervalSeconds(lastNanoseconds)

    fun fps(frameTime: FrameTime?): Double = fps(frameTime?.nanoseconds)
}
package io.github.sceneview.sample.arviewnode

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

data class FrameTime(val nanoseconds: Long, val lastNanoseconds: Long? = null) {

    /**
     * ### The duration between this frame and the last frame
     */
    val interval: Duration by lazy { interval(lastNanoseconds) }

    val intervalSeconds: Double by lazy { intervalSeconds(lastNanoseconds) }

    val fps: Double by lazy { fps(lastNanoseconds) }

    /**
     * ### The duration between this frame and the last frame
     */
    private fun interval(lastNanoseconds: Long?): Duration =
        (nanoseconds - (lastNanoseconds ?: 0)).nanoseconds

    private fun intervalSeconds(lastNanoseconds: Long?): Double = interval(lastNanoseconds).toDouble(
        DurationUnit.SECONDS
    )

    private fun fps(lastNanoseconds: Long?): Double = 1.0 / intervalSeconds(lastNanoseconds)

    fun fps(frameTime: FrameTime?): Double = fps(frameTime?.nanoseconds)
}


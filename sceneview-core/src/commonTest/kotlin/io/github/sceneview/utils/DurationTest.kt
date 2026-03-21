package io.github.sceneview.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

class DurationTest {

    @Test
    fun intervalReturnsDifferenceBetweenTimestamps() {
        val current = 2_000_000_000L
        val previous = 1_000_000_000L
        assertEquals(1_000_000_000L.nanoseconds, current.interval(previous))
    }

    @Test
    fun intervalWithNullOtherUsesZero() {
        val current = 500_000_000L
        assertEquals(500_000_000L.nanoseconds, current.interval(null))
    }

    @Test
    fun intervalSecondsReturnsCorrectValue() {
        val current = 2_000_000_000L
        val previous = 1_000_000_000L
        assertEquals(1.0, current.intervalSeconds(previous), 1e-9)
    }

    @Test
    fun intervalSecondsWithNullOther() {
        val current = 1_000_000_000L
        assertEquals(1.0, current.intervalSeconds(null), 1e-9)
    }

    @Test
    fun fpsReturnsFramesPerSecond() {
        // 16_666_667 ns ≈ 1/60th of a second → ~60 fps
        val current = 16_666_667L
        val previous = 0L
        val result = current.fps(previous)
        assertEquals(60.0, result, 0.1)
    }

    @Test
    fun fpsAt30Hz() {
        // 33_333_333 ns ≈ 1/30th of a second → ~30 fps
        val current = 33_333_333L
        val previous = 0L
        val result = current.fps(previous)
        assertEquals(30.0, result, 0.1)
    }
}

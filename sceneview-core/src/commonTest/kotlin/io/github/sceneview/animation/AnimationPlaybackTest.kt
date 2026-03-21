package io.github.sceneview.animation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationPlaybackTest {

    private val epsilon = 0.01f

    // --- computeAnimationFrame ---

    @Test
    fun zeroSpeedReturnsZeroTime() {
        val frame = computeAnimationFrame(
            currentTimeNanos = 2_000_000_000L,
            startTimeNanos = 0L,
            speed = 0f,
            duration = 5f,
            loop = false
        )
        assertEquals(0f, frame.animationTime, epsilon)
        assertFalse(frame.finished)
    }

    @Test
    fun forwardPlaybackAtNormalSpeed() {
        // 1 second elapsed at 1x speed
        val frame = computeAnimationFrame(
            currentTimeNanos = 1_000_000_000L,
            startTimeNanos = 0L,
            speed = 1f,
            duration = 5f,
            loop = false
        )
        assertEquals(1f, frame.animationTime, epsilon)
        assertFalse(frame.finished)
    }

    @Test
    fun forwardPlaybackAtDoubleSpeed() {
        // 1 second elapsed at 2x speed → 2 seconds of animation time
        val frame = computeAnimationFrame(
            currentTimeNanos = 1_000_000_000L,
            startTimeNanos = 0L,
            speed = 2f,
            duration = 5f,
            loop = false
        )
        assertEquals(2f, frame.animationTime, epsilon)
        assertFalse(frame.finished)
    }

    @Test
    fun reversePlayback() {
        // 1 second elapsed at -1x speed, 5s duration
        // animationTime = duration - adjustedTime = 5 - 1 = 4
        val frame = computeAnimationFrame(
            currentTimeNanos = 1_000_000_000L,
            startTimeNanos = 0L,
            speed = -1f,
            duration = 5f,
            loop = false
        )
        assertEquals(4f, frame.animationTime, epsilon)
        assertFalse(frame.finished)
    }

    @Test
    fun nonLoopingFinishesAtDuration() {
        // 6 seconds elapsed, 5s duration, non-looping → finished
        val frame = computeAnimationFrame(
            currentTimeNanos = 6_000_000_000L,
            startTimeNanos = 0L,
            speed = 1f,
            duration = 5f,
            loop = false
        )
        assertTrue(frame.finished, "Should be finished past duration")
    }

    @Test
    fun loopingNeverFinishes() {
        // 100 seconds elapsed, 5s duration, looping → not finished
        val frame = computeAnimationFrame(
            currentTimeNanos = 100_000_000_000L,
            startTimeNanos = 0L,
            speed = 1f,
            duration = 5f,
            loop = true
        )
        assertFalse(frame.finished, "Looping should never finish")
    }

    @Test
    fun halfSpeedTakesLonger() {
        // 2 seconds at 0.5x → 1 second of animation
        val frame = computeAnimationFrame(
            currentTimeNanos = 2_000_000_000L,
            startTimeNanos = 0L,
            speed = 0.5f,
            duration = 5f,
            loop = false
        )
        assertEquals(1f, frame.animationTime, epsilon)
    }

    @Test
    fun startTimeOffsetWorks() {
        // Current = 3s, start = 2s → 1 second elapsed
        val frame = computeAnimationFrame(
            currentTimeNanos = 3_000_000_000L,
            startTimeNanos = 2_000_000_000L,
            speed = 1f,
            duration = 5f,
            loop = false
        )
        assertEquals(1f, frame.animationTime, epsilon)
    }

    // --- scaleToFitUnits ---

    @Test
    fun unitCubeScaleIsOne() {
        val scale = scaleToFitUnits(0.5f, 0.5f, 0.5f, units = 1f)
        assertEquals(1f, scale, epsilon)
    }

    @Test
    fun doubleSizeCubeScalesDown() {
        // halfExtent = 1 → fullExtent = 2 → scale = 1/2 = 0.5
        val scale = scaleToFitUnits(1f, 1f, 1f, units = 1f)
        assertEquals(0.5f, scale, epsilon)
    }

    @Test
    fun smallObjectScalesUp() {
        // halfExtent = 0.1 → fullExtent = 0.2 → scale = 1/0.2 = 5
        val scale = scaleToFitUnits(0.1f, 0.1f, 0.1f, units = 1f)
        assertEquals(5f, scale, epsilon)
    }

    @Test
    fun usesLargestExtent() {
        // X is the largest at 2 → fullExtent = 4 → scale = 1/4 = 0.25
        val scale = scaleToFitUnits(2f, 0.5f, 1f, units = 1f)
        assertEquals(0.25f, scale, epsilon)
    }

    @Test
    fun customUnitsScalesProportionally() {
        // halfExtent = 1 → fullExtent = 2 → scale = 3/2 = 1.5
        val scale = scaleToFitUnits(1f, 1f, 1f, units = 3f)
        assertEquals(1.5f, scale, epsilon)
    }

    @Test
    fun zeroExtentReturnsFallback() {
        val scale = scaleToFitUnits(0f, 0f, 0f, units = 1f)
        assertEquals(1f, scale, epsilon)
    }

    // --- PlaybackState ---

    @Test
    fun playbackStateDefaults() {
        val state = PlaybackState(startTimeNanos = 0L)
        assertEquals(1f, state.speed)
        assertTrue(state.loop)
    }
}

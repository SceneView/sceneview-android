package io.github.sceneview.animation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for [ModelAnimation] companion object utility functions.
 *
 * All functions under test are pure (no Filament/Android dependencies):
 * - [ModelAnimation.frameToTime]
 * - [ModelAnimation.timeToFrame]
 * - [ModelAnimation.fractionToTime]
 * - [ModelAnimation.timeToFraction]
 * - [ModelAnimation.secondsToMillis]
 */
class ModelAnimationTest {

    // ── frameToTime ──────────────────────────────────────────────────────────────

    @Test
    fun `frameToTime converts frame 0 to time 0`() {
        assertEquals(0f, ModelAnimation.frameToTime(0, 24), 0f)
    }

    @Test
    fun `frameToTime converts frame at 24fps`() {
        // frame 24 at 24fps = 1 second
        assertEquals(1.0f, ModelAnimation.frameToTime(24, 24), 0.0001f)
    }

    @Test
    fun `frameToTime converts frame at 30fps`() {
        // frame 15 at 30fps = 0.5 seconds
        assertEquals(0.5f, ModelAnimation.frameToTime(15, 30), 0.0001f)
    }

    @Test
    fun `frameToTime converts frame at 60fps`() {
        // frame 120 at 60fps = 2 seconds
        assertEquals(2.0f, ModelAnimation.frameToTime(120, 60), 0.0001f)
    }

    @Test
    fun `frameToTime is inverse of timeToFrame for round values`() {
        val frameRate = 24
        val frame = 48
        val time = ModelAnimation.frameToTime(frame, frameRate)
        val backToFrame = ModelAnimation.timeToFrame(time, frameRate)
        assertEquals(frame, backToFrame)
    }

    // ── timeToFrame ──────────────────────────────────────────────────────────────

    @Test
    fun `timeToFrame converts time 0 to frame 0`() {
        assertEquals(0, ModelAnimation.timeToFrame(0f, 24))
    }

    @Test
    fun `timeToFrame converts 1 second at 24fps to frame 24`() {
        assertEquals(24, ModelAnimation.timeToFrame(1f, 24))
    }

    @Test
    fun `timeToFrame converts 0dot5 seconds at 30fps to frame 15`() {
        assertEquals(15, ModelAnimation.timeToFrame(0.5f, 30))
    }

    @Test
    fun `timeToFrame truncates fractional frames`() {
        // 1.9 seconds at 10fps = frame 19 (truncated from 19.0)
        assertEquals(19, ModelAnimation.timeToFrame(1.9f, 10))
    }

    @Test
    fun `timeToFrame converts 2 seconds at 60fps to frame 120`() {
        assertEquals(120, ModelAnimation.timeToFrame(2.0f, 60))
    }

    // ── fractionToTime ───────────────────────────────────────────────────────────

    @Test
    fun `fractionToTime fraction 0 returns time 0`() {
        assertEquals(0f, ModelAnimation.fractionToTime(0f, 5f), 0f)
    }

    @Test
    fun `fractionToTime fraction 1 returns full duration`() {
        assertEquals(5f, ModelAnimation.fractionToTime(1f, 5f), 0.0001f)
    }

    @Test
    fun `fractionToTime fraction 0dot5 returns half duration`() {
        assertEquals(2.5f, ModelAnimation.fractionToTime(0.5f, 5f), 0.0001f)
    }

    @Test
    fun `fractionToTime fraction 0dot25 returns quarter duration`() {
        assertEquals(1.25f, ModelAnimation.fractionToTime(0.25f, 5f), 0.0001f)
    }

    @Test
    fun `fractionToTime is inverse of timeToFraction`() {
        val duration = 3.6f
        val fraction = 0.7f
        val time = ModelAnimation.fractionToTime(fraction, duration)
        val back = ModelAnimation.timeToFraction(time, duration)
        assertEquals(fraction, back, 0.0001f)
    }

    // ── timeToFraction ───────────────────────────────────────────────────────────

    @Test
    fun `timeToFraction time 0 returns fraction 0`() {
        assertEquals(0f, ModelAnimation.timeToFraction(0f, 10f), 0f)
    }

    @Test
    fun `timeToFraction full duration returns fraction 1`() {
        assertEquals(1f, ModelAnimation.timeToFraction(10f, 10f), 0.0001f)
    }

    @Test
    fun `timeToFraction half duration returns fraction 0dot5`() {
        assertEquals(0.5f, ModelAnimation.timeToFraction(5f, 10f), 0.0001f)
    }

    @Test
    fun `timeToFraction one third duration returns fraction 0dot333`() {
        assertEquals(1f / 3f, ModelAnimation.timeToFraction(1f, 3f), 0.0001f)
    }

    // ── secondsToMillis ──────────────────────────────────────────────────────────

    @Test
    fun `secondsToMillis converts 0 seconds to 0ms`() {
        assertEquals(0L, ModelAnimation.secondsToMillis(0f))
    }

    @Test
    fun `secondsToMillis converts 1 second to 1000ms`() {
        assertEquals(1000L, ModelAnimation.secondsToMillis(1f))
    }

    @Test
    fun `secondsToMillis converts 0dot5 seconds to 500ms`() {
        assertEquals(500L, ModelAnimation.secondsToMillis(0.5f))
    }

    @Test
    fun `secondsToMillis converts 2dot5 seconds to 2500ms`() {
        assertEquals(2500L, ModelAnimation.secondsToMillis(2.5f))
    }

    @Test
    fun `secondsToMillis matches TimeUnit conversion for integer seconds`() {
        val seconds = 7f
        val expected = TimeUnit.SECONDS.toMillis(7)
        assertEquals(expected, ModelAnimation.secondsToMillis(seconds))
    }

    // ── frameCount via timeToFrame(getDuration, frameRate) ──────────────────────

    @Test
    fun `frame count for 1s at 24fps is 24`() {
        // equivalent of getFrameCount() with duration=1s, frameRate=24
        assertEquals(24, ModelAnimation.timeToFrame(1f, 24))
    }

    @Test
    fun `frame count for 2dot5s at 30fps is 75`() {
        assertEquals(75, ModelAnimation.timeToFrame(2.5f, 30))
    }
}

package io.github.sceneview.animation

import kotlin.test.Test
import kotlin.test.assertEquals

class AnimationTimeUtilsTest {

    @Test
    fun frameToTimeBasic() {
        assertEquals(0.5f, frameToTime(15, 30), 1e-5f)
    }

    @Test
    fun frameToTimeZeroFrame() {
        assertEquals(0f, frameToTime(0, 24), 1e-5f)
    }

    @Test
    fun timeToFrameBasic() {
        assertEquals(30, timeToFrame(1.0f, 30))
    }

    @Test
    fun timeToFrameHalf() {
        assertEquals(12, timeToFrame(0.5f, 24))
    }

    @Test
    fun fractionToTimeBasic() {
        assertEquals(1.5f, fractionToTime(0.5f, 3.0f), 1e-5f)
    }

    @Test
    fun fractionToTimeZero() {
        assertEquals(0f, fractionToTime(0f, 5.0f), 1e-5f)
    }

    @Test
    fun fractionToTimeOne() {
        assertEquals(2.0f, fractionToTime(1.0f, 2.0f), 1e-5f)
    }

    @Test
    fun timeToFractionBasic() {
        assertEquals(0.5f, timeToFraction(1.5f, 3.0f), 1e-5f)
    }

    @Test
    fun timeToFractionZeroDuration() {
        assertEquals(0f, timeToFraction(1.0f, 0f), 1e-5f)
    }

    @Test
    fun secondsToMillisBasic() {
        assertEquals(1500L, secondsToMillis(1.5f))
    }

    @Test
    fun secondsToMillisZero() {
        assertEquals(0L, secondsToMillis(0f))
    }

    @Test
    fun millisToSecondsBasic() {
        assertEquals(1.5f, millisToSeconds(1500L), 1e-5f)
    }

    @Test
    fun frameCountBasic() {
        assertEquals(72, frameCount(3.0f, 24))
    }

    @Test
    fun roundTripFrameTime() {
        val frameRate = 30
        val originalFrame = 45
        val time = frameToTime(originalFrame, frameRate)
        val recoveredFrame = timeToFrame(time, frameRate)
        assertEquals(originalFrame, recoveredFrame)
    }

    @Test
    fun roundTripFractionTime() {
        val duration = 4.0f
        val originalFraction = 0.75f
        val time = fractionToTime(originalFraction, duration)
        val fraction = timeToFraction(time, duration)
        assertEquals(originalFraction, fraction, 1e-5f)
    }
}

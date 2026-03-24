package io.github.sceneview

import io.github.sceneview.math.ulp
import io.github.sceneview.utils.nanoTime
import io.github.sceneview.logging.logWarning
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class JsPlatformTest {

    @Test
    fun ulpOfZeroIsMinValue() {
        assertEquals(Float.MIN_VALUE, ulp(0f))
    }

    @Test
    fun ulpOfOneIsSmall() {
        val result = ulp(1.0f)
        assertTrue(result > 0f)
        assertTrue(result < 0.001f)
    }

    @Test
    fun ulpOfNanIsNan() {
        assertTrue(ulp(Float.NaN).isNaN())
    }

    @Test
    fun ulpOfInfinityIsInfinity() {
        assertEquals(Float.POSITIVE_INFINITY, ulp(Float.POSITIVE_INFINITY))
    }

    @Test
    fun nanoTimeIncreases() {
        val t1 = nanoTime()
        // Small busy loop
        var sum = 0L
        for (i in 0..1000) sum += i
        val t2 = nanoTime()
        assertTrue(t2 >= t1, "nanoTime should be monotonic, got t1=$t1, t2=$t2")
    }

    @Test
    fun logWarningDoesNotThrow() {
        // Just ensure it doesn't crash
        logWarning("Test", "This is a test warning from JS")
    }
}

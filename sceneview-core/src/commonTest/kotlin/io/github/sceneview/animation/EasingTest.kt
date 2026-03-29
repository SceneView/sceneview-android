package io.github.sceneview.animation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EasingTest {

    private val epsilon = 0.01f

    // Helper: check that easing(0) ~ 0 and easing(1) ~ 1
    private fun assertEndpoints(easing: (Float) -> Float, name: String) {
        assertEquals(0f, easing(0f), epsilon, "$name at t=0")
        assertEquals(1f, easing(1f), epsilon, "$name at t=1")
    }

    // Helper: check monotonically increasing for standard easings
    private fun assertMonotonic(easing: (Float) -> Float, name: String) {
        var prev = easing(0f)
        for (i in 1..20) {
            val t = i / 20f
            val v = easing(t)
            // Allow small tolerance for floating point
            assertTrue(v >= prev - epsilon, "$name not monotonic at t=$t: $v < $prev")
            prev = v
        }
    }

    @Test
    fun linearEndpoints() = assertEndpoints(Easing.Linear, "Linear")

    @Test
    fun easeInEndpoints() = assertEndpoints(Easing.EaseIn, "EaseIn")

    @Test
    fun easeOutEndpoints() = assertEndpoints(Easing.EaseOut, "EaseOut")

    @Test
    fun easeInOutEndpoints() = assertEndpoints(Easing.EaseInOut, "EaseInOut")

    @Test
    fun easeInQuadEndpoints() = assertEndpoints(Easing.EaseInQuad, "EaseInQuad")

    @Test
    fun easeOutQuadEndpoints() = assertEndpoints(Easing.EaseOutQuad, "EaseOutQuad")

    @Test
    fun easeInOutQuadEndpoints() = assertEndpoints(Easing.EaseInOutQuad, "EaseInOutQuad")

    @Test
    fun easeInQuartEndpoints() = assertEndpoints(Easing.EaseInQuart, "EaseInQuart")

    @Test
    fun easeOutQuartEndpoints() = assertEndpoints(Easing.EaseOutQuart, "EaseOutQuart")

    @Test
    fun easeInQuintEndpoints() = assertEndpoints(Easing.EaseInQuint, "EaseInQuint")

    @Test
    fun easeOutQuintEndpoints() = assertEndpoints(Easing.EaseOutQuint, "EaseOutQuint")

    @Test
    fun easeInSineEndpoints() = assertEndpoints(Easing.EaseInSine, "EaseInSine")

    @Test
    fun easeOutSineEndpoints() = assertEndpoints(Easing.EaseOutSine, "EaseOutSine")

    @Test
    fun easeInOutSineEndpoints() = assertEndpoints(Easing.EaseInOutSine, "EaseInOutSine")

    @Test
    fun easeInCircEndpoints() = assertEndpoints(Easing.EaseInCirc, "EaseInCirc")

    @Test
    fun easeOutCircEndpoints() = assertEndpoints(Easing.EaseOutCirc, "EaseOutCirc")

    @Test
    fun easeInExpoEndpoints() = assertEndpoints(Easing.EaseInExpo, "EaseInExpo")

    @Test
    fun easeOutExpoEndpoints() = assertEndpoints(Easing.EaseOutExpo, "EaseOutExpo")

    @Test
    fun easeInBackEndpoints() = assertEndpoints(Easing.EaseInBack, "EaseInBack")

    @Test
    fun easeOutBackEndpoints() = assertEndpoints(Easing.EaseOutBack, "EaseOutBack")

    @Test
    fun easeInElasticEndpoints() = assertEndpoints(Easing.EaseInElastic, "EaseInElastic")

    @Test
    fun easeOutElasticEndpoints() = assertEndpoints(Easing.EaseOutElastic, "EaseOutElastic")

    @Test
    fun easeInBounceEndpoints() = assertEndpoints(Easing.EaseInBounce, "EaseInBounce")

    @Test
    fun easeOutBounceEndpoints() = assertEndpoints(Easing.EaseOutBounce, "EaseOutBounce")

    // Monotonicity tests (only for non-overshooting easings)
    @Test
    fun linearMonotonic() = assertMonotonic(Easing.Linear, "Linear")

    @Test
    fun easeInMonotonic() = assertMonotonic(Easing.EaseIn, "EaseIn")

    @Test
    fun easeOutMonotonic() = assertMonotonic(Easing.EaseOut, "EaseOut")

    @Test
    fun easeInOutMonotonic() = assertMonotonic(Easing.EaseInOut, "EaseInOut")

    @Test
    fun easeOutBounceValues() {
        // EaseOutBounce is monotonically increasing (but starts slow, then accelerates in each bounce)
        assertEquals(0f, Easing.EaseOutBounce(0f), epsilon)
        assertEquals(1f, Easing.EaseOutBounce(1f), epsilon)
        // At midpoint it should be past 0.5
        assertTrue(Easing.EaseOutBounce(0.5f) > 0.5f, "EaseOutBounce at t=0.5 should be > 0.5")
    }

    // Cubic bezier
    @Test
    fun cubicBezierLinear() {
        val linear = Easing.cubicBezier(0f, 0f, 1f, 1f)
        assertEquals(0f, linear(0f), 0.05f)
        assertEquals(0.5f, linear(0.5f), 0.05f)
        assertEquals(1f, linear(1f), 0.05f)
    }

    @Test
    fun cubicBezierEaseInOut() {
        val ease = Easing.cubicBezier(0.42f, 0f, 0.58f, 1f)
        assertEndpoints(ease, "cubicBezier ease-in-out")
    }

    // Spring easing
    @Test
    fun springEasingEndpoints() {
        val spring = Easing.spring(dampingRatio = 0.5f, stiffness = 500f)
        assertEquals(0f, spring(0f), 0.01f)
        // At t=1, should be close to 1 for most springs
        assertTrue(abs(spring(1f) - 1f) < 0.15f, "Spring should approach 1 by t=1")
    }

    // EaseIn is slower at start than linear
    @Test
    fun easeInSlowerAtStart() {
        val linearMid = Easing.Linear(0.3f)
        val easeInMid = Easing.EaseIn(0.3f)
        assertTrue(easeInMid < linearMid, "EaseIn should be below linear at t=0.3")
    }

    // EaseOut is faster at start than linear
    @Test
    fun easeOutFasterAtStart() {
        val linearMid = Easing.Linear(0.3f)
        val easeOutMid = Easing.EaseOut(0.3f)
        assertTrue(easeOutMid > linearMid, "EaseOut should be above linear at t=0.3")
    }
}

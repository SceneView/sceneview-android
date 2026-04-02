package io.github.sceneview.animation

import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.length
import dev.romainguy.kotlin.math.normalize
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class InterpolationTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 1e-4f) {
        assertTrue(
            abs(expected - actual) < epsilon,
            "Expected $expected but got $actual (delta=${abs(expected - actual)})"
        )
    }

    // ── lerp(Float) ──────────────────────────────────────────────────────────

    @Test
    fun lerpFloatAtZero() = assertClose(5f, lerp(5f, 10f, 0f))

    @Test
    fun lerpFloatAtOne() = assertClose(10f, lerp(5f, 10f, 1f))

    @Test
    fun lerpFloatAtHalf() = assertClose(7.5f, lerp(5f, 10f, 0.5f))

    @Test
    fun lerpFloatNegativeRange() = assertClose(-5f, lerp(-10f, 0f, 0.5f))

    @Test
    fun lerpFloatBeyondOne() {
        // lerp does not clamp — extrapolation is intentional
        assertClose(15f, lerp(0f, 10f, 1.5f))
    }

    // ── lerp(Float4) ─────────────────────────────────────────────────────────

    @Test
    fun lerpFloat4AtZero() {
        val result = lerp(Float4(1f, 2f, 3f, 4f), Float4(5f, 6f, 7f, 8f), 0f)
        assertClose(1f, result.x)
        assertClose(2f, result.y)
        assertClose(3f, result.z)
        assertClose(4f, result.w)
    }

    @Test
    fun lerpFloat4AtOne() {
        val result = lerp(Float4(1f, 2f, 3f, 4f), Float4(5f, 6f, 7f, 8f), 1f)
        assertClose(5f, result.x)
        assertClose(6f, result.y)
        assertClose(7f, result.z)
        assertClose(8f, result.w)
    }

    @Test
    fun lerpFloat4AtHalf() {
        val result = lerp(Float4(0f), Float4(2f), 0.5f)
        assertClose(1f, result.x)
        assertClose(1f, result.y)
        assertClose(1f, result.z)
        assertClose(1f, result.w)
    }

    // ── slerp(Quaternion) ─────────────────────────────────────────────────────

    @Test
    fun slerpAtZeroReturnsStart() {
        val q = normalize(Quaternion(0f, 0f, 0f, 1f)) // identity
        val p = normalize(Quaternion(0f, 1f, 0f, 1f))
        val result = slerp(q, p, 0f)
        // Should be very close to q
        assertTrue(abs(dot(result, q)) > 0.999f, "slerp(t=0) should equal start")
    }

    @Test
    fun slerpAtOneReturnsEnd() {
        val q = normalize(Quaternion(0f, 0f, 0f, 1f)) // identity
        val p = normalize(Quaternion(0f, 1f, 0f, 0f)) // 180° around Y
        val result = slerp(q, p, 1f)
        assertTrue(abs(dot(result, p)) > 0.999f, "slerp(t=1) should equal end")
    }

    @Test
    fun slerpResultIsNormalized() {
        val q = normalize(Quaternion(0.1f, 0.2f, 0.3f, 0.9f))
        val p = normalize(Quaternion(0.5f, 0.1f, 0.8f, 0.3f))
        for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val result = slerp(q, p, t)
            val len = length(result)
            assertTrue(abs(len - 1f) < 1e-4f, "slerp result not normalized at t=$t: len=$len")
        }
    }

    @Test
    fun slerpTakesShortestPath() {
        // q and -q represent the same rotation; slerp should NOT go the long way
        val q = normalize(Quaternion(0f, 0f, 0f, 1f))
        val negQ = Quaternion(-q.x, -q.y, -q.z, -q.w)
        val result = slerp(q, negQ, 0.5f)
        // Both paths give the same midpoint (identity), but the length should be 1
        assertTrue(abs(length(result) - 1f) < 1e-4f)
    }

    // ── Easing: extended functions ───────────────────────────────────────────

    @Test
    fun easingQuadInAtBounds() {
        assertClose(0f, Easing.EaseInQuad(0f))
        assertClose(1f, Easing.EaseInQuad(1f))
    }

    @Test
    fun easingQuadOutAtBounds() {
        assertClose(0f, Easing.EaseOutQuad(0f))
        assertClose(1f, Easing.EaseOutQuad(1f))
    }

    @Test
    fun easingQuartInAtBounds() {
        assertClose(0f, Easing.EaseInQuart(0f))
        assertClose(1f, Easing.EaseInQuart(1f))
    }

    @Test
    fun easingQuartOutAtBounds() {
        assertClose(0f, Easing.EaseOutQuart(0f))
        assertClose(1f, Easing.EaseOutQuart(1f))
    }

    @Test
    fun easingExpoInAtBounds() {
        assertClose(0f, Easing.EaseInExpo(0f))
        assertClose(1f, Easing.EaseInExpo(1f), epsilon = 1e-3f)
    }

    @Test
    fun easingExpoOutAtBounds() {
        assertClose(0f, Easing.EaseOutExpo(0f), epsilon = 1e-3f)
        assertClose(1f, Easing.EaseOutExpo(1f))
    }

    @Test
    fun easingSineInAtBounds() {
        assertClose(0f, Easing.EaseInSine(0f))
        assertClose(1f, Easing.EaseInSine(1f), epsilon = 1e-4f)
    }

    @Test
    fun easingSineOutAtBounds() {
        assertClose(0f, Easing.EaseOutSine(0f), epsilon = 1e-6f)
        assertClose(1f, Easing.EaseOutSine(1f), epsilon = 1e-4f)
    }

    @Test
    fun easingBounceOutAtBounds() {
        assertClose(0f, Easing.EaseOutBounce(0f))
        assertClose(1f, Easing.EaseOutBounce(1f))
    }

    @Test
    fun easingBounceInAtBounds() {
        assertClose(0f, Easing.EaseInBounce(0f))
        assertClose(1f, Easing.EaseInBounce(1f))
    }

    @Test
    fun easingElasticOutAtBounds() {
        assertClose(0f, Easing.EaseOutElastic(0f))
        assertClose(1f, Easing.EaseOutElastic(1f))
    }

    @Test
    fun easingElasticInAtBounds() {
        assertClose(0f, Easing.EaseInElastic(0f))
        assertClose(1f, Easing.EaseInElastic(1f))
    }

    @Test
    fun easingBackOutOvershootsAtMiddle() {
        // EaseOutBack should briefly exceed 1 near t=0.8
        var maxValue = 0f
        for (i in 0..100) {
            val t = i / 100f
            val v = Easing.EaseOutBack(t)
            if (v > maxValue) maxValue = v
        }
        assertTrue(maxValue > 1f, "EaseOutBack should overshoot 1, maxValue=$maxValue")
    }

    @Test
    fun easingCubicBezierLinearApproximation() {
        // cubic-bezier(0,0,1,1) approximates linear
        val linear = Easing.cubicBezier(0f, 0f, 1f, 1f)
        assertClose(0f, linear(0f))
        assertClose(0.5f, linear(0.5f), epsilon = 0.05f)
        assertClose(1f, linear(1f))
    }

    @Test
    fun easingSpringUnderdampedOvershoots() {
        val springEasing = Easing.spring(dampingRatio = 0.3f, stiffness = 500f)
        var maxValue = 0f
        for (i in 1..200) {
            val t = i / 200f
            val v = springEasing(t)
            if (v > maxValue) maxValue = v
        }
        assertTrue(maxValue > 1f, "Underdamped spring easing should overshoot, max=$maxValue")
    }

    @Test
    fun easingSpringCriticallyDampedApproachesOne() {
        val springEasing = Easing.spring(dampingRatio = 1f, stiffness = 300f)
        // At t=1 (end of normalized time), should be very close to 1
        val v = springEasing(1f)
        assertTrue(v > 0.9f, "Critically damped spring should approach 1 at t=1, got $v")
    }
}

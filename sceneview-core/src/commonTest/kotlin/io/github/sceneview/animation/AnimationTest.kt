package io.github.sceneview.animation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.01f) {
        assertTrue(
            abs(expected - actual) < epsilon,
            "Expected $expected but got $actual (delta=${abs(expected - actual)})"
        )
    }

    // --- Lerp ---

    @Test
    fun lerpAtZero() {
        assertClose(0f, lerp(0f, 10f, 0f))
    }

    @Test
    fun lerpAtHalf() {
        assertClose(5f, lerp(0f, 10f, 0.5f))
    }

    @Test
    fun lerpAtOne() {
        assertClose(10f, lerp(0f, 10f, 1f))
    }

    // --- Easing ---

    @Test
    fun easingLinearAtBounds() {
        assertClose(0f, Easing.Linear(0f))
        assertClose(1f, Easing.Linear(1f))
    }

    @Test
    fun easingCubicInAtBounds() {
        assertClose(0f, Easing.EaseIn(0f))
        assertClose(1f, Easing.EaseIn(1f))
    }

    @Test
    fun easingCubicOutAtBounds() {
        assertClose(0f, Easing.EaseOut(0f))
        assertClose(1f, Easing.EaseOut(1f))
    }

    @Test
    fun easingCubicInOutAtBounds() {
        assertClose(0f, Easing.EaseInOut(0f))
        assertClose(1f, Easing.EaseInOut(1f))
    }

    @Test
    fun easeInSlowerThanLinearAtStart() {
        assertTrue(Easing.EaseIn(0.25f) < Easing.Linear(0.25f))
    }

    @Test
    fun easeOutFasterThanLinearAtStart() {
        assertTrue(Easing.EaseOut(0.25f) > Easing.Linear(0.25f))
    }

    // --- PropertyAnimation ---

    @Test
    fun animationAdvances() {
        val state = AnimationState(startValue = 0f, endValue = 10f, durationSeconds = 1f)
        val updated = animate(state, 0.5f)
        assertClose(5f, updated.value)
        assertTrue(updated.isRunning)
    }

    @Test
    fun animationStopsAtEnd() {
        val state = AnimationState(startValue = 0f, endValue = 10f, durationSeconds = 1f)
        val updated = animate(state, 2f)
        assertClose(10f, updated.value)
        assertFalse(updated.isRunning)
        assertTrue(updated.isFinished)
    }

    @Test
    fun loopingAnimationWraps() {
        val state = AnimationState(
            startValue = 0f, endValue = 10f,
            durationSeconds = 1f,
            playbackMode = PlaybackMode.LOOP
        )
        val updated = animate(state, 1.5f)
        assertTrue(updated.isRunning)
        assertClose(0.5f, updated.linearFraction)
    }

    // --- SpringAnimator ---

    @Test
    fun springConvergesToTarget() {
        val animator = SpringAnimator(SpringConfig.SMOOTH)
        repeat(120) { animator.update(1f / 60f) }
        assertTrue(animator.isSettled)
        assertClose(1f, animator.value)
    }

    @Test
    fun bouncySpringOvershoots() {
        val animator = SpringAnimator(SpringConfig.BOUNCY)
        var maxValue = 0f
        repeat(60) {
            animator.update(1f / 60f)
            if (animator.value > maxValue) maxValue = animator.value
        }
        assertTrue(maxValue > 1f, "Bouncy spring should overshoot, max=$maxValue")
    }

    @Test
    fun springResetRestoresInitialState() {
        val animator = SpringAnimator(SpringConfig.STIFF)
        repeat(120) { animator.update(1f / 60f) }
        animator.reset()
        assertClose(0f, animator.value)
        assertFalse(animator.isSettled)
    }
}

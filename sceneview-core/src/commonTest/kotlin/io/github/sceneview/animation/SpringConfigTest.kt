package io.github.sceneview.animation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SpringConfigTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.01f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected but got $actual")
    }

    // ── SpringConfig validation ──────────────────────────────────────────────

    @Test
    fun springConfigValidStiffnessAndDamping() {
        val config = SpringConfig(stiffness = 500f, dampingRatio = 0.8f)
        assertEquals(500f, config.stiffness)
        assertEquals(0.8f, config.dampingRatio)
    }

    @Test
    fun springConfigZeroDampingIsValid() {
        // dampingRatio = 0 means undamped oscillation
        val config = SpringConfig(stiffness = 100f, dampingRatio = 0f)
        assertEquals(0f, config.dampingRatio)
    }

    @Test
    fun springConfigNegativeStiffnessThrows() {
        assertFailsWith<IllegalArgumentException> {
            SpringConfig(stiffness = -1f, dampingRatio = 1f)
        }
    }

    @Test
    fun springConfigZeroStiffnessThrows() {
        assertFailsWith<IllegalArgumentException> {
            SpringConfig(stiffness = 0f, dampingRatio = 1f)
        }
    }

    @Test
    fun springConfigNegativeDampingThrows() {
        assertFailsWith<IllegalArgumentException> {
            SpringConfig(stiffness = 100f, dampingRatio = -0.1f)
        }
    }

    @Test
    fun springConfigPresetsExist() {
        // Smoke-test that companion presets are accessible
        assertTrue(SpringConfig.BOUNCY.dampingRatio < 1f, "BOUNCY should be underdamped")
        assertTrue(SpringConfig.SMOOTH.dampingRatio == 1f, "SMOOTH should be critically damped")
        assertTrue(SpringConfig.STIFF.stiffness > SpringConfig.SMOOTH.stiffness, "STIFF stiffer than SMOOTH")
    }

    @Test
    fun springConfigInitialVelocityDefault() {
        val config = SpringConfig(stiffness = 100f, dampingRatio = 1f)
        assertEquals(0f, config.initialVelocity)
    }

    @Test
    fun springConfigWithInitialVelocity() {
        val config = SpringConfig(stiffness = 100f, dampingRatio = 1f, initialVelocity = 5f)
        assertEquals(5f, config.initialVelocity)
    }

    // ── SpringAnimator behaviour ──────────────────────────────────────────────

    @Test
    fun springAnimatorStartsAtZero() {
        val animator = SpringAnimator(SpringConfig.SMOOTH)
        assertClose(0f, animator.value)
    }

    @Test
    fun springAnimatorNotSettledInitially() {
        val animator = SpringAnimator(SpringConfig.SMOOTH)
        assertTrue(!animator.isSettled)
    }

    @Test
    fun springAnimatorOverdampedSettles() {
        val config = SpringConfig(stiffness = 100f, dampingRatio = 2f) // overdamped
        val animator = SpringAnimator(config)
        repeat(300) { animator.update(1f / 60f) }
        assertTrue(animator.isSettled, "Overdamped spring should settle")
        assertClose(1f, animator.value)
    }

    @Test
    fun springAnimatorValueIncreasesOverTime() {
        val animator = SpringAnimator(SpringConfig.SMOOTH)
        val initial = animator.value
        animator.update(1f / 60f)
        assertTrue(animator.value > initial, "Value should increase toward target")
    }

    @Test
    fun springAnimatorNegativeInitialVelocitySlowsStart() {
        // Negative initial velocity means we start moving away before converging
        val config = SpringConfig(stiffness = 300f, dampingRatio = 0.8f, initialVelocity = -10f)
        val animator = SpringAnimator(config)
        animator.update(1f / 60f)
        // Value can go below 0 initially due to negative velocity
        // After many frames, it should still settle
        repeat(300) { animator.update(1f / 60f) }
        assertTrue(animator.isSettled, "Should settle despite negative initial velocity")
    }

    @Test
    fun springAnimatorMultipleResetsWork() {
        val animator = SpringAnimator(SpringConfig.SMOOTH)
        repeat(120) { animator.update(1f / 60f) }
        assertTrue(animator.isSettled)

        animator.reset()
        assertClose(0f, animator.value)
        assertTrue(!animator.isSettled)

        repeat(120) { animator.update(1f / 60f) }
        assertTrue(animator.isSettled)
        assertClose(1f, animator.value)
    }
}

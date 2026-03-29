package io.github.sceneview.animation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationSequenceTest {

    @Test
    fun singleStepEvaluation() {
        val seq = AnimationSequence(listOf(
            SequenceStep(0f, 10f, 1f)
        ))
        assertEquals(0f, seq.evaluate(0f), 1e-5f)
        assertEquals(5f, seq.evaluate(0.5f), 1e-5f)
        assertEquals(10f, seq.evaluate(1f), 1e-5f)
    }

    @Test
    fun twoSteps() {
        val seq = AnimationSequence(listOf(
            SequenceStep(0f, 10f, 1f),
            SequenceStep(10f, 0f, 1f)
        ))
        assertEquals(0f, seq.evaluate(0f), 1e-5f)
        assertEquals(10f, seq.evaluate(1f), 1e-5f)
        assertEquals(0f, seq.evaluate(2f), 1e-5f)
        // Midpoint of second step
        assertEquals(5f, seq.evaluate(1.5f), 1e-5f)
    }

    @Test
    fun totalDuration() {
        val seq = AnimationSequence(listOf(
            SequenceStep(0f, 1f, 0.5f),
            SequenceStep(1f, 2f, 0.3f),
            SequenceStep(2f, 0f, 0.2f)
        ))
        assertEquals(1f, seq.totalDuration, 1e-5f)
    }

    @Test
    fun loopingSequence() {
        val seq = AnimationSequence(
            listOf(SequenceStep(0f, 10f, 1f)),
            loop = true
        )
        // At t=1.5 with loop, should be at 0.5 of the sequence
        assertEquals(5f, seq.evaluate(1.5f), 1e-4f)
    }

    @Test
    fun clampsPastEnd() {
        val seq = AnimationSequence(listOf(
            SequenceStep(0f, 10f, 1f)
        ))
        assertEquals(10f, seq.evaluate(5f), 1e-5f)
    }

    @Test
    fun withEasing() {
        val seq = AnimationSequence(listOf(
            SequenceStep(0f, 10f, 1f, Easing.EaseIn)
        ))
        val mid = seq.evaluate(0.5f)
        // EaseIn at 0.5 should be below linear midpoint (5)
        assertTrue(mid < 5f, "EaseIn midpoint should be < 5, got $mid")
    }

    // --- SequenceState ---

    @Test
    fun sequenceStateAdvances() {
        val seq = AnimationSequence(listOf(
            SequenceStep(0f, 10f, 1f),
            SequenceStep(10f, 0f, 1f)
        ))
        var state = SequenceState(seq)
        state = advanceSequence(state, 0.5f)
        assertEquals(5f, state.value, 1e-4f)
        assertEquals(0, state.currentStepIndex)
    }

    @Test
    fun sequenceStateSecondStep() {
        val seq = AnimationSequence(listOf(
            SequenceStep(0f, 10f, 1f),
            SequenceStep(10f, 0f, 1f)
        ))
        var state = SequenceState(seq)
        state = advanceSequence(state, 1.5f)
        assertEquals(1, state.currentStepIndex)
    }

    @Test
    fun sequenceStateFinishes() {
        val seq = AnimationSequence(listOf(
            SequenceStep(0f, 10f, 1f)
        ))
        var state = SequenceState(seq)
        state = advanceSequence(state, 2f)
        assertTrue(state.isFinished)
    }

    // --- Builder DSL ---

    @Test
    fun builderDsl() {
        val seq = animationSequence {
            step(0f, 1f, 0.5f)
            delay(0.2f)
            step(1f, 0f, 0.3f, Easing.EaseOut)
            loop = true
        }
        assertEquals(3, seq.steps.size)
        assertTrue(seq.loop)
        assertEquals(1f, seq.totalDuration, 1e-5f)
    }

    @Test
    fun builderDelay() {
        val seq = animationSequence {
            step(0f, 5f, 1f)
            delay(0.5f)
        }
        // During delay, value should stay at 5
        assertEquals(5f, seq.evaluate(1.25f), 1e-4f)
    }
}

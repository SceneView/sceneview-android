package io.github.sceneview.animation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyframeAnimationTest {

    @Test
    fun singleKeyframeReturnsConstant() {
        val track = KeyframeTrack(listOf(Keyframe(0f, 5f)))
        assertEquals(5f, track.evaluate(0f), 1e-5f)
        assertEquals(5f, track.evaluate(100f), 1e-5f)
    }

    @Test
    fun twoKeyframeLinearInterpolation() {
        val track = KeyframeTrack(listOf(
            Keyframe(0f, 0f),
            Keyframe(1f, 10f)
        ))
        assertEquals(0f, track.evaluate(0f), 1e-5f)
        assertEquals(5f, track.evaluate(0.5f), 1e-5f)
        assertEquals(10f, track.evaluate(1f), 1e-5f)
    }

    @Test
    fun keyframeBeforeFirstReturnsFirst() {
        val track = KeyframeTrack(listOf(
            Keyframe(1f, 5f),
            Keyframe(2f, 10f)
        ))
        assertEquals(5f, track.evaluate(0f), 1e-5f)
    }

    @Test
    fun keyframeAfterLastReturnsLast() {
        val track = KeyframeTrack(listOf(
            Keyframe(0f, 0f),
            Keyframe(1f, 10f)
        ))
        assertEquals(10f, track.evaluate(5f), 1e-5f)
    }

    @Test
    fun keyframeWithEasing() {
        val track = KeyframeTrack(listOf(
            Keyframe(0f, 0f, Easing.EaseIn),
            Keyframe(1f, 10f)
        ))
        // EaseIn at t=0.5 should be below linear midpoint (5)
        val value = track.evaluate(0.5f)
        assertTrue(value < 5f, "EaseIn should produce value < 5 at midpoint, got $value")
    }

    @Test
    fun threeKeyframes() {
        val track = KeyframeTrack(listOf(
            Keyframe(0f, 0f),
            Keyframe(1f, 10f),
            Keyframe(2f, 0f)
        ))
        assertEquals(0f, track.evaluate(0f), 1e-5f)
        assertEquals(10f, track.evaluate(1f), 1e-5f)
        assertEquals(0f, track.evaluate(2f), 1e-5f)
    }

    @Test
    fun keyframeDuration() {
        val track = KeyframeTrack(listOf(
            Keyframe(0f, 0f),
            Keyframe(3f, 10f)
        ))
        assertEquals(3f, track.duration, 1e-5f)
    }

    @Test
    fun keyframesSortedByTime() {
        val track = KeyframeTrack(listOf(
            Keyframe(2f, 20f),
            Keyframe(0f, 0f),
            Keyframe(1f, 10f)
        ))
        assertEquals(0f, track.keyframes[0].time, 1e-5f)
        assertEquals(1f, track.keyframes[1].time, 1e-5f)
        assertEquals(2f, track.keyframes[2].time, 1e-5f)
    }

    // --- KeyframeAnimationState ---

    @Test
    fun keyframeAnimationAdvances() {
        val track = KeyframeTrack(listOf(
            Keyframe(0f, 0f),
            Keyframe(1f, 10f)
        ))
        var state = KeyframeAnimationState(track)
        state = advanceKeyframeAnimation(state, 0.5f)
        assertEquals(5f, state.value, 1e-4f)
    }

    @Test
    fun keyframeAnimationFinishes() {
        val track = KeyframeTrack(listOf(
            Keyframe(0f, 0f),
            Keyframe(1f, 10f)
        ))
        var state = KeyframeAnimationState(track)
        state = advanceKeyframeAnimation(state, 2f)
        assertTrue(state.isFinished)
        assertEquals(10f, state.value, 1e-4f)
    }

    @Test
    fun keyframeAnimationLoops() {
        val track = KeyframeTrack(listOf(
            Keyframe(0f, 0f),
            Keyframe(1f, 10f)
        ))
        var state = KeyframeAnimationState(track, loop = true)
        state = advanceKeyframeAnimation(state, 1.5f)
        assertFalse(state.isFinished)
        // Should have looped: 1.5 % 1.0 = 0.5
        assertEquals(5f, state.value, 1e-4f)
    }
}

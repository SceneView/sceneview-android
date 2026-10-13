package io.github.sceneview.ar

import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ARTrackingStateLogTest {

    private val second = 1_000_000_000L

    /** One frame every 33 ms from [fromNanos], for [frames] frames; the lines that came out. */
    private fun ARTrackingStateLog.frames(
        state: TrackingState,
        reason: TrackingFailureReason,
        fromNanos: Long,
        frames: Int,
    ): List<String> = (0 until frames).mapNotNull { index ->
        transition(state, reason, fromNanos + index * 33_000_000L)
    }

    @Test
    fun `the first frame is logged, a camera that never tracks included`() {
        val log = ARTrackingStateLog()

        assertEquals(
            "AR tracking: state=PAUSED reason=NONE",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 5 * second)
        )
    }

    @Test
    fun `an unchanged state is logged once, not once per frame`() {
        val log = ARTrackingStateLog()

        // 300 frames is 9.9 s: one line, the first.
        val lines = log.frames(TrackingState.PAUSED, TrackingFailureReason.NONE, 0L, frames = 300)

        assertEquals(listOf("AR tracking: state=PAUSED reason=NONE"), lines)
    }

    @Test
    fun `a reason that changes under the same state is logged`() {
        val log = ARTrackingStateLog()
        log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 0L)

        assertEquals(
            "AR tracking: state=PAUSED reason=CAMERA_UNAVAILABLE",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.CAMERA_UNAVAILABLE, second)
        )
        assertEquals(
            "AR tracking: state=PAUSED reason=BAD_STATE",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.BAD_STATE, 2 * second)
        )
    }

    @Test
    fun `reaching tracking and losing it again are both logged`() {
        val log = ARTrackingStateLog()
        log.transition(TrackingState.PAUSED, TrackingFailureReason.INSUFFICIENT_FEATURES, 0L)

        assertEquals(
            "AR tracking: state=TRACKING reason=NONE",
            log.transition(TrackingState.TRACKING, TrackingFailureReason.NONE, second)
        )
        assertNull(log.transition(TrackingState.TRACKING, TrackingFailureReason.NONE, 2 * second))
        assertEquals(
            "AR tracking: state=PAUSED reason=EXCESSIVE_MOTION",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.EXCESSIVE_MOTION, 3 * second)
        )
    }

    @Test
    fun `a camera that stays out of tracking says so again every ten seconds, with how long`() {
        val log = ARTrackingStateLog()

        // 35 s of frames in the same state, starting at an arbitrary session time.
        val lines =
            log.frames(TrackingState.PAUSED, TrackingFailureReason.NONE, 100 * second, frames = 1061)

        assertEquals(
            listOf(
                "AR tracking: state=PAUSED reason=NONE",
                "AR tracking: state=PAUSED reason=NONE for=10s",
                "AR tracking: state=PAUSED reason=NONE for=20s",
                "AR tracking: state=PAUSED reason=NONE for=30s",
            ),
            lines
        )
    }

    @Test
    fun `a tracking camera repeats nothing, however long it lasts`() {
        val log = ARTrackingStateLog()

        // Ten minutes.
        val lines =
            log.frames(TrackingState.TRACKING, TrackingFailureReason.NONE, 0L, frames = 18_200)

        assertEquals(listOf("AR tracking: state=TRACKING reason=NONE"), lines)
    }

    @Test
    fun `the duration counts from the transition, so lost tracking reads apart from never tracked`() {
        val log = ARTrackingStateLog()
        log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 0L)
        log.transition(TrackingState.TRACKING, TrackingFailureReason.NONE, second)
        log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 60 * second)

        assertNull(log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 69 * second))
        assertEquals(
            "AR tracking: state=PAUSED reason=NONE for=12s",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 72 * second)
        )
    }

    @Test
    fun `a timestamp that goes back starts the count over instead of reporting a stale duration`() {
        val log = ARTrackingStateLog()
        log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 500 * second)

        // Another recording, or a session that was recreated: its clock starts lower.
        assertNull(log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 2 * second))
        assertNull(log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 11 * second))
        assertEquals(
            "AR tracking: state=PAUSED reason=NONE for=10s",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE, 12 * second)
        )
    }
}

package io.github.sceneview.ar

import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ARTrackingStateLogTest {

    @Test
    fun `the first frame is logged, a camera that never tracks included`() {
        val log = ARTrackingStateLog()

        assertEquals(
            "AR tracking: state=PAUSED reason=NONE",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE)
        )
    }

    @Test
    fun `an unchanged state is logged once, not once per frame`() {
        val log = ARTrackingStateLog()
        log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE)

        repeat(300) {
            assertNull(log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE))
        }
    }

    @Test
    fun `a reason that changes under the same state is logged`() {
        val log = ARTrackingStateLog()
        log.transition(TrackingState.PAUSED, TrackingFailureReason.NONE)

        assertEquals(
            "AR tracking: state=PAUSED reason=CAMERA_UNAVAILABLE",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.CAMERA_UNAVAILABLE)
        )
        assertEquals(
            "AR tracking: state=PAUSED reason=BAD_STATE",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.BAD_STATE)
        )
    }

    @Test
    fun `reaching tracking and losing it again are both logged`() {
        val log = ARTrackingStateLog()
        log.transition(TrackingState.PAUSED, TrackingFailureReason.INSUFFICIENT_FEATURES)

        assertEquals(
            "AR tracking: state=TRACKING reason=NONE",
            log.transition(TrackingState.TRACKING, TrackingFailureReason.NONE)
        )
        assertNull(log.transition(TrackingState.TRACKING, TrackingFailureReason.NONE))
        assertEquals(
            "AR tracking: state=PAUSED reason=EXCESSIVE_MOTION",
            log.transition(TrackingState.PAUSED, TrackingFailureReason.EXCESSIVE_MOTION)
        )
    }
}

package io.github.sceneview.demo.common

import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARMotionSensorHealth
import io.github.sceneview.ar.ARMotionSensorSample
import io.github.sceneview.ar.PlacementPhase
import io.github.sceneview.demo.common.placement.PlacementCard
import io.github.sceneview.demo.common.placement.placementCard
import io.github.sceneview.demo.feedback.BugReportInfo
import io.github.sceneview.demo.feedback.formatShareText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which message an AR screen that is not tracking shows (#4522): the restart message only for
 * a session that never tracked, for ten seconds, with no reason from ARCore, on a phone whose
 * gyroscope was measured silent. Everything else stays as it is.
 */
class MotionSensorCheckTest {

    private fun startup(
        framesArriving: Boolean = true,
        everTracked: Boolean = false,
        tracking: Boolean = false,
        failureReason: TrackingFailureReason? = null,
        waitedMillis: Long = AR_NEVER_TRACKED_MS,
    ) = ArStartup(framesArriving, everTracked, tracking, failureReason, waitedMillis)

    private val silent = ARMotionSensorHealth.SILENT

    // ── The message ──────────────────────────────────────────────────────────────────────

    @Test
    fun `never tracked for ten seconds on a silent gyroscope says restart`() {
        assertEquals(ArStartupMessage.RESTART_PHONE, arStartupMessage(startup(), silent))
        // ARCore's `NONE` is the same as no reason at all.
        assertEquals(
            ArStartupMessage.RESTART_PHONE,
            arStartupMessage(startup(failureReason = TrackingFailureReason.NONE), silent),
        )
    }

    @Test
    fun `a session that has only just started says nothing, whatever the sensors`() {
        // A working phone reaches tracking in about a second: nine seconds in is still starting.
        listOf(0L, 1_000L, AR_NEVER_TRACKED_MS - 1).forEach { waited ->
            assertEquals(
                "after $waited ms",
                ArStartupMessage.NONE,
                arStartupMessage(startup(waitedMillis = waited), silent),
            )
        }
    }

    @Test
    fun `tracking lost after it had started never says restart`() {
        // The coaching card owns this one: "Keep looking around", "Point back at your object".
        val lost = startup(everTracked = true, waitedMillis = 60_000)
        assertEquals(ArStartupMessage.NONE, arStartupMessage(lost, silent))
        assertFalse(lost.neverStartedUnexplained)
    }

    @Test
    fun `a reason from ARCore keeps its own message`() {
        // A white wall, a dark room, a camera another app holds: ARCore says so, and the
        // coaching card already tells the person what to change.
        TrackingFailureReason.values().filter { it != TrackingFailureReason.NONE }.forEach { reason ->
            assertEquals(
                reason.name,
                ArStartupMessage.NONE,
                arStartupMessage(startup(failureReason = reason, waitedMillis = 60_000), silent),
            )
        }
    }

    @Test
    fun `a gyroscope that delivers says nothing, however long the surface takes`() {
        // Sensors alive, no surface yet — or a phone lying still on a desk.
        val waiting = startup(waitedMillis = 120_000)
        assertEquals(ArStartupMessage.NONE, arStartupMessage(waiting, ARMotionSensorHealth.DELIVERING))
    }

    @Test
    fun `no verdict says nothing`() {
        assertEquals(ArStartupMessage.NONE, arStartupMessage(startup(), ARMotionSensorHealth.UNKNOWN))
        // Not measured yet, or a phone with no gyroscope at all.
        assertEquals(ArStartupMessage.NONE, arStartupMessage(startup(), null))
    }

    @Test
    fun `a session that is tracking says nothing`() {
        val tracking = startup(everTracked = true, tracking = true)
        assertEquals(ArStartupMessage.NONE, arStartupMessage(tracking, silent))
    }

    @Test
    fun `no camera frame is the camera error's business, not this one's`() {
        assertEquals(ArStartupMessage.NONE, arStartupMessage(startup(framesArriving = false), silent))
    }

    // ── When the sensors are listened to ─────────────────────────────────────────────────

    @Test
    fun `the sensors are listened to once, at the ten second mark`() {
        assertFalse(motionSensorProbeDue(startup(waitedMillis = AR_NEVER_TRACKED_MS - 1), alreadyProbed = false))
        assertTrue(motionSensorProbeDue(startup(), alreadyProbed = false))
        assertFalse(motionSensorProbeDue(startup(), alreadyProbed = true))
    }

    @Test
    fun `the sensors are never listened to while tracking works, or once it has`() {
        assertFalse(motionSensorProbeDue(startup(tracking = true, everTracked = true), alreadyProbed = false))
        assertFalse(motionSensorProbeDue(startup(everTracked = true), alreadyProbed = false))
    }

    @Test
    fun `the sensors are not listened to when ARCore explains the wait, or when no frame came`() {
        val explained = startup(failureReason = TrackingFailureReason.INSUFFICIENT_FEATURES)
        assertFalse(motionSensorProbeDue(explained, alreadyProbed = false))
        assertFalse(motionSensorProbeDue(startup(framesArriving = false), alreadyProbed = false))
    }

    @Test
    fun `the ten second mark is the one the funnel reports a stalled session at`() {
        assertEquals(10_000L, AR_NEVER_TRACKED_MS)
    }

    // ── The card ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `the restart card outranks every phase, and is absent without the verdict`() {
        PlacementPhase.values().forEach { phase ->
            assertEquals(
                phase.name,
                PlacementCard.MOTION_SENSOR_SILENT,
                placementCard(phase, motionSensorSilent = true),
            )
        }
        assertNull(placementCard(PlacementPhase.INITIALIZING, motionSensorSilent = false))
        assertEquals(placementCard(PlacementPhase.NO_SURFACE), placementCard(PlacementPhase.NO_SURFACE, false))
    }

    // ── The bug report ───────────────────────────────────────────────────────────────────

    @Test
    fun `the report line carries both counts and the window`() {
        assertEquals(
            "0 events in 1 s (accelerometer: 204)",
            MotionSensorReport.line(ARMotionSensorSample(0, 204, 1_000)),
        )
        assertEquals(
            "198 events in 1.5 s (accelerometer: 301)",
            MotionSensorReport.line(ARMotionSensorSample(198, 301, 1_500)),
        )
    }

    @Test
    fun `nothing measured, no line`() {
        assertNull(MotionSensorReport.line(null))
    }

    @Test
    fun `the report text reads gyroscope N events in 1 s`() {
        val line = MotionSensorReport.line(ARMotionSensorSample(0, 204, 1_000))!!
        val info = BugReportInfo(
            metadata = linkedMapOf("Device" to "Google Pixel 9", MotionSensorReport.KEY to line),
            logcat = emptyList(),
        )
        assertTrue(
            formatShareText(info, note = "AR never starts")
                .lines()
                .contains("gyroscope: 0 events in 1 s (accelerometer: 204)"),
        )
    }
}

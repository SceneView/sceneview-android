package io.github.sceneview.demo.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARMotionSensorHealth
import io.github.sceneview.ar.ARMotionSensorSample
import io.github.sceneview.ar.probeARMotionSensors
import io.github.sceneview.demo.DemoSettings
import io.github.sceneview.demo.telemetry.AnalyticsEvent
import io.github.sceneview.demo.telemetry.ArFunnelLog
import io.github.sceneview.demo.telemetry.Telemetry
import kotlinx.coroutines.delay

/**
 * Where an AR session stands on the way to its first tracked frame — everything
 * [arStartupMessage] and [motionSensorProbeDue] decide on (#4522).
 *
 * @property framesArriving the session has delivered at least one camera frame.
 * @property everTracked the camera reached `TrackingState.TRACKING` at least once.
 * @property tracking the camera is tracking now.
 * @property failureReason ARCore's own reason for not tracking; `null` and `NONE` both mean
 *   it gave none.
 * @property waitedMillis how long, in the foreground, the session has been delivering frames
 *   without tracking and without a reason.
 */
data class ArStartup(
    val framesArriving: Boolean,
    val everTracked: Boolean,
    val tracking: Boolean,
    val failureReason: TrackingFailureReason?,
    val waitedMillis: Long,
) {
    /**
     * The silent stall: a live camera image, no tracking ever, and nothing from ARCore to
     * explain it. A session that tracked and lost it is not this — its coaching card says what
     * to do — and neither is one ARCore gives a reason for (a white wall, a dark room).
     */
    val neverStartedUnexplained: Boolean
        get() = framesArriving && !everTracked && !tracking &&
            (failureReason == null || failureReason == TrackingFailureReason.NONE)

    /** [neverStartedUnexplained] for [AR_NEVER_TRACKED_MS] — a working phone tracks in about a second. */
    val stalled: Boolean
        get() = neverStartedUnexplained && waitedMillis >= AR_NEVER_TRACKED_MS
}

/** What the AR screen says instead of "Getting ready". */
enum class ArStartupMessage {
    /** Nothing of its own: the screen stays exactly as it is. */
    NONE,

    /** "Restart your phone — its motion sensor isn't responding, and AR can't start without it." */
    RESTART_PHONE,
}

/**
 * How long a session may deliver frames without ever tracking before the motion sensors are
 * checked. The same ten seconds after which the funnel files `ArTrackingStalled` (#4512).
 */
const val AR_NEVER_TRACKED_MS: Long = ArFunnelLog.TRACKING_STALL_MS

/** Whether the motion sensors should be listened to now. Once per session, never while tracking works. */
fun motionSensorProbeDue(startup: ArStartup, alreadyProbed: Boolean): Boolean =
    startup.stalled && !alreadyProbed

/**
 * The one decision of #4522: the restart message only for a session that is [ArStartup.stalled]
 * **and** whose gyroscope was measured silent while the accelerometer delivered. Every other
 * outcome — no measurement yet, a gyroscope that delivers, a measurement that says nothing —
 * leaves the screen as it is.
 */
fun arStartupMessage(startup: ArStartup, sensors: ARMotionSensorHealth?): ArStartupMessage =
    if (startup.stalled && sensors == ARMotionSensorHealth.SILENT) {
        ArStartupMessage.RESTART_PHONE
    } else {
        ArStartupMessage.NONE
    }

/**
 * The last motion-sensor measurement of this process, for the bug report: the report says how
 * many events each sensor delivered instead of leaving a maintainer to ask for
 * `dumpsys sensorservice`. `null` until an AR session stalled and was checked.
 */
object MotionSensorReport {
    @Volatile
    var last: ARMotionSensorSample? = null

    /** The bug report's key for [line]. */
    const val KEY = "gyroscope"

    /** `0 events in 1 s (accelerometer: 204)`, or `null` when nothing was measured. */
    fun line(sample: ARMotionSensorSample? = last): String? {
        sample ?: return null
        val seconds = sample.windowMillis / 1_000.0
        val window = if (seconds % 1.0 == 0.0) seconds.toLong().toString() else seconds.toString()
        return "${sample.gyroscopeEvents} events in $window s " +
            "(accelerometer: ${sample.accelerometerEvents})"
    }
}

/** The `reason` of the `ar_session_failed` record a silent gyroscope leaves on the AR funnel. */
const val MOTION_SENSOR_SILENT_REASON = "motion_sensor_silent"

/**
 * The `qa_state` id that pins an AR screen on the restart message, so the card can be captured
 * on an emulator: `--ez qa_mode true --es qa_state motion_sensor_silent`. Honoured only under
 * [qaStateOverridesAllowed], like every other pinned state.
 */
const val MOTION_SENSOR_SILENT_QA_STATE = "motion_sensor_silent"

/** Whether QA pinned this screen on the restart message. */
@Composable
fun motionSensorSilentForced(): Boolean =
    DemoSettings.qaDemoState == MOTION_SENSOR_SILENT_QA_STATE && qaStateOverridesAllowed()

/**
 * Watches one AR session's start and returns `true` once it is known that the phone's
 * gyroscope delivers nothing (#4522) — the session then shows a restart message instead of a
 * "Getting ready" card that would never leave.
 *
 * The sensors are listened to once, for one second, and only after the session has delivered
 * frames for [AR_NEVER_TRACKED_MS] in the foreground without tracking and without a reason from
 * ARCore. A session that tracks — the normal case, within a second — never registers a sensor
 * listener. One `ar_session_failed` record with [MOTION_SENSOR_SILENT_REASON] is logged when
 * the verdict falls.
 *
 * @param framesArriving the session delivered a camera frame.
 * @param tracking the camera is tracking now.
 * @param failureReason ARCore's reason for not tracking, if it gave one.
 * @param sampleId the funnel's sample id for this screen.
 */
@Composable
fun rememberMotionSensorSilent(
    framesArriving: Boolean,
    tracking: Boolean,
    failureReason: TrackingFailureReason?,
    sampleId: String,
): Boolean {
    var everTracked by remember { mutableStateOf(false) }
    var waitedMillis by remember { mutableLongStateOf(0L) }
    var probed by remember { mutableStateOf(false) }
    var health by remember { mutableStateOf<ARMotionSensorHealth?>(null) }
    SideEffect { if (tracking) everTracked = true }

    val startup = ArStartup(framesArriving, everTracked || tracking, tracking, failureReason, waitedMillis)
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val waiting = startup.neverStartedUnexplained && !probed
    LaunchedEffect(waiting, lifecycle) {
        if (!waiting) return@LaunchedEffect
        // Foreground time only, and from scratch after a trip to the background: the session
        // itself pauses there, and a paused session is not a stalled one.
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            waitedMillis = 0L
            delay(AR_NEVER_TRACKED_MS)
            waitedMillis = AR_NEVER_TRACKED_MS
            val due = motionSensorProbeDue(startup.copy(waitedMillis = AR_NEVER_TRACKED_MS), probed)
            if (!due) return@repeatOnLifecycle
            val sample = probeARMotionSensors(context)
            MotionSensorReport.last = sample
            health = sample?.health
            if (sample?.health == ARMotionSensorHealth.SILENT) {
                Telemetry.analytics.log(
                    AnalyticsEvent.ArSessionFailed(sampleId, MOTION_SENSOR_SILENT_REASON),
                )
            }
            probed = true
        }
    }
    return motionSensorSilentForced() ||
        arStartupMessage(startup, health) == ArStartupMessage.RESTART_PHONE
}

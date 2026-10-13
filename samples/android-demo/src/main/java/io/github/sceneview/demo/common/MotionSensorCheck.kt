package io.github.sceneview.demo.common

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARMotionSensorHealth
import io.github.sceneview.ar.ARMotionSensorSample
import io.github.sceneview.ar.probeARMotionSensors
import io.github.sceneview.demo.DemoSettings
import io.github.sceneview.demo.telemetry.ArFunnelLog
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

/**
 * How many times one session listens to the motion sensors at most. A first measurement that
 * says nothing — the sensor service refused the listener, or had not started delivering — is
 * taken again [AR_NEVER_TRACKED_MS] later; after this many, the screen stays as it is.
 */
const val MOTION_SENSOR_MAX_PROBES = 3

/**
 * Whether this session still has a measurement to wait for: it never tracked and ARCore gives
 * no reason, and either nothing was measured yet or the last measurement said nothing
 * ([ARMotionSensorHealth.UNKNOWN]) and [MOTION_SENSOR_MAX_PROBES] is not reached. A verdict —
 * silent or delivering — is final, and so is a phone with no gyroscope ([last] `null` after a
 * measurement). Never while tracking works, nor once it has.
 *
 * @param probes how many times the sensors were listened to in this session.
 * @param last the verdict of the latest measurement.
 */
fun motionSensorProbeDue(startup: ArStartup, probes: Int, last: ARMotionSensorHealth?): Boolean =
    startup.neverStartedUnexplained && probes < MOTION_SENSOR_MAX_PROBES &&
        (probes == 0 || last == ARMotionSensorHealth.UNKNOWN)

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
 *
 * The measurement carries its age, and it is dropped as soon as an AR session tracks: a report
 * filed later does not present an old count as the phone's present state.
 */
object MotionSensorReport {
    @Volatile
    private var last: ARMotionSensorSample? = null

    @Volatile
    private var measuredAtMillis: Long = 0L

    /** The bug report's key for [line]. */
    const val KEY = "gyroscope"

    /** Keeps [sample] (or forgets the previous one, on `null`) as measured at [nowMillis]. */
    fun record(sample: ARMotionSensorSample?, nowMillis: Long = SystemClock.elapsedRealtime()) {
        last = sample
        measuredAtMillis = nowMillis
    }

    /** An AR session tracked: whatever was measured before no longer describes this phone. */
    fun clear() {
        last = null
    }

    /**
     * `0 events in 1 s (accelerometer: 204), measured 12 s ago`, or `null` when nothing was
     * measured since an AR session last tracked.
     */
    fun line(nowMillis: Long = SystemClock.elapsedRealtime()): String? {
        val sample = last ?: return null
        val age = ((nowMillis - measuredAtMillis) / 1_000).coerceAtLeast(0)
        return "${line(sample)}, measured $age s ago"
    }

    /** `0 events in 1 s (accelerometer: 204)`; a sensor that was not listened to reads `not listened to`. */
    fun line(sample: ARMotionSensorSample): String {
        val seconds = sample.windowMillis / 1_000.0
        val window = if (seconds % 1.0 == 0.0) seconds.toLong().toString() else seconds.toString()
        return "${count(sample.gyroscopeEvents)} in $window s " +
            "(accelerometer: ${count(sample.accelerometerEvents, unit = "")})"
    }

    private fun count(events: Int, unit: String = " events"): String =
        if (events < 0) "not listened to" else "$events$unit"
}

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
 * "Getting ready" card that would never leave. It returns `false` again, for good, the moment
 * the session tracks.
 *
 * The sensors are listened to for one second, and only after the session has delivered frames
 * for [AR_NEVER_TRACKED_MS] in the foreground without tracking and without a reason from
 * ARCore. A session that tracks — the normal case, within a second — never registers a sensor
 * listener, and tracking that starts during the wait or the measurement cancels it. Time in the
 * background does not count: the wait starts over on return. A measurement that says nothing is
 * taken again after the same wait, [MOTION_SENSOR_MAX_PROBES] times at most.
 *
 * @param framesArriving the session delivered a camera frame.
 * @param tracking the camera is tracking now.
 * @param failureReason ARCore's reason for not tracking, if it gave one.
 * @param onSilent called when a measurement finds the gyroscope silent — the caller's funnel
 *   records it ([ArFunnelLog.motionSensorSilent], once per session).
 * @param probe the measurement itself; the tests replace it.
 */
@Composable
fun rememberMotionSensorSilent(
    framesArriving: Boolean,
    tracking: Boolean,
    failureReason: TrackingFailureReason?,
    onSilent: () -> Unit,
    probe: suspend (Context) -> ARMotionSensorSample? = { probeARMotionSensors(it) },
): Boolean {
    var everTracked by remember { mutableStateOf(false) }
    var waitedMillis by remember { mutableLongStateOf(0L) }
    var probes by remember { mutableIntStateOf(0) }
    var health by remember { mutableStateOf<ARMotionSensorHealth?>(null) }
    SideEffect {
        if (tracking && !everTracked) {
            everTracked = true
            MotionSensorReport.clear()
        }
    }

    val startup = ArStartup(framesArriving, everTracked || tracking, tracking, failureReason, waitedMillis)
    val context = LocalContext.current.applicationContext
    val currentProbe by rememberUpdatedState(probe)
    val currentOnSilent by rememberUpdatedState(onSilent)
    // Foreground time only, and from scratch after a trip to the background: the session itself
    // pauses there, and a paused session is not a stalled one.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val watching = motionSensorProbeDue(startup, probes, health) &&
        lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(watching, probes) {
        if (!watching) return@LaunchedEffect
        delay(AR_NEVER_TRACKED_MS)
        val sample = currentProbe(context)
        MotionSensorReport.record(sample)
        if (sample?.health == ARMotionSensorHealth.SILENT) currentOnSilent()
        // Written last: a measurement that says nothing restarts this effect for the next one.
        waitedMillis = AR_NEVER_TRACKED_MS
        health = sample?.health
        probes++
    }
    return motionSensorSilentForced() ||
        arStartupMessage(startup, health) == ArStartupMessage.RESTART_PHONE
}

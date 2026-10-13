package io.github.sceneview.demo.telemetry

import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARSessionFailure

/** `sample_id` of the AR session the AR View tab hosts outside any sample. */
const val AR_VIEW_SAMPLE_ID = "ar_view"

/**
 * The five AR funnel events of `docs/docs/recipes/measure-ar-funnel.md`, fed from ARSceneView's
 * public callbacks. One instance per AR session.
 *
 * - `ar_tracking_ready` logs once, on the first frame whose camera is TRACKING ([frameTracked],
 *   from `onSessionUpdated`). Not from `onTrackingFailureChanged(null)`: that callback only
 *   fires on a *change* of failure reason, so a session that tracks at once never calls it.
 * - `ar_first_placement` logs once.
 * - `ar_tracking_lost` logs every time ARCore reports a new failure reason (ARSceneView
 *   already de-duplicates).
 * - `ar_session_failed` logs once per failure reason. ARCore retries a session it could not
 *   create on every resume (`create()` at ON_CREATE, then `resume()` finds no session and
 *   creates again), so one visit to an AR screen on a device that cannot run AR reports the
 *   same failure twice, then once more per return from the background.
 *
 * Two things the funnel cannot say are filed with the crash reports instead, as problems the app
 * survived (#4510) — no new analytics event, the taxonomy stays the one shared with iOS:
 * - a session failure with no code of its own (`unknown`, `fatal`), with the exception that
 *   caused it, once;
 * - a session that delivers camera frames for [TRACKING_STALL_MS] and never tracks — the screen
 *   that stays on "Getting ready" over a live camera image — with ARCore's own reason, once.
 *   It carries the sample id, the reason and a duration: no image, no pose, nothing about the
 *   place or the person.
 *
 * @param monotonicMillis the clock the stall is measured on; injected by the tests.
 */
class ArFunnelLog(
    private val analytics: () -> DemoAnalytics = { Telemetry.analytics },
    private val monotonicMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    var sampleId: String = AR_VIEW_SAMPLE_ID

    private var tracked = false
    private var placedOnce = false
    private val failuresLogged = mutableSetOf<String>()

    private var lastUntrackedFrameAt = NO_FRAME
    private var untrackedForMillis = 0L
    private var stallReported = false
    private var lastFailureReason: TrackingFailureReason? = null

    fun sessionCreated() = analytics().log(AnalyticsEvent.ArSessionCreated(sampleId))

    /** Called on every frame (60 Hz): a single boolean check once tracking was reached. */
    fun frameTracked(isTracking: Boolean) {
        if (tracked) return
        if (!isTracking) {
            if (!stallReported) countUntrackedFrame()
            return
        }
        tracked = true
        analytics().log(AnalyticsEvent.ArTrackingReady(sampleId))
    }

    /**
     * Adds this frame to the time spent receiving frames without ever tracking. Only the time
     * between two frames close together counts, so a session left in the background for a
     * minute is not a stalled one.
     */
    private fun countUntrackedFrame() {
        val now = monotonicMillis()
        val sinceLast = now - lastUntrackedFrameAt
        if (lastUntrackedFrameAt != NO_FRAME && sinceLast in 0..MAX_FRAME_GAP_MS) {
            untrackedForMillis += sinceLast
        }
        lastUntrackedFrameAt = now
        if (untrackedForMillis < TRACKING_STALL_MS) return
        stallReported = true
        analytics().recordNonFatal(
            ArTrackingStalled(
                sampleId = sampleId,
                reason = lastFailureReason?.name?.lowercase() ?: "none",
                seconds = untrackedForMillis / 1_000,
            ),
        )
    }

    fun trackingFailureChanged(reason: TrackingFailureReason?) {
        lastFailureReason = reason
        reason ?: return
        analytics().log(AnalyticsEvent.ArTrackingLost(sampleId, reason.name.lowercase()))
    }

    fun placed() {
        if (placedOnce) return
        placedOnce = true
        analytics().log(AnalyticsEvent.ArFirstPlacement(sampleId))
    }

    fun sessionFailed(failure: ARSessionFailure) {
        val reason = failureReason(failure)
        if (!failuresLogged.add(reason)) return
        analytics().log(AnalyticsEvent.ArSessionFailed(sampleId, reason))
        // `unknown` and `fatal` name no cause: the exception itself is the only lead.
        if (failure is ARSessionFailure.Other || failure is ARSessionFailure.Fatal) {
            analytics().recordNonFatal(failure.cause)
        }
    }

    /**
     * An AR session that received camera frames for [seconds] and never reached tracking.
     * [reason] is ARCore's `TrackingFailureReason` in lower case, or `none` when ARCore gave none.
     */
    class ArTrackingStalled(val sampleId: String, val reason: String, val seconds: Long) :
        RuntimeException("AR never tracked in $sampleId after $seconds s of frames, reason=$reason")

    companion object {
        /** Frames without tracking for this long, and the session is reported as stalled. */
        const val TRACKING_STALL_MS = 10_000L

        /** Two frames further apart than this are not the same stretch of frames. */
        const val MAX_FRAME_GAP_MS = 1_000L

        private const val NO_FRAME = -1L

        /**
         * A stable snake_case code per failure type, the same vocabulary as iOS. Spelled out
         * rather than derived from the class name, which R8 renames in a release build; the
         * `when` is exhaustive, so a failure type added to ARSceneView fails this build until
         * it gets a code.
         */
        @Suppress("CyclomaticComplexMethod")
        fun failureReason(failure: ARSessionFailure): String = when (failure) {
            is ARSessionFailure.ArCoreNotInstalled -> "arcore_not_installed"
            is ARSessionFailure.UserDeclinedInstall -> "user_declined_install"
            is ARSessionFailure.ApkTooOld -> "apk_too_old"
            is ARSessionFailure.SdkTooOld -> "sdk_too_old"
            is ARSessionFailure.DeviceNotCompatible -> "device_not_compatible"
            is ARSessionFailure.FineLocationMissing -> "fine_location_missing"
            is ARSessionFailure.GooglePlayServicesLocationLibraryNotLinked -> "location_library_not_linked"
            is ARSessionFailure.CameraNotAvailable -> "camera_not_available"
            is ARSessionFailure.TextureNotSet -> "texture_not_set"
            is ARSessionFailure.MissingGlContext -> "missing_gl_context"
            is ARSessionFailure.ResourceExhausted -> "resource_exhausted"
            is ARSessionFailure.DeadlineExceeded -> "deadline_exceeded"
            is ARSessionFailure.Fatal -> "fatal"
            is ARSessionFailure.CloudAnchorsNotConfigured -> "cloud_anchors_not_configured"
            is ARSessionFailure.AnchorNotSupportedForHosting -> "anchor_not_supported_for_hosting"
            is ARSessionFailure.ImageInsufficientQuality -> "image_insufficient_quality"
            is ARSessionFailure.RecordingFailed -> "recording_failed"
            is ARSessionFailure.PlaybackFailed -> "playback_failed"
            is ARSessionFailure.DataInvalidFormat -> "data_invalid_format"
            is ARSessionFailure.DataUnsupportedVersion -> "data_unsupported_version"
            is ARSessionFailure.MetadataNotFound -> "metadata_not_found"
            is ARSessionFailure.SessionUnsupported -> "session_unsupported"
            is ARSessionFailure.SessionPaused -> "session_paused"
            is ARSessionFailure.SessionNotPaused -> "session_not_paused"
            is ARSessionFailure.NotTracking -> "not_tracking"
            is ARSessionFailure.NotYetAvailable -> "not_yet_available"
            is ARSessionFailure.UnsupportedConfiguration -> "unsupported_configuration"
            is ARSessionFailure.Other -> "unknown"
        }
    }
}

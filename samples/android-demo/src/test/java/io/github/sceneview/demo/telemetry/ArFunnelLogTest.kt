package io.github.sceneview.demo.telemetry

import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import io.github.sceneview.ar.ARSessionFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The AR funnel events fed from ARSceneView's callbacks: what repeats and what logs once. */
class ArFunnelLogTest {

    private val events = mutableListOf<AnalyticsEvent>()
    private val nonFatals = mutableListOf<Throwable>()
    private var nowMillis = 0L
    private val sink = object : DemoAnalytics {
        override fun log(event: AnalyticsEvent) {
            events += event
        }
        override fun recordNonFatal(throwable: Throwable) {
            nonFatals += throwable
        }
        override fun setUserProperty(property: UserProperty, value: String) = Unit
        override fun setCollectionEnabled(enabled: Boolean) = Unit
        override fun resetData() = Unit
    }
    private val funnel = ArFunnelLog({ sink }, { nowMillis }).apply { sampleId = "ar-placement" }

    /** Delivers one frame every 16 ms for [millis], none of them tracking. */
    private fun untrackedFramesFor(millis: Long) {
        val until = nowMillis + millis
        while (nowMillis <= until) {
            funnel.frameTracked(false)
            nowMillis += 16
        }
    }

    @Test
    fun `ready and first placement log once, tracking losses every time`() {
        funnel.sessionCreated()
        funnel.frameTracked(false)
        funnel.trackingFailureChanged(TrackingFailureReason.INSUFFICIENT_LIGHT)
        funnel.frameTracked(true)
        funnel.trackingFailureChanged(null)
        funnel.placed()
        funnel.trackingFailureChanged(TrackingFailureReason.EXCESSIVE_MOTION)
        funnel.frameTracked(false)
        funnel.frameTracked(true)
        funnel.placed()
        assertEquals(
            listOf(
                AnalyticsEvent.ArSessionCreated("ar-placement"),
                AnalyticsEvent.ArTrackingLost("ar-placement", "insufficient_light"),
                AnalyticsEvent.ArTrackingReady("ar-placement"),
                AnalyticsEvent.ArFirstPlacement("ar-placement"),
                AnalyticsEvent.ArTrackingLost("ar-placement", "excessive_motion"),
            ),
            events,
        )
    }

    @Test
    fun `a session that tracks at once still logs ready - no failure callback needed`() {
        // onTrackingFailureChanged only fires on a change of reason: a session that is
        // TRACKING from its first frame never calls it, and ready must not depend on it.
        funnel.sessionCreated()
        repeat(120) { funnel.frameTracked(true) }
        assertEquals(
            listOf(AnalyticsEvent.ArSessionCreated("ar-placement"), AnalyticsEvent.ArTrackingReady("ar-placement")),
            events,
        )
    }

    @Test
    fun `session failures are reported as snake_case codes`() {
        val cases = mapOf(
            ARSessionFailure.DeviceNotCompatible(UnavailableDeviceNotCompatibleException()) to "device_not_compatible",
            ARSessionFailure.ArCoreNotInstalled(UnavailableArcoreNotInstalledException()) to "arcore_not_installed",
            ARSessionFailure.CameraNotAvailable(CameraNotAvailableException()) to "camera_not_available",
            ARSessionFailure.Other(IllegalStateException("boom")) to "unknown",
        )
        cases.forEach { (failure, code) -> assertEquals(code, ArFunnelLog.failureReason(failure)) }
        funnel.sessionFailed(cases.keys.first())
        assertEquals(AnalyticsEvent.ArSessionFailed("ar-placement", "device_not_compatible"), events.single())
    }

    @Test
    fun `a failure ARCore retries on resume logs once per reason`() {
        // create() fails at ON_CREATE, then resume() finds no session and fails again: one
        // attempt, one event. A different reason on a later retry is new information.
        repeat(3) { funnel.sessionFailed(ARSessionFailure.from(IllegalStateException("fatal"))) }
        funnel.sessionFailed(ARSessionFailure.from(CameraNotAvailableException()))
        funnel.sessionFailed(ARSessionFailure.from(CameraNotAvailableException()))
        assertEquals(
            listOf(
                AnalyticsEvent.ArSessionFailed("ar-placement", "unknown"),
                AnalyticsEvent.ArSessionFailed("ar-placement", "camera_not_available"),
            ),
            events,
        )
    }

    @Test
    fun `every code fits GA4 - snake_case and at most 100 characters`() {
        val codes = listOf(
            ARSessionFailure.from(UnavailableDeviceNotCompatibleException()),
            ARSessionFailure.from(CameraNotAvailableException()),
            ARSessionFailure.from(IllegalArgumentException()),
        ).map(ArFunnelLog::failureReason)
        codes.forEach { assertTrue(it, it.matches(Regex("[a-z][a-z0-9_]*")) && it.length <= AnalyticsEvent.MAX_VALUE) }
    }

    @Test
    fun `a failure with no code of its own also files its exception, once`() {
        // #4510: `unknown` says an AR session failed and nothing about why.
        val cause = IllegalStateException("light estimation blew up")
        repeat(3) { funnel.sessionFailed(ARSessionFailure.from(cause)) }
        assertEquals(listOf<Throwable>(cause), nonFatals)
    }

    @Test
    fun `a failure the funnel already names files no exception`() {
        funnel.sessionFailed(ARSessionFailure.from(CameraNotAvailableException()))
        funnel.sessionFailed(ARSessionFailure.from(UnavailableDeviceNotCompatibleException()))
        assertEquals(emptyList<Throwable>(), nonFatals)
    }

    @Test
    fun `frames that never track are reported once, with ARCore's reason`() {
        // #4510: the camera image is live, the screen stays on "Getting ready".
        funnel.sessionCreated()
        funnel.trackingFailureChanged(TrackingFailureReason.INSUFFICIENT_FEATURES)
        untrackedFramesFor(ArFunnelLog.TRACKING_STALL_MS - 500)
        assertEquals(emptyList<Throwable>(), nonFatals)

        untrackedFramesFor(60_000)

        val stall = nonFatals.single() as ArFunnelLog.ArTrackingStalled
        assertEquals("ar-placement", stall.sampleId)
        assertEquals("insufficient_features", stall.reason)
        assertEquals(ArFunnelLog.TRACKING_STALL_MS / 1_000, stall.seconds)
        assertEquals(
            "AR never tracked in ar-placement after 10 s of frames, reason=insufficient_features",
            stall.message,
        )
    }

    @Test
    fun `a stall ARCore gives no reason for says none`() {
        untrackedFramesFor(ArFunnelLog.TRACKING_STALL_MS + 100)
        assertEquals("none", (nonFatals.single() as ArFunnelLog.ArTrackingStalled).reason)
    }

    @Test
    fun `a session that tracks in time is not a stall, and never becomes one`() {
        untrackedFramesFor(4_000)
        funnel.frameTracked(true)
        // Tracking lost later is `ar_tracking_lost`, not "never tracked".
        untrackedFramesFor(60_000)
        assertEquals(emptyList<Throwable>(), nonFatals)
    }

    @Test
    fun `time spent in the background is not time without tracking`() {
        untrackedFramesFor(4_000)
        nowMillis += 120_000 // the app is left; no frame is delivered
        untrackedFramesFor(4_000)
        assertEquals(emptyList<Throwable>(), nonFatals)

        untrackedFramesFor(3_000)
        assertEquals(1, nonFatals.size)
    }
}

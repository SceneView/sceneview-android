package io.github.sceneview.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the two rules `ARSceneView`'s frame loop gained in #4510.
 *
 * Before them the loop caught everything a frame threw and only wrote "ARCore session update
 * failed" to logcat: a loop that threw on every frame left the camera image live and the host on
 * its "starting" state for ever, and a host callback that threw was blamed on ARCore.
 *
 * [ARFrameFailureStreak] decides when a failing loop reaches `onSessionFailure`;
 * [guardHostCallback] keeps one callback's throw from skipping the next one.
 */
class ARFrameFailureStreakTest {

    private val frame = 16_666_667L // one 60 Hz frame, in nanoseconds
    private val second = 1_000_000_000L

    /** Fails every frame from [fromNanos] for [durationNanos]; returns the frames that reported. */
    private fun ARFrameFailureStreak.failEveryFrame(
        fromNanos: Long,
        durationNanos: Long,
        exception: () -> Exception = { IllegalStateException("boom") },
    ): List<Long> {
        val reportedAt = ArrayList<Long>()
        var now = fromNanos
        while (now <= fromNanos + durationNanos) {
            if (frameFailed(exception(), now)) reportedAt += now
            now += frame
        }
        return reportedAt
    }

    @Test
    fun `one failed frame is not reported`() {
        val streak = ARFrameFailureStreak()

        assertFalse(streak.frameFailed(IllegalStateException("boom"), 0L))
    }

    @Test
    fun `a loop failing for less than the threshold is not reported`() {
        val streak = ARFrameFailureStreak()

        assertEquals(emptyList<Long>(), streak.failEveryFrame(0L, second))
    }

    @Test
    fun `a loop failing on every frame is reported once, when the threshold is reached`() {
        val streak = ARFrameFailureStreak()

        val reportedAt = streak.failEveryFrame(0L, 5 * second)

        assertEquals(1, reportedAt.size)
        assertTrue(
            "reported at ${reportedAt.single()} ns",
            reportedAt.single() in ARFrameFailureStreak.PERSISTENT_AFTER_NANOS..
                    ARFrameFailureStreak.PERSISTENT_AFTER_NANOS + frame
        )
    }

    @Test
    fun `a good frame in between starts the count again`() {
        val streak = ARFrameFailureStreak()

        // One second of failures, one good frame, one more second of failures: 2 s of trouble,
        // but never 1.5 s without a frame going through.
        assertEquals(emptyList<Long>(), streak.failEveryFrame(0L, second))
        streak.frameSucceeded()
        assertEquals(emptyList<Long>(), streak.failEveryFrame(second + 2 * frame, second))
    }

    @Test
    fun `failures separated by a long silence are separate incidents`() {
        val streak = ARFrameFailureStreak()

        // The session pauses (no frame at all, good or bad) between two bursts.
        assertEquals(emptyList<Long>(), streak.failEveryFrame(0L, second))
        assertEquals(emptyList<Long>(), streak.failEveryFrame(10 * second, second))
    }

    @Test
    fun `a slow frame rate still counts as one streak`() {
        val streak = ARFrameFailureStreak()

        // 10 fps: 100 ms between failures, well under the 500 ms gap.
        var reports = 0
        var now = 0L
        while (now <= 3 * second) {
            if (streak.frameFailed(IllegalStateException("boom"), now)) reports++
            now += 100_000_000L
        }

        assertEquals(1, reports)
    }

    @Test
    fun `each exception type is reported once`() {
        val streak = ARFrameFailureStreak()

        assertEquals(1, streak.failEveryFrame(0L, 3 * second).size)
        // Same streak, another type takes over: reported at once, the streak is already old.
        assertTrue(streak.frameFailed(UnsupportedOperationException("other"), 3 * second + frame))
        assertFalse(streak.frameFailed(UnsupportedOperationException("other"), 3 * second + 2 * frame))
        assertFalse(streak.frameFailed(IllegalStateException("boom"), 3 * second + 3 * frame))
    }

    @Test
    fun `a type already reported stays quiet in a later streak`() {
        val streak = ARFrameFailureStreak()

        assertEquals(1, streak.failEveryFrame(0L, 3 * second).size)
        streak.frameSucceeded()

        assertEquals(emptyList<Long>(), streak.failEveryFrame(20 * second, 3 * second))
    }

    @Test
    fun `a host callback that returns reports nothing`() {
        val thrown = ArrayList<String>()
        var ran = false

        val failure = guardHostCallback("onSessionUpdated", { name, _ -> thrown += name }) {
            ran = true
        }

        assertTrue(ran)
        assertNull(failure)
        assertEquals(emptyList<String>(), thrown)
    }

    @Test
    fun `a host callback that throws is named, and the next callback still runs`() {
        val thrown = ArrayList<Pair<String, Exception>>()
        val boom = IllegalStateException("host bug")
        var sessionUpdatedRan = false

        // The order `onARFrame` uses: tracking-failure change first, then the per-frame callback.
        val first = guardHostCallback(
            "onTrackingFailureChanged", { name, e -> thrown += name to e }
        ) { throw boom }
        val second = guardHostCallback("onSessionUpdated", { name, e -> thrown += name to e }) {
            sessionUpdatedRan = true
        }

        assertSame(boom, first)
        assertNull(second)
        assertTrue(sessionUpdatedRan)
        assertEquals(listOf("onTrackingFailureChanged" to boom), thrown)
    }
}

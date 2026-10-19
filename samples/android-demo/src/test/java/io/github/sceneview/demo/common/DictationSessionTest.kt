package io.github.sceneview.demo.common

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #4618: dictation keeps listening through pauses and never loses what it heard. */
class DictationSessionTest {

    private class FakeEngine : SpeechEngine {
        var starts = 0
        var stops = 0
        var cancels = 0
        override fun startListening() { starts++ }
        override fun stopListening() { stops++ }
        override fun cancel() { cancels++ }
    }

    private val engine = FakeEngine()
    private var now = 0L
    private val updates = mutableListOf<DictationState>()
    private val session = DictationSession(
        engine = engine,
        clock = { now },
        onUpdate = { updates += it },
        maxSilenceMillis = 8_000L,
    )

    @Test
    fun startListensWithAnEmptyTranscript() {
        session.start()
        assertTrue(session.listening)
        assertEquals(1, engine.starts)
        assertEquals(DictationState(listening = true, transcript = ""), updates.last())
    }

    @Test
    fun partialResultsShowLive() {
        session.start()
        session.onPartialResult("the model")
        assertEquals("the model", updates.last().transcript)
        assertTrue(updates.last().listening)
    }

    @Test
    fun aFinishedSegmentIsKeptAndTheNextOneAppendsToIt() {
        session.start()
        session.onPartialResult("the model")
        now += 1_000
        session.onSegmentResult("the model is too dark")
        assertEquals(2, engine.starts)
        assertTrue(session.listening)
        session.onPartialResult("on the floor")
        assertEquals("the model is too dark on the floor", updates.last().transcript)
        session.onSegmentResult("on the floor")
        assertEquals("the model is too dark on the floor", session.transcript)
    }

    @Test
    fun aSilenceErrorWhileListeningRestartsAndKeepsThePartial() {
        session.start()
        session.onPartialResult("hello")
        now += 2_000
        session.onError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(2, engine.starts)
        now += 3_000
        session.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        assertEquals(3, engine.starts)
        assertTrue(session.listening)
        assertEquals("hello", updates.last().transcript)
    }

    @Test
    fun anEmptyFinalResultDoesNotDropThePartial() {
        session.start()
        session.onPartialResult("keep me")
        session.onSegmentResult(null)
        assertEquals("keep me", session.transcript)
        assertTrue(session.listening)
    }

    @Test
    fun aLongSilenceEndsTheSession() {
        session.start()
        session.onPartialResult("done")
        now += 8_000
        session.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        assertFalse(session.listening)
        assertEquals(1, engine.starts)
        assertEquals(DictationState(listening = false, transcript = "done"), updates.last())
    }

    @Test
    fun theWatchdogEndsASegmentHeldOpenBySilence() {
        session.start()
        session.onPartialResult("still here")
        now += 7_000
        session.onTick()
        assertTrue(session.listening)
        now += 1_000
        session.onTick()
        assertFalse(session.listening)
        assertEquals(1, engine.cancels)
        assertEquals("still here", updates.last().transcript)
        session.onError(SpeechRecognizer.ERROR_CLIENT)
        assertEquals(1, engine.starts)
    }

    @Test
    fun userStopEndsListeningAndTakesTheFinalResult() {
        session.start()
        session.onPartialResult("tap stop")
        session.stop()
        assertFalse(session.listening)
        assertEquals(1, engine.stops)
        assertFalse(updates.last().listening)
        assertEquals("tap stop", updates.last().transcript)
        session.onSegmentResult("tap stop now")
        assertEquals("tap stop now", session.transcript)
        assertEquals(1, engine.starts)
    }

    @Test
    fun nothingRestartsAfterStop() {
        session.start()
        session.stop()
        session.onError(SpeechRecognizer.ERROR_NO_MATCH)
        session.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        session.onSegmentResult("late")
        session.onPartialResult("later")
        assertEquals(1, engine.starts)
        assertFalse(session.listening)
        assertEquals("", session.transcript)
    }

    @Test
    fun aHardErrorEndsTheSessionAndReportsIt() {
        session.start()
        session.onPartialResult("network")
        session.onError(SpeechRecognizer.ERROR_NETWORK)
        assertFalse(session.listening)
        assertEquals(1, engine.starts)
        assertEquals(SpeechRecognizer.ERROR_NETWORK, updates.last().error)
        assertEquals("network", updates.last().transcript)
    }

    @Test
    fun busyIsRetriedOnlyAFewTimesInARow() {
        session.start()
        repeat(3) { session.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY) }
        assertTrue(session.listening)
        assertEquals(4, engine.starts)
        session.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        assertFalse(session.listening)
    }

    @Test
    fun segmentedSessionEndRestartsWhileListening() {
        session.start()
        session.onSegmentResult("first")
        session.onEndOfSegmentedSession()
        assertEquals(3, engine.starts)
        assertEquals("first", session.transcript)
    }

    @Test
    fun startingAgainBeginsAFreshTranscript() {
        session.start()
        session.onSegmentResult("old")
        session.stop()
        session.start()
        assertEquals("", session.transcript)
        assertNull(updates.last().error)
    }

    @Test
    fun disposeCancelsTheRecognizer() {
        session.start()
        session.dispose()
        session.onError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(1, engine.cancels)
        assertEquals(1, engine.starts)
        assertFalse(session.listening)
    }

    @Test
    fun appendDictationJoinsAfterTheExistingText() {
        assertEquals("heard", appendDictation("", "heard"))
        assertEquals("typed heard", appendDictation("typed  ", "heard"))
    }
}

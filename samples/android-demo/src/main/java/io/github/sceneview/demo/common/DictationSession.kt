package io.github.sceneview.demo.common

import android.speech.SpeechRecognizer

/**
 * The recognizer a [DictationSession] drives. [AndroidSpeechEngine] wraps the platform
 * `SpeechRecognizer`; tests pass a fake.
 */
interface SpeechEngine {
    /** Starts one recognition segment. */
    fun startListening()

    /** Asks the current segment to finish and deliver its final result. */
    fun stopListening()

    /** Drops the current segment without a result. */
    fun cancel()
}

/** What a dictation shows: whether it is still listening, and everything heard so far. */
data class DictationState(
    val listening: Boolean = false,
    val transcript: String = "",
    /** The `SpeechRecognizer.ERROR_*` code that ended the session, or null. */
    val error: Int? = null,
)

/**
 * A dictation that keeps listening through natural pauses (#4618).
 *
 * Google's recognizer ends a segment after well under a second of silence and ignores the
 * `EXTRA_SPEECH_INPUT_*_SILENCE_LENGTH_MILLIS` extras, so one `startListening()` call can never
 * hold a spoken paragraph. This session chains segments instead: every time the recognizer
 * ends one (a final result, "no match", "speech timeout"), it keeps what was heard and starts
 * the next one, until the user taps stop or nothing has been said for [maxSilenceMillis].
 *
 * The transcript is every finished segment plus the live partial of the current one, so
 * the words on screen never disappear when a segment ends. All calls must come from one
 * thread (the main thread: `SpeechRecognizer` requires it).
 */
class DictationSession(
    private val engine: SpeechEngine,
    private val clock: () -> Long,
    private val onUpdate: (DictationState) -> Unit,
    private val maxSilenceMillis: Long = DEFAULT_MAX_SILENCE_MILLIS,
) {
    private val segments = mutableListOf<String>()
    private var partial = ""
    private var lastSpeechAt = 0L
    private var engineErrorsInARow = 0

    /** True between [start] and the end of the session (user stop, long silence, error). */
    var listening = false
        private set

    /** True after the user stopped, until the last segment's final result arrives. */
    private var finishing = false

    val transcript: String
        get() = (segments + partial).filter { it.isNotBlank() }.joinToString(" ")

    /** Starts a new dictation; the transcript starts empty. No-op while already listening. */
    fun start() {
        if (listening) return
        segments.clear()
        partial = ""
        finishing = false
        engineErrorsInARow = 0
        lastSpeechAt = clock()
        listening = true
        engine.startListening()
        emit()
    }

    /**
     * The user tapped stop. The current partial stays in the transcript; the recognizer is
     * asked for its final result, which replaces it if it arrives. Nothing restarts after this.
     */
    fun stop() {
        if (!listening) return
        listening = false
        finishing = true
        engine.stopListening()
        emit()
    }

    /** The screen is going away: end without waiting for anything. */
    fun dispose() {
        listening = false
        finishing = false
        engine.cancel()
    }

    /**
     * Periodic silence check. The recognizer can hold one segment open for a long time on
     * background noise without producing words, so the session cannot rely on segment ends
     * alone to notice a long silence.
     */
    fun onTick() {
        if (!listening) return
        if (clock() - lastSpeechAt < maxSilenceMillis) return
        commitPartial()
        listening = false
        engine.cancel()
        emit()
    }

    fun onPartialResult(text: String?) {
        if (!listening && !finishing) return
        if (text.isNullOrBlank()) return
        partial = text
        lastSpeechAt = clock()
        engineErrorsInARow = 0
        emit()
    }

    /** A segment ended with its final text (`onResults`, `onSegmentResults`). */
    fun onSegmentResult(text: String?) {
        if (!listening && !finishing) return
        if (!text.isNullOrBlank()) {
            partial = ""
            segments += text
            lastSpeechAt = clock()
            engineErrorsInARow = 0
        } else {
            commitPartial()
        }
        continueOrFinish()
    }

    /** A segmented session ended (API 33+ `onEndOfSegmentedSession`). */
    fun onEndOfSegmentedSession() = onSegmentResult(null)

    fun onError(code: Int) {
        if (!listening && !finishing) return
        commitPartial()
        when {
            finishing -> continueOrFinish()
            code in SILENCE_ERRORS -> continueOrFinish()
            code in TRANSIENT_ERRORS && ++engineErrorsInARow <= MAX_TRANSIENT_RETRIES ->
                continueOrFinish()
            else -> {
                listening = false
                emit(error = code)
            }
        }
    }

    private fun commitPartial() {
        if (partial.isNotBlank()) segments += partial
        partial = ""
    }

    private fun continueOrFinish() {
        if (finishing) {
            finishing = false
            emit()
            return
        }
        if (!listening) return
        if (clock() - lastSpeechAt >= maxSilenceMillis) {
            listening = false
            emit()
            return
        }
        engine.startListening()
        emit()
    }

    private fun emit(error: Int? = null) {
        onUpdate(DictationState(listening = listening, transcript = transcript, error = error))
    }

    companion object {
        /**
         * Silence after which the session ends on its own — clearly longer than a pause for
         * breath or thought. Checked when the recognizer ends a segment, which it does every
         * few seconds without speech, so the real cut-off lands between this and ~5 s more.
         */
        const val DEFAULT_MAX_SILENCE_MILLIS = 8_000L

        /** Segment ends that only mean "nobody spoke": keep listening. */
        private val SILENCE_ERRORS = setOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        )

        /** Restart races the recognizer sometimes reports; retried a few times in a row. */
        private val TRANSIENT_ERRORS = setOf(
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_CLIENT,
        )
        private const val MAX_TRANSIENT_RETRIES = 3
    }
}

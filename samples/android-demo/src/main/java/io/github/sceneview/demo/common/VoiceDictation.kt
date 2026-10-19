package io.github.sceneview.demo.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.sceneview.demo.R

/**
 * A text field's voice dictation (#4618), shared by the bug-report note and Point & Ask's
 * question.
 *
 * Tapping the mic listens in-app with a [DictationSession], which keeps going through pauses
 * and streams what it hears into the field until the user taps stop (or stays silent for
 * several seconds). It needs `RECORD_AUDIO`, asked on the first tap. Without it — refused, or
 * no in-app recognizer on the device — the mic falls back to the system "Speak now" dialog,
 * which still works but ends at the first pause.
 */
@Stable
class VoiceDictation internal constructor(
    /** False when the device has no recognizer at all: hide the mic. */
    val available: Boolean,
) {
    /** True while the in-app recognizer is listening: show the stop button. */
    var listening by mutableStateOf(false)
        internal set

    internal var onToggle: () -> Unit = {}

    /** Starts listening, or stops if already listening. */
    fun toggle() = onToggle()
}

/**
 * @param prompt shown by the system dialog fallback.
 * @param text the field's current value.
 * @param onTextChange receives the field's new value every time the transcript grows.
 * @param append true to add what is heard after [text] as it was when the mic was tapped,
 * false to replace it.
 */
@Composable
fun rememberVoiceDictation(
    prompt: String,
    text: String,
    onTextChange: (String) -> Unit,
    append: Boolean = true,
): VoiceDictation {
    val context = LocalContext.current
    val latestText by rememberUpdatedState(text)
    val latestOnTextChange by rememberUpdatedState(onTextChange)
    // What the field held when the mic was tapped; the transcript is joined after it.
    val base = remember { arrayOf("") }
    val latestOnTranscript: (String) -> Unit = { heard ->
        latestOnTextChange(if (append) appendDictation(base[0], heard) else heard)
    }
    val inAppAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }
    val dialogAvailable = remember(context) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(context.packageManager) != null
    }

    val dialogLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { latestOnTranscript(it) }
        }
    }
    val launchDialog = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putVoiceSilenceExtras()
        }
        runCatching { dialogLauncher.launch(intent) }
        Unit
    }

    // Holder filled once the session exists; the dictation object itself is stable.
    val holder = remember { arrayOfNulls<DictationSession>(1) }
    val dictation = remember { VoiceDictation(available = inAppAvailable || dialogAvailable) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val session = holder[0]
        if (granted && session != null) session.start() else if (dialogAvailable) launchDialog()
    }

    DisposableEffect(context, inAppAvailable) {
        if (!inAppAvailable) return@DisposableEffect onDispose { }
        val engine = AndroidSpeechEngine(context)
        val session = DictationSession(
            engine = engine,
            clock = SystemClock::elapsedRealtime,
            onUpdate = { state ->
                dictation.listening = state.listening
                if (state.transcript.isNotBlank()) latestOnTranscript(state.transcript)
            },
        )
        engine.bind(session)
        holder[0] = session
        onDispose {
            holder[0] = null
            session.dispose()
            engine.destroy()
            dictation.listening = false
        }
    }

    // Never keep the mic open behind another app or a locked screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) holder[0]?.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    dictation.onToggle = {
        val session = holder[0]
        if (session?.listening != true) base[0] = latestText
        when {
            session == null -> if (dialogAvailable) launchDialog()
            session.listening -> session.stop()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> session.start()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    return dictation
}

/** A text field's trailing mic: a mic while idle, a stop button while [VoiceDictation.listening]. */
@Composable
fun VoiceDictationButton(dictation: VoiceDictation, idleContentDescription: String) {
    IconButton(onClick = dictation::toggle) {
        if (dictation.listening) {
            Icon(
                Icons.Filled.Stop,
                contentDescription = stringResource(R.string.voice_dictation_stop_cd),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(Icons.Outlined.Mic, contentDescription = idleContentDescription)
        }
    }
}

/** A text field's supporting line while dictation listens — tells the user pauses are fine. */
@Composable
fun VoiceDictationListeningHint() {
    Text(
        text = stringResource(R.string.voice_dictation_listening),
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Joins dictated text after what the field held when the mic was tapped. */
fun appendDictation(base: String, transcript: String): String =
    if (base.isBlank()) transcript else "${base.trimEnd()} $transcript"

/** The platform [SpeechRecognizer] behind a [DictationSession]. Main thread only. */
private class AndroidSpeechEngine(context: Context) : SpeechEngine {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val handler = Handler(Looper.getMainLooper())
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putVoiceSilenceExtras()
    }

    // Restarting from inside a recognizer callback races the service ("busy"); post it.
    private val startNow = Runnable { runCatching { recognizer.startListening(intent) } }

    fun bind(session: DictationSession) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onError(error: Int) = session.onError(error)
            override fun onPartialResults(partialResults: Bundle?) =
                session.onPartialResult(partialResults.firstResult())
            override fun onResults(results: Bundle?) = session.onSegmentResult(results.firstResult())
            override fun onSegmentResults(segmentResults: Bundle) =
                session.onSegmentResult(segmentResults.firstResult())
            override fun onEndOfSegmentedSession() = session.onEndOfSegmentedSession()
        })
    }

    override fun startListening() {
        handler.removeCallbacks(startNow)
        handler.post(startNow)
    }

    override fun stopListening() {
        handler.removeCallbacks(startNow)
        runCatching { recognizer.stopListening() }
    }

    override fun cancel() {
        handler.removeCallbacks(startNow)
        runCatching { recognizer.cancel() }
    }

    fun destroy() {
        handler.removeCallbacks(startNow)
        runCatching { recognizer.destroy() }
    }
}

private fun Bundle?.firstResult(): String? =
    this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

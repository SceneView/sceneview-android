package io.github.sceneview.demo.demos.internal.semantic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.StringRes
import io.github.sceneview.demo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import kotlin.coroutines.cancellation.CancellationException

/*
 * The Android side of scene recognition: the detector downloaded on first use, the replay's photos
 * as detector images, and the labels' names in the app's language. Everything else is plain Kotlin
 * the JVM tests run.
 */

private const val TAG = "SceneRecognition"

/** The side a keyframe is decoded down to before the detector letterboxes it into 416 pixels. */
const val SCENE_IMAGE_MAX_SIDE = 640

/** The detector's folder in app storage: kept across launches, never cleared as a cache is. */
private const val SCENE_MODEL_DIR = "scene-detector"

/**
 * The furniture detector, `null` when it cannot be had — no network for its first download
 * ([SceneModelState.Offline]), or a file that fails its check or that ONNX Runtime will not load
 * ([SceneModelState.Unusable]): the replay then runs without it. [onState] hears where it is, on
 * the caller's dispatcher: the progress of its download, then [SceneModelState.Ready] once its
 * session is built, or why there is none.
 *
 * The first call downloads the model ([SceneModelStore]) — or finds it in app storage —, maps it
 * and builds its session, all off the main thread; every later one, each replay opened again,
 * shares that session ([SharedDetector]). Closing what it returns lets go of the share, never the
 * shared session.
 */
suspend fun loadSceneDetector(context: Context, onState: (SceneModelState) -> Unit = {}): SceneDetector? =
    SharedDetector.share(context.applicationContext, onState)

/**
 * The detector, built once for the process. A model that failed its check, or that ONNX Runtime
 * would not load, is not tried again before the app restarts; a download that found no network is,
 * by the next replay, and resumes where it stopped. The session lives as long as the process,
 * whose end frees it; the shares handed out close nothing ([Share]), so a replay that closes its
 * detector cannot pull the session from under another one.
 */
private object SharedDetector {
    private val lock = Mutex()
    private var store: SceneModelStore? = null
    private var detector: SceneDetector? = null
    private var unusable = false

    suspend fun share(context: Context, onState: (SceneModelState) -> Unit): SceneDetector? = lock.withLock {
        detector?.let {
            onState(SceneModelState.Ready)
            return@withLock Share(it)
        }
        if (unusable) {
            onState(SceneModelState.Unusable)
            return@withLock null
        }
        val models = store ?: withContext(Dispatchers.IO) {
            SceneModelStore(File(context.filesDir, SCENE_MODEL_DIR)) { failure ->
                Log.w(TAG, "The scene detector could not be fetched: ${failure.message}")
            }
        }.also { store = it }
        var ended: SceneModelState? = null
        // Ready is said once the session is built, not when the file is merely on the device.
        val file = models.fetch { state ->
            ended = state
            if (state != SceneModelState.Ready) onState(state)
        }
        if (file == null) {
            unusable = ended == SceneModelState.Unusable
            Log.i(TAG, "No scene detector (${ended ?: "not fetched"}): the scan is recognised without it")
            return@withLock null
        }
        val built = open(file)
        if (built == null) {
            unusable = true
            onState(SceneModelState.Unusable)
            return@withLock null
        }
        detector = built
        onState(SceneModelState.Ready)
        Share(built)
    }

    /**
     * [file] mapped in place — ONNX Runtime reads a direct buffer without copying it onto the heap —
     * and its session built: the mapping on [Dispatchers.IO], the session on [Dispatchers.Default].
     */
    private suspend fun open(file: File): SceneDetector? = try {
        val mapped = withContext(Dispatchers.IO) {
            FileInputStream(file).use { it.channel.map(FileChannel.MapMode.READ_ONLY, 0, it.channel.size()) }
        }
        withContext(Dispatchers.Default) { OnnxSceneDetector(mapped, name = SceneModel.NAME) }
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
        Log.w(TAG, "The scene detector could not be loaded: ${failure.message}")
        null
    }
}

/** A share of the process's detector: it detects with it, and closing it leaves it open. */
private class Share(detector: SceneDetector) : SceneDetector by detector {
    override fun close() = Unit
}

/**
 * An encoded photo, decoded down by powers of two until its longer side is under twice
 * [maxSide] pixels — the detector letterboxes it smaller still — as a detector reads it.
 * [decode] decodes it with the options it is given (`null` when it cannot), as
 * `RerunReplayMedia.decode` does.
 */
fun sceneImageOf(maxSide: Int = SCENE_IMAGE_MAX_SIDE, decode: (BitmapFactory.Options) -> Bitmap?): SceneImage? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    decode(bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val bitmap = decode(BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    return try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        SceneImage(bitmap.width, bitmap.height, pixels)
    } finally {
        bitmap.recycle()
    }
}

/** The label's name as the replay writes it over a box. */
@StringRes
fun SceneLabel.nameRes(): Int = when (this) {
    SceneLabel.Bed -> R.string.demo_ar_rerun_recognized_bed
    SceneLabel.Sofa -> R.string.demo_ar_rerun_recognized_sofa
    SceneLabel.Chair -> R.string.demo_ar_rerun_recognized_chair
    SceneLabel.Table -> R.string.demo_ar_rerun_recognized_table
    SceneLabel.Bench -> R.string.demo_ar_rerun_recognized_bench
    SceneLabel.Toilet -> R.string.demo_ar_rerun_recognized_toilet
    SceneLabel.Sink -> R.string.demo_ar_rerun_recognized_sink
    SceneLabel.Refrigerator -> R.string.demo_ar_rerun_recognized_refrigerator
    SceneLabel.Oven -> R.string.demo_ar_rerun_recognized_oven
    SceneLabel.Microwave -> R.string.demo_ar_rerun_recognized_microwave
    SceneLabel.Television -> R.string.demo_ar_rerun_recognized_television
    SceneLabel.Plant -> R.string.demo_ar_rerun_recognized_plant
    SceneLabel.Door -> R.string.demo_ar_rerun_recognized_door
    SceneLabel.Window -> R.string.demo_ar_rerun_recognized_window
}

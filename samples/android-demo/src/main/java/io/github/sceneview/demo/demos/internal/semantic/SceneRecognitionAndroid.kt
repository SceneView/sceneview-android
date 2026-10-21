package io.github.sceneview.demo.demos.internal.semantic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.StringRes
import io.github.sceneview.demo.R
import java.io.FileNotFoundException
import java.nio.channels.FileChannel

/*
 * The Android side of scene recognition: the bundled model, the replay's photos as detector
 * images, and the labels' names in the app's language. Everything else is plain Kotlin the JVM
 * tests run.
 */

private const val TAG = "SceneRecognition"

/** The detector bundled with the demo: YOLOX-Tiny (Apache-2.0), fetched into the assets by the build. */
const val SCENE_MODEL_ASSET = "ml/yolox_tiny.onnx"

/** The side a keyframe is decoded down to before the detector letterboxes it into 416 pixels. */
const val SCENE_IMAGE_MAX_SIDE = 640

private const val SCENE_MODEL_NAME = "yolox_tiny"

/**
 * The bundled detector, `null` when the build carries no model (assets not fetched) or ONNX
 * Runtime will not load it. Off the main thread: the first call maps the 20 MB model and builds
 * its session; every later one, each replay opened again, shares that session ([BundledDetector]).
 * Closing what it returns lets go of the share, never the shared session.
 */
fun loadSceneDetector(context: Context): SceneDetector? = BundledDetector.share(context)

/**
 * The bundled detector, built once for the process: a model that is not there, or that ONNX
 * Runtime will not load, is not looked for again either. The session lives as long as the
 * process, whose end frees it; the shares handed out close nothing ([Share]), so a replay that
 * closes its detector cannot pull the session from under another one.
 */
private object BundledDetector {
    private var loaded = false
    private var detector: SceneDetector? = null

    @Synchronized
    fun share(context: Context): SceneDetector? {
        if (!loaded) {
            detector = load(context)
            loaded = true
        }
        return detector?.let(::Share)
    }

    private fun load(context: Context): SceneDetector? = try {
        open(context)
    } catch (missing: FileNotFoundException) {
        Log.i(TAG, "No $SCENE_MODEL_ASSET in this build: the scan is not recognised (${missing.message})")
        null
    } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
        Log.w(TAG, "The scene detector could not be loaded: ${failure.message}")
        null
    }

    /**
     * The model mapped in place from the APK, where `noCompress 'onnx'` keeps it stored; read
     * whole only from a build that compressed it, which cannot hand out its file descriptor.
     */
    private fun open(context: Context): SceneDetector {
        val stored = try {
            context.assets.openFd(SCENE_MODEL_ASSET)
        } catch (@Suppress("SwallowedException") compressed: FileNotFoundException) {
            null
        }
        if (stored == null) {
            val bytes = context.assets.open(SCENE_MODEL_ASSET).use { it.readBytes() }
            return OnnxSceneDetector(bytes, name = SCENE_MODEL_NAME)
        }
        val mapped = stored.use { fd ->
            fd.createInputStream().use { it.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength) }
        }
        return OnnxSceneDetector(mapped, name = SCENE_MODEL_NAME)
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

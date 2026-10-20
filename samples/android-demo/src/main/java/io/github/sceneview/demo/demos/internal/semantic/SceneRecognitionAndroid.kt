package io.github.sceneview.demo.demos.internal.semantic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.StringRes
import io.github.sceneview.demo.R
import java.io.FileNotFoundException

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

/**
 * The bundled detector, `null` when the build carries no model (assets not fetched) or ONNX
 * Runtime will not load it. Off the main thread: it reads 20 MB and builds a session.
 */
fun loadSceneDetector(context: Context): SceneDetector? = try {
    val bytes = context.assets.open(SCENE_MODEL_ASSET).use { it.readBytes() }
    OnnxSceneDetector(bytes, name = "yolox_tiny")
} catch (missing: FileNotFoundException) {
    Log.i(TAG, "No $SCENE_MODEL_ASSET in this build: the scan is not recognised (${missing.message})")
    null
} catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
    Log.w(TAG, "The scene detector could not be loaded: ${failure.message}")
    null
}

/**
 * An encoded photo, decoded at most [maxSide] pixels on its longer side, as a detector reads it.
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

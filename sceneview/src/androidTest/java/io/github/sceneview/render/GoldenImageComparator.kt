package io.github.sceneview.render

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Compares rendered bitmaps against golden reference images with configurable tolerance.
 *
 * Follows the same approach as Filament's `diffimg` tool:
 * - Per-channel absolute difference threshold ([maxChannelDiff])
 * - Percentage of pixels allowed to differ ([maxDiffPixelsPercent])
 * - Generates a diff image for debugging when comparison fails
 *
 * ## Usage
 * ```kotlin
 * val comparator = GoldenImageComparator(
 *     maxChannelDiff = 5,           // max 5/255 difference per R/G/B channel
 *     maxDiffPixelsPercent = 1.0f   // up to 1% of pixels may differ
 * )
 * val result = comparator.compare(renderedBitmap, goldenBitmap)
 * assertTrue(result.message, result.passed)
 * ```
 *
 * ## Golden image management
 * Golden images are stored in `androidTest/assets/goldens/` and loaded via
 * [loadGolden]. When a golden doesn't exist yet, [saveGolden] writes the
 * rendered image as the new reference (useful for first-time setup).
 *
 * @param maxChannelDiff    Maximum absolute difference per colour channel (0–255).
 * @param maxDiffPixelsPercent Maximum percentage of pixels that may exceed [maxChannelDiff].
 */
class GoldenImageComparator(
    val maxChannelDiff: Int = 5,
    val maxDiffPixelsPercent: Float = 1.0f
) {
    /**
     * Result of a golden image comparison.
     */
    data class ComparisonResult(
        /** Whether the comparison passed within tolerance. */
        val passed: Boolean,
        /** Human-readable summary (OK or failure details). */
        val message: String,
        /** Number of pixels that exceeded [maxChannelDiff]. */
        val failingPixelCount: Int,
        /** Maximum per-channel difference found across all pixels. */
        val maxDiffFound: Int,
        /** Diff image highlighting failing pixels in red (null if passed). */
        val diffBitmap: Bitmap?
    )

    /**
     * Compares [rendered] against [golden] with the configured tolerance.
     *
     * @return a [ComparisonResult] indicating pass/fail and diagnostics.
     */
    fun compare(rendered: Bitmap, golden: Bitmap): ComparisonResult {
        require(rendered.width == golden.width && rendered.height == golden.height) {
            "Size mismatch: rendered=${rendered.width}x${rendered.height}, " +
                    "golden=${golden.width}x${golden.height}"
        }

        val w = rendered.width
        val h = rendered.height
        val totalPixels = w * h
        val diffBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        var failingPixels = 0
        var maxDiff = 0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val rPixel = rendered.getPixel(x, y)
                val gPixel = golden.getPixel(x, y)

                val dr = abs(Color.red(rPixel) - Color.red(gPixel))
                val dg = abs(Color.green(rPixel) - Color.green(gPixel))
                val db = abs(Color.blue(rPixel) - Color.blue(gPixel))
                val channelMax = maxOf(dr, dg, db)

                if (channelMax > maxChannelDiff) {
                    failingPixels++
                    // Highlight failing pixels in red, with brightness proportional to diff
                    val intensity = channelMax.coerceIn(50, 255)
                    diffBitmap.setPixel(x, y, Color.argb(255, intensity, 0, 0))
                } else {
                    // Passing pixel — show dim green
                    diffBitmap.setPixel(x, y, Color.argb(255, 0, 30, 0))
                }

                maxDiff = maxOf(maxDiff, channelMax)
            }
        }

        val failingPercent = failingPixels * 100f / totalPixels
        val passed = failingPercent <= maxDiffPixelsPercent

        val message = if (passed) {
            "OK — $failingPixels failing pixels (${String.format("%.2f", failingPercent)}%), " +
                    "max diff=$maxDiff"
        } else {
            "FAIL — $failingPixels / $totalPixels pixels differ " +
                    "(${String.format("%.2f", failingPercent)}% > ${maxDiffPixelsPercent}%), " +
                    "max diff=$maxDiff (threshold=$maxChannelDiff)"
        }

        return ComparisonResult(
            passed = passed,
            message = message,
            failingPixelCount = failingPixels,
            maxDiffFound = maxDiff,
            diffBitmap = if (passed) null else diffBitmap
        )
    }

    /**
     * Saves a bitmap to the device's external test output directory for later review.
     *
     * @param bitmap the image to save.
     * @param name   filename (without extension).
     * @return the saved [File].
     */
    fun saveToDisk(bitmap: Bitmap, name: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = context.getExternalFilesDir("render-test-output")
            ?: File(context.filesDir, "render-test-output")
        dir.mkdirs()
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}

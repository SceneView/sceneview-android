package io.github.sceneview.demo.demos

import android.os.Debug
import android.util.Log
import io.github.sceneview.demo.demos.internal.ArDebugGeometry
import io.github.sceneview.demo.demos.internal.ArDebugTrace
import io.github.sceneview.demo.demos.internal.DenseFusion
import io.github.sceneview.demo.demos.internal.DepthBackProjection
import io.github.sceneview.demo.demos.internal.ScanWalkthrough
import io.github.sceneview.demo.demos.internal.Vec3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * QA only (#4629, `--es qa_state record-long`): the capture screen's dense map fed a whole flat
 * ([ScanWalkthrough]) at the phone's depth pace, since the emulator has no depth camera. It runs
 * the real [ScanDenseMap] on the real budget, so the HUD's count, the coaching line and the live
 * 3D card show what a long scan does, and `adb logcat -s QaLongScan` gives its figures.
 *
 * `record-long-fixed` runs the map as it was before #4629 — one fixed 500 k cap, no coarsening —
 * for a before/after on the same walk.
 */
internal class QaLongScan(fixed: Boolean, origin: Vec3) {
    val dense = if (fixed) {
        ScanDenseMap(maxPoints = DenseFusion.MAX_POINTS, maxLevel = 0)
    } else {
        ScanDenseMap()
    }
    private val walk = ScanWalkthrough(origin = origin)

    /** Frames of the walk merged so far. */
    var played = 0
        private set

    /**
     * Plays the walk, one depth frame every [ScanCapture.DEPTH_INTERVAL_NS] while [integrates]
     * says the capture scans, holding on its end. Logs the map's figures every [LOG_EVERY] frames.
     */
    suspend fun run(integrates: () -> Boolean) = withContext(Dispatchers.Default) {
        Log.i(LOG_TAG, "start: budget ${dense.fusion.maxPoints} points, ${walk.frameCount} frames")
        while (played < walk.frameCount) {
            if (!integrates()) {
                delay(PAUSED_POLL_MS)
                continue
            }
            val started = System.nanoTime()
            dense.add(DepthBackProjection.project(walk.frame(played)))
            played++
            if (played % LOG_EVERY == 0 || played == walk.frameCount) log()
            val spent = (System.nanoTime() - started) / NANOS_PER_MS
            delay((ScanCapture.DEPTH_INTERVAL_NS / NANOS_PER_MS - spent).coerceAtLeast(0L))
        }
        val rooms = walk.pointsPerRoom(dense.savedCloud()).joinToString("/")
        Log.i(LOG_TAG, "done: saved ${dense.count} points, per room $rooms")
    }

    private fun log() {
        val runtime = Runtime.getRuntime()
        val javaMb = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB
        val nativeMb = Debug.getNativeHeapAllocatedSize() / BYTES_PER_MB
        Log.i(
            LOG_TAG,
            "frame $played room ${walk.roomAt(played - 1)}: ${dense.logLine()} " +
                "detail ${dense.detail()} java $javaMb MB native $nativeMb MB",
        )
    }

    companion object {
        /** The walk laid out from the middle of [sample]'s room, on its floor, so the 3D card frames it. */
        fun over(sample: ArDebugTrace, fixed: Boolean): QaLongScan {
            val b = ArDebugGeometry.contentBounds(sample.frameAt(sample.duration))
            val origin = if (b == null) Vec3(0f, 0f, 0f) else Vec3((b[0] + b[3]) / 2f, b[1], (b[2] + b[5]) / 2f)
            return QaLongScan(fixed, origin)
        }

        private const val LOG_TAG = "QaLongScan"
        private const val LOG_EVERY = 30
        private const val PAUSED_POLL_MS = 100L
        private const val NANOS_PER_MS = 1_000_000L
        private const val BYTES_PER_MB = 1_048_576L
    }
}

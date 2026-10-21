package io.github.sceneview.demo.demos

import io.github.sceneview.demo.demos.internal.ArDebugEvent
import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.DenseFuseStats
import io.github.sceneview.demo.demos.internal.DenseFusion
import io.github.sceneview.demo.demos.internal.AdaptiveConfidence
import io.github.sceneview.demo.demos.internal.DenseSamples
import io.github.sceneview.demo.demos.internal.DepthBackProjection
import io.github.sceneview.demo.demos.internal.DepthFrame
import io.github.sceneview.demo.demos.internal.MonotonicCount
import io.github.sceneview.demo.demos.internal.ScanDetail
import java.util.concurrent.atomic.AtomicInteger

/**
 * A depth scan's dense map as the capture screen sees it (#4629): [DenseFusion] on this phone's
 * budget ([DenseFusion.budgetFor] its heap), the one count the HUD shows — the points the saved
 * scan keeps, so the live figure and the saved one agree — the live 3D card's snapshot, and
 * whether the map has just coarsened part of itself to make room ([detail]).
 *
 * [add] runs on one worker thread at a time; the rest is read from the main thread.
 */
internal class ScanDenseMap(
    maxPoints: Int = DenseFusion.budgetFor(Runtime.getRuntime().maxMemory()),
    maxLevel: Int = DenseFusion.MAX_LEVEL,
    private val clock: () -> Long = System::nanoTime,
) {
    val fusion = DenseFusion(maxPoints = maxPoints, maxLevel = maxLevel)

    private val points = AtomicInteger(0)
    private val frame = AtomicInteger(0)

    @Volatile
    private var adjustedAtNanos = Long.MIN_VALUE

    @Volatile
    private var full = false

    /**
     * The dense map as the live 3D card draws it: a snapshot taken after a fusion at most every
     * [LIVE_INTERVAL_NS], [LIVE_MAX_SURFELS] at most of the surfels at least two depth frames
     * saw, meshed and coloured off the main thread, with the floor cells it covers. `null` until the first one.
     */
    @Volatile
    var live: ReplayDenseLayer? = null
        private set
    private var liveAtNanos = Long.MIN_VALUE

    /** How long the last [add] took, and the longest so far, in milliseconds: a device log's figures. */
    @Volatile
    var lastAddMs = 0f
        private set

    @Volatile
    var maxAddMs = 0f
        private set

    /** The points the saved scan keeps so far: what the review and the saved scan say. */
    val count: Int get() = points.get()

    /**
     * The HUD's one count (#4717): every point the capture confirmed, which only grows — a
     * compaction merges points the map held, and the figure on screen does not drop for it.
     */
    val captured: Int get() = shown.get()
    private val rising = MonotonicCount()
    private val shown = AtomicInteger(0)

    /** The depth confidence a pixel needs, relaxed on a plain surface (#4717). */
    val confidence = AdaptiveConfidence.dense()

    /**
     * [frame]'s surfels at the [confidence] threshold now, which the frame then feeds back. A
     * relaxed frame keeps its depth to [AdaptiveConfidence.RELAXED_FAR_M] at most, where
     * [farM] is the scan's own reach. On the worker, before [add].
     */
    fun project(frame: DepthFrame, nanos: Long, farM: Float = DepthBackProjection.FAR_M): DenseSamples {
        val relaxed = confidence.relaxed
        val threshold = confidence.threshold.toInt()
        val reach = if (relaxed) minOf(farM, AdaptiveConfidence.RELAXED_FAR_M) else farM
        val samples = DepthBackProjection.project(frame, minConfidence = threshold, farM = reach)
        var atBase = samples.count
        if (relaxed) {
            atBase = 0
            for (i in 0 until samples.count) {
                if ((samples.confidences[i].toInt() and 0xFF) >= confidence.base) atBase++
            }
        }
        confidence.record(nanos, samples.count, atBase)
        return samples
    }

    /** The depth frames merged so far ([DenseFusion.frames]). */
    val frames: Int get() = frame.get()

    /** Merges one depth frame's [samples], then takes [live] when it is due. On the worker. */
    fun add(samples: DenseSamples): DenseFuseStats {
        val started = clock()
        val compactions = fusion.compactions
        val stats = fusion.add(samples)
        if (fusion.compactions != compactions) adjustedAtNanos = clock()
        full = fusion.saturated
        points.set(stats.confirmed)
        shown.set(rising.update(stats.confirmed))
        frame.set(fusion.frames)
        snapshotLive()
        val ms = (clock() - started) / NANOS_PER_MS
        lastAddMs = ms
        if (ms > maxAddMs) maxAddMs = ms
        return stats
    }

    /** What the coaching line says of the map at [nowNanos] ([clock]'s time). */
    fun detail(nowNanos: Long = clock()): ScanDetail = when {
        full -> ScanDetail.Full
        adjustedAtNanos != Long.MIN_VALUE && nowNanos - adjustedAtNanos < ADJUSTING_SHOWN_NS -> ScanDetail.Adjusting
        else -> ScanDetail.Fine
    }

    /** The cloud the scan saves: every voxel at least [DenseFusion.MIN_VIEWS] depth frames saw. */
    fun savedCloud(): DenseCloud = fusion.cloud(minViews = DenseFusion.MIN_VIEWS)

    /**
     * [events] with each [ArDebugEvent.DepthStats] total turned into the prefix of [savedCloud]
     * found by then, [atFrames] holding the [frames] count each was recorded at, in order: what
     * a replay reveals, exact through a compaction, and its last value the saved count.
     */
    fun revealed(events: List<ArDebugEvent>, atFrames: IntArray): List<ArDebugEvent> {
        val totals = fusion.revealCounts(atFrames)
        var k = 0
        return events.map { e ->
            if (e !is ArDebugEvent.DepthStats || k >= totals.size) e else {
                ArDebugEvent.DepthStats(e.nanos, e.added, e.kept, totals[k++])
            }
        }
    }

    /** One line for a device log: the map's figures and what it costs. */
    fun logLine(): String {
        val levels = fusion.levelCounts().joinToString("/")
        return "dense kept ${fusion.count} saved $count drawn ${live?.cloud?.count ?: 0} " +
            "levels $levels compactions ${fusion.compactions} full $full " +
            "add ${"%.1f".format(lastAddMs)} ms (max ${"%.1f".format(maxAddMs)})"
    }

    private fun snapshotLive() {
        val now = clock()
        if (liveAtNanos != Long.MIN_VALUE && now - liveAtNanos < LIVE_INTERVAL_NS) return
        liveAtNanos = now
        val cloud = fusion.cloud(minViews = DenseFusion.MIN_VIEWS, maxPoints = LIVE_MAX_SURFELS)
        if (cloud.count == 0) return
        // The floor cells it covers are found here too, on the worker: the live map reads them.
        live = ReplayDenseLayer.of(cloud, fusion.voxelM, withCells = true)
    }

    companion object {
        /** A second between two snapshots of the dense map for the live 3D card. */
        const val LIVE_INTERVAL_NS = 1_000_000_000L

        /**
         * Surfels the live card draws at most: 600 k vertices, a seventh of what the replay may
         * draw, so the card keeps its frame rate while the camera and the fusion run.
         */
        const val LIVE_MAX_SURFELS = 150_000

        /** How long "Adjusting detail" stays up after the map coarsened: long enough to read. */
        const val ADJUSTING_SHOWN_NS = 4_000_000_000L

        private const val NANOS_PER_MS = 1e6f
    }
}
